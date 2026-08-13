package com.enterprise.payment.exception;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String message) {
        super(message);
    }

    public PaymentNotFoundException(String identifier, String value) {
        super(String.format("Payment not found with %s: %s", identifier, value));
    }
}
