package com.enterprise.legacy.service;

import com.enterprise.events.OrderCreatedEvent;
import com.enterprise.events.OrderConfirmedEvent;
import com.enterprise.events.producer.EventProducer;
import com.enterprise.events.config.PulsarTopics;
import com.enterprise.legacy.adapter.MainframeBatchAdapter;
import com.enterprise.legacy.adapter.TibcoEmsAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Orchestrates integration with legacy systems.
 *
 * This service demonstrates:
 * - Anti-corruption layer pattern
 * - Message transformation
 * - Request-reply over messaging
 * - Batch job orchestration
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LegacyIntegrationService {

    private final TibcoEmsAdapter tibcoEmsAdapter;
    private final MainframeBatchAdapter mainframeBatchAdapter;
    private final EventProducer eventProducer;

    /**
     * Process order through legacy systems
     */
    public String processOrderThroughLegacy(OrderCreatedEvent event) {
        log.info("Processing order through legacy systems | OrderId: {}", event.getOrderId());

        // Step 1: Send order to TIBCO EMS for legacy ERP integration
        String tibcoMessageId = sendToTibcoEms(event);

        // Step 2: Submit batch job for legacy reporting
        String batchJobId = submitMainframeBatchJob(event);

        // Step 3: Wait for TIBCO response (simulated)
        String tibcoResponse = waitForTibcoResponse(tibcoMessageId);

        log.info("Order processed through legacy systems | OrderId: {} | TIBCO: {} | Batch: {}",
                event.getOrderId(), tibcoMessageId, batchJobId);

        return tibcoResponse;
    }

    /**
     * Send order to TIBCO EMS
     */
    private String sendToTibcoEms(OrderCreatedEvent event) {
        String orderJson = String.format("""
                {
                    "orderId": "%s",
                    "orderNumber": "%s",
                    "customerId": "%s",
                    "totalAmount": %s,
                    "items": %s
                }""",
                event.getOrderId(),
                event.getOrderNumber(),
                event.getCustomerId(),
                event.getTotalAmount(),
                event.getItems() != null ? event.getItems().toString() : "[]"
        );

        return tibcoEmsAdapter.sendOrderToLegacy(orderJson);
    }

    /**
     * Submit batch job to mainframe
     */
    private String submitMainframeBatchJob(OrderCreatedEvent event) {
        String payload = String.format("""
                {
                    "orderId": "%s",
                    "operation": "ORDER_REPORT"
                }""",
                event.getOrderId()
        );

        return mainframeBatchAdapter.submitBatchJob("ORDER_PROCESSING", payload);
    }

    /**
     * Wait for TIBCO response (simulated)
     */
    private String waitForTibcoResponse(String messageId) {
        // In real world, this would be async via JMS
        return tibcoEmsAdapter.receiveFromLegacy(messageId);
    }

    /**
     * Process legacy confirmation
     */
    public void processLegacyConfirmation(String orderId, String legacyReference) {
        log.info("Processing legacy confirmation | OrderId: {} | LegacyRef: {}",
                orderId, legacyReference);

        // Publish order confirmed event
        OrderConfirmedEvent event = OrderConfirmedEvent.builder()
                .orderId(orderId)
                .orderNumber("LEG-" + legacyReference)
                .customerId("LEGACY")
                .build();

        eventProducer.publish(PulsarTopics.ORDER_CONFIRMED, event);
    }
}
