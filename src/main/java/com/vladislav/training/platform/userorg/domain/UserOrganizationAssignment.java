package com.vladislav.training.platform.userorg.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Запись данных {@code UserOrganizationAssignment}.
 */
public record UserOrganizationAssignment(
    Long id,
    Long userId,
    Long organizationalUnitId,
    OrganizationAssignmentType assignmentType,
    Instant validFrom,
    Instant validTo,
    Instant createdAt,
    Instant updatedAt
) {

    public UserOrganizationAssignment {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(organizationalUnitId, "organizationalUnitId must not be null");
        Objects.requireNonNull(assignmentType, "assignmentType must not be null");
        Objects.requireNonNull(validFrom, "validFrom must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}
