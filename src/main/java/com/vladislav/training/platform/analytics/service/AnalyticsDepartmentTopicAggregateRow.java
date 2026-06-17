package com.vladislav.training.platform.analytics.service;

import java.math.BigDecimal;
import java.time.Instant;

public record AnalyticsDepartmentTopicAggregateRow(
    Long organizationalUnitIdSnapshot,
    String organizationalPathSnapshot,
    Long topicId,
    BigDecimal averageScorePercent,
    BigDecimal passRatePercent,
    long attemptCount,
    long errorCount,
    Instant periodStartInclusive,
    Instant periodEndExclusive
) {
}
