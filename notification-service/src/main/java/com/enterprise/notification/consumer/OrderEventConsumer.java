package com.enterprise.notification.consumer;

import com.enterprise.events.*;
import com.enterprise.events.consumer.EventConsumer;
import com.enterprise.notification.service.NotificationService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Event consumer for the Notification Service.
 *
 * Subscribes to domain events and sends appropriate notifications.
 * This demonstrates:
 * - Async event consumption
 * - Event-driven architecture
 * - Decoupled notification logic
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final EventConsumer eventConsumer;
    private final NotificationService notificationService;

    @PostConstruct
    public void init() {
        // Subscribe to order events
        eventConsumer.subscribe(
                PulsarTopics.ORDER_CONFIRMED,
                "notification-service",
                this::handleOrderConfirmed,
                OrderConfirmedEvent.class
        );

        eventConsumer.subscribe(
                PulsarTopics.ORDER_CANCELLED,
                "notification-service",
                this::handleOrderCancelled,
                OrderCancelledEvent.class
        );

        eventConsumer.subscribe(
                PulsarTopics.PAYMENT_FAILED,
                "notification-service",
                this::handlePaymentFailed,
                PaymentFailedEvent.class
        );

        eventConsumer.subscribe(
                PulsarTopics.PAYMENT_REFUNDED,
                "notification-service",
                this::handlePaymentRefunded,
                PaymentRefundedEvent.class
        );

        log.info("Notification Service subscribed to events");
    }

    private void handleOrderConfirmed(OrderConfirmedEvent event) {
        log.info("Processing OrderConfirmed event: {}", event.getOrderId());
        notificationService.sendOrderConfirmation(
                event.getOrderNumber(),
                event.getCustomerId()
        );
    }

    private void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("Processing OrderCancelled event: {}", event.getOrderId());
        notificationService.sendOrderCancellation(
                event.getOrderNumber(),
                event.getCustomerId(),
                event.getReason()
        );
    }

    private void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("Processing PaymentFailed event: {}", event.getOrderId());
        notificationService.sendPaymentFailed(
                event.getOrderId(),
                event.getCustomerId(),
                event.getFailureReason()
        );
    }

    private void handlePaymentRefunded(PaymentRefundedEvent event) {
        log.info("Processing PaymentRefunded event: {}", event.getOrderId());
        notificationService.sendRefundConfirmation(
                event.getOrderId(),
                event.getCustomerId()
        );
    }
}
