package com.vladislav.training.platform.access.infrastructure.persistence;

import com.vladislav.training.platform.access.domain.TemporaryAccessArea;
import com.vladislav.training.platform.access.service.AccessReadScope;
import com.vladislav.training.platform.access.service.TemporaryAccessAreaAdminFilter;
import com.vladislav.training.platform.application.query.AccessScopeProjectionService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Контракт репозитория {@code PolicyScopedTemporaryAccessAreaReadRepository}.
 */
@Repository
@Transactional(readOnly = true)
public class PolicyScopedTemporaryAccessAreaReadRepository {

    private static final Sort HISTORY_SORT = Sort.by(
        Sort.Order.desc("validFrom"),
        Sort.Order.desc("id")
    );

    private final SpringDataTemporaryAccessAreaJpaRepository repository;
    private final AccessMapper mapper;
    private final AccessScopeProjectionService accessScopeProjectionService;

    public PolicyScopedTemporaryAccessAreaReadRepository(
        SpringDataTemporaryAccessAreaJpaRepository repository,
        AccessMapper mapper,
        AccessScopeProjectionService accessScopeProjectionService
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.accessScopeProjectionService = accessScopeProjectionService;
    }

    public List<TemporaryAccessArea> findTemporaryAccessAreasWithinScope(
        AccessReadScope scope,
        Instant effectiveAt,
        TemporaryAccessAreaAdminFilter filter
    ) {
        Set<Long> organizationalUnitIds = accessScopeProjectionService.resolveOrganizationalUnitIds(scope);
        Set<Long> visibleUserIds = accessScopeProjectionService.resolveVisibleUserIds(scope, effectiveAt);
        Specification<TemporaryAccessAreaEntity> filterSpecification = Specification.<TemporaryAccessAreaEntity>where(
            AccessReadScopeRestrictionSupport.accessAreaFieldWithinScope(
                scope,
                organizationalUnitIds,
                visibleUserIds,
                "organizationalUnitId",
                "userId"
            )
        );
        filterSpecification = andIfPresent(filterSpecification, hasUserId(filter.userId()));
        filterSpecification = andIfPresent(filterSpecification, hasOrganizationalUnitId(filter.organizationalUnitId()));
        filterSpecification = andIfPresent(filterSpecification, hasAccessScopeType(filter));
        filterSpecification = andIfPresent(filterSpecification, isActiveAt(filter.activeAt()));
        return mapper.toTemporaryAccessAreas(repository.findAll(filterSpecification, HISTORY_SORT));
    }

    private <T> Specification<T> andIfPresent(Specification<T> base, Specification<T> candidate) {
        return candidate == null ? base : base.and(candidate);
    }

    private Specification<TemporaryAccessAreaEntity> hasUserId(Long userId) {
        return userId == null ? null : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("userId"), userId);
    }

    private Specification<TemporaryAccessAreaEntity> hasOrganizationalUnitId(Long organizationalUnitId) {
        return organizationalUnitId == null
            ? null
            : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("organizationalUnitId"), organizationalUnitId);
    }

    private Specification<TemporaryAccessAreaEntity> hasAccessScopeType(TemporaryAccessAreaAdminFilter filter) {
        return filter.accessScopeType() == null
            ? null
            : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("accessScopeType"), filter.accessScopeType());
    }

    private Specification<TemporaryAccessAreaEntity> isActiveAt(Instant activeAt) {
        return activeAt == null
            ? null
            : (root, query, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.lessThanOrEqualTo(root.get("validFrom"), activeAt),
                criteriaBuilder.or(
                    criteriaBuilder.isNull(root.get("validTo")),
                    criteriaBuilder.greaterThan(root.get("validTo"), activeAt)
                )
            );
    }
}
