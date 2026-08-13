package com.enterprise.order.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response containing order status history (Saga trace).
 * Shows every state transition the order went through.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderHistoryResponse {

    private UUID orderId;
    private String orderNumber;
    private List<StatusTransition> transitions;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatusTransition {
        private OrderStatus fromStatus;
        private OrderStatus toStatus;
        private String reason;
        private String correlationId;
        private LocalDateTime transitionedAt;
    }
}
