package com.vladislav.training.platform.content.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Запись данных {@code Course}.
 */
public record Course(
    Long id,
    String name,
    String description,
    ContentStatus status,
    Integer sortOrder,
    Instant createdAt,
    Instant updatedAt
) {

    public Course {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (sortOrder != null && sortOrder < 0) {
            throw new IllegalArgumentException("sortOrder must be non-negative");
        }
    }
}
