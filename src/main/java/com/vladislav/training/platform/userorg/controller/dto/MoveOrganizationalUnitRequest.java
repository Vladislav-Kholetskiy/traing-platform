package com.vladislav.training.platform.userorg.controller.dto;

import jakarta.validation.constraints.Positive;

/**
 * Запрос {@code MoveOrganizationalUnitRequest}.
 */
public record MoveOrganizationalUnitRequest(@Positive Long newParentOrganizationalUnitId) {
}