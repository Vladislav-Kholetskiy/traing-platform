package com.vladislav.training.platform.analytics.repository;

import com.vladislav.training.platform.analytics.domain.AnalyticsUserTopicAggregate;
import java.time.Instant;
import java.util.List;

/**
 * Контракт репозитория {@code AnalyticsUserTopicAggregateRepository}.
 */
public interface AnalyticsUserTopicAggregateRepository {

    AnalyticsUserTopicAggregate findUserTopicAggregateById(Long analyticsUserTopicAggregateId);

    AnalyticsUserTopicAggregate findUserTopicAggregate(Long userId, Long topicId, Instant periodStart, Instant periodEnd);

    List<AnalyticsUserTopicAggregate> findUserTopicAggregatesByUserId(Long userId);

    List<AnalyticsUserTopicAggregate> findUserTopicAggregatesByTopicId(Long topicId);

    AnalyticsUserTopicAggregate saveUserTopicAggregate(AnalyticsUserTopicAggregate analyticsUserTopicAggregate);

    void deleteAllUserTopicAggregates();
}
