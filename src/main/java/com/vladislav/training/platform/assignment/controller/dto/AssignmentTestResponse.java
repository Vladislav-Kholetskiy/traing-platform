package com.vladislav.training.platform.assignment.controller.dto;

import com.vladislav.training.platform.assignment.domain.AssignmentTestRole;
import java.time.Instant;

public record AssignmentTestResponse(
    Long id,
    Long assignmentId,
    Long testId,
    String testName,
    String topicName,
    AssignmentTestRole assignmentTestRole,
    Long countedResultId,
    Instant closedAt,
    boolean isClosed,
    Instant createdAt,
    Instant updatedAt
) {
}
