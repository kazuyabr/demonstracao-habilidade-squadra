package com.enterprise.saga.domain;

/**
 * Represents the status of an individual step in a Saga.
 */
public enum StepStatus {
    /**
     * Step is waiting to execute
     */
    PENDING,

    /**
     * Step is executing
     */
    EXECUTING,

    /**
     * Step completed successfully
     */
    COMPLETED,

    /**
     * Step failed
     */
    FAILED,

    /**
     * Step's compensation is in progress
     */
    COMPENSATING,

    /**
     * Step's compensation completed
     */
    COMPENSATED,

    /**
     * Step was skipped (not applicable)
     */
    SKIPPED
}
