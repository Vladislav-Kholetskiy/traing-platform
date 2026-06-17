package com.vladislav.training.platform.userorg.repository;

import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import java.util.List;
import java.util.Optional;

/**
 * Контракт репозитория {@code OrganizationalUnitRepository}.
 */
public interface OrganizationalUnitRepository {

    OrganizationalUnit findOrganizationalUnitById(Long organizationalUnitId);

    OrganizationalUnit findOrganizationalUnitByExternalId(String externalId);

    Optional<OrganizationalUnit> findOptionalOrganizationalUnitByExternalId(String externalId);

    OrganizationalUnit findOrganizationalUnitByPath(String path);

    Optional<OrganizationalUnit> findOptionalOrganizationalUnitByPath(String path);

    List<OrganizationalUnit> findRootUnits();

    List<OrganizationalUnit> findChildUnits(Long parentUnitId);

    List<OrganizationalUnit> findUnitsByStatus(OrganizationalUnitStatus status);

    boolean existsOrganizationalUnitByPath(String path);

    boolean existsOrganizationalUnitByPathAndIdNot(String path, Long excludedOrganizationalUnitId);

    boolean existsOrganizationalUnitByExternalId(String externalId);

    boolean existsOrganizationalUnitByExternalIdAndIdNot(String externalId, Long excludedOrganizationalUnitId);

    OrganizationalUnit saveOrganizationalUnit(OrganizationalUnit organizationalUnit);
}
