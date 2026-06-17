package com.vladislav.training.platform.access.service;

import com.vladislav.training.platform.access.domain.AccessScopeType;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.userorg.service.UserOrgFoundationStateReadService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Вспомогательный тип {@code AccessManagementTargetValidationSupport}.
 */
@Service
@Transactional(readOnly = true)
class AccessManagementTargetValidationSupport {

    private final UserOrgFoundationStateReadService userOrgFoundationStateReadService;

    AccessManagementTargetValidationSupport(UserOrgFoundationStateReadService userOrgFoundationStateReadService) {
        this.userOrgFoundationStateReadService = userOrgFoundationStateReadService;
    }

    void ensureAccessAreaTargetAllowed(Long organizationalUnitId, AccessScopeType accessScopeType) {
        if (accessScopeType == AccessScopeType.GLOBAL) {
            return;
        }
        UserOrgFoundationStateReadService.OrganizationalUnitFoundationState target = requireActiveFoundationTarget(
            organizationalUnitId
        );
        if (!target.canHaveAccessArea()) {
            throw new ConflictException(
                "Organizational unit type does not allow user_access_area target: " + organizationalUnitId
            );
        }
        if (accessScopeType == AccessScopeType.UNIT_SUBTREE
            && target.functionalNode()
            && !target.participatesInSubtreeScope()) {
            throw new ConflictException(
                "FUNCTIONAL organizational unit cannot be used for UNIT_SUBTREE without participatesInSubtreeScope=true: "
                    + organizationalUnitId
            );
        }
    }

    void ensureManagementRelationTargetAllowed(Long organizationalUnitId) {
        UserOrgFoundationStateReadService.OrganizationalUnitFoundationState target = requireActiveFoundationTarget(
            organizationalUnitId
        );
        if (!target.canHaveManagementRelation()) {
            throw new ConflictException(
                "Organizational unit type does not allow management_relation target: " + organizationalUnitId
            );
        }
    }

    void ensureOperatorHomeUnitAllowed(Long organizationalUnitId) {
        UserOrgFoundationStateReadService.OrganizationalUnitFoundationState target = requireActiveFoundationTarget(
            organizationalUnitId
        );
        if (!target.active()) {
            throw new ConflictException(
                "Operator home unit must be ACTIVE organizational unit: " + organizationalUnitId
            );
        }
        if (!target.linearNode()) {
            throw new ConflictException(
                "Operator home unit must reference LINEAR organizational unit: " + organizationalUnitId
            );
        }
        if (!target.canBeOperatorHomeUnit()) {
            throw new ConflictException(
                "Organizational unit cannot be used as operator home unit by current SCN-17 foundation-state: "
                    + organizationalUnitId
            );
        }
    }

    private UserOrgFoundationStateReadService.OrganizationalUnitFoundationState requireActiveFoundationTarget(
        Long organizationalUnitId
    ) {
        UserOrgFoundationStateReadService.OrganizationalUnitFoundationState target =
            userOrgFoundationStateReadService.findOrganizationalUnitFoundationState(organizationalUnitId);
        if (!target.active()) {
            throw new ConflictException("Archived organizational unit cannot be used as target: " + organizationalUnitId);
        }
        return target;
    }
}
