package com.vladislav.training.platform.access.service;

import java.time.Instant;

/**
 * Фильтр {@code TemporaryManagementDelegationAdminFilter}.
 */
public record TemporaryManagementDelegationAdminFilter(
    Long userId,
    Long organizationalUnitId,
    Long managementRelationTypeId,
    Instant activeAt
) {
}
