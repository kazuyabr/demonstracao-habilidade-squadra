package com.enterprise.order.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderStatusTest {

    @Nested
    @DisplayName("State Transition Tests")
    class TransitionTests {

        @Test
        @DisplayName("PENDING can transition to PAYMENT_PROCESSING")
        void pendingCanTransitionToPaymentProcessing() {
            assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.PAYMENT_PROCESSING));
        }

        @Test
        @DisplayName("PENDING can transition to CANCELLED")
        void pendingCanTransitionToCancelled() {
            assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.CANCELLED));
        }

        @Test
        @DisplayName("PENDING cannot transition to CONFIRMED")
        void pendingCannotTransitionToConfirmed() {
            assertFalse(OrderStatus.PENDING.canTransitionTo(OrderStatus.CONFIRMED));
        }

        @Test
        @DisplayName("PAYMENT_PROCESSING can transition to PAYMENT_AUTHORIZED")
        void paymentProcessingCanTransitionToAuthorized() {
            assertTrue(OrderStatus.PAYMENT_PROCESSING.canTransitionTo(OrderStatus.PAYMENT_AUTHORIZED));
        }

        @Test
        @DisplayName("PAYMENT_PROCESSING can transition to PAYMENT_FAILED")
        void paymentProcessingCanTransitionToFailed() {
            assertTrue(OrderStatus.PAYMENT_PROCESSING.canTransitionTo(OrderStatus.PAYMENT_FAILED));
        }

        @Test
        @DisplayName("PAYMENT_AUTHORIZED can transition to INVENTORY_PROCESSING")
        void paymentAuthorizedCanTransitionToInventoryProcessing() {
            assertTrue(OrderStatus.PAYMENT_AUTHORIZED.canTransitionTo(OrderStatus.INVENTORY_PROCESSING));
        }

        @Test
        @DisplayName("INVENTORY_PROCESSING can transition to CONFIRMED")
        void inventoryProcessingCanTransitionToConfirmed() {
            assertTrue(OrderStatus.INVENTORY_PROCESSING.canTransitionTo(OrderStatus.CONFIRMED));
        }

        @Test
        @DisplayName("INVENTORY_PROCESSING can transition to INVENTORY_FAILED")
        void inventoryProcessingCanTransitionToInventoryFailed() {
            assertTrue(OrderStatus.INVENTORY_PROCESSING.canTransitionTo(OrderStatus.INVENTORY_FAILED));
        }

        @Test
        @DisplayName("INVENTORY_FAILED can transition to CANCELLED")
        void inventoryFailedCanTransitionToCancelled() {
            assertTrue(OrderStatus.INVENTORY_FAILED.canTransitionTo(OrderStatus.CANCELLED));
        }

        @Test
        @DisplayName("CONFIRMED is terminal - cannot transition")
        void confirmedIsTerminal() {
            assertFalse(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.CANCELLED));
            assertFalse(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.PENDING));
            assertTrue(OrderStatus.CONFIRMED.isTerminal());
        }

        @Test
        @DisplayName("CANCELLED is terminal - cannot transition")
        void cancelledIsTerminal() {
            assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.PENDING));
            assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.CONFIRMED));
            assertTrue(OrderStatus.CANCELLED.isTerminal());
        }
    }

    @Nested
    @DisplayName("Happy Path Flow")
    class HappyPathFlow {

        @Test
        @DisplayName("Complete happy path: PENDING → PAYMENT_PROCESSING → PAYMENT_AUTHORIZED → INVENTORY_PROCESSING → CONFIRMED")
        void completeHappyPath() {
            OrderStatus current = OrderStatus.PENDING;

            current = OrderStatus.PAYMENT_PROCESSING;
            assertTrue(current.canTransitionTo(OrderStatus.PAYMENT_AUTHORIZED));

            current = OrderStatus.PAYMENT_AUTHORIZED;
            assertTrue(current.canTransitionTo(OrderStatus.INVENTORY_PROCESSING));

            current = OrderStatus.INVENTORY_PROCESSING;
            assertTrue(current.canTransitionTo(OrderStatus.CONFIRMED));

            current = OrderStatus.CONFIRMED;
            assertTrue(current.isTerminal());
        }
    }

    @Nested
    @DisplayName("Failure Path Flow")
    class FailurePathFlow {

        @Test
        @DisplayName("Payment failure path: PENDING → PAYMENT_PROCESSING → PAYMENT_FAILED → CANCELLED")
        void paymentFailurePath() {
            OrderStatus current = OrderStatus.PENDING;

            current = OrderStatus.PAYMENT_PROCESSING;
            assertTrue(current.canTransitionTo(OrderStatus.PAYMENT_FAILED));

            current = OrderStatus.PAYMENT_FAILED;
            assertTrue(current.canTransitionTo(OrderStatus.CANCELLED));

            current = OrderStatus.CANCELLED;
            assertTrue(current.isTerminal());
        }

        @Test
        @DisplayName("Inventory failure path: PENDING → ... → PAYMENT_AUTHORIZED → INVENTORY_PROCESSING → INVENTORY_FAILED → CANCELLED")
        void inventoryFailurePath() {
            OrderStatus current = OrderStatus.PAYMENT_AUTHORIZED;
            current = OrderStatus.INVENTORY_PROCESSING;
            assertTrue(current.canTransitionTo(OrderStatus.INVENTORY_FAILED));

            current = OrderStatus.INVENTORY_FAILED;
            assertTrue(current.canTransitionTo(OrderStatus.CANCELLED));

            current = OrderStatus.CANCELLED;
            assertTrue(current.isTerminal());
        }
    }

    @Nested
    @DisplayName("Invalid Transitions")
    class InvalidTransitions {

        @Test
        @DisplayName("Cannot jump from PENDING to CONFIRMED")
        void cannotJumpToConfirmed() {
            assertFalse(OrderStatus.PENDING.canTransitionTo(OrderStatus.CONFIRMED));
        }

        @Test
        @DisplayName("Cannot go back from CONFIRMED to PENDING")
        void cannotGoBackFromConfirmed() {
            assertFalse(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.PENDING));
        }

        @Test
        @DisplayName("Cannot go back from CANCELLED to any state")
        void cannotGoBackFromCancelled() {
            assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.PENDING));
            assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.PAYMENT_PROCESSING));
            assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.CONFIRMED));
        }
    }
}
