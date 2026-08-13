package com.enterprise.inventory.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a stock reservation linked to an order.
 * When an order is placed, stock is reserved.
 * When the order is confirmed, the reservation is committed (stock is actually deducted).
 * When the order is cancelled, the reservation is released.
 */
@Document(collection = "stock_reservations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReservation {

    @Id
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

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void commit() {
        this.status = ReservationStatus.COMMITTED;
        this.committedAt = LocalDateTime.now();
    }

    public void release(String reason) {
        this.status = ReservationStatus.RELEASED;
        this.reason = reason;
        this.releasedAt = LocalDateTime.now();
    }
}
