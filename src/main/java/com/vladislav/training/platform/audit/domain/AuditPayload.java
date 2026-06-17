package com.vladislav.training.platform.audit.domain;

import java.util.Objects;

/**
 * Запись данных {@code AuditPayload}.
 */
public record AuditPayload(String json) {

    public AuditPayload {
        Objects.requireNonNull(json, "json must not be null");
    }
}
