package com.vladislav.training.platform.assignment.controller.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Запрос {@code ExtendAssignmentDeadlineRequest}.
 */
public record ExtendAssignmentDeadlineRequest(
    @NotNull Instant newDeadlineAt,
    String note
) {
}
