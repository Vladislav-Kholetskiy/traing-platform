package com.vladislav.training.platform.userorg.service;

import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitType;

/**
 * Контракт командного сервиса {@code OrganizationCommandService}.
 */
public interface OrganizationCommandService {

    OrganizationalUnit createOrganizationalUnit(OrganizationalUnit organizationalUnit);

    OrganizationalUnit updateOrganizationalUnit(UpdateOrganizationalUnitCommand command);

    OrganizationalUnit moveOrganizationalUnit(Long organizationalUnitId, Long newParentOrganizationalUnitId);

    OrganizationalUnit archiveOrganizationalUnit(Long organizationalUnitId);

    OrganizationalUnitType createOrganizationalUnitType(OrganizationalUnitType organizationalUnitType);

    OrganizationalUnitType updateOrganizationalUnitType(UpdateOrganizationalUnitTypeCommand command);
}
