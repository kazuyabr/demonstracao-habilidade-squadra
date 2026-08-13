package com.enterprise.events;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderConfirmedEvent extends DomainEvent {

    private String orderId;
    private String orderNumber;
    private String customerId;

    {
        setEventType("OrderConfirmed");
    }
}
