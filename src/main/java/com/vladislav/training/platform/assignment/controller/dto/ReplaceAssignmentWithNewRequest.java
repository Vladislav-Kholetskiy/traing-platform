package com.vladislav.training.platform.assignment.controller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

/**
 * Запрос {@code ReplaceAssignmentWithNewRequest}.
 */
public record ReplaceAssignmentWithNewRequest(
    @NotNull @Positive Long campaignId,
    @NotNull Instant newCycleDeadlineAt,
    String note
) {
}
