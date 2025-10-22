package com.jlapugot.notificationservice.service;

import com.jlapugot.notificationservice.model.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Simple email service implementation
 * In production, this would integrate with services like SendGrid, AWS SES, etc.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    @Override
    public void sendEmail(Notification notification) {
        log.info("Sending {} notification to {} for order {}",
                notification.getType(),
                notification.getCustomerEmail(),
                notification.getOrderId());

        // In production, integrate with actual email service
        // For now, just log the notification
        log.info("Email content: {}", notification.getMessage());

        // Simulate email sending
        log.info("Email sent successfully to {}", notification.getCustomerEmail());
    }
}
