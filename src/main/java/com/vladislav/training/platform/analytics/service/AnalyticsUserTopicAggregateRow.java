package com.vladislav.training.platform.analytics.service;

import java.math.BigDecimal;
import java.time.Instant;

public record AnalyticsUserTopicAggregateRow(
    Long userId,
    Long topicId,
    Long lastAssignedFinalResultId,
    Instant lastAssignedFinalCompletedAt,
    BigDecimal lastAssignedFinalScorePercent,
    Boolean lastAssignedFinalPassed,
    BigDecimal averageScorePercent,
    BigDecimal passRatePercent,
    long attemptCount,
    long errorCount,
    Instant periodStartInclusive,
    Instant periodEndExclusive
) {
}
