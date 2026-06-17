package com.vladislav.training.platform.integration.personnel.model;

/**
 * Запись данных {@code PersonnelBusinessIntent}.
 */
public record PersonnelBusinessIntent(
    int rowNumber,
    String employeeNumber,
    String externalIdConsistencyGuard,
    String lastName,
    String firstName,
    String middleName,
    boolean requiresExistingEmployee,
    PersonnelEmploymentAction employmentAction,
    String futureTargetUserStatus,
    String homeOrgUnitCode,
    PersonnelPositionMapping basePositionMapping,
    PersonnelTemporaryAppointmentIntent temporaryAppointmentIntent
) {

    public PersonnelBusinessIntent(
        int rowNumber,
        String employeeNumber,
        String externalIdConsistencyGuard,
        boolean requiresExistingEmployee,
        PersonnelEmploymentAction employmentAction,
        String futureTargetUserStatus,
        String homeOrgUnitCode,
        PersonnelPositionMapping basePositionMapping,
        PersonnelTemporaryAppointmentIntent temporaryAppointmentIntent
    ) {
        this(
            rowNumber,
            employeeNumber,
            externalIdConsistencyGuard,
            "Ivanov",
            "Ivan",
            "Ivanovich",
            requiresExistingEmployee,
            employmentAction,
            futureTargetUserStatus,
            homeOrgUnitCode,
            basePositionMapping,
            temporaryAppointmentIntent
        );
    }

    public PersonnelBusinessIntent {
        if (employeeNumber == null || employeeNumber.isBlank()) {
            throw new IllegalArgumentException("employeeNumber must not be blank");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("lastName must not be blank");
        }
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("firstName must not be blank");
        }
        if (employmentAction == null) {
            throw new IllegalArgumentException("employmentAction must not be null");
        }
        if (futureTargetUserStatus == null || futureTargetUserStatus.isBlank()) {
            throw new IllegalArgumentException("futureTargetUserStatus must not be blank");
        }
        if (homeOrgUnitCode == null || homeOrgUnitCode.isBlank()) {
            throw new IllegalArgumentException("homeOrgUnitCode must not be blank");
        }
        if (basePositionMapping == null) {
            throw new IllegalArgumentException("basePositionMapping must not be null");
        }
    }
}
