package com.vladislav.training.platform.analytics.repository;

import com.vladislav.training.platform.analytics.domain.AnalyticsDepartmentTopicAggregate;
import java.time.Instant;
import java.util.List;

/**
 * Контракт репозитория {@code AnalyticsDepartmentTopicAggregateRepository}.
 */
public interface AnalyticsDepartmentTopicAggregateRepository {

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

    AnalyticsDepartmentTopicAggregate saveDepartmentTopicAggregate(
        AnalyticsDepartmentTopicAggregate analyticsDepartmentTopicAggregate
    );

    void deleteAllDepartmentTopicAggregates();
}
