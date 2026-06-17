package com.vladislav.training.platform.analytics.service;

import java.util.Objects;

public class UnsupportedAnalyticsTopicKeyStrategy implements AnalyticsTopicKeyStrategy {

    private static final String UNSUPPORTED_REASON =
        "Approved immutable topic anchor is absent; topic snapshot is not available for SCN-11 topic-key resolution.";

    @Override
    public AnalyticsTopicKeyResolution resolveTopicKey(AnalyticsQuestionAggregateResultSourceRow sourceRow) {
        Objects.requireNonNull(sourceRow, "sourceRow must not be null");
        return new AnalyticsTopicKeyResolution(null, false, UNSUPPORTED_REASON);
    }
}
