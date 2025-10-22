package com.jlapugot.notificationservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Model representing a notification to be sent
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    private Long orderId;
    private String customerEmail;
    private NotificationType type;
    private String message;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
