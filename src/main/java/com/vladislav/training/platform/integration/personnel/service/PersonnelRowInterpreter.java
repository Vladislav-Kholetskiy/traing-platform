package com.vladislav.training.platform.integration.personnel.service;

import com.vladislav.training.platform.integration.personnel.model.PersonnelBusinessIntent;
import com.vladislav.training.platform.integration.personnel.model.PersonnelEmploymentAction;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPositionMapping;
import com.vladislav.training.platform.integration.personnel.model.PersonnelRow;
import com.vladislav.training.platform.integration.personnel.model.PersonnelTemporaryAppointmentIntent;
import java.util.Objects;

/**
 * Класс {@code PersonnelRowInterpreter}.
 */
public class PersonnelRowInterpreter {

    private final PersonnelPositionMappingService positionMappingService;

    public PersonnelRowInterpreter(PersonnelPositionMappingService positionMappingService) {
        this.positionMappingService = Objects.requireNonNull(positionMappingService, "positionMappingService must not be null");
    }

    public PersonnelBusinessIntent interpret(PersonnelRow row) {
        Objects.requireNonNull(row, "row must not be null");
        if (row.employeeNumber() == null || row.employeeNumber().isBlank()) {
            throw new IllegalArgumentException("employeeNumber must not be blank");
        }

        PersonnelEmploymentAction employmentAction = toEmploymentAction(row.employmentStatus());
        PersonnelPositionMapping basePositionMapping = positionMappingService.requireBasePosition(row.basePositionCode());
        PersonnelTemporaryAppointmentIntent temporaryAppointmentIntent = toTemporaryAppointmentIntent(row);
        boolean requiresExistingEmployee = employmentAction != PersonnelEmploymentAction.ENSURE_ACTIVE
            || temporaryAppointmentIntent != null;

        return new PersonnelBusinessIntent(
            row.rowNumber(),
            row.employeeNumber().trim(),
            normalizeOptional(row.externalId()),
            row.lastName(),
            row.firstName(),
            normalizeOptional(row.middleName()),
            requiresExistingEmployee,
            employmentAction,
            futureTargetUserStatus(employmentAction),
            row.homeOrgUnitCode(),
            basePositionMapping,
            temporaryAppointmentIntent
        );
    }

    private PersonnelEmploymentAction toEmploymentAction(String employmentStatus) {
        return switch (employmentStatus) {
            case "ACTIVE" -> PersonnelEmploymentAction.ENSURE_ACTIVE;
            case "INACTIVE" -> PersonnelEmploymentAction.DEACTIVATE;
            case "DISMISSED" -> PersonnelEmploymentAction.DISMISS_TO_INACTIVE;
            default -> throw new IllegalArgumentException("Unsupported employmentStatus: " + employmentStatus);
        };
    }

    private String futureTargetUserStatus(PersonnelEmploymentAction employmentAction) {
        return switch (employmentAction) {
            case ENSURE_ACTIVE -> "ACTIVE";
            case DEACTIVATE, DISMISS_TO_INACTIVE -> "INACTIVE";
        };
    }

    private PersonnelTemporaryAppointmentIntent toTemporaryAppointmentIntent(PersonnelRow row) {
        boolean hasTemporaryPosition = row.temporaryPositionCode() != null && !row.temporaryPositionCode().isBlank();
        boolean hasTemporaryOrgUnit = row.temporaryOrgUnitCode() != null && !row.temporaryOrgUnitCode().isBlank();
        boolean hasTemporaryFrom = row.temporaryValidFrom() != null;
        boolean hasTemporaryTo = row.temporaryValidTo() != null;
        int filledCount = (hasTemporaryPosition ? 1 : 0)
            + (hasTemporaryOrgUnit ? 1 : 0)
            + (hasTemporaryFrom ? 1 : 0)
            + (hasTemporaryTo ? 1 : 0);

        if (filledCount == 0) {
            return null;
        }
        if (filledCount < 4) {
            throw new IllegalArgumentException("Temporary appointment block must be either fully filled or fully empty");
        }

        PersonnelPositionMapping temporaryPositionMapping =
            positionMappingService.requireTemporaryPosition(row.temporaryPositionCode());
        return new PersonnelTemporaryAppointmentIntent(
            temporaryPositionMapping,
            row.temporaryOrgUnitCode(),
            row.temporaryValidFrom(),
            row.temporaryValidTo(),
            true
        );
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
