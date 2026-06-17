package com.vladislav.training.platform.notification.domain;

import java.util.Objects;

/**
 * Запись данных {@code NotificationChannel}.
 */
public record NotificationChannel(String value) {

    public NotificationChannel {
        Objects.requireNonNull(value, "value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }
}
