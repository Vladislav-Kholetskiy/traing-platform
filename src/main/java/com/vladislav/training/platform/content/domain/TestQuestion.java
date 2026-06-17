package com.vladislav.training.platform.content.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Запись данных {@code TestQuestion}.
 */
public record TestQuestion(
    Long id,
    Long testId,
    Long questionId,
    int displayOrder,
    BigDecimal weight,
    Instant createdAt,
    Instant updatedAt
) {

    public TestQuestion {
        Objects.requireNonNull(testId, "testId must not be null");
        Objects.requireNonNull(questionId, "questionId must not be null");
        Objects.requireNonNull(weight, "weight must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (displayOrder < 0) {
            throw new IllegalArgumentException("displayOrder must be non-negative");
        }
        if (weight.signum() <= 0) {
            throw new IllegalArgumentException("weight must be positive");
        }
    }
}
