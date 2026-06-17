package com.vladislav.training.platform.userorg.service;

import java.util.Objects;

/**
 * Команда {@code UpdateOrganizationalUnitCommand}.
 */
public record UpdateOrganizationalUnitCommand(
    Long organizationalUnitId,
    String name,
    String externalId,
    Long organizationalUnitTypeId
) {

    public UpdateOrganizationalUnitCommand {
        Objects.requireNonNull(organizationalUnitId, "organizationalUnitId must not be null");
        Objects.requireNonNull(name, "name must not be null");
    }
}
