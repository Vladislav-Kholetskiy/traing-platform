package com.vladislav.training.platform.access.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Запись данных {@code UserAccessArea}.
 */
public record UserAccessArea(
    Long id,
    Long userId,
    Long organizationalUnitId,
    AccessScopeType accessScopeType,
    Instant validFrom,
    Instant validTo,
    Instant createdAt,
    Instant updatedAt
) {

    public UserAccessArea {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(accessScopeType, "accessScopeType must not be null");
        Objects.requireNonNull(validFrom, "validFrom must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (accessScopeType == AccessScopeType.GLOBAL && organizationalUnitId != null) {
            throw new IllegalArgumentException("organizationalUnitId must be null for GLOBAL scope");
        }
        if ((accessScopeType == AccessScopeType.UNIT_ONLY || accessScopeType == AccessScopeType.UNIT_SUBTREE)
            && organizationalUnitId == null) {
            throw new IllegalArgumentException("organizationalUnitId must not be null for unit-scoped access");
        }
    }
}
