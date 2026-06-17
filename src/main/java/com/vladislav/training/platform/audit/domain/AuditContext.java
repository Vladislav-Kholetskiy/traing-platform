package com.vladislav.training.platform.audit.domain;

import java.util.Objects;

/**
 * Запись данных {@code AuditContext}.
 */
public record AuditContext(String json) {

    public AuditContext {
        Objects.requireNonNull(json, "json must not be null");
    }
}
