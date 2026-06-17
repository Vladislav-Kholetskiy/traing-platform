package com.vladislav.training.platform.notification.controller.dto;

import com.vladislav.training.platform.notification.domain.NotificationChannel;
import java.time.Instant;

/**
 * Ответ {@code NotificationSelfReadResponse}.
 */
public record NotificationSelfReadResponse(
    Long id,
    String title,
    String message,
    NotificationChannel channelCode,
    Instant createdAt,
    Instant readAt,
    boolean read,
    String notificationType,
    String companyName,
    java.util.List<NotificationAssignmentRecipientResponse> assignmentRecipients
) {

    public record NotificationAssignmentRecipientResponse(
        Long userId,
        String fullName,
        String courseName,
        String companyName,
        String organizationalUnitName
    ) {
    }
}
