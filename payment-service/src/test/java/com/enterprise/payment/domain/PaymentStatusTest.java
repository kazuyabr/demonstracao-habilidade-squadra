package com.enterprise.payment.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentStatusTest {

    @Nested
    @DisplayName("State Transition Tests")
    class TransitionTests {

        @Test
        @DisplayName("PENDING can transition to PROCESSING")
        void pendingCanTransitionToProcessing() {
            assertTrue(PaymentStatus.PENDING.canTransitionTo(PaymentStatus.PROCESSING));
        }

        @Test
        @DisplayName("PENDING can transition to FAILED")
        void pendingCanTransitionToFailed() {
            assertTrue(PaymentStatus.PENDING.canTransitionTo(PaymentStatus.FAILED));
        }

        @Test
        @DisplayName("PENDING cannot transition to AUTHORIZED")
        void pendingCannotTransitionToAuthorized() {
            assertFalse(PaymentStatus.PENDING.canTransitionTo(PaymentStatus.AUTHORIZED));
        }

        @Test
        @DisplayName("PROCESSING can transition to AUTHORIZED")
        void processingCanTransitionToAuthorized() {
            assertTrue(PaymentStatus.PROCESSING.canTransitionTo(PaymentStatus.AUTHORIZED));
        }

        @Test
        @DisplayName("PROCESSING can transition to FAILED")
        void processingCanTransitionToFailed() {
            assertTrue(PaymentStatus.PROCESSING.canTransitionTo(PaymentStatus.FAILED));
        }

        @Test
        @DisplayName("AUTHORIZED can transition to REFUND_REQUESTED")
        void authorizedCanTransitionToRefundRequested() {
            assertTrue(PaymentStatus.AUTHORIZED.canTransitionTo(PaymentStatus.REFUND_REQUESTED));
        }

        @Test
        @DisplayName("REFUND_REQUESTED can transition to REFUNDED")
        void refundRequestedCanTransitionToRefunded() {
            assertTrue(PaymentStatus.REFUND_REQUESTED.canTransitionTo(PaymentStatus.REFUNDED));
        }

        @Test
        @DisplayName("FAILED is terminal")
        void failedIsTerminal() {
            assertFalse(PaymentStatus.FAILED.canTransitionTo(PaymentStatus.PENDING));
            assertFalse(PaymentStatus.FAILED.canTransitionTo(PaymentStatus.PROCESSING));
            assertFalse(PaymentStatus.FAILED.canTransitionTo(PaymentStatus.AUTHORIZED));
            assertTrue(PaymentStatus.FAILED.isTerminal());
        }

        @Test
        @DisplayName("AUTHORIZED is terminal (for direct transitions)")
        void authorizedIsTerminal() {
            assertFalse(PaymentStatus.AUTHORIZED.canTransitionTo(PaymentStatus.PENDING));
            assertFalse(PaymentStatus.AUTHORIZED.canTransitionTo(PaymentStatus.PROCESSING));
            assertTrue(PaymentStatus.AUTHORIZED.isTerminal());
        }

        @Test
        @DisplayName("REFUNDED is terminal")
        void refundedIsTerminal() {
            assertFalse(PaymentStatus.REFUNDED.canTransitionTo(PaymentStatus.PENDING));
            assertFalse(PaymentStatus.REFUNDED.canTransitionTo(PaymentStatus.AUTHORIZED));
            assertTrue(PaymentStatus.REFUNDED.isTerminal());
        }
    }

    @Nested
    @DisplayName("Happy Path")
    class HappyPath {

        @Test
        @DisplayName("Complete happy path: PENDING → PROCESSING → AUTHORIZED")
        void completeHappyPath() {
            PaymentStatus current = PaymentStatus.PENDING;

            current = PaymentStatus.PROCESSING;
            assertTrue(current.canTransitionTo(PaymentStatus.AUTHORIZED));

            current = PaymentStatus.AUTHORIZED;
            assertTrue(current.isTerminal());
        }
    }

    @Nested
    @DisplayName("Compensation Path (Refund)")
    class CompensationPath {

        @Test
        @DisplayName("Refund path: AUTHORIZED → REFUND_REQUESTED → REFUNDED")
        void refundPath() {
            PaymentStatus current = PaymentStatus.AUTHORIZED;
            current = PaymentStatus.REFUND_REQUESTED;
            assertTrue(current.canTransitionTo(PaymentStatus.REFUNDED));

            current = PaymentStatus.REFUNDED;
            assertTrue(current.isTerminal());
        }
    }
}
