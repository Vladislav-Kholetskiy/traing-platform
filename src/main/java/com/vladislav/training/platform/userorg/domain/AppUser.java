package com.vladislav.training.platform.userorg.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Запись данных {@code AppUser}.
 */
public record AppUser(
    Long id,
    String employeeNumber,
    String externalId,
    String lastName,
    String firstName,
    String middleName,
    String positionTitle,
    UserStatus status,
    Instant createdAt,
    Instant updatedAt
) {

    public AppUser(
        Long id,
        String employeeNumber,
        String externalId,
        String lastName,
        String firstName,
        String middleName,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt
    ) {
        this(id, employeeNumber, externalId, lastName, firstName, middleName, null, status, createdAt, updatedAt);
    }

    public AppUser {
        Objects.requireNonNull(employeeNumber, "employeeNumber must not be null");
        Objects.requireNonNull(lastName, "lastName must not be null");
        Objects.requireNonNull(firstName, "firstName must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (employeeNumber.isBlank()) {
            throw new IllegalArgumentException("employeeNumber must not be blank");
        }
        if (lastName.isBlank()) {
            throw new IllegalArgumentException("lastName must not be blank");
        }
        if (firstName.isBlank()) {
            throw new IllegalArgumentException("firstName must not be blank");
        }
    }
}
