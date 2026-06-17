package com.vladislav.training.platform.analytics.query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Объект передачи данных {@code ExpertQuestionAnalyticsDto}.
 */
public record ExpertQuestionAnalyticsDto(
    Long questionId,
    Instant periodStart,
    Instant periodEnd,
    Integer attemptCount,
    Integer correctCount,
    Integer incorrectCount,
    BigDecimal averageEarnedScore,
    Instant calculatedAt,
    Instant refreshedAt
) {

    public ExpertQuestionAnalyticsDto {
        Objects.requireNonNull(questionId, "questionId must not be null");
        Objects.requireNonNull(periodStart, "periodStart must not be null");
        Objects.requireNonNull(periodEnd, "periodEnd must not be null");
        Objects.requireNonNull(attemptCount, "attemptCount must not be null");
        Objects.requireNonNull(correctCount, "correctCount must not be null");
        Objects.requireNonNull(incorrectCount, "incorrectCount must not be null");
        Objects.requireNonNull(averageEarnedScore, "averageEarnedScore must not be null");
        Objects.requireNonNull(calculatedAt, "calculatedAt must not be null");
        Objects.requireNonNull(refreshedAt, "refreshedAt must not be null");
    }
}
