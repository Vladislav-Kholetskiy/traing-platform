package com.vladislav.training.platform.notification.controller.dto;

import com.vladislav.training.platform.notification.domain.NotificationChannel;
import com.vladislav.training.platform.notification.domain.NotificationStatus;
import java.time.Instant;

/**
 * Ответ {@code NotificationAdminReadResponse}.
 */
public record NotificationAdminReadResponse(
    Long id,
    Long recipientUserId,
    String notificationType,
    NotificationChannel channelCode,
    NotificationStatus status,
    String sourceEntityType,
    String sourceEntityId,
    Instant scheduledAt,
    Instant sentAt,
    Instant readAt,
    int deliveryAttemptCount,
    String errorCode,
    Instant createdAt,
    Instant updatedAt
) {
}
