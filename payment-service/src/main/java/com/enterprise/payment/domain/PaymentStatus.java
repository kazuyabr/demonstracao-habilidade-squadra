package com.enterprise.payment.domain;

/**
 * Payment status lifecycle.
 *
 * Flow:
 *   PENDING → PROCESSING → AUTHORIZED → CAPTURED
 *                    ↓           ↓
 *                FAILED      REFUND_REQUESTED → REFUNDED
 *
 * Key concepts:
 * - Idempotency: Each payment has an idempotencyKey to prevent duplicate processing
 * - The Saga Orchestrator drives status transitions via REST API
 * - REFUND is a compensating transaction triggered by inventory failure
 */
public enum PaymentStatus {

    /** Payment request received, waiting to be processed */
    PENDING,

    /** Payment being processed by the gateway */
    PROCESSING,

    /** Payment authorized (funds reserved) */
    AUTHORIZED,

    /** Payment authorization failed */
    FAILED,

    /** Refund requested (compensating transaction) */
    REFUND_REQUESTED,

    /** Refund completed */
    REFUNDED;

    public boolean isTerminal() {
        return this == AUTHORIZED || this == FAILED || this == REFUNDED;
    }

    public boolean canTransitionTo(PaymentStatus next) {
        return switch (this) {
            case PENDING -> next == PROCESSING || next == FAILED;
            case PROCESSING -> next == AUTHORIZED || next == FAILED;
            case AUTHORIZED -> next == REFUND_REQUESTED;
            case FAILED -> false;
            case REFUND_REQUESTED -> next == REFUNDED;
            case REFUNDED -> false;
        };
    }
}
