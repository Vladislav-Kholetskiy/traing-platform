package com.vladislav.training.platform.analytics.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AnalyticsQuestionAggregateResultSourceReader {

    List<AnalyticsQuestionAggregateResultSourceRow> readQuestionAggregateRows(
        Instant periodStartInclusive,
        Instant periodEndExclusive
    );

    default Optional<AnalyticsResultSourceWindow> findAvailableResultSourceWindow() {
        return Optional.empty();
    }
}
