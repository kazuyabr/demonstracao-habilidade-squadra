package com.enterprise.order.domain;

/**
 * Represents the lifecycle states of an order.
 *
 * State Machine:
 *
 *   PENDING ──→ PAYMENT_PROCESSING ──→ PAYMENT_AUTHORIZED ──→ INVENTORY_PROCESSING ──→ CONFIRMED
 *       │              │                      │                       │
 *       │              └──→ PAYMENT_FAILED     │                       └──→ INVENTORY_FAILED
 *       │                      │              │                               │
 *       │                      └──────────────┘                               │
 *       │                          (compensation)                             │
 *       │                                                                     │
 *       └─────────────────────────────────────────────────────────────────────→ CANCELLED
 *                              (compensation: refund + cancel)
 *
 * Notes:
 * - PAYMENT_FAILED and INVENTORY_FAILED are intermediate states that trigger compensation
 * - From CANCELLED, no further transitions are possible
 * - The Saga Orchestrator drives transitions between services
 * - This enum defines valid states; transitions are enforced in OrderService
 */
public enum OrderStatus {

    /** Order created, waiting for payment processing */
    PENDING,

    /** Payment authorization in progress */
    PAYMENT_PROCESSING,

    /** Payment authorized successfully */
    PAYMENT_AUTHORIZED,

    /** Payment authorization failed */
    PAYMENT_FAILED,

    /** Inventory reservation in progress */
    INVENTORY_PROCESSING,

    /** Inventory reservation failed, compensation triggered */
    INVENTORY_FAILED,

    /** Order confirmed, all steps completed successfully */
    CONFIRMED,

    /** Order cancelled (payment failed or inventory failed) */
    CANCELLED;

    public boolean isTerminal() {
        return this == CONFIRMED || this == CANCELLED;
    }

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case PENDING -> next == PAYMENT_PROCESSING || next == CANCELLED;
            case PAYMENT_PROCESSING -> next == PAYMENT_AUTHORIZED
                    || next == PAYMENT_FAILED
                    || next == CANCELLED;
            case PAYMENT_AUTHORIZED -> next == INVENTORY_PROCESSING
                    || next == CANCELLED;
            case PAYMENT_FAILED -> next == CANCELLED;
            case INVENTORY_PROCESSING -> next == CONFIRMED
                    || next == INVENTORY_FAILED
                    || next == CANCELLED;
            case INVENTORY_FAILED -> next == CANCELLED;
            case CONFIRMED -> false;
            case CANCELLED -> false;
        };
    }
}
