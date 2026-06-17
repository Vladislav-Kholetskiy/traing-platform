package com.vladislav.training.platform.integration.personnel.model;

import java.time.LocalDate;

/**
 * Запись данных {@code PersonnelTemporaryAppointmentIntent}.
 */
public record PersonnelTemporaryAppointmentIntent(
    PersonnelPositionMapping positionMapping,
    String orgUnitCode,
    LocalDate validFrom,
    LocalDate validTo,
    boolean additiveOnly
) {

    public PersonnelTemporaryAppointmentIntent {
        if (positionMapping == null) {
            throw new IllegalArgumentException("positionMapping must not be null");
        }
        if (orgUnitCode == null || orgUnitCode.isBlank()) {
            throw new IllegalArgumentException("orgUnitCode must not be blank");
        }
        if (validFrom == null || validTo == null) {
            throw new IllegalArgumentException("temporary validity range must be complete");
        }
    }
}
