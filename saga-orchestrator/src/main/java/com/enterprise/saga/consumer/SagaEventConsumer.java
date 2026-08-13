package com.enterprise.saga.consumer;

import com.enterprise.events.*;
import com.enterprise.events.config.PulsarTopics;
import com.enterprise.events.consumer.EventConsumer;
import com.enterprise.saga.domain.SagaInstance;
import com.enterprise.saga.service.SagaOrchestrator;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Consumes domain events and triggers saga steps.
 *
 * This consumer listens to:
 * - OrderCreatedEvent: Starts a new saga
 * - PaymentAuthorizedEvent: Completes payment step
 * - PaymentFailedEvent: Fails payment step
 * - InventoryReservedEvent: Completes inventory step
 * - InventoryReservationFailedEvent: Fails inventory step
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

        // Listen to step completion events
        eventConsumer.subscribe(
                PulsarTopics.PAYMENT_AUTHORIZED,
                "saga-orchestrator",
                this::handlePaymentAuthorized,
                PaymentAuthorizedEvent.class
        );

        eventConsumer.subscribe(
                PulsarTopics.PAYMENT_FAILED,
                "saga-orchestrator",
                this::handlePaymentFailed,
                PaymentFailedEvent.class
        );

        eventConsumer.subscribe(
                PulsarTopics.INVENTORY_RESERVED,
                "saga-orchestrator",
                this::handleInventoryReserved,
                InventoryReservedEvent.class
        );

        eventConsumer.subscribe(
                PulsarTopics.INVENTORY_RESERVATION_FAILED,
                "saga-orchestrator",
                this::handleInventoryReservationFailed,
                InventoryReservationFailedEvent.class
        );

        log.info("Saga Orchestrator subscribed to events");
    }

    private void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreated event | OrderId: {}", event.getOrderId());
        sagaOrchestrator.createOrderSaga(event);
    }

    private void handlePaymentAuthorized(PaymentAuthorizedEvent event) {
        log.info("Received PaymentAuthorized event | SagaId: {}", event.getSagaInstanceId());
        if (event.getSagaInstanceId() != null) {
            sagaOrchestrator.handleStepCompleted(
                    event.getSagaInstanceId(),
                    "authorize-payment",
                    "Payment authorized successfully"
            );
        }
    }

    private void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("Received PaymentFailed event | SagaId: {}", event.getSagaInstanceId());
        if (event.getSagaInstanceId() != null) {
            sagaOrchestrator.handleStepFailed(
                    event.getSagaInstanceId(),
                    "authorize-payment",
                    event.getFailureReason()
            );
        }
    }

    private void handleInventoryReserved(InventoryReservedEvent event) {
        log.info("Received InventoryReserved event | SagaId: {}", event.getSagaInstanceId());
        if (event.getSagaInstanceId() != null) {
            sagaOrchestrator.handleStepCompleted(
                    event.getSagaInstanceId(),
                    "reserve-inventory",
                    "Inventory reserved successfully"
            );
        }
    }

    private void handleInventoryReservationFailed(InventoryReservationFailedEvent event) {
        log.info("Received InventoryReservationFailed event | SagaId: {}", event.getSagaInstanceId());
        if (event.getSagaInstanceId() != null) {
            sagaOrchestrator.handleStepFailed(
                    event.getSagaInstanceId(),
                    "reserve-inventory",
                    event.getFailureReason()
            );
        }
    }
}
