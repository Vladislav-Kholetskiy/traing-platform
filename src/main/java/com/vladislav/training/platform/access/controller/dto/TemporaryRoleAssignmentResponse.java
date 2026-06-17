package com.vladislav.training.platform.access.controller.dto;

import java.time.Instant;

/**
 * Ответ {@code TemporaryRoleAssignmentResponse}.
 */
public record TemporaryRoleAssignmentResponse(
    Long id,
    Long userId,
    Long roleId,
    Instant validFrom,
    Instant validTo,
    Instant createdAt,
    Instant updatedAt
) {
}
