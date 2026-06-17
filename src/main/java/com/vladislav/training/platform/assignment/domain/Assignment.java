package com.vladislav.training.platform.assignment.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Запись данных {@code Assignment}.
 */
public record Assignment(
    Long id,
    Long campaignId,
    Long userId,
    Long courseId,
    AssignmentStatus status,
    Instant assignedAt,
    Instant deadlineAt,
    Instant cancelledAt,
    Instant closedAt,
    Instant createdAt,
    Instant updatedAt
) {

    public Assignment {
        Objects.requireNonNull(campaignId, "campaignId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(courseId, "courseId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(assignedAt, "assignedAt must not be null");
        Objects.requireNonNull(deadlineAt, "deadlineAt must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (deadlineAt.isBefore(assignedAt)) {
            throw new IllegalArgumentException("deadlineAt must not be earlier than assignedAt");
        }
    }
}
