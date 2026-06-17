package com.vladislav.training.platform.analytics.service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Запись данных {@code AnalyticsResultRebuildOutcome}.
 */
public record AnalyticsResultRebuildOutcome(
    Instant periodStartInclusive,
    Instant periodEndExclusive,
    long sourceRowCount,
    long supportedTopicRowCount,
    long unsupportedTopicRowCount,
    long userTopicAggregateRowCount,
    long departmentTopicAggregateRowCount,
    long questionAggregateRowCount,
    List<AnalyticsUnsupportedTopicKeyReportRow> unsupportedTopicRows
) {

    public AnalyticsResultRebuildOutcome {
        Objects.requireNonNull(periodStartInclusive, "periodStartInclusive must not be null");
        Objects.requireNonNull(periodEndExclusive, "periodEndExclusive must not be null");
        Objects.requireNonNull(unsupportedTopicRows, "unsupportedTopicRows must not be null");
        unsupportedTopicRows = List.copyOf(unsupportedTopicRows);
    }
}
