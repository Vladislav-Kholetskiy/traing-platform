package com.vladislav.training.platform.access.controller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

/**
 * Запрос {@code AssignTemporaryRoleRequest}.
 */
public record AssignTemporaryRoleRequest(
    @NotNull @Positive Long userId,
    @NotNull @Positive Long roleId,
    Instant validFrom
) {
}
