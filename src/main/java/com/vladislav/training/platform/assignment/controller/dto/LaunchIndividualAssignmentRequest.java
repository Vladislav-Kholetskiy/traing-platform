package com.vladislav.training.platform.assignment.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.List;

/**
 * Запрос {@code LaunchIndividualAssignmentRequest}.
 */
public record LaunchIndividualAssignmentRequest(
    @NotBlank String name,
    String description,
    @NotNull @Positive Long userId,
    @NotEmpty List<@NotNull @Positive Long> courseIds,
    @NotNull @Valid DeadlinePolicyRequest deadlinePolicy
) {

    /**
     * Вложенная форма со сроком выполнения для индивидуального назначения.
     */
    public record DeadlinePolicyRequest(
        @NotNull Instant deadlineAt
    ) {
    }
}
