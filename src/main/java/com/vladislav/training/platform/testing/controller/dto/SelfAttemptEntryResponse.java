package com.vladislav.training.platform.testing.controller.dto;

import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import java.time.Instant;

/**
 * Ответ {@code SelfAttemptEntryResponse}.
 */
public record SelfAttemptEntryResponse(
    Long testAttemptId,
    Long testId,
    AttemptMode attemptMode,
    TestAttemptStatus status,
    Instant startedAt,
    Instant lastActivityAt
) {
}
