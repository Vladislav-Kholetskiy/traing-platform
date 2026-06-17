package com.vladislav.training.platform.analytics.infrastructure.persistence;

import com.vladislav.training.platform.analytics.domain.AnalyticsDepartmentTopicAggregate;
import com.vladislav.training.platform.analytics.repository.AnalyticsDepartmentTopicAggregateRepository;
import com.vladislav.training.platform.common.exception.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaAnalyticsDepartmentTopicAggregateRepositoryAdapter implements AnalyticsDepartmentTopicAggregateRepository {

    private final SpringDataAnalyticsDepartmentTopicAggregateJpaRepository repository;

    public JpaAnalyticsDepartmentTopicAggregateRepositoryAdapter(
        SpringDataAnalyticsDepartmentTopicAggregateJpaRepository repository
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public AnalyticsDepartmentTopicAggregate findDepartmentTopicAggregateById(Long analyticsDepartmentTopicAggregateId) {
        return repository.findById(analyticsDepartmentTopicAggregateId)
            .map(this::toDomain)
            .orElseThrow(() -> new NotFoundException(
                "Analytics department-topic aggregate not found: " + analyticsDepartmentTopicAggregateId
            ));
    }

    @Override
    public AnalyticsDepartmentTopicAggregate findDepartmentTopicAggregate(
        Long organizationalUnitIdSnapshot,
        Long topicId,
        Instant periodStart,
        Instant periodEnd
    ) {
        return repository.findAll().stream()
            .filter(entity -> Objects.equals(entity.getOrganizationalUnitIdSnapshot(), organizationalUnitIdSnapshot))
            .filter(entity -> Objects.equals(entity.getTopicId(), topicId))
            .filter(entity -> Objects.equals(entity.getPeriodStart(), periodStart))
            .filter(entity -> Objects.equals(entity.getPeriodEnd(), periodEnd))
            .findFirst()
            .map(this::toDomain)
            .orElseThrow(() -> new NotFoundException(
                "Analytics department-topic aggregate not found: organizationalUnitIdSnapshot="
                    + organizationalUnitIdSnapshot
                    + ", topicId="
                    + topicId
                    + ", periodStart="
                    + periodStart
                    + ", periodEnd="
                    + periodEnd
            ));
    }

    @Override
    public List<AnalyticsDepartmentTopicAggregate> findDepartmentTopicAggregatesByOrganizationalUnitIdSnapshot(
        Long organizationalUnitIdSnapshot
    ) {
        return repository.findAll().stream()
            .filter(entity -> Objects.equals(entity.getOrganizationalUnitIdSnapshot(), organizationalUnitIdSnapshot))
            .map(this::toDomain)
            .toList();
    }

    @Override
    public AnalyticsDepartmentTopicAggregate saveDepartmentTopicAggregate(
        AnalyticsDepartmentTopicAggregate analyticsDepartmentTopicAggregate
    ) {
        throw new UnsupportedOperationException("Этот адаптер аналитики поддерживает только чтение");
    }

    @Override
    public void deleteAllDepartmentTopicAggregates() {
        throw new UnsupportedOperationException("Этот адаптер аналитики поддерживает только чтение");
    }

    private AnalyticsDepartmentTopicAggregate toDomain(AnalyticsDepartmentTopicAggregateEntity entity) {
        return new AnalyticsDepartmentTopicAggregate(
            entity.getId(),
            entity.getOrganizationalUnitIdSnapshot(),
            entity.getOrganizationalPathSnapshot(),
            entity.getTopicId(),
            entity.getPeriodStart(),
            entity.getPeriodEnd(),
            entity.getAverageScorePercent(),
            entity.getPassRatePercent(),
            entity.getAttemptCount(),
            entity.getErrorCount(),
            entity.getCalculatedAt(),
            entity.getRefreshedAt(),
            entity.getReconciledAt()
        );
    }
}
