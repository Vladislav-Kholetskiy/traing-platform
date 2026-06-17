package com.vladislav.training.platform.analytics.infrastructure.persistence;

import com.vladislav.training.platform.analytics.domain.AnalyticsCampaignAggregate;
import com.vladislav.training.platform.analytics.repository.AnalyticsCampaignAggregateRepository;
import com.vladislav.training.platform.common.exception.NotFoundException;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaAnalyticsCampaignAggregateRepositoryAdapter implements AnalyticsCampaignAggregateRepository {

    private final SpringDataAnalyticsCampaignAggregateJpaRepository repository;

    public JpaAnalyticsCampaignAggregateRepositoryAdapter(SpringDataAnalyticsCampaignAggregateJpaRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public AnalyticsCampaignAggregate findCampaignAggregateById(Long analyticsCampaignAggregateId) {
        return repository.findById(analyticsCampaignAggregateId)
            .map(this::toDomain)
            .orElseThrow(() -> new NotFoundException(
                "Analytics campaign aggregate not found: " + analyticsCampaignAggregateId
            ));
    }

    @Override
    public AnalyticsCampaignAggregate findCampaignAggregateByCampaignId(Long campaignId) {
        return repository.findByCampaignId(campaignId)
            .map(this::toDomain)
            .orElseThrow(() -> new NotFoundException(
                "Analytics campaign aggregate not found by campaignId: " + campaignId
            ));
    }

    @Override
    public List<AnalyticsCampaignAggregate> findAllCampaignAggregates() {
        return repository.findAll().stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    @Transactional
    public AnalyticsCampaignAggregate saveCampaignAggregate(AnalyticsCampaignAggregate analyticsCampaignAggregate) {
        return toDomain(repository.save(toEntity(analyticsCampaignAggregate)));
    }

    @Override
    @Transactional
    public void deleteAllCampaignAggregates() {
        repository.deleteAllInBatch();
    }

    private AnalyticsCampaignAggregate toDomain(AnalyticsCampaignAggregateEntity entity) {
        return new AnalyticsCampaignAggregate(
            entity.getId(),
            entity.getCampaignId(),
            entity.getRecipientSnapshotCount(),
            entity.getNonCancelledAssignmentsFromCampaignSnapshot(),
            entity.getCompletedAssignments(),
            entity.getOverdueAssignments(),
            entity.getNonCancelledActivePool(),
            entity.getCancelledAssignments(),
            entity.getCoveragePercent(),
            entity.getOverduePercent(),
            entity.getCalculatedAt(),
            entity.getRefreshedAt(),
            entity.getReconciledAt()
        );
    }

    private AnalyticsCampaignAggregateEntity toEntity(AnalyticsCampaignAggregate aggregate) {
        AnalyticsCampaignAggregateEntity entity = new AnalyticsCampaignAggregateEntity();
        entity.setId(aggregate.id());
        entity.setCampaignId(aggregate.campaignId());
        entity.setRecipientSnapshotCount(aggregate.recipientSnapshotCount());
        entity.setNonCancelledAssignmentsFromCampaignSnapshot(
            aggregate.nonCancelledAssignmentsFromCampaignSnapshot()
        );
        entity.setCompletedAssignments(aggregate.completedAssignments());
        entity.setOverdueAssignments(aggregate.overdueAssignments());
        entity.setNonCancelledActivePool(aggregate.nonCancelledActivePool());
        entity.setCancelledAssignments(aggregate.cancelledAssignments());
        entity.setCoveragePercent(aggregate.coveragePercent());
        entity.setOverduePercent(aggregate.overduePercent());
        entity.setCalculatedAt(aggregate.calculatedAt());
        entity.setRefreshedAt(aggregate.refreshedAt());
        entity.setReconciledAt(aggregate.reconciledAt());
        return entity;
    }
}
