package com.vladislav.training.platform.access.service;

import java.time.Instant;
import java.util.Set;

/**
 * Контракт сервиса {@code AccessFoundationStateReadService}.
 */
public interface AccessFoundationStateReadService {

    OrganizationalUnitAccessUsage findOrganizationalUnitUsage(Long organizationalUnitId, Instant activeAt);

        ManagerialScopeFoundationState findActorManagerialScope(Long actorUserId, Instant activeAt);

        Set<Long> findActiveTemporaryRoleIds(Long userId, Instant activeAt);

        boolean managementRelationTypeExists(Long managementRelationTypeId);

    record OrganizationalUnitAccessUsage(
        boolean hasActiveUserAccessArea,
        boolean hasActiveSubtreeUserAccessArea,
        boolean hasActiveManagementRelation,
        boolean hasActiveTemporaryAccessArea,
        boolean hasActiveSubtreeTemporaryAccessArea,
        boolean hasActiveTemporaryManagementDelegation
    ) {
    }

    record ManagerialScopeFoundationState(
        Set<Long> unitAnchorIds,
        Set<String> subtreePaths
    ) {
    }
}
