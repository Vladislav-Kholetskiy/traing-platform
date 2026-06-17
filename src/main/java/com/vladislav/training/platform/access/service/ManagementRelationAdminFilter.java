package com.vladislav.training.platform.access.service;

import java.time.Instant;

/**
 * Фильтр {@code ManagementRelationAdminFilter}.
 */
public record ManagementRelationAdminFilter(
    Long userId,
    Long organizationalUnitId,
    Long managementRelationTypeId,
    Instant activeAt
) {
}
