package com.vladislav.training.platform.analytics.service;

import java.util.Objects;

public class SnapshotBackedAnalyticsTopicKeyStrategy implements AnalyticsTopicKeyStrategy {

    private static final String UNSUPPORTED_REASON =
        "Immutable topicIdSnapshot is absent in result_question_snapshot; analytics rebuild skips the row.";

    @Override
    public AnalyticsTopicKeyResolution resolveTopicKey(AnalyticsQuestionAggregateResultSourceRow sourceRow) {
        Objects.requireNonNull(sourceRow, "sourceRow must not be null");
        if (sourceRow.topicIdSnapshot() == null) {
            return new AnalyticsTopicKeyResolution(null, false, UNSUPPORTED_REASON);
        }
        return new AnalyticsTopicKeyResolution(sourceRow.topicIdSnapshot(), true, null);
    }
}
