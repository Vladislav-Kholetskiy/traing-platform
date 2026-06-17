package com.vladislav.training.platform.userorg.repository;

import com.vladislav.training.platform.userorg.domain.OrganizationalNodeKind;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitType;
import java.util.List;

/**
 * Контракт репозитория {@code OrganizationalUnitTypeRepository}.
 */
public interface OrganizationalUnitTypeRepository {

    OrganizationalUnitType findOrganizationalUnitTypeById(Long organizationalUnitTypeId);

    OrganizationalUnitType findOrganizationalUnitTypeByCode(String organizationalUnitTypeCode);

    List<OrganizationalUnitType> findUnitTypesByNodeKind(OrganizationalNodeKind nodeKind);

    boolean existsOrganizationalUnitTypeByCode(String code);

    boolean existsOrganizationalUnitTypeByCodeAndIdNot(String code, Long excludedOrganizationalUnitTypeId);

    OrganizationalUnitType saveOrganizationalUnitType(OrganizationalUnitType organizationalUnitType);
}
