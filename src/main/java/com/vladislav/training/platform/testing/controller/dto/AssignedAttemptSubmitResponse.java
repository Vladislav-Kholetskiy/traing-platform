package com.vladislav.training.platform.testing.controller.dto;

import com.vladislav.training.platform.testing.domain.TestAttemptStatus;

/**
 * Ответ {@code AssignedAttemptSubmitResponse}.
 */
public record AssignedAttemptSubmitResponse(
    Long testAttemptId,
    TestAttemptStatus status,
    Long resultId
) {
}
