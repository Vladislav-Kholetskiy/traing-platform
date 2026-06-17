package com.vladislav.training.platform.access.service;

import com.vladislav.training.platform.access.domain.AccessScopeType;
import com.vladislav.training.platform.access.domain.ManagementRelation;
import com.vladislav.training.platform.access.domain.TemporaryAccessArea;
import com.vladislav.training.platform.access.domain.TemporaryManagementDelegation;
import com.vladislav.training.platform.access.domain.TemporaryRoleAssignment;
import com.vladislav.training.platform.access.domain.UserAccessArea;
import java.time.Instant;

/**
 * Контракт командного сервиса {@code AccessAdministrationCommandService}.
 */
public interface AccessAdministrationCommandService {

    UserAccessArea assignUserAccessArea(Long userId, Long organizationalUnitId, AccessScopeType accessScopeType, Instant validFrom);

    UserAccessArea closeUserAccessArea(Long userAccessAreaId, Instant validTo);

    ManagementRelation assignManagementRelation(
        Long userId,
        Long organizationalUnitId,
        Long managementRelationTypeId,
        Instant validFrom
    );

    ManagementRelation closeManagementRelation(Long managementRelationId, Instant validTo);

    TemporaryRoleAssignment assignTemporaryRoleAssignment(Long userId, Long roleId, Instant validFrom);

    TemporaryRoleAssignment closeTemporaryRoleAssignment(Long temporaryRoleAssignmentId, Instant validTo);

    TemporaryAccessArea assignTemporaryAccessArea(
        Long userId,
        Long organizationalUnitId,
        AccessScopeType accessScopeType,
        Instant validFrom
    );

    TemporaryAccessArea closeTemporaryAccessArea(Long temporaryAccessAreaId, Instant validTo);

    TemporaryManagementDelegation assignTemporaryManagementDelegation(
        Long userId,
        Long organizationalUnitId,
        Long managementRelationTypeId,
        Instant validFrom
    );

    TemporaryManagementDelegation closeTemporaryManagementDelegation(Long temporaryManagementDelegationId, Instant validTo);
}
