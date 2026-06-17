package com.vladislav.training.platform.analytics.service;

public record AnalyticsTopicKeyResolution(
    Long topicId,
    boolean supported,
    String reason
) {
}
