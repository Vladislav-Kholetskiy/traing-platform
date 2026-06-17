package com.vladislav.training.platform.analytics.controller.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Запрос {@code AnalyticsResultRebuildRequest}.
 */
public record AnalyticsResultRebuildRequest(
    @NotNull Instant periodStart,
    @NotNull Instant periodEnd
) {
}
