package com.vladislav.training.platform.userorg.controller.dto;

import com.vladislav.training.platform.userorg.domain.OrganizationalNodeKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Запрос {@code CreateOrganizationalUnitTypeRequest}.
 */
public record CreateOrganizationalUnitTypeRequest(
    @NotBlank String code,
    @NotBlank String name,
    String description,
    @NotNull OrganizationalNodeKind nodeKind,
    @NotNull Boolean canBeOperatorHomeUnit,
    @NotNull Boolean canBeCampaignTarget,
    @NotNull Boolean participatesInSubtreeScope,
    @NotNull Boolean canHaveManagementRelation,
    @NotNull Boolean canHaveAccessArea
) {
}
