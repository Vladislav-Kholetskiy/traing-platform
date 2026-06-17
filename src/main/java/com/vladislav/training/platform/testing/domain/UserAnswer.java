package com.vladislav.training.platform.testing.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Запись данных {@code UserAnswer}.
 */
public record UserAnswer(
    Long id,
    Long testAttemptId,
    Long questionId,
    Instant createdAt,
    Instant updatedAt
) {

    public UserAnswer {
        Objects.requireNonNull(testAttemptId, "testAttemptId must not be null");
        Objects.requireNonNull(questionId, "questionId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}
