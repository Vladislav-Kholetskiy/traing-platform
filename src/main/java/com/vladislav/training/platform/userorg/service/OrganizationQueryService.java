package com.vladislav.training.platform.userorg.service;

import com.vladislav.training.platform.userorg.domain.OrganizationalNodeKind;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitType;
import java.util.List;
import java.util.Optional;

/**
 * Контракт сервиса чтения {@code OrganizationQueryService}.
 */
public interface OrganizationQueryService {

    OrganizationalUnit findOrganizationalUnitById(Long organizationalUnitId);

    OrganizationalUnit findOrganizationalUnitByExternalId(String externalId);

    Optional<OrganizationalUnit> findOptionalOrganizationalUnitByExternalId(String externalId);

    OrganizationalUnit findOrganizationalUnitByPath(String path);

    Optional<OrganizationalUnit> findOptionalOrganizationalUnitByPath(String path);

    List<OrganizationalUnit> findChildUnits(Long parentUnitId);

    List<OrganizationalUnit> findUnitsByStatus(OrganizationalUnitStatus status);

    OrganizationalUnitType findOrganizationalUnitTypeById(Long organizationalUnitTypeId);

    OrganizationalUnitType findOrganizationalUnitTypeByCode(String organizationalUnitTypeCode);

    List<OrganizationalUnitType> findUnitTypesByNodeKind(OrganizationalNodeKind nodeKind);
}
