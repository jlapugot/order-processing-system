package com.jlapugot.orderservice.service;

import com.jlapugot.common.exceptions.BusinessException;
import com.jlapugot.common.exceptions.ResourceNotFoundException;
import com.jlapugot.common.utils.CorrelationIdUtils;
import com.jlapugot.orderservice.dto.CreateOrderRequest;
import com.jlapugot.orderservice.dto.OrderResponse;
import com.jlapugot.orderservice.dto.UpdateOrderStatusRequest;
import com.jlapugot.orderservice.event.OrderEvent;
import com.jlapugot.orderservice.model.Order;
import com.jlapugot.orderservice.model.OrderStatus;
import com.jlapugot.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service layer for Order operations.
 * Handles business logic, caching, and event publishing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderEventPublisher eventPublisher;

    private static final String ORDER_CREATED_TOPIC = "order.created";
    private static final String ORDER_UPDATED_TOPIC = "order.updated";
    private static final String ORDER_CANCELLED_TOPIC = "order.cancelled";

    /**
     * Create a new order
     *
     * @param request the create order request
     * @return the created order response
     */
    @CacheEvict(value = "orders", allEntries = true)
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating new order for customer: {}", request.getCustomerId());

        // Validate business rules
        validateOrderCreation(request);

        // Convert DTO to entity
        Order order = orderMapper.toEntity(request);

        // Save to database
        Order savedOrder = orderRepository.save(order);
        log.info("Order created with ID: {}", savedOrder.getId());

        // Publish event to Kafka
        publishOrderCreatedEvent(savedOrder);

        return orderMapper.toResponse(savedOrder);
    }

    /**
     * Get order by ID
     *
     * @param id the order ID
     * @return the order response
     * @throws ResourceNotFoundException if order not found
     */
    @Cacheable(value = "orders", key = "#id")
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        log.info("Fetching order with ID: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));

        return orderMapper.toResponse(order);
    }

    /**
     * Get all orders with pagination
     *
     * @param pageable pagination information
     * @return page of orders
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        log.info("Fetching all orders - Page: {}, Size: {}", pageable.getPageNumber(), pageable.getPageSize());

        return orderRepository.findAll(pageable)
                .map(orderMapper::toResponse);
    }

    /**
     * Get orders by customer ID
     *
     * @param customerId the customer ID
     * @param pageable pagination information
     * @return page of orders
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByCustomerId(Long customerId, Pageable pageable) {
        log.info("Fetching orders for customer: {}", customerId);

        return orderRepository.findByCustomerId(customerId, pageable)
                .map(orderMapper::toResponse);
    }

    /**
     * Get orders by status
     *
     * @param status the order status
     * @param pageable pagination information
     * @return page of orders
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        log.info("Fetching orders with status: {}", status);

        return orderRepository.findByStatus(status, pageable)
                .map(orderMapper::toResponse);
    }

    /**
     * Update order status
     *
     * @param id the order ID
     * @param request the update status request
     * @return the updated order response
     * @throws ResourceNotFoundException if order not found
     * @throws BusinessException if status transition is invalid
     */
    @CacheEvict(value = "orders", key = "#id")
    public OrderResponse updateOrderStatus(Long id, UpdateOrderStatusRequest request) {
        log.info("Updating order {} to status: {}", id, request.getStatus());

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));

        OrderStatus previousStatus = order.getStatus();

        // Validate status transition
        validateStatusTransition(previousStatus, request.getStatus());

        // Update order
        order.setStatus(request.getStatus());
        if (request.getNotes() != null) {
            order.setNotes(request.getNotes());
        }

        Order updatedOrder = orderRepository.save(order);
        log.info("Order {} status updated from {} to {}", id, previousStatus, request.getStatus());

        // Publish event
        publishOrderStatusUpdatedEvent(updatedOrder, previousStatus);

        return orderMapper.toResponse(updatedOrder);
    }

    /**
     * Cancel an order
     *
     * @param id the order ID
     * @return the cancelled order response
     * @throws ResourceNotFoundException if order not found
     * @throws BusinessException if order cannot be cancelled
     */
    @CacheEvict(value = "orders", key = "#id")
    public OrderResponse cancelOrder(Long id) {
        log.info("Cancelling order: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));

        // Check if order can be cancelled
        if (!canBeCancelled(order.getStatus())) {
            throw new BusinessException(
                    String.format("Order with status %s cannot be cancelled", order.getStatus()),
                    "ORDER_NOT_CANCELLABLE"
            );
        }

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);

        Order cancelledOrder = orderRepository.save(order);
        log.info("Order {} cancelled successfully", id);

        // Publish cancellation event
        publishOrderCancelledEvent(cancelledOrder);

        return orderMapper.toResponse(cancelledOrder);
    }

    /**
     * Delete an order (soft delete by setting status to CANCELLED)
     *
     * @param id the order ID
     * @throws ResourceNotFoundException if order not found
     */
    @CacheEvict(value = "orders", key = "#id")
    public void deleteOrder(Long id) {
        log.info("Deleting order: {}", id);

        if (!orderRepository.existsById(id)) {
            throw new ResourceNotFoundException("Order", "id", id);
        }

        // For production, consider soft delete instead
        cancelOrder(id);
    }

    // ========== Private Helper Methods ==========

    private void validateOrderCreation(CreateOrderRequest request) {
        // Add business validation rules here
        if (request.getQuantity() <= 0) {
            throw new BusinessException("Quantity must be greater than zero", "INVALID_QUANTITY");
        }

        if (request.getUnitPrice().signum() <= 0) {
            throw new BusinessException("Unit price must be greater than zero", "INVALID_PRICE");
        }
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        // Define valid status transitions
        boolean isValid = switch (currentStatus) {
            case PENDING -> newStatus == OrderStatus.CONFIRMED || newStatus == OrderStatus.CANCELLED;
            case CONFIRMED -> newStatus == OrderStatus.PAID || newStatus == OrderStatus.CANCELLED;
            case PAID -> newStatus == OrderStatus.PROCESSING || newStatus == OrderStatus.CANCELLED;
            case PROCESSING -> newStatus == OrderStatus.SHIPPED || newStatus == OrderStatus.CANCELLED;
            case SHIPPED -> newStatus == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED, FAILED -> false; // Terminal states
        };

        if (!isValid) {
            throw new BusinessException(
                    String.format("Invalid status transition from %s to %s", currentStatus, newStatus),
                    "INVALID_STATUS_TRANSITION"
            );
        }
    }

    private boolean canBeCancelled(OrderStatus status) {
        return status == OrderStatus.PENDING ||
               status == OrderStatus.CONFIRMED ||
               status == OrderStatus.PAID ||
               status == OrderStatus.PROCESSING;
    }

    private void publishOrderCreatedEvent(Order order) {
        String correlationId = CorrelationIdUtils.getOrGenerate();

        OrderEvent event = OrderEvent.created(
                order.getId(),
                order.getCustomerId(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getProductId(),
                order.getProductName(),
                order.getQuantity(),
                order.getUnitPrice(),
                order.getTotalAmount(),
                order.getShippingAddress(),
                correlationId
        );

        eventPublisher.publishEvent(ORDER_CREATED_TOPIC, order.getId().toString(), event);
        log.info("Published order.created event for order: {}", order.getId());
    }

    private void publishOrderStatusUpdatedEvent(Order order, OrderStatus previousStatus) {
        String correlationId = CorrelationIdUtils.getOrGenerate();

        OrderEvent event = OrderEvent.statusUpdated(
                order.getId(),
                previousStatus,
                order.getStatus(),
                correlationId
        );

        eventPublisher.publishEvent(ORDER_UPDATED_TOPIC, order.getId().toString(), event);
        log.info("Published order.updated event for order: {}", order.getId());
    }

    private void publishOrderCancelledEvent(Order order) {
        String correlationId = CorrelationIdUtils.getOrGenerate();

        OrderEvent event = OrderEvent.cancelled(order.getId(), correlationId);

        eventPublisher.publishEvent(ORDER_CANCELLED_TOPIC, order.getId().toString(), event);
        log.info("Published order.cancelled event for order: {}", order.getId());
    }
}
