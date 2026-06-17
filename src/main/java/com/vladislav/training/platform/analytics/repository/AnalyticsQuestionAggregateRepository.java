package com.vladislav.training.platform.analytics.repository;

import com.vladislav.training.platform.analytics.domain.AnalyticsQuestionAggregate;
import java.time.Instant;
import java.util.List;

/**
 * Контракт репозитория {@code AnalyticsQuestionAggregateRepository}.
 */
public interface AnalyticsQuestionAggregateRepository {

    AnalyticsQuestionAggregate findQuestionAggregateById(Long analyticsQuestionAggregateId);

    AnalyticsQuestionAggregate findQuestionAggregate(Long questionId, Instant periodStart, Instant periodEnd);

    List<AnalyticsQuestionAggregate> findQuestionAggregatesByQuestionId(Long questionId);

    AnalyticsQuestionAggregate saveQuestionAggregate(AnalyticsQuestionAggregate analyticsQuestionAggregate);

    void deleteAllQuestionAggregates();
}
