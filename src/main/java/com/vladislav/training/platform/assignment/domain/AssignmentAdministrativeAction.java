package com.vladislav.training.platform.assignment.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Запись данных {@code AssignmentAdministrativeAction}.
 */
public record AssignmentAdministrativeAction(
    Long id,
    Long assignmentId,
    AssignmentAdministrativeActionType actionType,
    Instant occurredAt,
    String note,
    Instant createdAt
) {

    public AssignmentAdministrativeAction {
        Objects.requireNonNull(assignmentId, "assignmentId must not be null");
        Objects.requireNonNull(actionType, "actionType must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
