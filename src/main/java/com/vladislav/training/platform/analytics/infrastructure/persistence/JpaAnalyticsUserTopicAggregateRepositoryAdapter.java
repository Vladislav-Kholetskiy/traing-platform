package com.vladislav.training.platform.analytics.infrastructure.persistence;

import com.vladislav.training.platform.analytics.domain.AnalyticsUserTopicAggregate;
import com.vladislav.training.platform.analytics.repository.AnalyticsUserTopicAggregateRepository;
import com.vladislav.training.platform.common.exception.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaAnalyticsUserTopicAggregateRepositoryAdapter implements AnalyticsUserTopicAggregateRepository {

    private final SpringDataAnalyticsUserTopicAggregateJpaRepository repository;

    public JpaAnalyticsUserTopicAggregateRepositoryAdapter(SpringDataAnalyticsUserTopicAggregateJpaRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public AnalyticsUserTopicAggregate findUserTopicAggregateById(Long analyticsUserTopicAggregateId) {
        return repository.findById(analyticsUserTopicAggregateId)
            .map(this::toDomain)
            .orElseThrow(() -> new NotFoundException(
                "Analytics user-topic aggregate not found: " + analyticsUserTopicAggregateId
            ));
    }

    @Override
    public AnalyticsUserTopicAggregate findUserTopicAggregate(Long userId, Long topicId, Instant periodStart, Instant periodEnd) {
        return repository.findAll().stream()
            .filter(entity -> Objects.equals(entity.getUserId(), userId))
            .filter(entity -> Objects.equals(entity.getTopicId(), topicId))
            .filter(entity -> Objects.equals(entity.getPeriodStart(), periodStart))
            .filter(entity -> Objects.equals(entity.getPeriodEnd(), periodEnd))
            .findFirst()
            .map(this::toDomain)
            .orElseThrow(() -> new NotFoundException(
                "Analytics user-topic aggregate not found: userId="
                    + userId
                    + ", topicId="
                    + topicId
                    + ", periodStart="
                    + periodStart
                    + ", periodEnd="
                    + periodEnd
            ));
    }

    @Override
    public List<AnalyticsUserTopicAggregate> findUserTopicAggregatesByUserId(Long userId) {
        return repository.findAll().stream()
            .filter(entity -> Objects.equals(entity.getUserId(), userId))
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<AnalyticsUserTopicAggregate> findUserTopicAggregatesByTopicId(Long topicId) {
        return repository.findAll().stream()
            .filter(entity -> Objects.equals(entity.getTopicId(), topicId))
            .map(this::toDomain)
            .toList();
    }

    @Override
    public AnalyticsUserTopicAggregate saveUserTopicAggregate(AnalyticsUserTopicAggregate analyticsUserTopicAggregate) {
        throw new UnsupportedOperationException("Этот адаптер аналитики поддерживает только чтение");
    }

    @Override
    public void deleteAllUserTopicAggregates() {
        throw new UnsupportedOperationException("Этот адаптер аналитики поддерживает только чтение");
    }

    private AnalyticsUserTopicAggregate toDomain(AnalyticsUserTopicAggregateEntity entity) {
        return new AnalyticsUserTopicAggregate(
            entity.getId(),
            entity.getUserId(),
            entity.getTopicId(),
            entity.getPeriodStart(),
            entity.getPeriodEnd(),
            entity.getLastAssignedFinalResultId(),
            entity.getLastAssignedFinalCompletedAt(),
            entity.getLastAssignedFinalScorePercent(),
            entity.getLastAssignedFinalPassed(),
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
