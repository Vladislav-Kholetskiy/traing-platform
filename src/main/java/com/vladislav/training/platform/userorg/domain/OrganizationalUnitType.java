package com.vladislav.training.platform.userorg.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Запись данных {@code OrganizationalUnitType}.
 */
public record OrganizationalUnitType(
    Long id,
    String code,
    String name,
    String description,
    OrganizationalNodeKind nodeKind,
    boolean canBeOperatorHomeUnit,
    boolean canBeCampaignTarget,
    boolean participatesInSubtreeScope,
    boolean canHaveManagementRelation,
    boolean canHaveAccessArea,
    Instant createdAt,
    Instant updatedAt
) {

    public OrganizationalUnitType {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(nodeKind, "nodeKind must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}
