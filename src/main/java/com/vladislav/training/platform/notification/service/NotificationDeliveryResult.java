package com.vladislav.training.platform.notification.service;

import java.time.Instant;
import java.util.Objects;

/**
 * Запись данных {@code NotificationDeliveryResult}.
 */
public record NotificationDeliveryResult(
    boolean successful,
    Instant sentAt,
    String errorCode,
    String errorMessage
) {

    public NotificationDeliveryResult {
        if (successful && sentAt == null) {
            throw new IllegalArgumentException("sentAt must not be null for successful delivery result");
        }
        if (!successful && sentAt != null) {
            throw new IllegalArgumentException("sentAt must be null for failed delivery result");
        }
    }

    public static NotificationDeliveryResult success(Instant sentAt) {
        return new NotificationDeliveryResult(true, Objects.requireNonNull(sentAt, "sentAt must not be null"), null, null);
    }

    public static NotificationDeliveryResult failure(String errorCode, String errorMessage) {
        return new NotificationDeliveryResult(false, null, errorCode, errorMessage);
    }
}
