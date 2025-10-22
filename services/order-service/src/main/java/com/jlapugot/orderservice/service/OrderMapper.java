package com.jlapugot.orderservice.service;

import com.jlapugot.orderservice.dto.CreateOrderRequest;
import com.jlapugot.orderservice.dto.OrderResponse;
import com.jlapugot.orderservice.model.Order;
import com.jlapugot.common.model.OrderStatus;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Order entity and DTOs.
 * Using manual mapping instead of MapStruct for simplicity in this example.
 */
@Component
public class OrderMapper {

    /**
     * Convert CreateOrderRequest DTO to Order entity
     *
     * @param request the create order request
     * @return new Order entity
     */
    public Order toEntity(CreateOrderRequest request) {
        return Order.builder()
                .customerId(request.getCustomerId())
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .productId(request.getProductId())
                .productName(request.getProductName())
                .quantity(request.getQuantity())
                .unitPrice(request.getUnitPrice())
                .shippingAddress(request.getShippingAddress())
                .notes(request.getNotes())
                .status(OrderStatus.PENDING)
                .build();
    }

    /**
     * Convert Order entity to OrderResponse DTO
     *
     * @param order the order entity
     * @return OrderResponse DTO
     */
    public OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .customerName(order.getCustomerName())
                .customerEmail(order.getCustomerEmail())
                .productId(order.getProductId())
                .productName(order.getProductName())
                .quantity(order.getQuantity())
                .unitPrice(order.getUnitPrice())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .shippingAddress(order.getShippingAddress())
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
