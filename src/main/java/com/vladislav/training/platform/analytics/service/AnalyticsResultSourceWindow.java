package com.vladislav.training.platform.analytics.service;

import java.time.Instant;
import java.util.Objects;

public record AnalyticsResultSourceWindow(
    Instant periodStartInclusive,
    Instant periodEndExclusive
) {

    public AnalyticsResultSourceWindow {
        Objects.requireNonNull(periodStartInclusive, "periodStartInclusive must not be null");
        Objects.requireNonNull(periodEndExclusive, "periodEndExclusive must not be null");
        if (!periodEndExclusive.isAfter(periodStartInclusive)) {
            throw new IllegalArgumentException("periodEndExclusive must be after periodStartInclusive");
        }
    }
}
