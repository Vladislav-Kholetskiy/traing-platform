package com.vladislav.training.platform.testing.controller.dto;

import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import java.time.Instant;

/**
 * Ответ {@code CurrentAttemptResponse}.
 */
public record CurrentAttemptResponse(
    Long id,
    Long userId,
    Long testId,
    Long assignmentTestId,
    AttemptMode attemptMode,
    TestAttemptStatus status,
    Instant startedAt,
    Instant completedAt,
    Instant expiredAt,
    Instant abandonedAt,
    Instant lastActivityAt
) {
}
