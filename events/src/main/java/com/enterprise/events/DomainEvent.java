package com.enterprise.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for all domain events.
 * Provides common metadata for event tracing and correlation.
 */
public abstract class DomainEvent {

    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private String correlationId;
    private String sagaInstanceId;

    protected DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
    }

    protected DomainEvent(String correlationId, String sagaInstanceId) {
        this();
        this.correlationId = correlationId;
        this.sagaInstanceId = sagaInstanceId;
    }

    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getCorrelationId() { return correlationId; }
    public String getSagaInstanceId() { return sagaInstanceId; }

    protected void setEventType(String eventType) { this.eventType = eventType; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public void setSagaInstanceId(String sagaInstanceId) { this.sagaInstanceId = sagaInstanceId; }
}
