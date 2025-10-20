package com.jlapugot.common.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for date and time operations.
 * Provides consistent date/time formatting across all services.
 */
public class DateTimeUtils {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final ZoneId UTC_ZONE = ZoneId.of("UTC");

    private DateTimeUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Get current UTC timestamp
     *
     * @return current LocalDateTime in UTC
     */
    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(UTC_ZONE);
    }

    /**
     * Format a LocalDateTime to ISO-8601 string
     *
     * @param dateTime the date time to format
     * @return formatted string
     */
    public static String formatIso(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(ISO_FORMATTER) : null;
    }

    /**
     * Parse an ISO-8601 string to LocalDateTime
     *
     * @param dateTimeString the string to parse
     * @return parsed LocalDateTime
     */
    public static LocalDateTime parseIso(String dateTimeString) {
        return dateTimeString != null ? LocalDateTime.parse(dateTimeString, ISO_FORMATTER) : null;
    }

    /**
     * Convert LocalDateTime to ZonedDateTime in UTC
     *
     * @param dateTime the date time to convert
     * @return ZonedDateTime in UTC
     */
    public static ZonedDateTime toUtcZoned(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.atZone(UTC_ZONE) : null;
    }
}
