package com.vladislav.training.platform.access.service;

import java.time.Instant;

/**
 * Фильтр {@code TemporaryRoleAssignmentAdminFilter}.
 */
public record TemporaryRoleAssignmentAdminFilter(
    Long userId,
    Long roleId,
    Instant activeAt
) {
}
