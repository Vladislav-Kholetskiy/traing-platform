package com.vladislav.training.platform.userorg.infrastructure.persistence;

import com.vladislav.training.platform.access.service.AccessReadScope;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Контракт репозитория {@code PolicyScopedOrganizationalUnitReadRepository}.
 */
@Repository
@Transactional(readOnly = true)
public class PolicyScopedOrganizationalUnitReadRepository {

    private final SpringDataOrganizationalUnitJpaRepository repository;
    private final UserOrgMapper mapper;

    public PolicyScopedOrganizationalUnitReadRepository(
            SpringDataOrganizationalUnitJpaRepository repository,
            UserOrgMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public Optional<OrganizationalUnit> findOrganizationalUnitByIdWithinScope(
            AccessReadScope scope,
            Long organizationalUnitId
    ) {
        return repository.findOne(withScope(scope).and(hasId(organizationalUnitId)))
                .map(mapper::toDomain);
    }

    public Optional<OrganizationalUnit> findOrganizationalUnitByPathWithinScope(
            AccessReadScope scope,
            String path
    ) {
        return repository.findOne(withScope(scope).and(hasPath(path)))
                .map(mapper::toDomain);
    }

    public List<OrganizationalUnit> findChildUnitsWithinScope(
            AccessReadScope scope,
            Long parentUnitId
    ) {
        return mapper.toOrganizationalUnitDomains(
                repository.findAll(withScope(scope).and(hasParentId(parentUnitId)), Sort.by("id"))
        );
    }

    public List<OrganizationalUnit> findUnitsWithinScope(
            AccessReadScope scope,
            OrganizationalUnitStatus status
    ) {
        Specification<OrganizationalUnitEntity> querySpecification = withScope(scope);
        if (status != null) {
            querySpecification = querySpecification.and(hasStatus(status));
        }

        return mapper.toOrganizationalUnitDomains(repository.findAll(querySpecification, Sort.by("id")));
    }

    public List<OrganizationalUnit> findOrganizationalUnitsByIdsWithinScope(
            AccessReadScope scope,
            Collection<Long> organizationalUnitIds
    ) {
        if (organizationalUnitIds == null || organizationalUnitIds.isEmpty()) {
            return List.of();
        }
        return mapper.toOrganizationalUnitDomains(
                repository.findAll(withScope(scope).and(hasIds(organizationalUnitIds)), Sort.by("id"))
        );
    }

    private Specification<OrganizationalUnitEntity> withScope(AccessReadScope scope) {
        return Specification.where(UserOrgReadScopeJpaSupport.organizationalUnitWithinScope(scope));
    }

    private Specification<OrganizationalUnitEntity> hasId(Long organizationalUnitId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("id"), organizationalUnitId);
    }

    private Specification<OrganizationalUnitEntity> hasIds(Collection<Long> organizationalUnitIds) {
        return (root, query, criteriaBuilder) -> root.get("id").in(organizationalUnitIds);
    }

    private Specification<OrganizationalUnitEntity> hasPath(String path) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("path"), path);
    }

    private Specification<OrganizationalUnitEntity> hasParentId(Long parentUnitId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("parentId"), parentUnitId);
    }

    private Specification<OrganizationalUnitEntity> hasStatus(OrganizationalUnitStatus status) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status);
    }
}