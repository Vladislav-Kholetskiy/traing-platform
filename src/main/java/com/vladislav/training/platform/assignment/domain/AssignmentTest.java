package com.vladislav.training.platform.assignment.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Запись данных {@code AssignmentTest}.
 */
public record AssignmentTest(
    Long id,
    Long assignmentId,
    Long testId,
    AssignmentTestRole assignmentTestRole,
    Long countedResultId,
    Instant closedAt,
    boolean isClosed,
    Instant createdAt,
    Instant updatedAt
) {

    public AssignmentTest {
        Objects.requireNonNull(assignmentId, "assignmentId must not be null");
        Objects.requireNonNull(testId, "testId must not be null");
        Objects.requireNonNull(assignmentTestRole, "assignmentTestRole must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (!isClosed && closedAt != null) {
            throw new IllegalArgumentException("closedAt must be null when assignment test is open");
        }
        if (isClosed && closedAt == null) {
            throw new IllegalArgumentException("closedAt must not be null when assignment test is closed");
        }
    }
}
