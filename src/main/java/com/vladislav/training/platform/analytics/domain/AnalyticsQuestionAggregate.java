package com.vladislav.training.platform.analytics.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Запись данных {@code AnalyticsQuestionAggregate}.
 */
public record AnalyticsQuestionAggregate(
    Long id,
    Long questionId,
    Instant periodStart,
    Instant periodEnd,
    int attemptCount,
    int correctCount,
    int incorrectCount,
    BigDecimal averageEarnedScore,
    Instant calculatedAt,
    Instant refreshedAt,
    Instant reconciledAt
) {

    public AnalyticsQuestionAggregate {
        Objects.requireNonNull(questionId, "questionId must not be null");
        Objects.requireNonNull(periodStart, "periodStart must not be null");
        Objects.requireNonNull(periodEnd, "periodEnd must not be null");
        Objects.requireNonNull(averageEarnedScore, "averageEarnedScore must not be null");
        Objects.requireNonNull(calculatedAt, "calculatedAt must not be null");
        Objects.requireNonNull(refreshedAt, "refreshedAt must not be null");
        if (!periodEnd.isAfter(periodStart)) {
            throw new IllegalArgumentException("periodEnd must be after periodStart");
        }
        if (attemptCount < 0 || correctCount < 0 || incorrectCount < 0) {
            throw new IllegalArgumentException("aggregate counts must be non-negative");
        }
    }
}
