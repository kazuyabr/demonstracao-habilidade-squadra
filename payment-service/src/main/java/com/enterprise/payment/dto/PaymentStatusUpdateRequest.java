package com.enterprise.payment.dto;

import com.enterprise.payment.domain.PaymentStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentStatusUpdateRequest {

    private PaymentStatus status;

    private String reason;

    private String gatewayReference;

    private String correlationId;
}
