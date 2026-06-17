package com.vladislav.training.platform.analytics.service;

import java.time.Instant;

/**
 * Контракт сервиса {@code AnalyticsResultRebuildService}.
 */
public interface AnalyticsResultRebuildService {

    AnalyticsResultRebuildOutcome rebuildResultAnalytics(Instant periodStartInclusive, Instant periodEndExclusive);
}
