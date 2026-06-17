package com.vladislav.training.platform.analytics.service;

/**
 * Контракт сервиса {@code AnalyticsRebuildService}.
 */
public interface AnalyticsRebuildService {

    void rebuildAllAnalytics();

    void reconcileAnalytics();
}
