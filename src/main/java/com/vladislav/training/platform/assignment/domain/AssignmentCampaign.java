package com.vladislav.training.platform.assignment.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Запись данных {@code AssignmentCampaign}.
 */
public record AssignmentCampaign(
    Long id,
    String name,
    String description,
    String sourceType,
    String sourceRef,
    String sourceNameSnapshot,
    Instant createdAt,
    Instant updatedAt
) {

    public AssignmentCampaign {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(sourceType, "sourceType must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}
