package com.vladislav.training.platform.analytics.service;

import java.util.Objects;

public record AnalyticsUnsupportedTopicKeyReportRow(
    Long resultId,
    Long questionOriginalId,
    String reason
) {

    public AnalyticsUnsupportedTopicKeyReportRow {
        Objects.requireNonNull(reason, "reason must not be null");
    }
}
