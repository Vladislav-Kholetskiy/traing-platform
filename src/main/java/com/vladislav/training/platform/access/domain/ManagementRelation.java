package com.vladislav.training.platform.access.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Запись данных {@code ManagementRelation}.
 */
public record ManagementRelation(
    Long id,
    Long userId,
    Long organizationalUnitId,
    Long managementRelationTypeId,
    Instant validFrom,
    Instant validTo,
    Instant createdAt,
    Instant updatedAt
) {

    public ManagementRelation {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(organizationalUnitId, "organizationalUnitId must not be null");
        Objects.requireNonNull(managementRelationTypeId, "managementRelationTypeId must not be null");
        Objects.requireNonNull(validFrom, "validFrom must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}
