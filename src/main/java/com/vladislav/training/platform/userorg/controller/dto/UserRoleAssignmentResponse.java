package com.vladislav.training.platform.userorg.controller.dto;

import java.time.Instant;

/**
 * Ответ {@code UserRoleAssignmentResponse}.
 */
public record UserRoleAssignmentResponse(
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
