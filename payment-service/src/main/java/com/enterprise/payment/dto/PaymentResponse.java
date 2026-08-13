package com.enterprise.payment.dto;

import com.enterprise.payment.domain.PaymentMethod;
import com.enterprise.payment.domain.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private UUID id;
    private UUID orderId;
    private UUID customerId;
    private PaymentStatus status;
    private PaymentMethod paymentMethod;
    private BigDecimal amount;
    private String currency;
    private String idempotencyKey;
    private String gatewayReference;
    private String failureReason;
    private String correlationId;
    private String sagaInstanceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime authorizedAt;
    private LocalDateTime refundedAt;
}
