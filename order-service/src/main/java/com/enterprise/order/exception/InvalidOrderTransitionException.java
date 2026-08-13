package com.enterprise.order.exception;

import com.enterprise.order.domain.OrderStatus;

public class InvalidOrderTransitionException extends RuntimeException {

    public InvalidOrderTransitionException(OrderStatus from, OrderStatus to) {
        super(String.format("Invalid state transition from %s to %s", from, to));
    }
}
