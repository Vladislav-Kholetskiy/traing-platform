package com.vladislav.training.platform.analytics.service;

import com.vladislav.training.platform.analytics.domain.AnalyticsCampaignAggregate;
import com.vladislav.training.platform.analytics.domain.AnalyticsDepartmentTopicAggregate;
import com.vladislav.training.platform.analytics.domain.AnalyticsQuestionAggregate;
import com.vladislav.training.platform.analytics.domain.AnalyticsUserTopicAggregate;
import java.time.Instant;
import java.util.List;

/**
 * Контракт сервиса чтения {@code AnalyticsQueryService}.
 */
public interface AnalyticsQueryService {

    AnalyticsUserTopicAggregate findUserTopicAggregateById(Long analyticsUserTopicAggregateId);

    AnalyticsUserTopicAggregate findUserTopicAggregate(Long userId, Long topicId, Instant periodStart, Instant periodEnd);

    List<AnalyticsUserTopicAggregate> findUserTopicAggregatesByUserId(Long userId);

    List<AnalyticsUserTopicAggregate> findUserTopicAggregatesByTopicId(Long topicId);

    AnalyticsDepartmentTopicAggregate findDepartmentTopicAggregateById(Long analyticsDepartmentTopicAggregateId);

    AnalyticsDepartmentTopicAggregate findDepartmentTopicAggregate(
        Long organizationalUnitIdSnapshot,
        Long topicId,
        Instant periodStart,
        Instant periodEnd
    );

    List<AnalyticsDepartmentTopicAggregate> findDepartmentTopicAggregatesByOrganizationalUnitIdSnapshot(
        Long organizationalUnitIdSnapshot
    );

    AnalyticsQuestionAggregate findQuestionAggregateById(Long analyticsQuestionAggregateId);

    AnalyticsQuestionAggregate findQuestionAggregate(Long questionId, Instant periodStart, Instant periodEnd);

    List<AnalyticsQuestionAggregate> findQuestionAggregatesByQuestionId(Long questionId);

    AnalyticsCampaignAggregate findCampaignAggregateById(Long analyticsCampaignAggregateId);

    AnalyticsCampaignAggregate findCampaignAggregateByCampaignId(Long campaignId);
}
