package com.vladislav.training.platform.userorg.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Запись данных {@code UserRoleAssignment}.
 */
public record UserRoleAssignment(
    Long id,
    Long userId,
    Long roleId,
    Instant validFrom,
    Instant validTo,
    Instant createdAt,
    Instant updatedAt
) {

    public UserRoleAssignment {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(roleId, "roleId must not be null");
        Objects.requireNonNull(validFrom, "validFrom must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}
