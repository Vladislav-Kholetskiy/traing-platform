package com.vladislav.training.platform.userorg.controller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

/**
 * Запрос {@code AssignRoleRequest}.
 */
public record AssignRoleRequest(
    @NotNull @Positive Long roleId,
    Instant validFrom
) {
}
