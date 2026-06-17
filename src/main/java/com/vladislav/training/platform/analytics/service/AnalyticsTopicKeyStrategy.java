package com.vladislav.training.platform.analytics.service;

public interface AnalyticsTopicKeyStrategy {

    AnalyticsTopicKeyResolution resolveTopicKey(AnalyticsQuestionAggregateResultSourceRow sourceRow);
}
