package com.vladislav.training.platform.userorg.infrastructure.persistence;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.userorg.domain.OrganizationalNodeKind;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitType;
import com.vladislav.training.platform.userorg.repository.OrganizationalUnitTypeRepository;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Адаптер репозитория {@code JpaOrganizationalUnitTypeRepositoryAdapter}.
 */
@Repository
@Transactional(readOnly = true)
public class JpaOrganizationalUnitTypeRepositoryAdapter implements OrganizationalUnitTypeRepository {

    private final SpringDataOrganizationalUnitTypeJpaRepository repository;
    private final UserOrgMapper mapper;

    public JpaOrganizationalUnitTypeRepositoryAdapter(
        SpringDataOrganizationalUnitTypeJpaRepository repository,
        UserOrgMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public OrganizationalUnitType findOrganizationalUnitTypeById(Long organizationalUnitTypeId) {
        return repository.findById(organizationalUnitTypeId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException(
                "Organizational unit type not found by id: " + organizationalUnitTypeId
            ));
    }

    @Override
    public OrganizationalUnitType findOrganizationalUnitTypeByCode(String organizationalUnitTypeCode) {
        return repository.findByCode(organizationalUnitTypeCode)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException(
                "Organizational unit type not found by code: " + organizationalUnitTypeCode
            ));
    }

    @Override
    public List<OrganizationalUnitType> findUnitTypesByNodeKind(OrganizationalNodeKind nodeKind) {
        return mapper.toOrganizationalUnitTypeDomains(repository.findAllByNodeKindOrderByIdAsc(nodeKind));
    }

    @Override
    public boolean existsOrganizationalUnitTypeByCode(String code) {
        return repository.existsByCode(code);
    }

    @Override
    public boolean existsOrganizationalUnitTypeByCodeAndIdNot(String code, Long excludedOrganizationalUnitTypeId) {
        return repository.existsByCodeAndIdNot(code, excludedOrganizationalUnitTypeId);
    }

    @Override
    @Transactional
    public OrganizationalUnitType saveOrganizationalUnitType(OrganizationalUnitType organizationalUnitType) {
        OrganizationalUnitTypeEntity savedEntity = repository.save(mapper.toEntity(organizationalUnitType));
        return mapper.toDomain(savedEntity);
    }
}
