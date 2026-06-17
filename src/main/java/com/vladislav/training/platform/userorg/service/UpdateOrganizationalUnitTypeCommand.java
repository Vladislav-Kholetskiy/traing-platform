package com.vladislav.training.platform.userorg.service;

import com.vladislav.training.platform.userorg.domain.OrganizationalNodeKind;
import java.util.Objects;

/**
 * Команда {@code UpdateOrganizationalUnitTypeCommand}.
 */
public record UpdateOrganizationalUnitTypeCommand(
    Long organizationalUnitTypeId,
    String name,
    String description,
    OrganizationalNodeKind nodeKind,
    Boolean canBeOperatorHomeUnit,
    Boolean canBeCampaignTarget,
    Boolean participatesInSubtreeScope,
    Boolean canHaveManagementRelation,
    Boolean canHaveAccessArea
) {

    public UpdateOrganizationalUnitTypeCommand {
        Objects.requireNonNull(organizationalUnitTypeId, "organizationalUnitTypeId must not be null");
        Objects.requireNonNull(name, "name must not be null");
    }
}
