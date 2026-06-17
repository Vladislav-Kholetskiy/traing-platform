package com.vladislav.training.platform.analytics.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Запись данных {@code AnalyticsUserTopicAggregate}.
 */
public record AnalyticsUserTopicAggregate(
    Long id,
    Long userId,
    Long topicId,
    Instant periodStart,
    Instant periodEnd,
    Long lastAssignedFinalResultId,
    Instant lastAssignedFinalCompletedAt,
    BigDecimal lastAssignedFinalScorePercent,
    Boolean lastAssignedFinalPassed,
    BigDecimal averageScorePercent,
    BigDecimal passRatePercent,
    int attemptCount,
    int errorCount,
    Instant calculatedAt,
    Instant refreshedAt,
    Instant reconciledAt
) {

    public AnalyticsUserTopicAggregate {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(topicId, "topicId must not be null");
        Objects.requireNonNull(periodStart, "periodStart must not be null");
        Objects.requireNonNull(periodEnd, "periodEnd must not be null");
        Objects.requireNonNull(averageScorePercent, "averageScorePercent must not be null");
        Objects.requireNonNull(passRatePercent, "passRatePercent must not be null");
        Objects.requireNonNull(calculatedAt, "calculatedAt must not be null");
        Objects.requireNonNull(refreshedAt, "refreshedAt must not be null");
        if (!periodEnd.isAfter(periodStart)) {
            throw new IllegalArgumentException("periodEnd must be after periodStart");
        }
        if (attemptCount < 0 || errorCount < 0) {
            throw new IllegalArgumentException("aggregate counts must be non-negative");
        }
    }
}
