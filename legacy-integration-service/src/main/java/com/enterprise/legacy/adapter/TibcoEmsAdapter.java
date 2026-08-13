package com.enterprise.legacy.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simulates TIBCO EMS (Enterprise Message Service) adapter.
 *
 * In a real enterprise environment, TIBCO EMS is a JMS-compliant
 * messaging middleware used to integrate with legacy systems.
 *
 * This adapter demonstrates:
 * - Message sending to legacy queue
 * - Message receiving from legacy queue
 * - Request-reply pattern
 * - Message transformation
 */
@Component
@Slf4j
public class TibcoEmsAdapter {

    private final Map<String, String> legacyQueue = new ConcurrentHashMap<>();
    private final Map<String, String> legacyResponseQueue = new ConcurrentHashMap<>();

    /**
     * Send order to legacy system (simulated TIBCO EMS)
     */
    public String sendOrderToLegacy(String orderJson) {
        String messageId = "TIBCO-" + UUID.randomUUID().toString().substring(0, 8);

        log.info("📤 Sending to TIBCO EMS queue | MessageId: {}", messageId);
        log.debug("Payload: {}", orderJson);

        // Simulate message transformation for legacy format
        String legacyFormat = transformToLegacyFormat(orderJson);

        // Store in simulated queue
        legacyQueue.put(messageId, legacyFormat);

        // Simulate processing delay
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("✅ Message sent to TIBCO EMS | MessageId: {}", messageId);
        return messageId;
    }

    /**
     * Receive response from legacy system
     */
    public String receiveFromLegacy(String messageId) {
        log.info("📥 Receiving from TIBCO EMS | MessageId: {}", messageId);

        // Simulate processing
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Generate legacy response
        String response = String.format("""
                {
                    "messageId": "%s",
                    "status": "PROCESSED",
                    "legacyReference": "LEG-%s",
                    "timestamp": "%s"
                }""",
                messageId,
                System.currentTimeMillis(),
                java.time.LocalDateTime.now()
        );

        legacyResponseQueue.put(messageId, response);
        log.info("✅ Response received from TIBCO EMS | MessageId: {}", messageId);

        return response;
    }

    /**
     * Transform JSON to legacy format (XML-like)
     */
    private String transformToLegacyFormat(String json) {
        // Simple transformation - in real world this would be XSLT
        return "<LEGACY_ORDER>" +
                "<SOURCE>ENTERPRISE_PLATFORM</SOURCE>" +
                "<PAYLOAD>" + json + "</PAYLOAD>" +
                "<TIMESTAMP>" + java.time.LocalDateTime.now() + "</TIMESTAMP>" +
                "</LEGACY_ORDER>";
    }

    /**
     * Check if message was processed
     */
    public boolean isMessageProcessed(String messageId) {
        return legacyResponseQueue.containsKey(messageId);
    }
}
