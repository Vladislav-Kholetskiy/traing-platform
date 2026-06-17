package com.vladislav.training.platform.access.controller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

/**
 * Запрос {@code AssignManagementRelationRequest}.
 */
public record AssignManagementRelationRequest(
    @NotNull @Positive Long userId,
    @NotNull @Positive Long organizationalUnitId,
    @NotNull @Positive Long managementRelationTypeId,
    Instant validFrom
) {
}
