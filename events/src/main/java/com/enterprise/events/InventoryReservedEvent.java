package com.enterprise.events;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReservedEvent extends DomainEvent {

    private String reservationId;
    private String orderId;
    private String productId;
    private Integer quantity;
    private String reservationReference;

    {
        setEventType("InventoryReserved");
    }
}
