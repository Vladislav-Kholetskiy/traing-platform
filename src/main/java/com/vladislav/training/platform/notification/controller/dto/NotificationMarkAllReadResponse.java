package com.vladislav.training.platform.notification.controller.dto;

import java.time.Instant;

/**
 * Ответ {@code NotificationMarkAllReadResponse}.
 */
public record NotificationMarkAllReadResponse(
    int updatedCount,
    Instant readAt
) {
}
