package com.vladislav.training.platform.testing.domain;

import com.vladislav.training.platform.common.model.AttemptMode;
import java.time.Instant;
import java.util.Objects;

/**
 * Запись данных {@code TestAttempt}.
 */
public record TestAttempt(
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
    Instant lastActivityAt,
    Instant createdAt,
    Instant updatedAt
) {

    public TestAttempt {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(testId, "testId must not be null");
        Objects.requireNonNull(attemptMode, "attemptMode must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(startedAt, "startedAt must not be null");
        Objects.requireNonNull(lastActivityAt, "lastActivityAt must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}
