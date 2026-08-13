package com.enterprise.saga.domain;

/**
 * Represents the status of a Saga instance.
 * This is a state machine for the saga lifecycle.
 */
public enum SagaStatus {
    /**
     * Saga has been created but not started
     */
    CREATED,

    /**
     * Saga is executing steps
     */
    IN_PROGRESS,

    /**
     * All steps completed successfully
     */
    COMPLETED,

    /**
     * A step failed and compensation is in progress
     */
    COMPENSATING,

    /**
     * Compensation completed (rolled back)
     */
    COMPENSATED,

    /**
     * Saga failed and cannot be recovered
     */
    FAILED,

    /**
     * Saga was cancelled by user or timeout
     */
    CANCELLED
}
