package com.vladislav.training.platform.access.infrastructure.persistence;

import com.vladislav.training.platform.access.domain.TemporaryManagementDelegation;
import com.vladislav.training.platform.access.service.AccessReadScope;
import com.vladislav.training.platform.access.service.TemporaryManagementDelegationAdminFilter;
import com.vladislav.training.platform.application.query.AccessScopeProjectionService;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Контракт репозитория {@code PolicyScopedTemporaryManagementDelegationReadRepository}.
 */
@Repository
@Transactional(readOnly = true)
public class PolicyScopedTemporaryManagementDelegationReadRepository {

    private static final Sort HISTORY_SORT = Sort.by(
        Sort.Order.desc("validFrom"),
        Sort.Order.desc("id")
    );

    private final SpringDataTemporaryManagementDelegationJpaRepository repository;
    private final AccessMapper mapper;
    private final AccessScopeProjectionService accessScopeProjectionService;

    public PolicyScopedTemporaryManagementDelegationReadRepository(
        SpringDataTemporaryManagementDelegationJpaRepository repository,
        AccessMapper mapper,
        AccessScopeProjectionService accessScopeProjectionService
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.accessScopeProjectionService = accessScopeProjectionService;
    }

    public List<TemporaryManagementDelegation> findTemporaryManagementDelegationsWithinScope(
        AccessReadScope scope,
        TemporaryManagementDelegationAdminFilter filter
    ) {
        Set<Long> organizationalUnitIds = accessScopeProjectionService.resolveOrganizationalUnitIds(scope);
        Specification<TemporaryManagementDelegationEntity> filterSpecification = Specification.<TemporaryManagementDelegationEntity>where(
            AccessReadScopeRestrictionSupport.organizationalUnitFieldWithinScope(
                scope,
                organizationalUnitIds,
                "organizationalUnitId"
            )
        );
        filterSpecification = andIfPresent(filterSpecification, hasUserId(filter.userId()));
        filterSpecification = andIfPresent(filterSpecification, hasOrganizationalUnitId(filter.organizationalUnitId()));
        filterSpecification = andIfPresent(filterSpecification, hasManagementRelationTypeId(filter.managementRelationTypeId()));
        filterSpecification = andIfPresent(filterSpecification, isActiveAt(filter.activeAt()));
        return mapper.toTemporaryManagementDelegations(repository.findAll(filterSpecification, HISTORY_SORT));
    }

    private <T> Specification<T> andIfPresent(Specification<T> base, Specification<T> candidate) {
        return candidate == null ? base : base.and(candidate);
    }

    private Specification<TemporaryManagementDelegationEntity> hasUserId(Long userId) {
        return userId == null ? null : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("userId"), userId);
    }

    private Specification<TemporaryManagementDelegationEntity> hasOrganizationalUnitId(Long organizationalUnitId) {
        return organizationalUnitId == null
            ? null
            : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("organizationalUnitId"), organizationalUnitId);
    }

    private Specification<TemporaryManagementDelegationEntity> hasManagementRelationTypeId(Long managementRelationTypeId) {
        return managementRelationTypeId == null
            ? null
            : (root, query, criteriaBuilder) -> criteriaBuilder.equal(
                root.get("managementRelationTypeId"),
                managementRelationTypeId
            );
    }

    private Specification<TemporaryManagementDelegationEntity> isActiveAt(java.time.Instant activeAt) {
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
