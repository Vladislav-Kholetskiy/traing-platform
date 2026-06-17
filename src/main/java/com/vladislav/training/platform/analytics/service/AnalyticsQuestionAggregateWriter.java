package com.vladislav.training.platform.analytics.service;

import java.time.Instant;
import java.util.List;

public interface AnalyticsQuestionAggregateWriter {

    void replaceQuestionAggregates(
        Instant periodStartInclusive,
        Instant periodEndExclusive,
        List<AnalyticsQuestionAggregateRow> aggregateRows
    );
}
