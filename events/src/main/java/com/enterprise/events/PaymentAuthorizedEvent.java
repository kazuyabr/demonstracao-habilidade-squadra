package com.enterprise.events;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAuthorizedEvent extends DomainEvent {

    private String paymentId;
    private String orderId;
    private String customerId;
    private BigDecimal amount;
    private String currency;
    private String gatewayReference;

    {
        setEventType("PaymentAuthorized");
    }
}
