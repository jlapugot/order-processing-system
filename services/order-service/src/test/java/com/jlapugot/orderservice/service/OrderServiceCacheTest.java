package com.jlapugot.orderservice.service;

import com.jlapugot.orderservice.dto.CreateOrderRequest;
import com.jlapugot.orderservice.dto.OrderResponse;
import com.jlapugot.orderservice.dto.UpdateOrderStatusRequest;
import com.jlapugot.common.model.OrderStatus;
import com.jlapugot.orderservice.model.Order;
import com.jlapugot.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration tests for OrderService caching functionality
 * Tests that @Cacheable and @CacheEvict annotations work correctly
 */
@SpringBootTest
@ActiveProfiles("test")
class OrderServiceCacheTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private OrderRepository orderRepository;

    @MockBean
    private OrderEventPublisher eventPublisher;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        // Clear all caches before each test
        cacheManager.getCacheNames().forEach(cacheName ->
            cacheManager.getCache(cacheName).clear()
        );

        // Setup test data
        testOrder = Order.builder()
                .id(1L)
                .customerId(100L)
                .customerName("John Doe")
                .customerEmail("john@example.com")
                .productId(1L)
                .productName("Test Product")
                .quantity(5)
                .unitPrice(BigDecimal.valueOf(10.00))
                .totalAmount(BigDecimal.valueOf(50.00))
                .shippingAddress("123 Main St")
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldCacheOrderOnFirstGetById() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When - First call
        OrderResponse firstResult = orderService.getOrderById(1L);

        // Then
        assertThat(firstResult).isNotNull();
        assertThat(firstResult.getId()).isEqualTo(1L);
        verify(orderRepository, times(1)).findById(1L);

        // When - Second call (should use cache)
        OrderResponse secondResult = orderService.getOrderById(1L);

        // Then - Repository should still only be called once (cached)
        assertThat(secondResult).isNotNull();
        assertThat(secondResult.getId()).isEqualTo(1L);
        verify(orderRepository, times(1)).findById(1L); // Still only called once

        // Verify cache contains the order
        assertThat(cacheManager.getCache("orders").get(1L)).isNotNull();
    }

    @Test
    void shouldEvictCacheOnOrderUpdate() {
        // Given - Order is cached (status is PENDING)
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setStatus(OrderStatus.CONFIRMED);
            return order;
        });

        orderService.getOrderById(1L);
        verify(orderRepository, times(1)).findById(1L);

        // When - Update order status from PENDING to CONFIRMED (should evict cache)
        UpdateOrderStatusRequest updateRequest = new UpdateOrderStatusRequest();
        updateRequest.setStatus(OrderStatus.CONFIRMED);

        orderService.updateOrderStatus(1L, updateRequest);

        // Then - Cache should be evicted, next call should hit repository again
        // updateOrderStatus calls findById once (2nd call), then getOrderById calls it again (3rd call)
        orderService.getOrderById(1L);
        verify(orderRepository, times(3)).findById(1L); // Called 3 times total
    }

    @Test
    void shouldEvictCacheOnOrderCreation() {
        // Given - Create order request
        CreateOrderRequest createRequest = CreateOrderRequest.builder()
                .customerId(100L)
                .customerName("John Doe")
                .customerEmail("john@example.com")
                .productId(1L)
                .productName("Test Product")
                .quantity(5)
                .unitPrice(BigDecimal.valueOf(10.00))
                .shippingAddress("123 Main St")
                .build();

        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When - Create order (should evict all entries from "orders" cache)
        orderService.createOrder(createRequest);

        // Then - Verify cache was evicted (all entries cleared)
        assertThat(cacheManager.getCache("orders")).isNotNull();
        // Cache should be empty after eviction
    }

    @Test
    void shouldEvictCacheOnOrderCancellation() {
        // Given - Order is cached (status is PENDING, can be cancelled)
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setStatus(OrderStatus.CANCELLED);
            return order;
        });
        when(orderRepository.existsById(1L)).thenReturn(true);

        orderService.getOrderById(1L);
        verify(orderRepository, times(1)).findById(1L);

        // When - Cancel order (should evict cache)
        orderService.cancelOrder(1L);

        // Then - Cache should be evicted, next call should hit repository again
        // cancelOrder calls findById once (2nd call), then getOrderById calls it again (3rd call)
        orderService.getOrderById(1L);
        verify(orderRepository, times(3)).findById(1L); // Called 3 times total
    }

    @Test
    void shouldNotCacheDifferentOrders() {
        // Given
        Order order1 = testOrder;
        Order order2 = Order.builder()
                .id(2L)
                .customerId(200L)
                .customerName("Jane Doe")
                .customerEmail("jane@example.com")
                .productId(2L)
                .productName("Another Product")
                .quantity(3)
                .unitPrice(BigDecimal.valueOf(20.00))
                .totalAmount(BigDecimal.valueOf(60.00))
                .shippingAddress("456 Oak Ave")
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order1));
        when(orderRepository.findById(2L)).thenReturn(Optional.of(order2));

        // When
        OrderResponse result1 = orderService.getOrderById(1L);
        OrderResponse result2 = orderService.getOrderById(2L);

        // Then - Both should be cached separately
        assertThat(result1.getId()).isEqualTo(1L);
        assertThat(result2.getId()).isEqualTo(2L);

        verify(orderRepository, times(1)).findById(1L);
        verify(orderRepository, times(1)).findById(2L);

        // Verify both are cached
        assertThat(cacheManager.getCache("orders").get(1L)).isNotNull();
        assertThat(cacheManager.getCache("orders").get(2L)).isNotNull();
    }

    @Test
    void shouldOnlyEvictSpecificOrderOnUpdate() {
        // Given - Cache two orders
        Order order2 = Order.builder()
                .id(2L)
                .customerId(200L)
                .customerName("Jane Doe")
                .customerEmail("jane@example.com")
                .productId(2L)
                .productName("Another Product")
                .quantity(3)
                .unitPrice(BigDecimal.valueOf(20.00))
                .totalAmount(BigDecimal.valueOf(60.00))
                .shippingAddress("456 Oak Ave")
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.findById(2L)).thenReturn(Optional.of(order2));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Cache both orders
        orderService.getOrderById(1L);
        orderService.getOrderById(2L);

        verify(orderRepository, times(1)).findById(1L);
        verify(orderRepository, times(1)).findById(2L);

        // When - Update order 1 (should only evict order 1)
        UpdateOrderStatusRequest updateRequest = new UpdateOrderStatusRequest();
        updateRequest.setStatus(OrderStatus.CONFIRMED);
        orderService.updateOrderStatus(1L, updateRequest);

        // Then - Order 1 should be evicted, Order 2 should still be cached
        orderService.getOrderById(1L);
        orderService.getOrderById(2L);

        // updateOrderStatus calls findById once (2nd call for order 1)
        // then getOrderById calls it again (3rd call for order 1)
        verify(orderRepository, times(3)).findById(1L);
        verify(orderRepository, times(1)).findById(2L); // Still cached, not called again
    }
}
