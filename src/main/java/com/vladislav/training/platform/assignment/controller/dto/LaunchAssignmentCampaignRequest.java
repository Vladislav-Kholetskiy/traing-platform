package com.vladislav.training.platform.assignment.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.List;

/**
 * Запрос {@code LaunchAssignmentCampaignRequest}.
 */
public record LaunchAssignmentCampaignRequest(
    @NotBlank String name,
    String description,
    @NotBlank String sourceType,
    String sourceRef,
    String sourceNameSnapshot,
    @NotEmpty List<@NotNull @Positive Long> courseIds,
    @NotNull @Valid TargetingRequest targeting,
    @NotNull @Valid DeadlinePolicyRequest deadlinePolicy
) {

    public record TargetingRequest(
        @NotBlank String basisType,
        @NotBlank String basisRef
    ) {
    }

    public record DeadlinePolicyRequest(
        @NotNull Instant deadlineAt
    ) {
    }
}
