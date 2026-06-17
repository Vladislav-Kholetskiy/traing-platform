package com.vladislav.training.platform.analytics.infrastructure.persistence;

import com.vladislav.training.platform.analytics.domain.AnalyticsQuestionAggregate;
import com.vladislav.training.platform.analytics.repository.AnalyticsQuestionAggregateRepository;
import com.vladislav.training.platform.common.exception.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaAnalyticsQuestionAggregateRepositoryAdapter implements AnalyticsQuestionAggregateRepository {

    private final SpringDataAnalyticsQuestionAggregateJpaRepository repository;

    public JpaAnalyticsQuestionAggregateRepositoryAdapter(SpringDataAnalyticsQuestionAggregateJpaRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public AnalyticsQuestionAggregate findQuestionAggregateById(Long analyticsQuestionAggregateId) {
        return repository.findById(analyticsQuestionAggregateId)
            .map(this::toDomain)
            .orElseThrow(() -> new NotFoundException(
                "Analytics question aggregate not found: " + analyticsQuestionAggregateId
            ));
    }

    @Override
    public AnalyticsQuestionAggregate findQuestionAggregate(Long questionId, Instant periodStart, Instant periodEnd) {
        return repository.findAll().stream()
            .filter(entity -> Objects.equals(entity.getQuestionId(), questionId))
            .filter(entity -> Objects.equals(entity.getPeriodStart(), periodStart))
            .filter(entity -> Objects.equals(entity.getPeriodEnd(), periodEnd))
            .findFirst()
            .map(this::toDomain)
            .orElseThrow(() -> new NotFoundException(
                "Analytics question aggregate not found: questionId="
                    + questionId
                    + ", periodStart="
                    + periodStart
                    + ", periodEnd="
                    + periodEnd
            ));
    }

    @Override
    public List<AnalyticsQuestionAggregate> findQuestionAggregatesByQuestionId(Long questionId) {
        return repository.findAll().stream()
            .filter(entity -> Objects.equals(entity.getQuestionId(), questionId))
            .map(this::toDomain)
            .toList();
    }

    @Override
    public AnalyticsQuestionAggregate saveQuestionAggregate(AnalyticsQuestionAggregate analyticsQuestionAggregate) {
        throw new UnsupportedOperationException("Этот адаптер аналитики поддерживает только чтение");
    }

    @Override
    public void deleteAllQuestionAggregates() {
        throw new UnsupportedOperationException("Этот адаптер аналитики поддерживает только чтение");
    }

    private AnalyticsQuestionAggregate toDomain(AnalyticsQuestionAggregateEntity entity) {
        return new AnalyticsQuestionAggregate(
            entity.getId(),
            entity.getQuestionId(),
            entity.getPeriodStart(),
            entity.getPeriodEnd(),
            entity.getAttemptCount(),
            entity.getCorrectCount(),
            entity.getIncorrectCount(),
            entity.getAverageEarnedScore(),
            entity.getCalculatedAt(),
            entity.getRefreshedAt(),
            entity.getReconciledAt()
        );
    }
}
