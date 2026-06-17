package com.vladislav.training.platform.access.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Запись данных {@code ManagementRelationType}.
 */
public record ManagementRelationType(
    Long id,
    String code,
    String name,
    String description,
    Instant createdAt,
    Instant updatedAt
) {

    public ManagementRelationType {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }
}
