package com.enterprise.order.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    private Order order;

    @BeforeEach
    void setUp() {
        order = Order.builder()
                .orderNumber("ORD-20260813-00001")
                .customerId(java.util.UUID.randomUUID())
                .status(OrderStatus.PENDING)
                .currency("BRL")
                .totalAmount(BigDecimal.ZERO)
                .build();
    }

    @Test
    @DisplayName("Should transition to valid next state")
    void shouldTransitionToValidState() {
        order.transitionTo(OrderStatus.PAYMENT_PROCESSING);
        assertEquals(OrderStatus.PAYMENT_PROCESSING, order.getStatus());
    }

    @Test
    @DisplayName("Should throw exception on invalid transition")
    void shouldThrowOnInvalidTransition() {
        assertThrows(IllegalStateException.class, () -> {
            order.transitionTo(OrderStatus.CONFIRMED);
        });
    }

    @Test
    @DisplayName("Should set confirmedAt when transitioning to CONFIRMED")
    void shouldSetConfirmedAt() {
        order.transitionTo(OrderStatus.PAYMENT_PROCESSING);
        order.transitionTo(OrderStatus.PAYMENT_AUTHORIZED);
        order.transitionTo(OrderStatus.INVENTORY_PROCESSING);
        order.transitionTo(OrderStatus.CONFIRMED);

        assertNotNull(order.getConfirmedAt());
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }

    @Test
    @DisplayName("Should set cancelledAt when transitioning to CANCELLED")
    void shouldSetCancelledAt() {
        order.transitionTo(OrderStatus.CANCELLED);

        assertNotNull(order.getCancelledAt());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    @DisplayName("Should add items and recalculate total")
    void shouldAddItemsAndRecalculateTotal() {
        OrderItem item1 = OrderItem.builder()
                .productId("PROD-001")
                .productName("Laptop")
                .quantity(1)
                .unitPrice(new BigDecimal("2500.00"))
                .build();
        item1.calculateSubtotal();

        OrderItem item2 = OrderItem.builder()
                .productId("PROD-002")
                .productName("Mouse")
                .quantity(2)
                .unitPrice(new BigDecimal("50.00"))
                .build();
        item2.calculateSubtotal();

        order.addItem(item1);
        order.addItem(item2);

        assertEquals(2, order.getItems().size());
        assertEquals(new BigDecimal("2600.00"), order.getTotalAmount());
    }

    @Test
    @DisplayName("Order number should be set correctly")
    void shouldHaveOrderNumber() {
        assertEquals("ORD-20260813-00001", order.getOrderNumber());
    }
}
