package com.vladislav.training.platform.testing.controller.dto;

import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import java.time.Instant;

/**
 * Ответ {@code AssignedAttemptEntryResponse}.
 */
public record AssignedAttemptEntryResponse(
    Long testAttemptId,
    Long assignmentTestId,
    Long testId,
    AttemptMode attemptMode,
    TestAttemptStatus status,
    Instant startedAt,
    Instant lastActivityAt
) {
}
