package com.enterprise.payment.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_idempotency", columnList = "idempotencyKey", unique = true),
        @Index(name = "idx_payment_order", columnList = "orderId"),
        @Index(name = "idx_payment_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID orderId;

    @Column(nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    /**
     * Idempotency key used to prevent duplicate payment processing.
     * The caller provides this key. If a payment with this key already exists,
     * the existing payment is returned instead of creating a new one.
     */
    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    /** Reference from the payment gateway after authorization */
    @Column(name = "gateway_reference")
    private String gatewayReference;

    /** Reason for failure or refund */
    @Column(length = 1000)
    private String failureReason;

    /** Correlation ID for distributed tracing across services */
    @Column(name = "correlation_id")
    private String correlationId;

    /** Saga instance that owns this payment */
    @Column(name = "saga_instance_id")
    private String sagaInstanceId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "authorized_at")
    private LocalDateTime authorizedAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    public void transitionTo(PaymentStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    String.format("Cannot transition payment from %s to %s", this.status, newStatus)
            );
        }
        this.status = newStatus;

        if (newStatus == PaymentStatus.AUTHORIZED) {
            this.authorizedAt = LocalDateTime.now();
        } else if (newStatus == PaymentStatus.REFUNDED) {
            this.refundedAt = LocalDateTime.now();
        }
    }
}
