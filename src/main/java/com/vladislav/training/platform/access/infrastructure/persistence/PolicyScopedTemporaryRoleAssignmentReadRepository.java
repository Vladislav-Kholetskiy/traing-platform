package com.vladislav.training.platform.access.infrastructure.persistence;

import com.vladislav.training.platform.access.domain.TemporaryRoleAssignment;
import com.vladislav.training.platform.access.service.AccessReadScope;
import com.vladislav.training.platform.access.service.TemporaryRoleAssignmentAdminFilter;
import com.vladislav.training.platform.application.query.AccessScopeProjectionService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Контракт репозитория {@code PolicyScopedTemporaryRoleAssignmentReadRepository}.
 */
@Repository
@Transactional(readOnly = true)
public class PolicyScopedTemporaryRoleAssignmentReadRepository {

    private static final Sort HISTORY_SORT = Sort.by(
        Sort.Order.desc("validFrom"),
        Sort.Order.desc("id")
    );

    private final SpringDataTemporaryRoleAssignmentJpaRepository repository;
    private final AccessMapper mapper;
    private final AccessScopeProjectionService accessScopeProjectionService;

    public PolicyScopedTemporaryRoleAssignmentReadRepository(
        SpringDataTemporaryRoleAssignmentJpaRepository repository,
        AccessMapper mapper,
        AccessScopeProjectionService accessScopeProjectionService
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.accessScopeProjectionService = accessScopeProjectionService;
    }

    public List<TemporaryRoleAssignment> findTemporaryRoleAssignmentsWithinScope(
        AccessReadScope scope,
        Instant effectiveAt,
        TemporaryRoleAssignmentAdminFilter filter
    ) {
        Set<Long> visibleUserIds = accessScopeProjectionService.resolveVisibleUserIds(scope, effectiveAt);
        Specification<TemporaryRoleAssignmentEntity> specification = Specification.<TemporaryRoleAssignmentEntity>where(
            AccessReadScopeRestrictionSupport.currentUserVisibleWithinScope(scope, visibleUserIds, "userId")
        );
        specification = andIfPresent(specification, hasUserId(filter.userId()));
        specification = andIfPresent(specification, hasRoleId(filter.roleId()));
        specification = andIfPresent(specification, isActiveAt(filter.activeAt()));
        return mapper.toTemporaryRoleAssignments(repository.findAll(specification, HISTORY_SORT));
    }

    private <T> Specification<T> andIfPresent(Specification<T> base, Specification<T> candidate) {
        return candidate == null ? base : base.and(candidate);
    }

    private Specification<TemporaryRoleAssignmentEntity> hasUserId(Long userId) {
        return userId == null ? null : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("userId"), userId);
    }

    private Specification<TemporaryRoleAssignmentEntity> hasRoleId(Long roleId) {
        return roleId == null ? null : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("roleId"), roleId);
    }

    private Specification<TemporaryRoleAssignmentEntity> isActiveAt(Instant activeAt) {
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
