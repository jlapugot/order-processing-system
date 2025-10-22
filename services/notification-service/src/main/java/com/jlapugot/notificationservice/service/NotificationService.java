package com.jlapugot.notificationservice.service;

import com.jlapugot.common.model.OrderStatus;
import com.jlapugot.notificationservice.model.Notification;
import com.jlapugot.notificationservice.model.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing notifications
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final EmailService emailService;

    // Track sent notifications to prevent duplicates (order ID + notification type)
    private final Set<String> sentNotifications = ConcurrentHashMap.newKeySet();

    /**
     * Send order created notification
     */
    public void sendOrderCreatedNotification(Long orderId, String customerEmail, Long productId, Integer quantity) {
        log.info("Preparing order created notification for order {}", orderId);

        String notificationKey = buildNotificationKey(orderId, NotificationType.ORDER_CREATED);

        // Check for duplicate
        if (sentNotifications.contains(notificationKey)) {
            log.warn("Notification already sent for order {} with type ORDER_CREATED. Skipping.", orderId);
            return;
        }

        Notification notification = Notification.builder()
                .orderId(orderId)
                .customerEmail(customerEmail)
                .type(NotificationType.ORDER_CREATED)
                .message(String.format("Your Order #%d has been created successfully. " +
                        "Product ID: %d, Quantity: %d.", orderId, productId, quantity))
                .build();

        emailService.sendEmail(notification);
        sentNotifications.add(notificationKey);

        log.info("Order created notification sent for order {}", orderId);
    }

    /**
     * Send order confirmed notification
     */
    public void sendOrderConfirmedNotification(Long orderId, String customerEmail) {
        log.info("Preparing order confirmed notification for order {}", orderId);

        String notificationKey = buildNotificationKey(orderId, NotificationType.ORDER_CONFIRMED);

        if (sentNotifications.contains(notificationKey)) {
            log.warn("Notification already sent for order {} with type ORDER_CONFIRMED. Skipping.", orderId);
            return;
        }

        Notification notification = Notification.builder()
                .orderId(orderId)
                .customerEmail(customerEmail)
                .type(NotificationType.ORDER_CONFIRMED)
                .message(String.format("Your Order #%d has been confirmed and is being prepared for shipment.",
                        orderId))
                .build();

        emailService.sendEmail(notification);
        sentNotifications.add(notificationKey);

        log.info("Order confirmed notification sent for order {}", orderId);
    }

    /**
     * Send order shipped notification
     */
    public void sendOrderShippedNotification(Long orderId, String customerEmail) {
        log.info("Preparing order shipped notification for order {}", orderId);

        String notificationKey = buildNotificationKey(orderId, NotificationType.ORDER_SHIPPED);

        if (sentNotifications.contains(notificationKey)) {
            log.warn("Notification already sent for order {} with type ORDER_SHIPPED. Skipping.", orderId);
            return;
        }

        Notification notification = Notification.builder()
                .orderId(orderId)
                .customerEmail(customerEmail)
                .type(NotificationType.ORDER_SHIPPED)
                .message(String.format("Great news! Your Order #%d has been shipped and is on its way.",
                        orderId))
                .build();

        emailService.sendEmail(notification);
        sentNotifications.add(notificationKey);

        log.info("Order shipped notification sent for order {}", orderId);
    }

    /**
     * Send order cancelled notification
     */
    public void sendOrderCancelledNotification(Long orderId, String customerEmail) {
        log.info("Preparing order cancelled notification for order {}", orderId);

        String notificationKey = buildNotificationKey(orderId, NotificationType.ORDER_CANCELLED);

        if (sentNotifications.contains(notificationKey)) {
            log.warn("Notification already sent for order {} with type ORDER_CANCELLED. Skipping.", orderId);
            return;
        }

        Notification notification = Notification.builder()
                .orderId(orderId)
                .customerEmail(customerEmail)
                .type(NotificationType.ORDER_CANCELLED)
                .message(String.format("Your Order #%d has been cancelled as requested.",
                        orderId))
                .build();

        emailService.sendEmail(notification);
        sentNotifications.add(notificationKey);

        log.info("Order cancelled notification sent for order {}", orderId);
    }

    /**
     * Send order failed notification
     */
    public void sendOrderFailedNotification(Long orderId, String customerEmail, String reason) {
        log.info("Preparing order failed notification for order {}", orderId);

        String notificationKey = buildNotificationKey(orderId, NotificationType.ORDER_FAILED);

        if (sentNotifications.contains(notificationKey)) {
            log.warn("Notification already sent for order {} with type ORDER_FAILED. Skipping.", orderId);
            return;
        }

        Notification notification = Notification.builder()
                .orderId(orderId)
                .customerEmail(customerEmail)
                .type(NotificationType.ORDER_FAILED)
                .message(String.format("Unfortunately, your Order #%d has failed. Reason: %s",
                        orderId, reason))
                .build();

        emailService.sendEmail(notification);
        sentNotifications.add(notificationKey);

        log.info("Order failed notification sent for order {}", orderId);
    }

    /**
     * Handle order status changes and send appropriate notifications
     */
    public void handleOrderStatusChange(Long orderId, String customerEmail,
                                       OrderStatus previousStatus, OrderStatus newStatus) {
        log.info("Handling order status change for order {}: {} -> {}",
                orderId, previousStatus, newStatus);

        // Send notification based on the new status
        switch (newStatus) {
            case CONFIRMED:
                sendOrderConfirmedNotification(orderId, customerEmail);
                break;
            case SHIPPED:
                sendOrderShippedNotification(orderId, customerEmail);
                break;
            case CANCELLED:
                sendOrderCancelledNotification(orderId, customerEmail);
                break;
            case FAILED:
                // For failed status, we need the reason, which should come from a different event
                // Skip here as it will be handled by a specific failed event
                log.debug("Status changed to FAILED for order {}. Waiting for specific failed event with reason.", orderId);
                break;
            default:
                log.debug("No notification required for status change to {} for order {}", newStatus, orderId);
        }
    }

    /**
     * Build a unique key for tracking sent notifications
     */
    private String buildNotificationKey(Long orderId, NotificationType type) {
        return orderId + ":" + type;
    }
}
