package com.vladislav.training.platform.analytics.service;

import com.vladislav.training.platform.analytics.domain.AnalyticsCampaignAggregate;
import com.vladislav.training.platform.analytics.domain.AnalyticsDepartmentTopicAggregate;
import com.vladislav.training.platform.analytics.domain.AnalyticsQuestionAggregate;
import com.vladislav.training.platform.analytics.domain.AnalyticsUserTopicAggregate;
import java.time.Instant;

/**
 * Контракт сервиса {@code AnalyticsRefreshService}.
 */
public interface AnalyticsRefreshService {

    AnalyticsUserTopicAggregate refreshUserTopicAggregate(Long userId, Long topicId, Instant periodStart, Instant periodEnd);

    AnalyticsDepartmentTopicAggregate refreshDepartmentTopicAggregate(
        Long organizationalUnitIdSnapshot,
        Long topicId,
        Instant periodStart,
        Instant periodEnd
    );

    AnalyticsQuestionAggregate refreshQuestionAggregate(Long questionId, Instant periodStart, Instant periodEnd);

    AnalyticsCampaignAggregate refreshCampaignAggregate(Long campaignId);
}
