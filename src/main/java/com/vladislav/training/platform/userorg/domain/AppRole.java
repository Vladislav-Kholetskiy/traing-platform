package com.vladislav.training.platform.userorg.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Запись данных {@code AppRole}.
 */
public record AppRole(
    Long id,
    String code,
    String name,
    String description,
    Instant createdAt,
    Instant updatedAt
) {

    public AppRole {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}
