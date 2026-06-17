package com.vladislav.training.platform.userorg.infrastructure.persistence;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.repository.OrganizationalUnitRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Адаптер репозитория {@code JpaOrganizationalUnitRepositoryAdapter}.
 */
@Repository
@Transactional(readOnly = true)
public class JpaOrganizationalUnitRepositoryAdapter implements OrganizationalUnitRepository {

    private final SpringDataOrganizationalUnitJpaRepository repository;
    private final UserOrgMapper mapper;

    public JpaOrganizationalUnitRepositoryAdapter(
        SpringDataOrganizationalUnitJpaRepository repository,
        UserOrgMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public OrganizationalUnit findOrganizationalUnitById(Long organizationalUnitId) {
        return repository.findById(organizationalUnitId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException("Organizational unit not found by id: " + organizationalUnitId));
    }

    @Override
    public OrganizationalUnit findOrganizationalUnitByExternalId(String externalId) {
        return repository.findByExternalId(externalId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException("Organizational unit not found by externalId: " + externalId));
    }

    @Override
    public Optional<OrganizationalUnit> findOptionalOrganizationalUnitByExternalId(String externalId) {
        if (externalId == null) {
            return Optional.empty();
        }
        return repository.findByExternalId(externalId).map(mapper::toDomain);
    }

    @Override
    public OrganizationalUnit findOrganizationalUnitByPath(String path) {
        return repository.findByPath(path)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException("Organizational unit not found by path: " + path));
    }

    @Override
    public Optional<OrganizationalUnit> findOptionalOrganizationalUnitByPath(String path) {
        if (path == null) {
            return Optional.empty();
        }
        return repository.findByPath(path).map(mapper::toDomain);
    }

    @Override
    public List<OrganizationalUnit> findRootUnits() {
        return mapper.toOrganizationalUnitDomains(repository.findAllByParentIdIsNullOrderByIdAsc());
    }

    @Override
    public List<OrganizationalUnit> findChildUnits(Long parentUnitId) {
        return mapper.toOrganizationalUnitDomains(repository.findAllByParentIdOrderByIdAsc(parentUnitId));
    }

    @Override
    public List<OrganizationalUnit> findUnitsByStatus(OrganizationalUnitStatus status) {
        return mapper.toOrganizationalUnitDomains(repository.findAllByStatusOrderByIdAsc(status));
    }

    @Override
    public boolean existsOrganizationalUnitByPath(String path) {
        return repository.existsByPath(path);
    }

    @Override
    public boolean existsOrganizationalUnitByPathAndIdNot(String path, Long excludedOrganizationalUnitId) {
        return repository.existsByPathAndIdNot(path, excludedOrganizationalUnitId);
    }

    @Override
    public boolean existsOrganizationalUnitByExternalId(String externalId) {
        if (externalId == null) {
            return false;
        }
        return repository.existsByExternalId(externalId);
    }

    @Override
    public boolean existsOrganizationalUnitByExternalIdAndIdNot(String externalId, Long excludedOrganizationalUnitId) {
        if (externalId == null) {
            return false;
        }
        return repository.existsByExternalIdAndIdNot(externalId, excludedOrganizationalUnitId);
    }

    @Override
    @Transactional
    public OrganizationalUnit saveOrganizationalUnit(OrganizationalUnit organizationalUnit) {
        OrganizationalUnitEntity savedEntity = repository.save(mapper.toEntity(organizationalUnit));
        return mapper.toDomain(savedEntity);
    }
}
