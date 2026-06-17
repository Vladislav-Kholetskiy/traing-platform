package com.vladislav.training.platform.analytics.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataAnalyticsDepartmentTopicAggregateJpaRepository
    extends JpaRepository<AnalyticsDepartmentTopicAggregateEntity, Long> {
}
