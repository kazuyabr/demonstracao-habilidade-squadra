package com.enterprise.events.config;

/**
 * Centralized topic definitions for Apache Pulsar.
 *
 * Why a constants class?
 * - Single source of truth for topic names
 * - Easy to rename/reorganize topics
 * - Prevents typos in topic names
 * - Makes it clear what topics exist in the system
 *
 * Topic naming convention:
 *   persistent://public/default/<domain>-<event-type>
 *
 * We use a flat topic structure (not hierarchical) because:
 * - Each domain event has its own topic
 * - Consumers subscribe to specific event types
 * - Easier to monitor and debug
 */
public final class PulsarTopics {

    private PulsarTopics() {}

    // Order events
    public static final String ORDER_CREATED = "persistent://public/default/order-created";
    public static final String ORDER_CONFIRMED = "persistent://public/default/order-confirmed";
    public static final String ORDER_CANCELLED = "persistent://public/default/order-cancelled";

    // Payment events
    public static final String PAYMENT_AUTHORIZED = "persistent://public/default/payment-authorized";
    public static final String PAYMENT_FAILED = "persistent://public/default/payment-failed";
    public static final String PAYMENT_REFUNDED = "persistent://public/default/payment-refunded";

    // Inventory events
    public static final String INVENTORY_RESERVED = "persistent://public/default/inventory-reserved";
    public static final String INVENTORY_RESERVATION_FAILED = "persistent://public/default/inventory-reservation-failed";
    public static final String INVENTORY_RELEASED = "persistent://public/default/inventory-released";

    // Saga events (for orchestrator communication)
    public static final String SAGA_STEP_COMPLETED = "persistent://public/default/saga-step-completed";
    public static final String SAGA_STEP_FAILED = "persistent://public/default/saga-step-failed";
    public static final String SAGA_COMPLETED = "persistent://public/default/saga-completed";
    public static final String SAGA_FAILED = "persistent://public/default/saga-failed";
}
