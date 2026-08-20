package com.enterprise.saga.service;

import com.enterprise.events.*;
import com.enterprise.events.config.PulsarTopics;
import com.enterprise.events.producer.EventProducer;
import com.enterprise.saga.domain.*;
import com.enterprise.saga.integration.ServiceClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Core Saga Orchestrator service.
 *
 * Responsibilities:
 * 1. Create saga instances for order processing
 * 2. Execute steps sequentially
 * 3. Handle failures with compensating transactions
 * 4. Track saga state and progress
 * 5. Handle timeouts
 *
 * Flow:
 * OrderCreated â†’ Authorize Payment â†’ Reserve Inventory â†’ Confirm Order
 *     â†“ (if any step fails)
 * Refund Payment â† Release Inventory â† Cancel Order
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SagaOrchestrator {

    private final SagaInstanceRepository sagaInstanceRepository;
    private final SagaStepRepository sagaStepRepository;
    private final EventProducer eventProducer;
    private final ServiceClient serviceClient;
    private final ObjectMapper objectMapper;

    // Service URLs
    private final Map<String, String> serviceUrls = Map.of(
            "order-service", "http://localhost:8081",
            "payment-service", "http://localhost:8082",
            "inventory-service", "http://localhost:8083"
    );

    /**
     * Create a new saga for order processing
     */
    @Transactional
    public SagaInstance createOrderSaga(OrderCreatedEvent event) {
        log.info("Creating saga for order: {} | OrderId: {} | items={} | total={}",
                event.getOrderNumber(), event.getOrderId(),
                event.getItems() != null ? event.getItems().size() : "null",
                event.getTotalAmount());

        // Check if saga already exists for this order
        if (sagaInstanceRepository.findByOrderId(event.getOrderId()).isPresent()) {
            log.warn("Saga already exists for order: {}", event.getOrderId());
            return sagaInstanceRepository.findByOrderId(event.getOrderId()).get();
        }

        // Create saga instance
        SagaInstance saga = SagaInstance.builder()
                .sagaType("ORDER_PROCESSING")
                .orderId(event.getOrderId())
                .customerId(event.getCustomerId())
                .orderNumber(event.getOrderNumber())
                .timeoutSeconds(300)
                .orderPayload(toOrderPayloadJson(event))
                .build();

        // Define steps
        SagaStep authorizePayment = createStep(
                saga,
                "authorize-payment",
                1,
                "Authorize Payment",
                "payment-service",
                StepType.FORWARD
        );

        SagaStep reserveInventory = createStep(
                saga,
                "reserve-inventory",
                2,
                "Reserve Inventory",
                "inventory-service",
                StepType.FORWARD
        );

        SagaStep confirmOrder = createStep(
                saga,
                "confirm-order",
                3,
                "Confirm Order",
                "order-service",
                StepType.FORWARD
        );

        // Compensating steps
        SagaStep refundPayment = createStep(
                saga,
                "refund-payment",
                1,
                "Refund Payment",
                "payment-service",
                StepType.COMPENSATE
        );

        SagaStep releaseInventory = createStep(
                saga,
                "release-inventory",
                2,
                "Release Inventory",
                "inventory-service",
                StepType.COMPENSATE
        );

        SagaStep cancelOrder = createStep(
                saga,
                "cancel-order",
                3,
                "Cancel Order",
                "order-service",
                StepType.COMPENSATE
        );

        saga.setTotalSteps(3); // Only forward steps count
        saga = sagaInstanceRepository.save(saga);

        log.info("Saga created: {} with {} steps", saga.getSagaId(), saga.getSteps().size());

        // Start executing
        startSaga(saga);

        return saga;
    }

    /**
     * Create a saga step
     */
    private SagaStep createStep(SagaInstance saga, String stepId, int order,
                                String name, String targetService, StepType type) {
        SagaStep step = SagaStep.builder()
                .stepId(stepId)
                .stepOrder(order)
                .name(name)
                .targetService(targetService)
                .type(type)
                .status(StepStatus.PENDING)
                .retryCount(0)
                .maxRetries(3)
                .sagaInstance(saga)
                .build();

        saga.getSteps().add(step);
        return step;
    }

    /**
     * Start saga execution
     */
    @Transactional
    public void startSaga(SagaInstance saga) {
        log.info("Starting saga: {}", saga.getSagaId());
        saga.start();
        sagaInstanceRepository.save(saga);

        // Execute first step
        executeNextStep(saga);
    }

    /**
     * Execute the next forward step
     */
    @Transactional
    public void executeNextStep(SagaInstance saga) {
        log.info("Executing next step for saga: {} | Current: {}", saga.getSagaId(), saga.getCurrentStepId());

        // Find next forward step to execute
        List<SagaStep> forwardSteps = saga.getSteps().stream()
                .filter(step -> step.getType() == StepType.FORWARD)
                .filter(step -> step.getStatus() == StepStatus.PENDING)
                .sorted((a, b) -> a.getStepOrder().compareTo(b.getStepOrder()))
                .toList();

        if (forwardSteps.isEmpty()) {
            log.info("All forward steps completed for saga: {}", saga.getSagaId());
            saga.complete();
            sagaInstanceRepository.save(saga);
            return;
        }

        SagaStep nextStep = forwardSteps.get(0);
        saga.setCurrentStepId(nextStep.getStepId());
        sagaInstanceRepository.save(saga);

        // Execute the step
        executeStep(saga, nextStep);
    }

    /**
     * Execute a saga step
     */
    @Transactional
    public void executeStep(SagaInstance saga, SagaStep step) {
        log.info("Executing step: {} | Service: {} | Type: {}",
                step.getStepId(), step.getTargetService(), step.getType());

        step.start();
        sagaStepRepository.save(step);

        // Build request payload based on step
        String payload = buildStepPayload(saga, step);
        step.setRequestPayload(payload);

        try {
            performStepCall(saga, step);
        } catch (Exception e) {
            log.error("Failed to execute step: {} | Error: {}", step.getStepId(), e.getMessage());
            handleStepFailed(saga.getSagaId(), step.getStepId(), e.getMessage());
        }
    }

    /**
     * Perform the actual HTTP call for a step (used for the first attempt and
     * for retries). Does not reset retry state.
     */
    @Transactional
    public void performStepCall(SagaInstance saga, SagaStep step) {
        if (step.getType() == StepType.FORWARD) {
            executeForwardStep(saga, step);
        } else {
            executeCompensateStep(saga, step);
        }
    }

    /**
     * Execute a forward step by calling the real microservice over HTTP.
     * Each step also advances the order through its real state machine so the
     * Saga's progress is visible on the order itself.
     */
    private void executeForwardStep(SagaInstance saga, SagaStep step) {
        ServiceClient.CallResult result;
        String stepId = step.getStepId();

        switch (stepId) {
            case "authorize-payment" -> {
                // PENDING -> PAYMENT_PROCESSING -> (authorize) -> PAYMENT_AUTHORIZED
                serviceClient.updateOrderStatus(saga.getOrderId(), "PAYMENT_PROCESSING", "Saga started", saga.getSagaId());
                result = serviceClient.authorizePayment(
                        UUID.fromString(saga.getOrderId()),
                        UUID.fromString(saga.getCustomerId()),
                        totalAmount(saga),
                        "BRL",
                        "saga-" + saga.getSagaId(),
                        saga.getOrderId()
                );
                if (result.success()) {
                    serviceClient.updateOrderStatus(saga.getOrderId(), "PAYMENT_AUTHORIZED", "Payment authorized", saga.getSagaId());
                }
                handleForwardResult(saga, step, result, "id");
            }
            case "reserve-inventory" -> {
                // PAYMENT_AUTHORIZED -> INVENTORY_PROCESSING -> (reserve) -> CONFIRMED
                serviceClient.updateOrderStatus(saga.getOrderId(), "INVENTORY_PROCESSING", "Reserving stock", saga.getSagaId());
                OrderCreatedEvent.OrderItemData item = firstItem(saga);
                result = serviceClient.reserveInventory(
                        saga.getOrderId(),
                        item != null ? item.getProductId() : "PROD-001",
                        item != null ? item.getQuantity() : 1,
                        saga.getOrderId()
                );
                if (result.success()) {
                    serviceClient.updateOrderStatus(saga.getOrderId(), "CONFIRMED", "Saga completed successfully", saga.getSagaId());
                }
                handleForwardResult(saga, step, result, "reservationReference");
            }
            case "confirm-order" -> {
                // Order is already CONFIRMED by the reserve-inventory step; this
                // step finalizes the saga and publishes the confirmation event.
                result = new ServiceClient.CallResult(true, null, null);
                handleForwardResult(saga, step, result, "orderId");
            }
            default -> throw new IllegalArgumentException("Unknown forward step: " + stepId);
        }
    }

    /**
     * Execute a compensating step by calling the real microservice over HTTP.
     */
    private void executeCompensateStep(SagaInstance saga, SagaStep step) {
        String stepId = step.getStepId();
        ServiceClient.CallResult result;

        switch (stepId) {
            case "refund-payment" -> {
                String paymentId = extractReference(saga, "authorize-payment", "paymentId");
                result = serviceClient.refundPayment(paymentId != null ? paymentId : "", "Saga compensation");
            }
            case "release-inventory" -> {
                OrderCreatedEvent.OrderItemData item = firstItem(saga);
                result = serviceClient.releaseInventory(
                        saga.getOrderId(),
                        item != null ? item.getProductId() : "PROD-001"
                );
            }
            case "cancel-order" -> {
                result = serviceClient.cancelOrder(saga.getOrderId(), saga.getSagaId());
            }
            default -> throw new IllegalArgumentException("Unknown compensate step: " + stepId);
        }

        if (result.success()) {
            step.setResponsePayload(result.body() != null ? result.body().toString() : "{}");
            step.compensate();
            sagaStepRepository.save(step);
            publishStepResultEvent(saga, step, true, null);
            log.info("Compensating step completed: {} | saga: {}", stepId, saga.getSagaId());
            handleCompensationProgress(saga);
        } else {
            log.error("Compensating step failed: {} | saga: {} | error: {}",
                    stepId, saga.getSagaId(), result.error());
            step.fail(result.error() != null ? result.error() : "Compensation failed");
            sagaStepRepository.save(step);
            // Mark saga as failed if compensation cannot complete.
            saga.fail("Compensation failed at step " + stepId);
            sagaInstanceRepository.save(saga);
        }
    }

    /**
     * Track compensation progress and finalize when all compensate steps are done.
     */
    private void handleCompensationProgress(SagaInstance saga) {
        boolean allCompensated = saga.getSteps().stream()
                .filter(step -> step.getType() == StepType.COMPENSATE)
                .allMatch(step -> step.getStatus() == StepStatus.COMPENSATED);

        if (allCompensated) {
            log.info("All compensating steps completed for saga: {}", saga.getSagaId());
            saga.compensate();
            sagaInstanceRepository.save(saga);
        }
    }

    /**
     * Handle the result of a forward/compensate HTTP call.
     */
    private void handleForwardResult(SagaInstance saga, SagaStep step, ServiceClient.CallResult result, String refField) {
        if (result.success()) {
            String reference = result.body() != null && result.body().has(refField)
                    ? result.body().get(refField).asText()
                    : null;
            String responsePayload = result.body() != null ? result.body().toString() : "{}";
            step.setResponsePayload(responsePayload);
            // Persist the extracted reference so compensation can find it later.
            if (reference != null) {
                step.setErrorMessage(null);
                sagaStepRepository.save(step);
                storeReference(saga, step, refField, reference);
            }
            publishStepResultEvent(saga, step, true, reference);
            handleStepCompleted(saga.getSagaId(), step.getStepId(), responsePayload);
        } else {
            publishStepResultEvent(saga, step, false, null);
            handleStepFailed(saga.getSagaId(), step.getStepId(),
                    result.error() != null ? result.error() : "Service call failed");
        }
    }

    /**
     * Publish the real result of a step for observability / notifications.
     */
    private void publishStepResultEvent(SagaInstance saga, SagaStep step, boolean success, String reference) {
        try {
            switch (step.getStepId()) {
                case "authorize-payment" -> {
                    if (success) {
                        publishEvent(saga, PaymentAuthorizedEvent.builder()
                                .orderId(saga.getOrderId())
                                .customerId(saga.getCustomerId())
                                .amount(BigDecimal.ZERO)
                                .build(), PulsarTopics.PAYMENT_AUTHORIZED);
                    } else {
                        publishEvent(saga, PaymentFailedEvent.builder()
                                .orderId(saga.getOrderId())
                                .customerId(saga.getCustomerId())
                                .failureReason("Payment authorization rejected")
                                .build(), PulsarTopics.PAYMENT_FAILED);
                    }
                }
                case "reserve-inventory" -> {
                    if (success) {
                        publishEvent(saga, InventoryReservedEvent.builder()
                                .orderId(saga.getOrderId())
                                .reservationId(reference != null ? reference : "RES-" + saga.getSagaId())
                                .build(), PulsarTopics.INVENTORY_RESERVED);
                    } else {
                        publishEvent(saga, InventoryReservationFailedEvent.builder()
                                .orderId(saga.getOrderId())
                                .failureReason("Insufficient stock")
                                .build(), PulsarTopics.INVENTORY_RESERVATION_FAILED);
                    }
                }
                case "confirm-order" -> {
                    if (success) {
                        publishEvent(saga, OrderConfirmedEvent.builder()
                                .orderId(saga.getOrderId())
                                .orderNumber(saga.getOrderNumber())
                                .customerId(saga.getCustomerId())
                                .build(), PulsarTopics.ORDER_CONFIRMED);
                    }
                }
                case "refund-payment" -> publishEvent(saga, PaymentRefundedEvent.builder()
                        .orderId(saga.getOrderId())
                        .customerId(saga.getCustomerId())
                        .reason("Saga compensation")
                        .build(), PulsarTopics.PAYMENT_REFUNDED);
                case "release-inventory" -> publishEvent(saga, InventoryReleasedEvent.builder()
                        .orderId(saga.getOrderId())
                        .build(), PulsarTopics.INVENTORY_RELEASED);
                case "cancel-order" -> publishEvent(saga, OrderCancelledEvent.builder()
                        .orderId(saga.getOrderId())
                        .orderNumber(saga.getOrderNumber())
                        .customerId(saga.getCustomerId())
                        .reason("Saga compensation")
                        .build(), PulsarTopics.ORDER_CANCELLED);
                default -> { /* no notification for unknown steps */ }
            }
        } catch (Exception e) {
            log.warn("Failed to publish result event for step {} | error={}", step.getStepId(), e.getMessage());
        }
    }

    /**
     * Build the payload for a step
     */
    private String buildStepPayload(SagaInstance saga, SagaStep step) {
        return String.format("""
                {
                    "sagaId": "%s",
                    "orderId": "%s",
                    "orderNumber": "%s",
                    "customerId": "%s",
                    "stepId": "%s"
                }""",
                saga.getSagaId(),
                saga.getOrderId(),
                saga.getOrderNumber(),
                saga.getCustomerId(),
                step.getStepId()
        );
    }

    /**
     * Persist an extracted reference (e.g. paymentId, reservationId) on the
     * step so a later compensating step can locate the resource to undo.
     */
    private void storeReference(SagaInstance saga, SagaStep step, String field, String value) {
        String refs = saga.getOrderPayload();
        try {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(refs == null ? "{}" : refs);
            ((com.fasterxml.jackson.databind.node.ObjectNode) node).put("_ref_" + step.getStepId(), value);
            saga.setOrderPayload(node.toString());
            sagaInstanceRepository.save(saga);
        } catch (Exception e) {
            log.warn("Could not store reference {} on saga {}", field, saga.getSagaId());
        }
    }

    /**
     * Extract a previously stored reference from a forward step's response.
     */
    private String extractReference(SagaInstance saga, String forwardStepId, String field) {
        SagaStep step = saga.getStepById(forwardStepId);
        if (step == null || step.getResponsePayload() == null) {
            return null;
        }
        try {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(step.getResponsePayload());
            // The payment service returns the id as "id" (not "paymentId").
            if ("paymentId".equals(field) && node.has("id")) {
                return node.get("id").asText();
            }
            return node.has(field) ? node.get(field).asText() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * First line item from the original order (used for inventory reservation).
     */
    private OrderCreatedEvent.OrderItemData firstItem(SagaInstance saga) {
        try {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(saga.getOrderPayload());
            com.fasterxml.jackson.databind.JsonNode items = node.get("items");
            if (items != null && items.isArray() && items.size() > 0) {
                com.fasterxml.jackson.databind.JsonNode first = items.get(0);
                return OrderCreatedEvent.OrderItemData.builder()
                        .productId(first.path("productId").asText())
                        .productName(first.path("productName").asText(null))
                        .quantity(first.path("quantity").asInt(1))
                        .unitPrice(new java.math.BigDecimal(first.path("unitPrice").asText("0")))
                        .build();
            }
        } catch (Exception e) {
            log.warn("Could not parse order payload for saga {}", saga.getSagaId());
        }
        return null;
    }

    /**
     * Serialize the original order event for later reference.
     */
    private String toOrderPayloadJson(OrderCreatedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * Total order amount from the original order payload.
     * Falls back to summing the line items (quantity * unitPrice).
     */
    private BigDecimal totalAmount(SagaInstance saga) {
        try {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(saga.getOrderPayload());
            if (node.has("totalAmount") && !node.get("totalAmount").asText().equals("0")) {
                return new BigDecimal(node.get("totalAmount").asText("0"));
            }
            BigDecimal sum = BigDecimal.ZERO;
            com.fasterxml.jackson.databind.JsonNode items = node.get("items");
            if (items != null && items.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode item : items) {
                    BigDecimal price = new BigDecimal(item.path("unitPrice").asText("0"));
                    int qty = item.path("quantity").asInt(0);
                    sum = sum.add(price.multiply(BigDecimal.valueOf(qty)));
                }
            }
            return sum;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Publish a domain event carrying the saga correlation metadata.
     */
    private void publishEvent(SagaInstance saga, DomainEvent event, String topic) {
        event.setSagaInstanceId(saga.getSagaId());
        event.setCorrelationId(saga.getOrderId());
        eventProducer.publish(topic, event);
    }

    /**
     * Handle step completion
     */
    @Transactional
    public void handleStepCompleted(String sagaId, String stepId, String responsePayload) {
        log.info("Handling step completion | Saga: {} | Step: {}", sagaId, stepId);

        SagaInstance saga = sagaInstanceRepository.findBySagaId(sagaId)
                .orElseThrow(() -> new RuntimeException("Saga not found: " + sagaId));

        SagaStep step = saga.getStepById(stepId);
        if (step == null) {
            log.error("Step not found: {} in saga: {}", stepId, sagaId);
            return;
        }

        step.complete();
        step.setResponsePayload(responsePayload);
        sagaStepRepository.save(step);

        saga.advanceToNextStep();

        if (saga.allStepsCompleted()) {
            log.info("All steps completed for saga: {}", sagaId);
            saga.complete();
            sagaInstanceRepository.save(saga);
        } else {
            sagaInstanceRepository.save(saga);
            executeNextStep(saga);
        }
    }

    /**
     * Handle step failure
     */
    @Transactional
    public void handleStepFailed(String sagaId, String stepId, String errorMessage) {
        log.error("Handling step failure | Saga: {} | Step: {} | Error: {}",
                sagaId, stepId, errorMessage);

        SagaInstance saga = sagaInstanceRepository.findBySagaId(sagaId)
                .orElseThrow(() -> new RuntimeException("Saga not found: " + sagaId));

        SagaStep step = saga.getStepById(stepId);
        if (step == null) {
            log.error("Step not found: {} in saga: {}", stepId, sagaId);
            return;
        }

        // Check if we can retry
        if (step.canRetry()) {
            step.incrementRetry();
            step.setStatus(StepStatus.EXECUTING);
            step.setErrorMessage(null);
            sagaStepRepository.save(step);

            log.info("Retrying step: {} | Attempt: {}/{}", stepId, step.getRetryCount(), step.getMaxRetries());
            performStepCall(saga, step);
        } else {
            step.fail(errorMessage);
            sagaStepRepository.save(step);

            // Start compensation
            startCompensation(saga);
        }
    }

    /**
     * Start compensation process
     */
    @Transactional
    public void startCompensation(SagaInstance saga) {
        log.info("Starting compensation for saga: {}", saga.getSagaId());
        saga.startCompensation();
        sagaInstanceRepository.save(saga);

        // Execute all compensating steps (reverse order). We run every
        // COMPENSATE step, not just those whose forward step completed, so the
        // order is always cancelled even when a later forward step failed.
        List<SagaStep> compensateSteps = saga.getSteps().stream()
                .filter(step -> step.getType() == StepType.COMPENSATE)
                .filter(step -> step.getStatus() == StepStatus.PENDING
                        || step.getStatus() == StepStatus.EXECUTING)
                .sorted((a, b) -> b.getStepOrder().compareTo(a.getStepOrder()))
                .toList();

        if (compensateSteps.isEmpty()) {
            log.info("No compensating steps to run for saga: {}", saga.getSagaId());
            saga.compensate();
            sagaInstanceRepository.save(saga);
            return;
        }

        // Execute compensations sequentially
        for (SagaStep compensateStep : compensateSteps) {
            executeStep(saga, compensateStep);
        }
    }

    /**
     * Get the compensate step ID for a forward step
     */
    private String getCompensateStepId(String forwardStepId) {
        return switch (forwardStepId) {
            case "authorize-payment" -> "refund-payment";
            case "reserve-inventory" -> "release-inventory";
            case "confirm-order" -> "cancel-order";
            default -> throw new IllegalArgumentException("No compensation for step: " + forwardStepId);
        };
    }

    /**
     * Handle compensation completion
     */
    @Transactional
    public void handleCompensationCompleted(String sagaId) {
        log.info("Compensation completed for saga: {}", sagaId);

        SagaInstance saga = sagaInstanceRepository.findBySagaId(sagaId)
                .orElseThrow(() -> new RuntimeException("Saga not found: " + sagaId));

        // Check if all compensate steps are completed
        List<SagaStep> pendingCompensations = saga.getSteps().stream()
                .filter(step -> step.getType() == StepType.COMPENSATE)
                .filter(step -> step.getStatus() == StepStatus.EXECUTING)
                .toList();

        if (pendingCompensations.isEmpty()) {
            saga.compensate();
            sagaInstanceRepository.save(saga);
        }
    }
}
