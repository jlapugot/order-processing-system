package com.jlapugot.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlapugot.common.exceptions.ResourceNotFoundException;
import com.jlapugot.orderservice.dto.CreateOrderRequest;
import com.jlapugot.orderservice.dto.OrderResponse;
import com.jlapugot.orderservice.dto.UpdateOrderStatusRequest;
import com.jlapugot.orderservice.model.OrderStatus;
import com.jlapugot.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Order Controller Tests")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    private CreateOrderRequest createOrderRequest;
    private OrderResponse orderResponse;

    @BeforeEach
    void setUp() {
        createOrderRequest = CreateOrderRequest.builder()
                .customerId(1L)
                .customerName("John Doe")
                .customerEmail("john@example.com")
                .productId(100L)
                .productName("Test Product")
                .quantity(2)
                .unitPrice(new BigDecimal("99.99"))
                .shippingAddress("123 Test St, Test City, TC 12345")
                .notes("Test order")
                .build();

        orderResponse = OrderResponse.builder()
                .id(1L)
                .customerId(1L)
                .customerName("John Doe")
                .customerEmail("john@example.com")
                .productId(100L)
                .productName("Test Product")
                .quantity(2)
                .unitPrice(new BigDecimal("99.99"))
                .totalAmount(new BigDecimal("199.98"))
                .status(OrderStatus.PENDING)
                .shippingAddress("123 Test St, Test City, TC 12345")
                .notes("Test order")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/orders - Should create order successfully")
    void shouldCreateOrderSuccessfully() throws Exception {
        // Given
        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(orderResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.customerName").value("John Doe"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(199.98));
    }

    @Test
    @DisplayName("POST /api/v1/orders - Should return 400 for invalid request")
    void shouldReturn400ForInvalidRequest() throws Exception {
        // Given - invalid request with missing required fields
        createOrderRequest.setCustomerName(null);

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/orders/{id} - Should get order by ID")
    void shouldGetOrderById() throws Exception {
        // Given
        when(orderService.getOrderById(1L)).thenReturn(orderResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/orders/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.customerName").value("John Doe"));
    }

    @Test
    @DisplayName("GET /api/v1/orders/{id} - Should return 404 when order not found")
    void shouldReturn404WhenOrderNotFound() throws Exception {
        // Given
        when(orderService.getOrderById(999L))
                .thenThrow(new ResourceNotFoundException("Order", "id", 999L));

        // When & Then
        mockMvc.perform(get("/api/v1/orders/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/orders - Should get all orders with pagination")
    void shouldGetAllOrdersWithPagination() throws Exception {
        // Given
        when(orderService.getAllOrders(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(orderResponse)));

        // When & Then
        mockMvc.perform(get("/api/v1/orders")
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/orders/customer/{customerId} - Should get orders by customer")
    void shouldGetOrdersByCustomerId() throws Exception {
        // Given
        when(orderService.getOrdersByCustomerId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(orderResponse)));

        // When & Then
        mockMvc.perform(get("/api/v1/orders/customer/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].customerId").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/orders/status/{status} - Should get orders by status")
    void shouldGetOrdersByStatus() throws Exception {
        // Given
        when(orderService.getOrdersByStatus(eq(OrderStatus.PENDING), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(orderResponse)));

        // When & Then
        mockMvc.perform(get("/api/v1/orders/status/PENDING")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("PUT /api/v1/orders/{id}/status - Should update order status")
    void shouldUpdateOrderStatus() throws Exception {
        // Given
        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.CONFIRMED)
                .build();

        orderResponse.setStatus(OrderStatus.CONFIRMED);
        when(orderService.updateOrderStatus(eq(1L), any(UpdateOrderStatusRequest.class)))
                .thenReturn(orderResponse);

        // When & Then
        mockMvc.perform(put("/api/v1/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("DELETE /api/v1/orders/{id} - Should cancel order")
    void shouldCancelOrder() throws Exception {
        // Given
        orderResponse.setStatus(OrderStatus.CANCELLED);
        when(orderService.cancelOrder(1L)).thenReturn(orderResponse);

        // When & Then
        mockMvc.perform(delete("/api/v1/orders/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
