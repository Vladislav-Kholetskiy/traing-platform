package com.vladislav.training.platform.access.controller.dto;

import java.time.Instant;

/**
 * Ответ {@code ManagementRelationResponse}.
 */
public record ManagementRelationResponse(
    Long id,
    Long userId,
    Long organizationalUnitId,
    Long managementRelationTypeId,
    Instant validFrom,
    Instant validTo,
    Instant createdAt,
    Instant updatedAt
) {
}
