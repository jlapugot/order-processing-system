package com.jlapugot.orderservice.controller;

import com.jlapugot.orderservice.dto.CreateOrderRequest;
import com.jlapugot.orderservice.dto.OrderResponse;
import com.jlapugot.orderservice.dto.UpdateOrderStatusRequest;
import com.jlapugot.common.model.OrderStatus;
import com.jlapugot.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Order operations.
 * Exposes HTTP endpoints for managing orders with proper validation and documentation.
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Orders", description = "Order management API")
public class OrderController {

    private final OrderService orderService;

    /**
     * Create a new order
     */
    @PostMapping
    @Operation(summary = "Create a new order", description = "Creates a new order with the provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        log.info("REST request to create order for customer: {}", request.getCustomerId());

        OrderResponse response = orderService.createOrder(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Get order by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID", description = "Retrieves an order by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order found"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<OrderResponse> getOrderById(
            @Parameter(description = "Order ID", required = true)
            @PathVariable Long id) {
        log.info("REST request to get order: {}", id);

        OrderResponse response = orderService.getOrderById(id);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all orders with pagination
     */
    @GetMapping
    @Operation(summary = "Get all orders", description = "Retrieves all orders with pagination support")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Page<OrderResponse>> getAllOrders(
            @Parameter(description = "Pagination parameters")
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        log.info("REST request to get all orders - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<OrderResponse> response = orderService.getAllOrders(pageable);

        return ResponseEntity.ok(response);
    }

    /**
     * Get orders by customer ID
     */
    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get orders by customer", description = "Retrieves all orders for a specific customer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Page<OrderResponse>> getOrdersByCustomerId(
            @Parameter(description = "Customer ID", required = true)
            @PathVariable Long customerId,
            @Parameter(description = "Pagination parameters")
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        log.info("REST request to get orders for customer: {}", customerId);

        Page<OrderResponse> response = orderService.getOrdersByCustomerId(customerId, pageable);

        return ResponseEntity.ok(response);
    }

    /**
     * Get orders by status
     */
    @GetMapping("/status/{status}")
    @Operation(summary = "Get orders by status", description = "Retrieves all orders with a specific status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Page<OrderResponse>> getOrdersByStatus(
            @Parameter(description = "Order status", required = true)
            @PathVariable OrderStatus status,
            @Parameter(description = "Pagination parameters")
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        log.info("REST request to get orders with status: {}", status);

        Page<OrderResponse> response = orderService.getOrdersByStatus(status, pageable);

        return ResponseEntity.ok(response);
    }

    /**
     * Update order status
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "Update order status", description = "Updates the status of an existing order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @Parameter(description = "Order ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        log.info("REST request to update order {} status to: {}", id, request.getStatus());

        OrderResponse response = orderService.updateOrderStatus(id, request);

        return ResponseEntity.ok(response);
    }

    /**
     * Cancel an order
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel order", description = "Cancels an existing order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Order cannot be cancelled"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<OrderResponse> cancelOrder(
            @Parameter(description = "Order ID", required = true)
            @PathVariable Long id) {
        log.info("REST request to cancel order: {}", id);

        OrderResponse response = orderService.cancelOrder(id);

        return ResponseEntity.ok(response);
    }
}
