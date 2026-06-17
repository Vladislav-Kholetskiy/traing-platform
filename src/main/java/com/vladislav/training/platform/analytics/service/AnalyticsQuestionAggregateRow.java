package com.vladislav.training.platform.analytics.service;

import java.math.BigDecimal;
import java.time.Instant;

public record AnalyticsQuestionAggregateRow(
    Long questionId,
    long attemptCount,
    long correctCount,
    long incorrectCount,
    BigDecimal averageEarnedScore,
    Instant periodStartInclusive,
    Instant periodEndExclusive
) {
}
