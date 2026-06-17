package com.vladislav.training.platform.userorg.infrastructure.persistence;

import com.vladislav.training.platform.access.service.AccessReadScope;
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Контракт репозитория {@code PolicyScopedAppUserReadRepository}.
 */
@Repository
@Transactional(readOnly = true)
public class PolicyScopedAppUserReadRepository {

    private static final Sort USER_SORT = Sort.by("id");

    private final SpringDataAppUserJpaRepository repository;
    private final UserOrgMapper mapper;

    public PolicyScopedAppUserReadRepository(SpringDataAppUserJpaRepository repository, UserOrgMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public List<AppUser> findUsersWithinScope(AccessReadScope scope, UserStatus status, Instant effectiveAt) {
        Specification<AppUserEntity> specification = Specification.<AppUserEntity>where(
            UserOrgReadScopeJpaSupport.currentUserVisibleWithinScope(scope, effectiveAt, "id")
        ).and(hasStatus(status));
        return mapper.toAppUsers(repository.findAll(specification, USER_SORT));
    }

    public Optional<AppUser> findUserByIdWithinScope(AccessReadScope scope, Instant effectiveAt, Long userId) {
        Specification<AppUserEntity> specification = Specification.<AppUserEntity>where(
            UserOrgReadScopeJpaSupport.currentUserVisibleWithinScope(scope, effectiveAt, "id")
        ).and(hasId(userId));
        return repository.findOne(specification).map(mapper::toDomain);
    }

    private Specification<AppUserEntity> hasStatus(UserStatus status) {
        return status == null
            ? (root, query, criteriaBuilder) -> criteriaBuilder.conjunction()
            : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status);
    }

    private Specification<AppUserEntity> hasId(Long userId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("id"), userId);
    }
}
