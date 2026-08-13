package com.enterprise.payment.exception;

import com.enterprise.payment.domain.PaymentStatus;

public class InvalidPaymentTransitionException extends RuntimeException {

    public InvalidPaymentTransitionException(PaymentStatus from, PaymentStatus to) {
        super(String.format("Invalid payment transition from %s to %s", from, to));
    }
}
