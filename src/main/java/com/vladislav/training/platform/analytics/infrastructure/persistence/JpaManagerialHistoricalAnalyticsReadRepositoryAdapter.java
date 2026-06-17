package com.vladislav.training.platform.analytics.infrastructure.persistence;

import com.vladislav.training.platform.analytics.repository.ManagerialHistoricalAnalyticsReadRepository;
import com.vladislav.training.platform.analytics.repository.ManagerialHistoricalAnalyticsReadRepository.ManagerialDepartmentTopicAnalyticsReadRow;
import com.vladislav.training.platform.analytics.repository.ManagerialHistoricalAnalyticsReadRepository.ManagerialHistoricalAnalyticsReadCriteria;
import com.vladislav.training.platform.analytics.repository.ManagerialHistoricalAnalyticsReadRepository.ManagerialUserTopicAnalyticsReadRow;
import com.vladislav.training.platform.content.infrastructure.persistence.TopicEntity;
import com.vladislav.training.platform.result.infrastructure.persistence.ResultEntity;
import com.vladislav.training.platform.userorg.infrastructure.persistence.AppUserEntity;
import com.vladislav.training.platform.userorg.infrastructure.persistence.OrganizationalUnitEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaManagerialHistoricalAnalyticsReadRepositoryAdapter implements ManagerialHistoricalAnalyticsReadRepository {

    private final EntityManager entityManager;

    public JpaManagerialHistoricalAnalyticsReadRepositoryAdapter(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager must not be null");
    }

    @Override
    public List<ManagerialUserTopicAnalyticsReadRow> findUserTopicRows(ManagerialHistoricalAnalyticsReadCriteria criteria) {
        Objects.requireNonNull(criteria, "criteria must not be null");
        if (!criteria.managerialReadScope().readScope().readAllowed()) {
            return List.of();
        }

        var readScope = criteria.managerialReadScope().readScope();
        if (!readScope.fullOrganizationalUnitAccess()
            && readScope.unitOnlyIds().isEmpty()
            && readScope.subtreePaths().isEmpty()) {
            return List.of();
        }

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ManagerialUserTopicAnalyticsReadRow> query = criteriaBuilder.createQuery(
            ManagerialUserTopicAnalyticsReadRow.class
        );
        Root<AnalyticsUserTopicAggregateEntity> root = query.from(AnalyticsUserTopicAggregateEntity.class);
        Root<AppUserEntity> userRoot = query.from(AppUserEntity.class);
        Root<TopicEntity> topicRoot = query.from(TopicEntity.class);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(periodRangePredicate(criteriaBuilder, root, criteria.periodStart(), criteria.periodEnd()));
        predicates.add(criteriaBuilder.equal(userRoot.get("id"), root.get("userId")));
        predicates.add(criteriaBuilder.equal(topicRoot.get("id"), root.get("topicId")));

        Predicate scopePredicate = userTopicScopePredicate(criteriaBuilder, query, root, criteria);
        if (scopePredicate == null) {
            return List.of();
        }
        predicates.add(scopePredicate);

        query.select(criteriaBuilder.construct(
            ManagerialUserTopicAnalyticsReadRow.class,
            root.get("userId"),
            userRoot.get("employeeNumber"),
            userRoot.get("lastName"),
            userRoot.get("firstName"),
            userRoot.get("middleName"),
            root.get("topicId"),
            topicRoot.get("name"),
            root.get("periodStart"),
            root.get("periodEnd"),
            root.get("averageScorePercent"),
            root.get("passRatePercent"),
            root.get("attemptCount"),
            root.get("errorCount"),
            root.get("calculatedAt"),
            root.get("refreshedAt")
        ));
        query.where(predicates.toArray(Predicate[]::new));
        query.orderBy(
            criteriaBuilder.asc(root.get("userId")),
            criteriaBuilder.asc(root.get("topicId")),
            criteriaBuilder.asc(root.get("periodStart"))
        );

        return entityManager.createQuery(query).getResultList();
    }

    @Override
    public List<ManagerialDepartmentTopicAnalyticsReadRow> findDepartmentTopicRows(
        ManagerialHistoricalAnalyticsReadCriteria criteria
    ) {
        Objects.requireNonNull(criteria, "criteria must not be null");
        if (!criteria.managerialReadScope().readScope().readAllowed()) {
            return List.of();
        }

        var readScope = criteria.managerialReadScope().readScope();
        if (!readScope.fullOrganizationalUnitAccess()
            && readScope.unitOnlyIds().isEmpty()
            && readScope.subtreePaths().isEmpty()) {
            return List.of();
        }

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ManagerialDepartmentTopicAnalyticsReadRow> query = criteriaBuilder.createQuery(
            ManagerialDepartmentTopicAnalyticsReadRow.class
        );
        Root<AnalyticsDepartmentTopicAggregateEntity> root = query.from(AnalyticsDepartmentTopicAggregateEntity.class);
        Root<OrganizationalUnitEntity> organizationalUnitRoot = query.from(OrganizationalUnitEntity.class);
        Root<TopicEntity> topicRoot = query.from(TopicEntity.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(periodRangePredicate(criteriaBuilder, root, criteria.periodStart(), criteria.periodEnd()));
        predicates.add(criteriaBuilder.equal(
            organizationalUnitRoot.get("id"),
            root.get("organizationalUnitIdSnapshot")
        ));
        predicates.add(criteriaBuilder.equal(topicRoot.get("id"), root.get("topicId")));

        Predicate scopePredicate = departmentScopePredicate(criteriaBuilder, root, criteria);
        if (scopePredicate == null) {
            return List.of();
        }
        predicates.add(scopePredicate);

        query.select(criteriaBuilder.construct(
            ManagerialDepartmentTopicAnalyticsReadRow.class,
            root.get("organizationalUnitIdSnapshot"),
            organizationalUnitRoot.get("name"),
            root.get("organizationalPathSnapshot"),
            root.get("topicId"),
            topicRoot.get("name"),
            root.get("periodStart"),
            root.get("periodEnd"),
            root.get("averageScorePercent"),
            root.get("passRatePercent"),
            root.get("attemptCount"),
            root.get("errorCount"),
            root.get("calculatedAt"),
            root.get("refreshedAt")
        ));
        query.where(predicates.toArray(Predicate[]::new));
        query.orderBy(
            criteriaBuilder.asc(root.get("organizationalUnitIdSnapshot")),
            criteriaBuilder.asc(root.get("topicId")),
            criteriaBuilder.asc(root.get("periodStart"))
        );

        TypedQuery<ManagerialDepartmentTopicAnalyticsReadRow> typedQuery = entityManager.createQuery(query);
        return typedQuery.getResultList();
    }

    private Predicate periodRangePredicate(
        CriteriaBuilder criteriaBuilder,
        Root<?> root,
        Instant periodStart,
        Instant periodEnd
    ) {
        return criteriaBuilder.and(
            criteriaBuilder.greaterThanOrEqualTo(root.get("periodStart"), periodStart),
            criteriaBuilder.lessThanOrEqualTo(root.get("periodEnd"), periodEnd)
        );
    }

    private Predicate departmentScopePredicate(
        CriteriaBuilder criteriaBuilder,
        Root<AnalyticsDepartmentTopicAggregateEntity> root,
        ManagerialHistoricalAnalyticsReadCriteria criteria
    ) {
        var readScope = criteria.managerialReadScope().readScope();
        if (readScope.fullOrganizationalUnitAccess()) {
            return criteriaBuilder.conjunction();
        }

        List<Predicate> allowedPredicates = new ArrayList<>();
        if (!readScope.unitOnlyIds().isEmpty()) {
            allowedPredicates.add(root.get("organizationalUnitIdSnapshot").in(readScope.unitOnlyIds()));
        }
        if (!readScope.subtreePaths().isEmpty()) {
            Path<String> path = root.get("organizationalPathSnapshot");
            List<Predicate> subtreePredicates = new ArrayList<>();
            for (String rawPrefix : readScope.subtreePaths()) {
                String prefix = normalizePathPrefix(rawPrefix);
                subtreePredicates.add(criteriaBuilder.or(
                    criteriaBuilder.equal(path, prefix),
                    criteriaBuilder.like(path, prefix + "/%")
                ));
            }
            allowedPredicates.add(criteriaBuilder.or(subtreePredicates.toArray(Predicate[]::new)));
        }

        if (allowedPredicates.isEmpty()) {
            return null;
        }
        return criteriaBuilder.or(allowedPredicates.toArray(Predicate[]::new));
    }

    private Predicate userTopicScopePredicate(
        CriteriaBuilder criteriaBuilder,
        CriteriaQuery<?> query,
        Root<AnalyticsUserTopicAggregateEntity> root,
        ManagerialHistoricalAnalyticsReadCriteria criteria
    ) {
        var readScope = criteria.managerialReadScope().readScope();
        if (readScope.fullOrganizationalUnitAccess()) {
            return criteriaBuilder.conjunction();
        }

        List<Predicate> resultScopePredicates = new ArrayList<>();
        if (!readScope.unitOnlyIds().isEmpty()) {
            resultScopePredicates.add(resultUnitScopePredicate(criteriaBuilder, query, root, readScope.unitOnlyIds()));
        }
        if (!readScope.subtreePaths().isEmpty()) {
            resultScopePredicates.add(resultSubtreeScopePredicate(criteriaBuilder, query, root, readScope.subtreePaths()));
        }

        if (resultScopePredicates.isEmpty()) {
            return null;
        }
        return criteriaBuilder.or(resultScopePredicates.toArray(Predicate[]::new));
    }

    private Predicate resultUnitScopePredicate(
        CriteriaBuilder criteriaBuilder,
        CriteriaQuery<?> query,
        Root<AnalyticsUserTopicAggregateEntity> root,
        java.util.Set<Long> unitIds
    ) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<ResultEntity> resultRoot = subquery.from(ResultEntity.class);
        subquery.select(resultRoot.get("id"));
        subquery.where(
            criteriaBuilder.and(
                criteriaBuilder.equal(resultRoot.get("id"), root.get("lastAssignedFinalResultId")),
                resultRoot.get("organizationalUnitIdSnapshot").in(unitIds)
            )
        );
        return criteriaBuilder.exists(subquery);
    }

    private Predicate resultSubtreeScopePredicate(
        CriteriaBuilder criteriaBuilder,
        CriteriaQuery<?> query,
        Root<AnalyticsUserTopicAggregateEntity> root,
        java.util.Set<String> subtreePaths
    ) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<ResultEntity> resultRoot = subquery.from(ResultEntity.class);
        Path<String> resultPath = resultRoot.get("organizationalPathSnapshot");
        List<Predicate> subtreePredicates = new ArrayList<>();
        for (String rawPrefix : subtreePaths) {
            String prefix = normalizePathPrefix(rawPrefix);
            subtreePredicates.add(criteriaBuilder.or(
                criteriaBuilder.equal(resultPath, prefix),
                criteriaBuilder.like(resultPath, prefix + "/%")
            ));
        }
        subquery.select(resultRoot.get("id"));
        subquery.where(
            criteriaBuilder.and(
                criteriaBuilder.equal(resultRoot.get("id"), root.get("lastAssignedFinalResultId")),
                criteriaBuilder.or(subtreePredicates.toArray(Predicate[]::new))
            )
        );
        return criteriaBuilder.exists(subquery);
    }

    private String normalizePathPrefix(String rawPrefix) {
        String prefix = rawPrefix;
        while (prefix.length() > 1 && prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return prefix;
    }
}
