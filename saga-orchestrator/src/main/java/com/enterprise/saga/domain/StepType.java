package com.enterprise.saga.domain;

/**
 * Represents the type of action a saga step performs.
 */
public enum StepType {
    /**
     * Forward action (e.g., reserve inventory, authorize payment)
     */
    FORWARD,

    /**
     * Compensating action (e.g., release inventory, refund payment)
     */
    COMPENSATE
}
