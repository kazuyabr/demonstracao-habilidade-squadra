package com.enterprise.order.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Audit trail for order state transitions.
 * Tracks every status change with timestamp and reason.
 */
@Entity
@Table(name = "order_status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus toStatus;

    @Column(length = 1000)
    private String reason;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(nullable = false)
    private LocalDateTime transitionedAt;

    @PrePersist
    public void prePersist() {
        if (transitionedAt == null) {
            transitionedAt = LocalDateTime.now();
        }
    }
}
