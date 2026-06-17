package com.vladislav.training.platform.assignment.controller.dto;

import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import java.time.Instant;

/**
 * Ответ {@code ManagerialCurrentSupervisionResponse}.
 */
public record ManagerialCurrentSupervisionResponse(
    Long assignmentId,
    Long userId,
    String userDisplayName,
    Long courseId,
    String courseName,
    Long assignmentTestCount,
    Instant assignedAt,
    Instant deadlineAt,
    AssignmentStatus assignmentStatus
) {
}
