package com.vladislav.training.platform.userorg.infrastructure.persistence;

import com.vladislav.training.platform.access.service.AccessReadScope;
import com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Контракт репозитория {@code PolicyScopedUserOrganizationAssignmentReadRepository}.
 */
@Repository
@Transactional(readOnly = true)
public class PolicyScopedUserOrganizationAssignmentReadRepository {

    private static final Sort HISTORY_SORT = Sort.by(
        Sort.Order.desc("validFrom"),
        Sort.Order.desc("id")
    );

    private final SpringDataUserOrganizationAssignmentJpaRepository repository;
    private final UserOrgMapper mapper;

    public PolicyScopedUserOrganizationAssignmentReadRepository(
        SpringDataUserOrganizationAssignmentJpaRepository repository,
        UserOrgMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public List<UserOrganizationAssignment> findOrganizationAssignmentsByUserIdWithinScope(
        AccessReadScope scope,
        Instant effectiveAt,
        Long userId
    ) {
        Specification<UserOrganizationAssignmentEntity> specification = Specification.<UserOrganizationAssignmentEntity>where(
            UserOrgReadScopeJpaSupport.currentUserVisibleWithinScope(scope, effectiveAt, "userId")
        ).and(UserOrgReadScopeJpaSupport.organizationalUnitFieldWithinScope(scope, "organizationalUnitId"))
            .and(hasUserId(userId));
        return mapper.toUserOrganizationAssignments(repository.findAll(specification, HISTORY_SORT));
    }

    public List<UserOrganizationAssignment> findActiveOrganizationAssignmentsByUserIdWithinScope(
        AccessReadScope scope,
        Instant effectiveAt,
        Long userId
    ) {
        Specification<UserOrganizationAssignmentEntity> specification = Specification.<UserOrganizationAssignmentEntity>where(
            UserOrgReadScopeJpaSupport.currentUserVisibleWithinScope(scope, effectiveAt, "userId")
        ).and(UserOrgReadScopeJpaSupport.organizationalUnitFieldWithinScope(scope, "organizationalUnitId"))
            .and(hasUserId(userId)).and(isActiveAt(effectiveAt));
        return mapper.toUserOrganizationAssignments(repository.findAll(specification, HISTORY_SORT));
    }

    private Specification<UserOrganizationAssignmentEntity> hasUserId(Long userId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("userId"), userId);
    }

    private Specification<UserOrganizationAssignmentEntity> isActiveAt(Instant activeAt) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
            criteriaBuilder.lessThanOrEqualTo(root.get("validFrom"), activeAt),
            criteriaBuilder.or(
                criteriaBuilder.isNull(root.get("validTo")),
                criteriaBuilder.greaterThan(root.get("validTo"), activeAt)
            )
        );
    }
}
