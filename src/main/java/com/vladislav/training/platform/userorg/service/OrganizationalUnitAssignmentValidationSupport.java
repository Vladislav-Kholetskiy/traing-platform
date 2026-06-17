package com.vladislav.training.platform.userorg.service;

import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.userorg.domain.OrganizationalNodeKind;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitType;
import com.vladislav.training.platform.userorg.repository.OrganizationalUnitRepository;
import com.vladislav.training.platform.userorg.repository.OrganizationalUnitTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Вспомогательный тип {@code OrganizationalUnitAssignmentValidationSupport}.
 */
@Service
@Transactional(readOnly = true)
class OrganizationalUnitAssignmentValidationSupport {

    private final OrganizationalUnitRepository organizationalUnitRepository;
    private final OrganizationalUnitTypeRepository organizationalUnitTypeRepository;

    OrganizationalUnitAssignmentValidationSupport(
        OrganizationalUnitRepository organizationalUnitRepository,
        OrganizationalUnitTypeRepository organizationalUnitTypeRepository
    ) {
        this.organizationalUnitRepository = organizationalUnitRepository;
        this.organizationalUnitTypeRepository = organizationalUnitTypeRepository;
    }

    OrganizationalUnitTarget requireAssignableTarget(Long organizationalUnitId) {
        OrganizationalUnit organizationalUnit = organizationalUnitRepository.findOrganizationalUnitById(organizationalUnitId);
        if (organizationalUnit.status() == OrganizationalUnitStatus.ARCHIVED) {
            throw new ConflictException("Archived organizational unit cannot be used as assignment target: " + organizationalUnitId);
        }
        OrganizationalUnitType organizationalUnitType = organizationalUnitTypeRepository.findOrganizationalUnitTypeById(
            organizationalUnit.organizationalUnitTypeId()
        );
        return new OrganizationalUnitTarget(organizationalUnit, organizationalUnitType);
    }

    void ensureOperatorHomeUnitAllowed(Long organizationalUnitId) {
        OrganizationalUnitTarget target = requireAssignableTarget(organizationalUnitId);
        if (target.organizationalUnit().status() != OrganizationalUnitStatus.ACTIVE) {
            throw new ConflictException(
                "Operator home unit must be ACTIVE organizational unit: " + organizationalUnitId
            );
        }
        if (target.organizationalUnitType().nodeKind() != OrganizationalNodeKind.LINEAR) {
            throw new ConflictException(
                "Operator home unit must reference LINEAR organizational unit: " + organizationalUnitId
            );
        }
        if (!target.organizationalUnitType().canBeOperatorHomeUnit()) {
            throw new ConflictException(
                "Organizational unit cannot be used as operator home unit by current SCN-17 foundation-state: "
                    + organizationalUnitId
            );
        }
    }

    record OrganizationalUnitTarget(OrganizationalUnit organizationalUnit, OrganizationalUnitType organizationalUnitType) {
    }
}
