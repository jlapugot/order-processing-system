package com.jlapugot.notificationservice.service;

import com.jlapugot.common.model.OrderStatus;
import com.jlapugot.notificationservice.model.Notification;
import com.jlapugot.notificationservice.model.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Notification Service Tests")
class NotificationServiceTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationService notificationService;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    private Long orderId;
    private String customerEmail;

    @BeforeEach
    void setUp() {
        orderId = 1L;
        customerEmail = "customer@example.com";
    }

    @Test
    @DisplayName("Should send order created notification")
    void shouldSendOrderCreatedNotification() {
        // Given
        Long productId = 100L;
        Integer quantity = 10;

        // When
        notificationService.sendOrderCreatedNotification(orderId, customerEmail, productId, quantity);

        // Then
        verify(emailService).sendEmail(notificationCaptor.capture());
        Notification captured = notificationCaptor.getValue();

        assertThat(captured.getOrderId()).isEqualTo(orderId);
        assertThat(captured.getCustomerEmail()).isEqualTo(customerEmail);
        assertThat(captured.getType()).isEqualTo(NotificationType.ORDER_CREATED);
        assertThat(captured.getMessage()).contains("Order #1", "created successfully");
    }

    @Test
    @DisplayName("Should send order confirmed notification")
    void shouldSendOrderConfirmedNotification() {
        // When
        notificationService.sendOrderConfirmedNotification(orderId, customerEmail);

        // Then
        verify(emailService).sendEmail(notificationCaptor.capture());
        Notification captured = notificationCaptor.getValue();

        assertThat(captured.getOrderId()).isEqualTo(orderId);
        assertThat(captured.getCustomerEmail()).isEqualTo(customerEmail);
        assertThat(captured.getType()).isEqualTo(NotificationType.ORDER_CONFIRMED);
        assertThat(captured.getMessage()).contains("Order #1", "confirmed");
    }

    @Test
    @DisplayName("Should send order shipped notification")
    void shouldSendOrderShippedNotification() {
        // When
        notificationService.sendOrderShippedNotification(orderId, customerEmail);

        // Then
        verify(emailService).sendEmail(notificationCaptor.capture());
        Notification captured = notificationCaptor.getValue();

        assertThat(captured.getOrderId()).isEqualTo(orderId);
        assertThat(captured.getCustomerEmail()).isEqualTo(customerEmail);
        assertThat(captured.getType()).isEqualTo(NotificationType.ORDER_SHIPPED);
        assertThat(captured.getMessage()).contains("Order #1", "shipped");
    }

    @Test
    @DisplayName("Should send order cancelled notification")
    void shouldSendOrderCancelledNotification() {
        // When
        notificationService.sendOrderCancelledNotification(orderId, customerEmail);

        // Then
        verify(emailService).sendEmail(notificationCaptor.capture());
        Notification captured = notificationCaptor.getValue();

        assertThat(captured.getOrderId()).isEqualTo(orderId);
        assertThat(captured.getCustomerEmail()).isEqualTo(customerEmail);
        assertThat(captured.getType()).isEqualTo(NotificationType.ORDER_CANCELLED);
        assertThat(captured.getMessage()).contains("Order #1", "cancelled");
    }

    @Test
    @DisplayName("Should send order failed notification")
    void shouldSendOrderFailedNotification() {
        // When
        notificationService.sendOrderFailedNotification(orderId, customerEmail, "Insufficient stock");

        // Then
        verify(emailService).sendEmail(notificationCaptor.capture());
        Notification captured = notificationCaptor.getValue();

        assertThat(captured.getOrderId()).isEqualTo(orderId);
        assertThat(captured.getCustomerEmail()).isEqualTo(customerEmail);
        assertThat(captured.getType()).isEqualTo(NotificationType.ORDER_FAILED);
        assertThat(captured.getMessage()).contains("Order #1", "failed", "Insufficient stock");
    }

    @Test
    @DisplayName("Should handle order status change from PENDING to CONFIRMED")
    void shouldHandleOrderStatusChangeToConfirmed() {
        // When
        notificationService.handleOrderStatusChange(orderId, customerEmail, OrderStatus.PENDING, OrderStatus.CONFIRMED);

        // Then
        verify(emailService).sendEmail(any(Notification.class));
    }

    @Test
    @DisplayName("Should handle order status change from CONFIRMED to SHIPPED")
    void shouldHandleOrderStatusChangeToShipped() {
        // When
        notificationService.handleOrderStatusChange(orderId, customerEmail, OrderStatus.CONFIRMED, OrderStatus.SHIPPED);

        // Then
        verify(emailService).sendEmail(any(Notification.class));
    }

    @Test
    @DisplayName("Should handle order status change to CANCELLED")
    void shouldHandleOrderStatusChangeToCancelled() {
        // When
        notificationService.handleOrderStatusChange(orderId, customerEmail, OrderStatus.PENDING, OrderStatus.CANCELLED);

        // Then
        verify(emailService).sendEmail(any(Notification.class));
    }

    @Test
    @DisplayName("Should not send notification for irrelevant status changes")
    void shouldNotSendNotificationForIrrelevantStatusChanges() {
        // When - status change that doesn't require notification
        notificationService.handleOrderStatusChange(orderId, customerEmail, OrderStatus.PENDING, OrderStatus.PENDING);

        // Then - no email should be sent
        verify(emailService, never()).sendEmail(any(Notification.class));
    }

    @Test
    @DisplayName("Should skip duplicate notification for same order and type")
    void shouldSkipDuplicateNotification() {
        // Given - send first notification
        notificationService.sendOrderConfirmedNotification(orderId, customerEmail);

        // When - attempt to send duplicate
        notificationService.sendOrderConfirmedNotification(orderId, customerEmail);

        // Then - email service should be called only once
        verify(emailService, times(1)).sendEmail(any(Notification.class));
    }
}
