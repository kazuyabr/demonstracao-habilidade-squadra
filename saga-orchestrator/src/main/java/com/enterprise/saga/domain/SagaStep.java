package com.enterprise.saga.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a single step in a Saga transaction.
 * Each step has a forward action and a compensating action.
 *
 * Example:
 * - Step 1: Authorize Payment (forward) → Refund Payment (compensate)
 * - Step 2: Reserve Inventory (forward) → Release Inventory (compensate)
 * - Step 3: Confirm Order (forward) → Cancel Order (compensate)
 */
@Entity
@Table(name = "saga_steps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"sagaInstance"})
public class SagaStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique identifier for this step within the saga
     */
    @Column(nullable = false)
    private String stepId;

    /**
     * Sequence order of this step
     */
    @Column(nullable = false)
    private Integer stepOrder;

    /**
     * Human-readable name (e.g., "Authorize Payment")
     */
    @Column(nullable = false)
    private String name;

    /**
     * Target service to execute this step
     */
    @Column(nullable = false)
    private String targetService;

    /**
     * Type of step (FORWARD or COMPENSATE)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StepType type;

    /**
     * Current status of this step
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StepStatus status;

    /**
     * Request payload sent to the target service
     */
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String requestPayload;

    /**
     * Response payload received from the target service
     */
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String responsePayload;

    /**
     * Error message if step failed
     */
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String errorMessage;

    /**
     * Number of retry attempts
     */
    @Column(nullable = false)
    private Integer retryCount;

    /**
     * Maximum number of retries allowed
     */
    @Column(nullable = false)
    private Integer maxRetries;

    /**
     * When this step started executing
     */
    private LocalDateTime startedAt;

    /**
     * When this step completed
     */
    private LocalDateTime completedAt;

    /**
     * Timestamp when this step was created
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when this step was last updated
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Parent saga instance
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "saga_id", nullable = false)
    private SagaInstance sagaInstance;

    /**
     * Start executing this step
     */
    public void start() {
        this.status = StepStatus.EXECUTING;
        this.startedAt = LocalDateTime.now();
        this.retryCount = 0;
    }

    /**
     * Mark step as completed
     */
    public void complete() {
        this.status = StepStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Mark step as failed
     */
    public void fail(String errorMessage) {
        this.status = StepStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Start compensation
     */
    public void startCompensation() {
        this.status = StepStatus.COMPENSATING;
        this.retryCount = 0;
    }

    /**
     * Mark compensation as completed
     */
    public void compensate() {
        this.status = StepStatus.COMPENSATED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Increment retry counter
     */
    public void incrementRetry() {
        this.retryCount++;
    }

    /**
     * Check if step can be retried
     */
    public boolean canRetry() {
        return this.retryCount < this.maxRetries;
    }

    /**
     * Check if step has completed successfully
     */
    public boolean isCompleted() {
        return this.status == StepStatus.COMPLETED;
    }

    /**
     * Check if step has failed
     */
    public boolean isFailed() {
        return this.status == StepStatus.FAILED;
    }

    /**
     * Check if step is in compensation
     */
    public boolean isCompensating() {
        return this.status == StepStatus.COMPENSATING;
    }
}
