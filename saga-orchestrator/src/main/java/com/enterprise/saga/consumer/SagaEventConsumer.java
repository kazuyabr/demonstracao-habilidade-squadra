package com.enterprise.saga.consumer;

import com.enterprise.events.OrderCreatedEvent;
import com.enterprise.events.config.PulsarTopics;
import com.enterprise.events.consumer.EventConsumer;
import com.enterprise.saga.service.SagaOrchestrator;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Consumes the order-created event to start a new saga.
 *
 * After the saga starts, step progression is driven synchronously by the
 * orchestrator calling the real services over HTTP (see SagaOrchestrator).
 * The orchestrator no longer consumes its own "success" events - those are
 * published only as notifications for observability.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SagaEventConsumer {

    private final EventConsumer eventConsumer;
    private final SagaOrchestrator sagaOrchestrator;

    @PostConstruct
    public void init() {
        // Listen to OrderCreated to start saga
        eventConsumer.subscribe(
                PulsarTopics.ORDER_CREATED,
                "saga-orchestrator",
                this::handleOrderCreated,
                OrderCreatedEvent.class
        );

        log.info("Saga Orchestrator subscribed to events");
    }

    private void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreated event | OrderId: {}", event.getOrderId());
        sagaOrchestrator.createOrderSaga(event);
    }
}
