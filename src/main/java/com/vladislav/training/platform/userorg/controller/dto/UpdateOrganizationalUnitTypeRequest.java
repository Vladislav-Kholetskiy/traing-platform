package com.vladislav.training.platform.userorg.controller.dto;

import com.vladislav.training.platform.userorg.domain.OrganizationalNodeKind;
import jakarta.validation.constraints.NotBlank;

/**
 * Запрос {@code UpdateOrganizationalUnitTypeRequest}.
 */
public record UpdateOrganizationalUnitTypeRequest(
    @NotBlank String name,
    String description,
    OrganizationalNodeKind nodeKind,
    Boolean canBeOperatorHomeUnit,
    Boolean canBeCampaignTarget,
    Boolean participatesInSubtreeScope,
    Boolean canHaveManagementRelation,
    Boolean canHaveAccessArea
) {
}
