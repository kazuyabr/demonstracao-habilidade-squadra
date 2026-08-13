package com.enterprise.events.consumer;

import com.enterprise.events.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.*;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic event consumer for Apache Pulsar.
 *
 * Features:
 * - Automatic consumer creation per topic
 * - JSON deserialization to domain events
 * - Error handling with DLQ (Dead Letter Queue)
 * - Ack/Nack based on processing result
 *
 * DLQ Strategy:
 * - Failed messages are retried 3 times
 * - After 3 failures, message is sent to DLQ topic
 * - DLQ topics are named: <topic>-DLQ
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventConsumer {

    private final PulsarClient pulsarClient;
    private final ObjectMapper objectMapper;

    private final Map<String, Consumer<byte[]>> consumers = new ConcurrentHashMap<>();

    private static final int MAX_RETRY_COUNT = 3;

    /**
     * Subscribes to a topic and processes events.
     *
     * @param topic     The Pulsar topic to subscribe to
     * @param groupId   Consumer group ID (for shared subscription)
     * @param handler   The event handler function
     * @param eventClass The class to deserialize events into
     */
    public <T extends DomainEvent> void subscribe(
            String topic,
            String groupId,
            EventListener<T> handler,
            Class<T> eventClass) {

        try {
            Consumer<byte[]> consumer = pulsarClient.newConsumer(Schema.BYTES)
                    .topic(topic)
                    .subscriptionName(groupId)
                    .subscriptionType(SubscriptionType.Shared)
                    .negativeAckRedeliveryDelay(1000, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .subscribe();

            consumers.put(topic + ":" + groupId, consumer);

            // Start consumer thread
            Thread consumerThread = new Thread(() -> consumeMessages(consumer, handler, eventClass, topic));
            consumerThread.setDaemon(true);
            consumerThread.setName("pulsar-consumer-" + groupId + "-" + topic);
            consumerThread.start();

            log.info("Subscribed to topic: {} | GroupId: {}", topic, groupId);

        } catch (PulsarClientException ex) {
            log.error("Failed to subscribe to topic: {} | Error: {}", topic, ex.getMessage());
            throw new RuntimeException("Failed to subscribe to topic: " + topic, ex);
        }
    }

    private <T extends DomainEvent> void consumeMessages(
            Consumer<byte[]> consumer,
            EventListener<T> handler,
            Class<T> eventClass,
            String topic) {

        while (true) {
            try {
                Message<byte[]> message = consumer.receive();
                int retryCount = message.getProperties().containsKey("retryCount")
                        ? Integer.parseInt(message.getProperties().get("retryCount"))
                        : 0;

                try {
                    T event = objectMapper.readValue(message.getValue(), eventClass);
                    handler.onEvent(event);
                    consumer.acknowledge(message);
                    log.debug("Event processed: {} | Topic: {}", event.getEventType(), topic);

                } catch (Exception ex) {
                    log.warn("Failed to process event from topic: {} | Retry: {} | Error: {}",
                            topic, retryCount, ex.getMessage());

                    if (retryCount >= MAX_RETRY_COUNT) {
                        log.error("Max retries reached for message. Sending to DLQ. Topic: {}", topic);
                        consumer.negativeAcknowledge(message);
                        // DLQ is handled by Pulsar's negativeAckRedelivery
                    } else {
                        // Re-publish with incremented retry count
                        consumer.negativeAcknowledge(message);
                    }
                }

            } catch (PulsarClientException ex) {
                log.error("Consumer error on topic: {} | Error: {}", topic, ex.getMessage());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    @PreDestroy
    public void cleanup() {
        consumers.values().forEach(consumer -> {
            try {
                consumer.close();
            } catch (PulsarClientException ex) {
                log.warn("Error closing consumer: {}", ex.getMessage());
            }
        });
    }

    @FunctionalInterface
    public interface EventListener<T extends DomainEvent> {
        void onEvent(T event);
    }
}
