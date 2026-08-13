package com.enterprise.events;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreatedEvent extends DomainEvent {

    private String orderId;
    private String orderNumber;
    private String customerId;
    private BigDecimal totalAmount;
    private String currency;
    private List<OrderItemData> items;

    {
        setEventType("OrderCreated");
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemData {
        private String productId;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
    }
}
