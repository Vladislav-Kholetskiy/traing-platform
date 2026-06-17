package com.vladislav.training.platform.analytics.infrastructure.persistence;

import com.vladislav.training.platform.analytics.repository.ExpertQuestionAnalyticsReadRepository;
import com.vladislav.training.platform.analytics.repository.ExpertQuestionAnalyticsReadRepository.ExpertQuestionAnalyticsReadCriteria;
import com.vladislav.training.platform.analytics.repository.ExpertQuestionAnalyticsReadRepository.ExpertQuestionAnalyticsReadRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Адаптер репозитория {@code JpaExpertQuestionAnalyticsReadRepositoryAdapter}.
 */
@Repository
@Transactional(readOnly = true)
public class JpaExpertQuestionAnalyticsReadRepositoryAdapter implements ExpertQuestionAnalyticsReadRepository {

    private final EntityManager entityManager;

    public JpaExpertQuestionAnalyticsReadRepositoryAdapter(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager must not be null");
    }

    @Override
    public List<ExpertQuestionAnalyticsReadRow> findQuestionAnalyticsRows(ExpertQuestionAnalyticsReadCriteria criteria) {
        Objects.requireNonNull(criteria, "criteria must not be null");

        if (!criteria.accessReadScope().readAllowed()) {
            return List.of();
        }
        if (!criteria.accessReadScope().fullOrganizationalUnitAccess()) {
            return List.of();
        }

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ExpertQuestionAnalyticsReadRow> query = criteriaBuilder.createQuery(ExpertQuestionAnalyticsReadRow.class);
        Root<AnalyticsQuestionAggregateEntity> root = query.from(AnalyticsQuestionAggregateEntity.class);

        query.select(criteriaBuilder.construct(
            ExpertQuestionAnalyticsReadRow.class,
            root.get("questionId"),
            root.get("periodStart"),
            root.get("periodEnd"),
            root.get("attemptCount"),
            root.get("correctCount"),
            root.get("incorrectCount"),
            root.get("averageEarnedScore"),
            root.get("calculatedAt"),
            root.get("refreshedAt")
        ));
        query.where(periodOverlapPredicate(criteriaBuilder, root, criteria));
        query.orderBy(
            criteriaBuilder.asc(root.get("questionId")),
            criteriaBuilder.asc(root.get("periodStart")),
            criteriaBuilder.asc(root.get("periodEnd"))
        );

        return entityManager.createQuery(query).getResultList();
    }

    private Predicate periodOverlapPredicate(
        CriteriaBuilder criteriaBuilder,
        Root<AnalyticsQuestionAggregateEntity> root,
        ExpertQuestionAnalyticsReadCriteria criteria
    ) {
        return criteriaBuilder.and(
            criteriaBuilder.lessThan(root.get("periodStart"), criteria.periodEnd()),
            criteriaBuilder.greaterThan(root.get("periodEnd"), criteria.periodStart())
        );
    }
}
