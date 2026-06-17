package com.vladislav.training.platform.notification.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Запись данных {@code NotificationRule}.
 */
public record NotificationRule(
    Long id,
    String ruleCode,
    String name,
    String notificationType,
    NotificationChannel channelCode,
    boolean isEnabled,
    Integer daysBeforeDeadline,
    Integer repeatIntervalDays,
    String triggerMode,
    String recipientScopeCode,
    Instant createdAt,
    Instant updatedAt
) {

    public NotificationRule {
        Objects.requireNonNull(ruleCode, "ruleCode must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(notificationType, "notificationType must not be null");
        Objects.requireNonNull(channelCode, "channelCode must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (ruleCode.isBlank()) {
            throw new IllegalArgumentException("ruleCode must not be blank");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (notificationType.isBlank()) {
            throw new IllegalArgumentException("notificationType must not be blank");
        }
        if (daysBeforeDeadline != null && daysBeforeDeadline < 0) {
            throw new IllegalArgumentException("daysBeforeDeadline must be non-negative");
        }
        if (repeatIntervalDays != null && repeatIntervalDays <= 0) {
            throw new IllegalArgumentException("repeatIntervalDays must be positive");
        }
    }
}
