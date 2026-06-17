package com.vladislav.training.platform.userorg.infrastructure.persistence;

import com.vladislav.training.platform.access.service.AccessReadScope;
import com.vladislav.training.platform.userorg.domain.UserRoleAssignment;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Контракт репозитория {@code PolicyScopedUserRoleAssignmentReadRepository}.
 */
@Repository
@Transactional(readOnly = true)
public class PolicyScopedUserRoleAssignmentReadRepository {

    private static final Sort HISTORY_SORT = Sort.by(
        Sort.Order.desc("validFrom"),
        Sort.Order.desc("id")
    );

    private final SpringDataUserRoleAssignmentJpaRepository repository;
    private final UserOrgMapper mapper;

    public PolicyScopedUserRoleAssignmentReadRepository(
        SpringDataUserRoleAssignmentJpaRepository repository,
        UserOrgMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public List<UserRoleAssignment> findRoleAssignmentsByUserIdWithinScope(
        AccessReadScope scope,
        Instant effectiveAt,
        Long userId
    ) {
        Specification<UserRoleAssignmentEntity> specification = Specification.<UserRoleAssignmentEntity>where(
            UserOrgReadScopeJpaSupport.currentUserVisibleWithinScope(scope, effectiveAt, "userId")
        ).and(hasUserId(userId));
        return mapper.toUserRoleAssignments(repository.findAll(specification, HISTORY_SORT));
    }

    public List<UserRoleAssignment> findActiveRoleAssignmentsByUserIdWithinScope(
        AccessReadScope scope,
        Instant effectiveAt,
        Long userId
    ) {
        Specification<UserRoleAssignmentEntity> specification = Specification.<UserRoleAssignmentEntity>where(
            UserOrgReadScopeJpaSupport.currentUserVisibleWithinScope(scope, effectiveAt, "userId")
        ).and(hasUserId(userId)).and(isActiveAt(effectiveAt));
        return mapper.toUserRoleAssignments(repository.findAll(specification, HISTORY_SORT));
    }

    private Specification<UserRoleAssignmentEntity> hasUserId(Long userId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("userId"), userId);
    }

    private Specification<UserRoleAssignmentEntity> isActiveAt(Instant activeAt) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
            criteriaBuilder.lessThanOrEqualTo(root.get("validFrom"), activeAt),
            criteriaBuilder.or(
                criteriaBuilder.isNull(root.get("validTo")),
                criteriaBuilder.greaterThan(root.get("validTo"), activeAt)
            )
        );
    }
}
