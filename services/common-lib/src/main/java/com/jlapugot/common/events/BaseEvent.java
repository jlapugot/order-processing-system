package com.jlapugot.common.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for all domain events in the system.
 * Provides common fields like event ID, timestamp, and correlation ID for distributed tracing.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEvent {

    /**
     * Unique identifier for this event instance
     */
    private String eventId;

    /**
     * Type of the event (e.g., ORDER_CREATED, INVENTORY_RESERVED)
     */
    private String eventType;

    /**
     * Timestamp when the event was created
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp;

    /**
     * Correlation ID for tracing requests across services
     */
    private String correlationId;

    /**
     * Version of the event schema for backward compatibility
     */
    private String version;

    /**
     * Initialize event with default values
     */
    protected void initializeEvent(String eventType) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.timestamp = LocalDateTime.now();
        this.version = "1.0";
        if (this.correlationId == null) {
            this.correlationId = UUID.randomUUID().toString();
        }
    }
}
