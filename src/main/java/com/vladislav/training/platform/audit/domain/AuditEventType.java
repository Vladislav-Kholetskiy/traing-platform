package com.vladislav.training.platform.audit.domain;

import java.util.Objects;

/**
 * Запись данных {@code AuditEventType}.
 */
public record AuditEventType(String value) {

    public AuditEventType {
        Objects.requireNonNull(value, "value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }
}
