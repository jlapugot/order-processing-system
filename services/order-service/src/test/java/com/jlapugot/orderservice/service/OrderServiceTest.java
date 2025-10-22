package com.jlapugot.orderservice.service;

import com.jlapugot.common.exceptions.BusinessException;
import com.jlapugot.common.exceptions.ResourceNotFoundException;
import com.jlapugot.orderservice.dto.CreateOrderRequest;
import com.jlapugot.orderservice.dto.OrderResponse;
import com.jlapugot.orderservice.dto.UpdateOrderStatusRequest;
import com.jlapugot.common.events.OrderEvent;
import com.jlapugot.common.model.OrderStatus;
import com.jlapugot.orderservice.model.Order;
import com.jlapugot.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Order Service Tests")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    private CreateOrderRequest createOrderRequest;
    private Order order;
    private OrderResponse orderResponse;

    @BeforeEach
    void setUp() {
        // Setup test data
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

        order = Order.builder()
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
    @DisplayName("Should create order successfully")
    void shouldCreateOrderSuccessfully() {
        // Given
        when(orderMapper.toEntity(createOrderRequest)).thenReturn(order);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);

        // When
        OrderResponse result = orderService.createOrder(createOrderRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCustomerName()).isEqualTo("John Doe");
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);

        verify(orderRepository).save(any(Order.class));
        verify(eventPublisher).publishEvent(eq("order.created"), eq("1"), any(OrderEvent.class));
    }

    @Test
    @DisplayName("Should throw exception when creating order with invalid quantity")
    void shouldThrowExceptionWhenCreatingOrderWithInvalidQuantity() {
        // Given
        createOrderRequest.setQuantity(0);

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(createOrderRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Quantity must be greater than zero");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Should get order by ID successfully")
    void shouldGetOrderByIdSuccessfully() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);

        // When
        OrderResponse result = orderService.getOrderById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(orderRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when order not found")
    void shouldThrowResourceNotFoundExceptionWhenOrderNotFound() {
        // Given
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.getOrderById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found");

        verify(orderRepository).findById(999L);
    }

    @Test
    @DisplayName("Should get all orders with pagination")
    void shouldGetAllOrdersWithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(List.of(order));
        when(orderRepository.findAll(pageable)).thenReturn(orderPage);
        when(orderMapper.toResponse(any(Order.class))).thenReturn(orderResponse);

        // When
        Page<OrderResponse> result = orderService.getAllOrders(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findAll(pageable);
    }

    @Test
    @DisplayName("Should update order status successfully")
    void shouldUpdateOrderStatusSuccessfully() {
        // Given
        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.CONFIRMED)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponse(any(Order.class))).thenReturn(orderResponse);

        // When
        OrderResponse result = orderService.updateOrderStatus(1L, request);

        // Then
        assertThat(result).isNotNull();
        verify(orderRepository).save(any(Order.class));
        verify(eventPublisher).publishEvent(eq("order.updated"), eq("1"), any(OrderEvent.class));
    }

    @Test
    @DisplayName("Should throw exception for invalid status transition")
    void shouldThrowExceptionForInvalidStatusTransition() {
        // Given
        order.setStatus(OrderStatus.DELIVERED);
        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.PENDING)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid status transition");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Should cancel order successfully")
    void shouldCancelOrderSuccessfully() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponse(any(Order.class))).thenReturn(orderResponse);

        // When
        OrderResponse result = orderService.cancelOrder(1L);

        // Then
        assertThat(result).isNotNull();

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(eventPublisher).publishEvent(eq("order.cancelled"), eq("1"), any(OrderEvent.class));
    }

    @Test
    @DisplayName("Should throw exception when cancelling non-cancellable order")
    void shouldThrowExceptionWhenCancellingNonCancellableOrder() {
        // Given
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot be cancelled");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Should get orders by customer ID")
    void shouldGetOrdersByCustomerId() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(List.of(order));
        when(orderRepository.findByCustomerId(1L, pageable)).thenReturn(orderPage);
        when(orderMapper.toResponse(any(Order.class))).thenReturn(orderResponse);

        // When
        Page<OrderResponse> result = orderService.getOrdersByCustomerId(1L, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(orderRepository).findByCustomerId(1L, pageable);
    }

    @Test
    @DisplayName("Should get orders by status")
    void shouldGetOrdersByStatus() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(List.of(order));
        when(orderRepository.findByStatus(OrderStatus.PENDING, pageable)).thenReturn(orderPage);
        when(orderMapper.toResponse(any(Order.class))).thenReturn(orderResponse);

        // When
        Page<OrderResponse> result = orderService.getOrdersByStatus(OrderStatus.PENDING, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(orderRepository).findByStatus(OrderStatus.PENDING, pageable);
    }
}
