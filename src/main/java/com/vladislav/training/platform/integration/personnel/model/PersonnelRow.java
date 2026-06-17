package com.vladislav.training.platform.integration.personnel.model;

import java.time.LocalDate;

/**
 * Запись данных {@code PersonnelRow}.
 */
public record PersonnelRow(
    int rowNumber,
    String employeeNumber,
    String externalId,
    String lastName,
    String firstName,
    String middleName,
    String employmentStatus,
    String homeOrgUnitCode,
    String basePositionCode,
    String temporaryPositionCode,
    String temporaryOrgUnitCode,
    LocalDate temporaryValidFrom,
    LocalDate temporaryValidTo,
    String comment
) {

    public PersonnelRow(
        int rowNumber,
        String employeeNumber,
        String lastName,
        String firstName,
        String middleName,
        String employmentStatus,
        String homeOrgUnitCode,
        String basePositionCode,
        String temporaryPositionCode,
        String temporaryOrgUnitCode,
        LocalDate temporaryValidFrom,
        LocalDate temporaryValidTo,
        String comment
    ) {
        this(
            rowNumber,
            employeeNumber,
            "",
            lastName,
            firstName,
            middleName,
            employmentStatus,
            homeOrgUnitCode,
            basePositionCode,
            temporaryPositionCode,
            temporaryOrgUnitCode,
            temporaryValidFrom,
            temporaryValidTo,
            comment
        );
    }
}
