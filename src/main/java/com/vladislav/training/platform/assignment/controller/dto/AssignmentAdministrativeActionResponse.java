package com.vladislav.training.platform.assignment.controller.dto;

import com.vladislav.training.platform.assignment.domain.AssignmentAdministrativeActionType;
import java.time.Instant;

public record AssignmentAdministrativeActionResponse(
    Long id,
    Long assignmentId,
    AssignmentAdministrativeActionType actionType,
    Instant occurredAt,
    String note,
    Instant createdAt
) {
}
