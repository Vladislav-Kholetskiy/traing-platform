package com.vladislav.training.platform.userorg.controller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

/**
 * Запрос {@code ReplacePrimaryHomeUnitRequest}.
 */
public record ReplacePrimaryHomeUnitRequest(
    @NotNull @Positive Long organizationalUnitId,
    Instant effectiveAt
) {
}
