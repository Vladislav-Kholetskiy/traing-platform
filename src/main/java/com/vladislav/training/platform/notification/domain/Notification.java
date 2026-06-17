package com.vladislav.training.platform.notification.domain;

import java.time.Instant;
import java.util.Objects;
/**
 * Запись данных {@code Notification}.
 */
public record Notification(
    Long id,
    Long recipientUserId,
    String notificationType,
    NotificationChannel channelCode,
    NotificationStatus status,
    String dedupKey,
    String sourceEntityType,
    String sourceEntityId,
    Instant scheduledAt,
    Instant sentAt,
    Instant readAt,
    int deliveryAttemptCount,
    String errorCode,
    String errorMessage,
    String payloadSnapshot,
    Instant createdAt,
    Instant updatedAt
) {

    public Notification {
        Objects.requireNonNull(recipientUserId, "recipientUserId must not be null");
        Objects.requireNonNull(notificationType, "notificationType must not be null");
        Objects.requireNonNull(channelCode, "channelCode must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(dedupKey, "dedupKey must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (notificationType.isBlank()) {
            throw new IllegalArgumentException("notificationType must not be blank");
        }
        if (dedupKey.isBlank()) {
            throw new IllegalArgumentException("dedupKey must not be blank");
        }
        if ((sourceEntityType == null) != (sourceEntityId == null)) {
            throw new IllegalArgumentException("sourceEntityType and sourceEntityId must be both null or both non-null");
        }
        if (status == NotificationStatus.SENT && sentAt == null) {
            throw new IllegalArgumentException("sentAt must not be null when status is SENT");
        }
        if (deliveryAttemptCount < 0) {
            throw new IllegalArgumentException("deliveryAttemptCount must be non-negative");
        }
    }
}
