package com.enterprise.events;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReleasedEvent extends DomainEvent {

    private String orderId;
    private String productId;
    private Integer quantity;
    private String reason;

    {
        setEventType("InventoryReleased");
    }
}
