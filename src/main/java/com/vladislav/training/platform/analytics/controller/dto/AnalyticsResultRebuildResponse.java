package com.vladislav.training.platform.analytics.controller.dto;

import java.time.Instant;

/**
 * Ответ {@code AnalyticsResultRebuildResponse}.
 */
public record AnalyticsResultRebuildResponse(
    Instant periodStart,
    Instant periodEnd,
    long sourceRowCount,
    long supportedTopicRowCount,
    long unsupportedTopicRowCount,
    long userTopicAggregateRowCount,
    long departmentTopicAggregateRowCount,
    long questionAggregateRowCount
) {
}
