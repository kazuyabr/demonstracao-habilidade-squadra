package com.enterprise.saga.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a Saga instance - a distributed transaction that coordinates
 * multiple services with compensating transactions for rollback.
 *
 * The Saga follows the Orchestrator pattern:
 * 1. Orchestrator tells each service what to do
 * 2. If a step fails, orchestrator tells previous services to compensate
 * 3. Each service is responsible for its own local transaction
 *
 * Flow:
 * - Created → In Progress → Completed (or Compensated/Failed)
 *
 * Example:
 * - Order Created → Authorize Payment → Reserve Inventory → Confirm Order
 * - If Inventory fails → Refund Payment → Cancel Order
 */
@Entity
@Table(name = "saga_instances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SagaInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique identifier for this saga instance
     */
    @Column(nullable = false, unique = true)
    private String sagaId;

    /**
     * Type of saga (e.g., "ORDER_PROCESSING")
     */
    @Column(nullable = false)
    private String sagaType;

    /**
     * Order ID being processed
     */
    @Column(nullable = false)
    private String orderId;

    /**
     * Customer ID for the order
     */
    @Column(nullable = false)
    private String customerId;

    /**
     * Current status of the saga
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SagaStatus status;

    /**
     * Order number for the order being processed
     */
    @Column(nullable = false)
    private String orderNumber;

    /**
     * Current step being executed
     */
    @Column(nullable = false)
    private String currentStepId;

    /**
     * Total number of steps in this saga
     */
    @Column(nullable = false)
    private Integer totalSteps;

    /**
     * Number of steps completed successfully
     */
    @Column(nullable = false)
    private Integer completedSteps;

    /**
     * Reason for failure or compensation
     */
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String failureReason;

    /**
     * Timestamp when saga was created
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when saga was last updated
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Timestamp when saga completed
     */
    private LocalDateTime completedAt;

    /**
     * Timeout for this saga in seconds
     */
    @Column(nullable = false)
    private Integer timeoutSeconds;

    /**
     * Whether saga has been timed out
     */
    @Column(nullable = false)
    private Boolean timedOut;

    /**
     * Steps in this saga
     */
    @OneToMany(mappedBy = "sagaInstance", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderBy("stepOrder ASC")
    @Builder.Default
    private List<SagaStep> steps = new ArrayList<>();

    /**
     * Create a new saga instance
     */
    @PrePersist
    protected void onCreate() {
        if (this.sagaId == null) {
            this.sagaId = UUID.randomUUID().toString();
        }
        if (this.status == null) {
            this.status = SagaStatus.CREATED;
        }
        if (this.completedSteps == null) {
            this.completedSteps = 0;
        }
        if (this.totalSteps == null) {
            this.totalSteps = 0;
        }
        if (this.timedOut == null) {
            this.timedOut = false;
        }
        if (this.timeoutSeconds == null) {
            this.timeoutSeconds = 300; // 5 minutes default
        }
    }

    /**
     * Start the saga
     */
    public void start() {
        this.status = SagaStatus.IN_PROGRESS;
    }

    /**
     * Mark saga as completed
     */
    public void complete() {
        this.status = SagaStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Start compensation
     */
    public void startCompensation() {
        this.status = SagaStatus.COMPENSATING;
    }

    /**
     * Mark saga as compensated
     */
    public void compensate() {
        this.status = SagaStatus.COMPENSATED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Mark saga as failed
     */
    public void fail(String reason) {
        this.status = SagaStatus.FAILED;
        this.failureReason = reason;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Mark saga as timed out
     */
    public void timeout() {
        this.status = SagaStatus.FAILED;
        this.timedOut = true;
        this.failureReason = "Saga timed out after " + timeoutSeconds + " seconds";
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Move to next step
     */
    public void advanceToNextStep() {
        this.completedSteps++;
    }

    /**
     * Check if all steps are completed
     */
    public boolean allStepsCompleted() {
        return this.completedSteps >= this.totalSteps;
    }

    /**
     * Check if saga is in progress
     */
    public boolean isInProgress() {
        return this.status == SagaStatus.IN_PROGRESS;
    }

    /**
     * Check if saga is compensating
     */
    public boolean isCompensating() {
        return this.status == SagaStatus.COMPENSATING;
    }

    /**
     * Check if saga has completed
     */
    public boolean isCompleted() {
        return this.status == SagaStatus.COMPLETED;
    }

    /**
     * Check if saga has failed
     */
    public boolean isFailed() {
        return this.status == SagaStatus.FAILED;
    }

    /**
     * Check if saga has been compensated
     */
    public boolean isCompensated() {
        return this.status == SagaStatus.COMPENSATED;
    }

    /**
     * Get step by ID
     */
    public SagaStep getStepById(String stepId) {
        return steps.stream()
                .filter(step -> step.getStepId().equals(stepId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Add a step to this saga
     */
    public void addStep(SagaStep step) {
        steps.add(step);
        step.setSagaInstance(this);
        totalSteps++;
    }
}
