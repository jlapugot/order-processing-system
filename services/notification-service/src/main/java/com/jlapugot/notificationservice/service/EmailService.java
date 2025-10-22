package com.jlapugot.notificationservice.service;

import com.jlapugot.notificationservice.model.Notification;

/**
 * Service interface for sending emails
 */
public interface EmailService {

    /**
     * Send an email notification
     */
    void sendEmail(Notification notification);
}
