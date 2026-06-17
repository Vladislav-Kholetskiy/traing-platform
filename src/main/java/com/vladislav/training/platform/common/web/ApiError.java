package com.vladislav.training.platform.common.web;

import java.time.Instant;

/**
 * Запись данных {@code ApiError}.
 */
public record ApiError(
    Instant timestamp,
    int status,
    String error,
    String message,
    String correlationId
) {
}
