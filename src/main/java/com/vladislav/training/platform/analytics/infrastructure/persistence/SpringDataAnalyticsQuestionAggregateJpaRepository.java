package com.vladislav.training.platform.analytics.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataAnalyticsQuestionAggregateJpaRepository
    extends JpaRepository<AnalyticsQuestionAggregateEntity, Long> {
}
