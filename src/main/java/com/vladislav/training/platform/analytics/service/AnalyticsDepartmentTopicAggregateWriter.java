package com.vladislav.training.platform.analytics.service;

import java.time.Instant;
import java.util.List;

public interface AnalyticsDepartmentTopicAggregateWriter {

    void replaceDepartmentTopicAggregates(
        Instant periodStartInclusive,
        Instant periodEndExclusive,
        List<AnalyticsDepartmentTopicAggregateRow> aggregateRows
    );
}
