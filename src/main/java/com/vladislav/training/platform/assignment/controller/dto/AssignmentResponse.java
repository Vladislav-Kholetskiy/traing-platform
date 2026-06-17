package com.vladislav.training.platform.assignment.controller.dto;

import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import java.time.Instant;

/**
 * Ответ {@code AssignmentResponse}.
 */
public record AssignmentResponse(
    Long id,
    Long campaignId,
    Long userId,
    Long courseId,
    String courseName,
    AssignmentStatus status,
    Instant assignedAt,
    Instant deadlineAt,
    Instant cancelledAt,
    Instant closedAt,
    Instant createdAt,
    Instant updatedAt
) {
}
