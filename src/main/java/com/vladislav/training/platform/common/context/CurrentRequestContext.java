package com.vladislav.training.platform.common.context;

import java.util.Objects;

/**
 * Запись данных {@code CurrentRequestContext}.
 */
public record CurrentRequestContext(String correlationId, String requestId) {

    public CurrentRequestContext(String correlationId) {
        this(correlationId, null);
    }

    public CurrentRequestContext {
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        if (correlationId.isBlank()) {
            throw new IllegalArgumentException("correlationId must not be blank");
        }
        if (requestId != null && requestId.isBlank()) {
            throw new IllegalArgumentException("requestId must not be blank when provided");
        }
    }
}
