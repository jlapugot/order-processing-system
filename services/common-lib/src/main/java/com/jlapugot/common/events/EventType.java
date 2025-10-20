package com.jlapugot.common.events;

/**
 * Enumeration of all event types in the system.
 * Maintains a centralized registry of events for better discoverability.
 */
public enum EventType {
    // Order Events
    ORDER_CREATED("order.created"),
    ORDER_UPDATED("order.updated"),
    ORDER_CANCELLED("order.cancelled"),
    ORDER_COMPLETED("order.completed"),

    // Inventory Events
    INVENTORY_RESERVED("inventory.reserved"),
    INVENTORY_RELEASED("inventory.released"),
    INVENTORY_UNAVAILABLE("inventory.unavailable"),

    // Notification Events
    NOTIFICATION_EMAIL("notification.email"),
    NOTIFICATION_SMS("notification.sms");

    private final String value;

    EventType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
