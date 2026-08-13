package com.enterprise.order.exception;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String message) {
        super(message);
    }

    public OrderNotFoundException(String identifier, String value) {
        super(String.format("Order not found with %s: %s", identifier, value));
    }
}
