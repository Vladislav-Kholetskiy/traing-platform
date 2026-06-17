package com.vladislav.training.platform.userorg.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Запись данных {@code OrganizationalUnit}.
 */
public record OrganizationalUnit(
    Long id,
    Long parentId,
    Long organizationalUnitTypeId,
    String name,
    OrganizationalUnitStatus status,
    String path,
    int depth,
    String externalId,
    Instant createdAt,
    Instant updatedAt
) {

    public OrganizationalUnit {
        Objects.requireNonNull(organizationalUnitTypeId, "organizationalUnitTypeId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (path.isBlank()) {
            throw new IllegalArgumentException("path must not be blank");
        }
        if (depth < 0) {
            throw new IllegalArgumentException("depth must be non-negative");
        }
        if (id != null && parentId != null && id.equals(parentId)) {
            throw new IllegalArgumentException("parentId must not equal id");
        }
    }
}