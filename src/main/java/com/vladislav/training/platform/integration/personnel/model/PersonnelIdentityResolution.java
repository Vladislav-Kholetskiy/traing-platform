package com.vladislav.training.platform.integration.personnel.model;

/**
 * Запись данных {@code PersonnelIdentityResolution}.
 */
public record PersonnelIdentityResolution(
    String employeeNumber,
    boolean resolved,
    boolean identityMismatch,
    String expectedExternalId,
    String actualExternalId,
    PersonnelCurrentState currentState
) {

    public static PersonnelIdentityResolution resolved(PersonnelCurrentState currentState) {
        return new PersonnelIdentityResolution(
            currentState.employeeNumber(),
            true,
            false,
            null,
            currentState.externalId(),
            currentState
        );
    }

    public static PersonnelIdentityResolution unresolved(String employeeNumber) {
        return new PersonnelIdentityResolution(employeeNumber, false, false, null, null, null);
    }

    public static PersonnelIdentityResolution identityMismatch(
        String employeeNumber,
        String expectedExternalId,
        String actualExternalId
    ) {
        return new PersonnelIdentityResolution(
            employeeNumber,
            false,
            true,
            expectedExternalId,
            actualExternalId,
            null
        );
    }
}
