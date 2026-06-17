package com.vladislav.training.platform.access.controller.dto;

import java.time.Instant;

/**
 * Ответ {@code TemporaryManagementDelegationResponse}.
 */
public record TemporaryManagementDelegationResponse(
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
