package com.vladislav.training.platform.userorg.service;

import java.time.Instant;

/**
 * Запись данных {@code UserAdministrationRoleAssignmentView}.
 */
public record UserAdministrationRoleAssignmentView(
        Long id,
        Long userId,
        Long roleId,
        String roleCode,
        String roleName,
        Instant validFrom,
        Instant validTo,
        Instant createdAt,
        Instant updatedAt
) {
}