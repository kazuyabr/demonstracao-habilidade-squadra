package com.enterprise.events.producer;

import com.enterprise.events.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Schema;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic event producer for Apache Pulsar.
 *
 * Why a generic producer?
 * - All services use the same publishing mechanism
 * - Consistent error handling and logging
 * - Easy to add metrics, tracing, retry logic
 * - Topic-specific producers are created lazily
 *
 * Usage:
 *   eventProducer.publish(PulsarTopics.ORDER_CREATED, orderCreatedEvent);
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventProducer {

    private final PulsarClient pulsarClient;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, Producer<byte[]>> producers = new ConcurrentHashMap<>();

    /**
     * Publishes an event to the specified topic.
     * Creates the producer lazily if it doesn't exist.
     */
    public void publish(String topic, DomainEvent event) {
        try {
            Producer<byte[]> producer = producers.computeIfAbsent(topic, this::createProducer);

            byte[] payload = objectMapper.writeValueAsBytes(event);
            producer.send(payload);

            log.info("Event published: {} | Topic: {} | EventId: {} | CorrelationId: {}",
                    event.getEventType(), topic, event.getEventId(), event.getCorrelationId());

        } catch (Exception ex) {
            log.error("Failed to publish event: {} | Topic: {} | Error: {}",
                    event.getEventType(), topic, ex.getMessage(), ex);
            throw new RuntimeException("Failed to publish event", ex);
        }
    }

    private Producer<byte[]> createProducer(String topic) {
        try {
            return pulsarClient.newProducer(Schema.BYTES)
                    .topic(topic)
                    .create();
        } catch (PulsarClientException ex) {
            throw new RuntimeException("Failed to create producer for topic: " + topic, ex);
        }
    }

    @PreDestroy
    public void cleanup() {
        producers.values().forEach(producer -> {
            try {
                producer.close();
            } catch (PulsarClientException ex) {
                log.warn("Error closing producer: {}", ex.getMessage());
            }
        });
    }
}
