package com.vladislav.training.platform.content.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Запись данных {@code Material}.
 */
public record Material(
    Long id,
    Long topicId,
    String name,
    String description,
    String body,
    String videoUrl,
    MaterialType materialType,
    ContentStatus status,
    int sortOrder,
    Instant createdAt,
    Instant updatedAt
) {

    public Material {
        Objects.requireNonNull(topicId, "topicId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(materialType, "materialType must not be null");
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
