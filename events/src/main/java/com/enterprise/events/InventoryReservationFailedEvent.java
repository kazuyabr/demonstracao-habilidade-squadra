package com.enterprise.events;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReservationFailedEvent extends DomainEvent {

    private String orderId;
    private String productId;
    private Integer quantity;
    private String failureReason;

    {
        setEventType("InventoryReservationFailed");
    }
}
