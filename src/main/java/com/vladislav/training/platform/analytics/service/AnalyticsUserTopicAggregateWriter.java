package com.vladislav.training.platform.analytics.service;

import java.time.Instant;
import java.util.List;

public interface AnalyticsUserTopicAggregateWriter {

    void replaceUserTopicAggregates(
        Instant periodStartInclusive,
        Instant periodEndExclusive,
        List<AnalyticsUserTopicAggregateRow> aggregateRows
    );
}
