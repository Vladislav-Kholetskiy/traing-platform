package com.vladislav.training.platform.userorg.service;

import java.time.Instant;
import java.util.Collection;
import java.util.Set;

/**
 * Контракт сервиса {@code UserOrgFoundationStateReadService}.
 */
public interface UserOrgFoundationStateReadService {

        UserAccessPolicyFoundationState findUserAccessPolicyFoundationState(Long userId, Instant effectiveAt);

        ActorCommandFoundationState findActorCommandFoundationState(Long actorUserId, Instant effectiveAt);

        TargetUserCommandFoundationState findTargetUserCommandFoundationState(Long userId);

    Set<String> findRoleCodesByIds(Collection<Long> roleIds);

    UserIdentityFoundationState findUserIdentityFoundationStateByEmployeeNumber(String employeeNumber);

    OrganizationalUnitFoundationState findOrganizationalUnitFoundationState(Long organizationalUnitId);

    boolean organizationalUnitTypeExists(Long organizationalUnitTypeId);

    record UserAccessPolicyFoundationState(
        Long userId,
        boolean active,
        Set<String> activePermanentRoleCodes
    ) {
    }

    record ActorCommandFoundationState(
        Long actorUserId,
        boolean active,
        Set<String> activePermanentRoleCodes
    ) {
    }

    record TargetUserCommandFoundationState(
        Long userId,
        boolean active
    ) {
    }

    record UserIdentityFoundationState(
        Long userId,
        String employeeNumber,
        boolean active
    ) {
    }

    record OrganizationalUnitFoundationState(
        Long organizationalUnitId,
        boolean active,
        String path,
        boolean linearNode,
        boolean functionalNode,
        boolean canBeOperatorHomeUnit,
        boolean canHaveManagementRelation,
        boolean canHaveAccessArea,
        boolean participatesInSubtreeScope
    ) {
    }
}