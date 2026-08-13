package com.enterprise.events;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRefundedEvent extends DomainEvent {

    private String paymentId;
    private String orderId;
    private String customerId;
    private String reason;

    {
        setEventType("PaymentRefunded");
    }
}
