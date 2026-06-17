package com.vladislav.training.platform.content.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Запись данных {@code Topic}.
 */
public record Topic(
    Long id,
    Long courseId,
    String name,
    String description,
    ContentStatus status,
    int sortOrder,
    Instant createdAt,
    Instant updatedAt
) {

    public Topic {
        Objects.requireNonNull(courseId, "courseId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (sortOrder < 0) {
            throw new IllegalArgumentException("sortOrder must be non-negative");
        }
    }
}
