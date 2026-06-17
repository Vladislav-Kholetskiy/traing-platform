package com.vladislav.training.platform.access.infrastructure.persistence;

import com.vladislav.training.platform.access.domain.ManagementRelation;
import com.vladislav.training.platform.access.service.AccessReadScope;
import com.vladislav.training.platform.access.service.ManagementRelationAdminFilter;
import com.vladislav.training.platform.application.query.AccessScopeProjectionService;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Контракт репозитория {@code PolicyScopedManagementRelationReadRepository}.
 */
@Repository
@Transactional(readOnly = true)
public class PolicyScopedManagementRelationReadRepository {

    private static final Sort HISTORY_SORT = Sort.by(
        Sort.Order.desc("validFrom"),
        Sort.Order.desc("id")
    );

    private final SpringDataManagementRelationJpaRepository repository;
    private final AccessMapper mapper;
    private final AccessScopeProjectionService accessScopeProjectionService;

    public PolicyScopedManagementRelationReadRepository(
        SpringDataManagementRelationJpaRepository repository,
        AccessMapper mapper,
        AccessScopeProjectionService accessScopeProjectionService
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.accessScopeProjectionService = accessScopeProjectionService;
    }

    public List<ManagementRelation> findManagementRelationsWithinScope(
        AccessReadScope scope,
        ManagementRelationAdminFilter filter
    ) {
        Set<Long> organizationalUnitIds = accessScopeProjectionService.resolveOrganizationalUnitIds(scope);
        Specification<ManagementRelationEntity> filterSpecification = Specification.<ManagementRelationEntity>where(
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
        return mapper.toManagementRelations(repository.findAll(filterSpecification, HISTORY_SORT));
    }

    private <T> Specification<T> andIfPresent(Specification<T> base, Specification<T> candidate) {
        return candidate == null ? base : base.and(candidate);
    }

    private Specification<ManagementRelationEntity> hasUserId(Long userId) {
        return userId == null ? null : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("userId"), userId);
    }

    private Specification<ManagementRelationEntity> hasOrganizationalUnitId(Long organizationalUnitId) {
        return organizationalUnitId == null
            ? null
            : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("organizationalUnitId"), organizationalUnitId);
    }

    private Specification<ManagementRelationEntity> hasManagementRelationTypeId(Long managementRelationTypeId) {
        return managementRelationTypeId == null
            ? null
            : (root, query, criteriaBuilder) -> criteriaBuilder.equal(
                root.get("managementRelationTypeId"),
                managementRelationTypeId
            );
    }

    private Specification<ManagementRelationEntity> isActiveAt(java.time.Instant activeAt) {
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
