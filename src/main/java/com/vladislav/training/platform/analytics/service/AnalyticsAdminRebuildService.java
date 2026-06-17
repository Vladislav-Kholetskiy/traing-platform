package com.vladislav.training.platform.analytics.service;

import java.time.Instant;

/**
 * Контракт сервиса {@code AnalyticsAdminRebuildService}.
 */
public interface AnalyticsAdminRebuildService {

    AnalyticsResultRebuildOutcome rebuildResultAnalytics(Long actorUserId, Instant periodStartInclusive, Instant periodEndExclusive);
}
