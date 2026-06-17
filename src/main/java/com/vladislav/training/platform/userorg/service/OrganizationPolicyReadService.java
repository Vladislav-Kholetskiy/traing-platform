package com.vladislav.training.platform.userorg.service;

import com.vladislav.training.platform.access.service.AccessReadScope;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Контракт сервиса {@code OrganizationPolicyReadService}.
 */
public interface OrganizationPolicyReadService {

    Optional<OrganizationalUnit> findOrganizationalUnitByIdWithinScope(AccessReadScope scope, Long organizationalUnitId);

    Optional<OrganizationalUnit> findOrganizationalUnitByPathWithinScope(AccessReadScope scope, String path);

    List<OrganizationalUnit> findChildUnitsWithinScope(AccessReadScope scope, Long parentUnitId);

    List<OrganizationalUnit> findUnitsWithinScope(AccessReadScope scope, OrganizationalUnitStatus status);

    List<OrganizationalUnit> findOrganizationalUnitsByIdsWithinScope(
            AccessReadScope scope,
            Collection<Long> organizationalUnitIds
    );
}