package com.enterprise.saga.dto;

import com.enterprise.saga.domain.SagaStatus;
import com.enterprise.saga.domain.StepStatus;
import com.enterprise.saga.domain.StepType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API response for a Saga instance. Uses a flat DTO to avoid the circular
 * reference between SagaInstance and SagaStep entities during serialization.
 */
@Getter
@Builder
public class SagaResponse {

    private Long id;
    private String sagaId;
    private String sagaType;
    private String orderId;
    private String customerId;
    private String orderNumber;
    private SagaStatus status;
    private String currentStepId;
    private Integer completedSteps;
    private Integer totalSteps;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    private Integer timeoutSeconds;
    private Boolean timedOut;
    private List<SagaStepResponse> steps;

    @Getter
    @Builder
    public static class SagaStepResponse {
        private Long id;
        private String stepId;
        private Integer stepOrder;
        private String name;
        private String targetService;
        private StepType type;
        private StepStatus status;
        private String errorMessage;
        private Integer retryCount;
        private Integer maxRetries;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private LocalDateTime createdAt;
    }
}
