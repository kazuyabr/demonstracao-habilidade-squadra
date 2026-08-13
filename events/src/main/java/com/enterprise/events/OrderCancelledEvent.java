package com.enterprise.events;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCancelledEvent extends DomainEvent {

    private String orderId;
    private String orderNumber;
    private String customerId;
    private String reason;

    {
        setEventType("OrderCancelled");
    }
}
