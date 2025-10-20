package com.jlapugot.orderservice.model;

/**
 * Enumeration of possible order states in the order lifecycle.
 */
public enum OrderStatus {
    /**
     * Order has been created but not yet confirmed
     */
    PENDING,

    /**
     * Order has been confirmed and payment is being processed
     */
    CONFIRMED,

    /**
     * Payment has been completed successfully
     */
    PAID,

    /**
     * Order is being prepared for shipment
     */
    PROCESSING,

    /**
     * Order has been shipped to the customer
     */
    SHIPPED,

    /**
     * Order has been delivered to the customer
     */
    DELIVERED,

    /**
     * Order has been cancelled by customer or system
     */
    CANCELLED,

    /**
     * Order failed due to payment or inventory issues
     */
    FAILED
}
