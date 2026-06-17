package com.vladislav.training.platform.userorg.service;

import com.vladislav.training.platform.userorg.domain.OrganizationalNodeKind;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitType;
import com.vladislav.training.platform.userorg.repository.OrganizationalUnitRepository;
import com.vladislav.training.platform.userorg.repository.OrganizationalUnitTypeRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса чтения {@code OrganizationQueryServiceImpl}.
 */
@Service
@Transactional(readOnly = true)
public class OrganizationQueryServiceImpl implements OrganizationQueryService {

    private final OrganizationalUnitRepository organizationalUnitRepository;
    private final OrganizationalUnitTypeRepository organizationalUnitTypeRepository;

    public OrganizationQueryServiceImpl(
        OrganizationalUnitRepository organizationalUnitRepository,
        OrganizationalUnitTypeRepository organizationalUnitTypeRepository
    ) {
        this.organizationalUnitRepository = organizationalUnitRepository;
        this.organizationalUnitTypeRepository = organizationalUnitTypeRepository;
    }

    @Override
    public OrganizationalUnit findOrganizationalUnitById(Long organizationalUnitId) {
        return organizationalUnitRepository.findOrganizationalUnitById(organizationalUnitId);
    }

    @Override
    public OrganizationalUnit findOrganizationalUnitByExternalId(String externalId) {
        return organizationalUnitRepository.findOrganizationalUnitByExternalId(externalId);
    }

    @Override
    public Optional<OrganizationalUnit> findOptionalOrganizationalUnitByExternalId(String externalId) {
        return organizationalUnitRepository.findOptionalOrganizationalUnitByExternalId(externalId);
    }

    @Override
    public OrganizationalUnit findOrganizationalUnitByPath(String path) {
        return organizationalUnitRepository.findOrganizationalUnitByPath(path);
    }

    @Override
    public Optional<OrganizationalUnit> findOptionalOrganizationalUnitByPath(String path) {
        return organizationalUnitRepository.findOptionalOrganizationalUnitByPath(path);
    }

    @Override
    public List<OrganizationalUnit> findChildUnits(Long parentUnitId) {
        return organizationalUnitRepository.findChildUnits(parentUnitId);
    }

    @Override
    public List<OrganizationalUnit> findUnitsByStatus(OrganizationalUnitStatus status) {
        return organizationalUnitRepository.findUnitsByStatus(status);
    }

    @Override
    public OrganizationalUnitType findOrganizationalUnitTypeById(Long organizationalUnitTypeId) {
        return organizationalUnitTypeRepository.findOrganizationalUnitTypeById(organizationalUnitTypeId);
    }

    @Override
    public OrganizationalUnitType findOrganizationalUnitTypeByCode(String organizationalUnitTypeCode) {
        return organizationalUnitTypeRepository.findOrganizationalUnitTypeByCode(organizationalUnitTypeCode);
    }

    @Override
    public List<OrganizationalUnitType> findUnitTypesByNodeKind(OrganizationalNodeKind nodeKind) {
        return organizationalUnitTypeRepository.findUnitTypesByNodeKind(nodeKind);
    }
}
