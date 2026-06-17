package com.vladislav.training.platform.access.controller.dto;

import com.vladislav.training.platform.access.domain.AccessScopeType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

/**
 * Запрос {@code AssignTemporaryAccessAreaRequest}.
 */
public record AssignTemporaryAccessAreaRequest(
    @NotNull @Positive Long userId,
    @Positive Long organizationalUnitId,
    @NotNull AccessScopeType accessScopeType,
    Instant validFrom
) {
}
