package com.vladislav.training.platform.userorg.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * Запрос {@code CreateOrganizationalUnitRequest}.
 */
public record CreateOrganizationalUnitRequest(
    @Positive Long parentId,
    @NotNull @Positive Long organizationalUnitTypeId,
    @NotBlank String name,
    @Pattern(regexp = ".*\\S.*", message = "externalId must not be blank when provided") String externalId
) {
}