package com.enterprise.order.dto;

import com.enterprise.order.domain.OrderStatus;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusUpdateRequest {

    private OrderStatus status;

    private String reason;

    private String correlationId;
}
