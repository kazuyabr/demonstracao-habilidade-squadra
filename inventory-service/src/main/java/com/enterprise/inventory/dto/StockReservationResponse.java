package com.enterprise.inventory.dto;

import com.enterprise.inventory.domain.ReservationStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReservationResponse {

    private String id;
    private String orderId;
    private String productId;
    private Integer quantity;
    private ReservationStatus status;
    private String reservationReference;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime committedAt;
    private LocalDateTime releasedAt;
}
