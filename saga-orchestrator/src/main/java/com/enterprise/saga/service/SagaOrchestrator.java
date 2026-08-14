package com.enterprise.saga.service;

import com.enterprise.events.*;
import com.enterprise.events.config.PulsarTopics;
import com.enterprise.events.producer.EventProducer;
import com.enterprise.saga.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
 * OrderCreated → Authorize Payment → Reserve Inventory → Confirm Order
 *     ↓ (if any step fails)
 * Refund Payment ← Release Inventory ← Cancel Order
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SagaOrchestrator {

    private final SagaInstanceRepository sagaInstanceRepository;
    private final SagaStepRepository sagaStepRepository;
    private final EventProducer eventProducer;

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
        log.info("Creating saga for order: {} | OrderId: {}", event.getOrderNumber(), event.getOrderId());

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
            // Send event to trigger the step
            publishStepEvent(saga, step);
            log.info("Step {} published to {}", step.getStepId(), step.getTargetService());
        } catch (Exception e) {
            log.error("Failed to execute step: {} | Error: {}", step.getStepId(), e.getMessage());
            step.fail(e.getMessage());
            sagaStepRepository.save(step);

            // Start compensation
            startCompensation(saga);
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
     * Publish event to trigger step execution
     */
    private void publishStepEvent(SagaInstance saga, SagaStep step) {
        switch (step.getStepId()) {
            case "authorize-payment" -> {
                PaymentAuthorizedEvent event = PaymentAuthorizedEvent.builder()
                        .orderId(saga.getOrderId())
                        .customerId(saga.getCustomerId())
                        .amount(java.math.BigDecimal.ZERO)
                        .build();
                publishEvent(saga, event, PulsarTopics.PAYMENT_AUTHORIZED);
            }
            case "reserve-inventory" -> {
                InventoryReservedEvent event = InventoryReservedEvent.builder()
                        .orderId(saga.getOrderId())
                        .reservationId("RES-" + System.currentTimeMillis())
                        .build();
                publishEvent(saga, event, PulsarTopics.INVENTORY_RESERVED);
            }
            case "confirm-order" -> {
                OrderConfirmedEvent event = OrderConfirmedEvent.builder()
                        .orderId(saga.getOrderId())
                        .orderNumber(saga.getOrderNumber())
                        .customerId(saga.getCustomerId())
                        .build();
                publishEvent(saga, event, PulsarTopics.ORDER_CONFIRMED);
            }
            case "refund-payment" -> {
                PaymentRefundedEvent event = PaymentRefundedEvent.builder()
                        .orderId(saga.getOrderId())
                        .customerId(saga.getCustomerId())
                        .reason("Saga compensation")
                        .build();
                publishEvent(saga, event, PulsarTopics.PAYMENT_REFUNDED);
            }
            case "release-inventory" -> {
                InventoryReleasedEvent event = InventoryReleasedEvent.builder()
                        .orderId(saga.getOrderId())
                        .build();
                publishEvent(saga, event, PulsarTopics.INVENTORY_RELEASED);
            }
            case "cancel-order" -> {
                OrderCancelledEvent event = OrderCancelledEvent.builder()
                        .orderId(saga.getOrderId())
                        .orderNumber(saga.getOrderNumber())
                        .customerId(saga.getCustomerId())
                        .reason("Saga compensation")
                        .build();
                publishEvent(saga, event, PulsarTopics.ORDER_CANCELLED);
            }
            default -> throw new IllegalArgumentException("Unknown step: " + step.getStepId());
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
            step.setStatus(StepStatus.PENDING);
            step.setErrorMessage(null);
            sagaStepRepository.save(step);

            log.info("Retrying step: {} | Attempt: {}/{}", stepId, step.getRetryCount(), step.getMaxRetries());
            executeStep(saga, step);
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

        // Find completed forward steps (need compensation in reverse order)
        List<SagaStep> completedSteps = saga.getSteps().stream()
                .filter(step -> step.getType() == StepType.FORWARD)
                .filter(step -> step.getStatus() == StepStatus.COMPLETED)
                .sorted((a, b) -> b.getStepOrder().compareTo(a.getStepOrder()))
                .toList();

        if (completedSteps.isEmpty()) {
            log.info("No completed steps to compensate for saga: {}", saga.getSagaId());
            saga.compensate();
            sagaInstanceRepository.save(saga);
            return;
        }

        // Execute compensations sequentially
        for (SagaStep step : completedSteps) {
            String compensateStepId = getCompensateStepId(step.getStepId());
            SagaStep compensateStep = saga.getStepById(compensateStepId);

            if (compensateStep != null) {
                executeStep(saga, compensateStep);
            }
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
