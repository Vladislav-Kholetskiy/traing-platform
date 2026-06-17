package com.vladislav.training.platform.integration.personnel.model;

/**
 * Запись данных {@code PersonnelPlannedMutation}.
 */
public record PersonnelPlannedMutation(
    PersonnelPlannedMutationType mutationType,
    String targetRef,
    String detail
) {

    public PersonnelPlannedMutation {
        if (mutationType == null) {
            throw new IllegalArgumentException("mutationType must not be null");
        }
    }
}
