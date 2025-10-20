package com.jlapugot.orderservice.event;

import com.jlapugot.common.events.BaseEvent;
import com.jlapugot.orderservice.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Domain event representing order state changes.
 * Published to Kafka for other services to consume.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderEvent extends BaseEvent {

    private Long orderId;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private OrderStatus previousStatus;
    private String shippingAddress;

    /**
     * Create an order created event
     */
    public static OrderEvent created(Long orderId, Long customerId, String customerName,
                                    String customerEmail, Long productId, String productName,
                                    Integer quantity, BigDecimal unitPrice, BigDecimal totalAmount,
                                    String shippingAddress, String correlationId) {
        OrderEvent event = OrderEvent.builder()
                .orderId(orderId)
                .customerId(customerId)
                .customerName(customerName)
                .customerEmail(customerEmail)
                .productId(productId)
                .productName(productName)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING)
                .shippingAddress(shippingAddress)
                .correlationId(correlationId)
                .build();
        event.initializeEvent("order.created");
        return event;
    }

    /**
     * Create an order status updated event
     */
    public static OrderEvent statusUpdated(Long orderId, OrderStatus previousStatus,
                                          OrderStatus newStatus, String correlationId) {
        OrderEvent event = OrderEvent.builder()
                .orderId(orderId)
                .status(newStatus)
                .previousStatus(previousStatus)
                .correlationId(correlationId)
                .build();
        event.initializeEvent("order.updated");
        return event;
    }

    /**
     * Create an order cancelled event
     */
    public static OrderEvent cancelled(Long orderId, String correlationId) {
        OrderEvent event = OrderEvent.builder()
                .orderId(orderId)
                .status(OrderStatus.CANCELLED)
                .correlationId(correlationId)
                .build();
        event.initializeEvent("order.cancelled");
        return event;
    }
}
