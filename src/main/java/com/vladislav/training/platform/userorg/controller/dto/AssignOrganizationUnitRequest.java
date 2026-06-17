package com.vladislav.training.platform.userorg.controller.dto;

import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

/**
 * Запрос {@code AssignOrganizationUnitRequest}.
 */
public record AssignOrganizationUnitRequest(
    @NotNull @Positive Long organizationalUnitId,
    @NotNull OrganizationAssignmentType assignmentType,
    Instant validFrom
) {
}
