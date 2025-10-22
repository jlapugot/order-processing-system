package com.jlapugot.notificationservice.consumer;

import com.jlapugot.common.events.OrderEvent;
import com.jlapugot.common.model.OrderStatus;
import com.jlapugot.notificationservice.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Order Event Consumer Tests")
class OrderEventConsumerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private OrderEventConsumer orderEventConsumer;

    private OrderEvent orderCreatedEvent;
    private OrderEvent orderUpdatedEvent;
    private OrderEvent orderCancelledEvent;

    @BeforeEach
    void setUp() {
        orderCreatedEvent = OrderEvent.builder()
                .orderId(1L)
                .customerId(100L)
                .customerEmail("customer@example.com")
                .productId(200L)
                .quantity(10)
                .status(OrderStatus.PENDING)
                .correlationId("corr-123")
                .build();

        orderUpdatedEvent = OrderEvent.builder()
                .orderId(1L)
                .customerId(100L)
                .customerEmail("customer@example.com")
                .previousStatus(OrderStatus.PENDING)
                .status(OrderStatus.CONFIRMED)
                .correlationId("corr-124")
                .build();

        orderCancelledEvent = OrderEvent.builder()
                .orderId(1L)
                .customerId(100L)
                .customerEmail("customer@example.com")
                .status(OrderStatus.CANCELLED)
                .correlationId("corr-125")
                .build();
    }

    @Test
    @DisplayName("Should handle order created event successfully")
    void shouldHandleOrderCreatedEvent() {
        // When
        orderEventConsumer.handleOrderCreated(orderCreatedEvent, "key", 0, 0L, acknowledgment);

        // Then
        verify(notificationService).sendOrderCreatedNotification(
                1L,
                "customer@example.com",
                200L,
                10
        );
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should handle order updated event successfully")
    void shouldHandleOrderUpdatedEvent() {
        // When
        orderEventConsumer.handleOrderUpdated(orderUpdatedEvent, "key", 0, 0L, acknowledgment);

        // Then
        verify(notificationService).handleOrderStatusChange(
                1L,
                "customer@example.com",
                OrderStatus.PENDING,
                OrderStatus.CONFIRMED
        );
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should handle order cancelled event successfully")
    void shouldHandleOrderCancelled() {
        // When
        orderEventConsumer.handleOrderCancelled(orderCancelledEvent, "key", 0, 0L, acknowledgment);

        // Then
        verify(notificationService).sendOrderCancelledNotification(
                1L,
                "customer@example.com"
        );
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should not acknowledge when order created event processing fails")
    void shouldNotAcknowledgeWhenOrderCreatedFails() {
        // Given
        doThrow(new RuntimeException("Email service error"))
                .when(notificationService).sendOrderCreatedNotification(anyLong(), anyString(), anyLong(), anyInt());

        // When & Then
        try {
            orderEventConsumer.handleOrderCreated(orderCreatedEvent, "key", 0, 0L, acknowledgment);
        } catch (RuntimeException e) {
            // Expected
        }

        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    @DisplayName("Should not acknowledge when order updated event processing fails")
    void shouldNotAcknowledgeWhenOrderUpdatedFails() {
        // Given
        doThrow(new RuntimeException("Email service error"))
                .when(notificationService).handleOrderStatusChange(anyLong(), anyString(), any(), any());

        // When & Then
        try {
            orderEventConsumer.handleOrderUpdated(orderUpdatedEvent, "key", 0, 0L, acknowledgment);
        } catch (RuntimeException e) {
            // Expected
        }

        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    @DisplayName("Should not acknowledge when order cancelled event processing fails")
    void shouldNotAcknowledgeWhenOrderCancelledFails() {
        // Given
        doThrow(new RuntimeException("Email service error"))
                .when(notificationService).sendOrderCancelledNotification(anyLong(), anyString());

        // When & Then
        try {
            orderEventConsumer.handleOrderCancelled(orderCancelledEvent, "key", 0, 0L, acknowledgment);
        } catch (RuntimeException e) {
            // Expected
        }

        verify(acknowledgment, never()).acknowledge();
    }
}
