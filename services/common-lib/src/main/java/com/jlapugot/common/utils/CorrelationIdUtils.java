package com.jlapugot.common.utils;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * Utility class for managing correlation IDs in distributed systems.
 * Uses SLF4J MDC (Mapped Diagnostic Context) for thread-safe correlation ID storage.
 */
public class CorrelationIdUtils {

    private static final String CORRELATION_ID_KEY = "correlationId";

    private CorrelationIdUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Generate a new correlation ID and set it in MDC
     *
     * @return the generated correlation ID
     */
    public static String generateAndSet() {
        String correlationId = UUID.randomUUID().toString();
        MDC.put(CORRELATION_ID_KEY, correlationId);
        return correlationId;
    }

    /**
     * Set a specific correlation ID in MDC
     *
     * @param correlationId the correlation ID to set
     */
    public static void set(String correlationId) {
        if (correlationId != null && !correlationId.isEmpty()) {
            MDC.put(CORRELATION_ID_KEY, correlationId);
        }
    }

    /**
     * Get the current correlation ID from MDC
     *
     * @return the correlation ID, or null if not set
     */
    public static String get() {
        return MDC.get(CORRELATION_ID_KEY);
    }

    /**
     * Get the current correlation ID, or generate a new one if not set
     *
     * @return the correlation ID
     */
    public static String getOrGenerate() {
        String correlationId = get();
        if (correlationId == null) {
            correlationId = generateAndSet();
        }
        return correlationId;
    }

    /**
     * Clear the correlation ID from MDC
     */
    public static void clear() {
        MDC.remove(CORRELATION_ID_KEY);
    }
}
