package com.vladislav.training.platform.analytics.controller.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Ответ {@code ExpertQuestionAnalyticsResponse}.
 */
public record ExpertQuestionAnalyticsResponse(
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
}
