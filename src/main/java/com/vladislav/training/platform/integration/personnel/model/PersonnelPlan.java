package com.vladislav.training.platform.integration.personnel.model;

import java.util.List;

/**
 * Запись данных {@code PersonnelPlan}.
 */
public record PersonnelPlan(
    int rowNumber,
    String employeeNumber,
    PersonnelPlanOutcomeCode outcomeCode,
    String targetUserStatus,
    List<String> issues,
    List<PersonnelPlannedMutation> plannedMutations,
    Long resolvedUserId
) {

    public PersonnelPlan(
        int rowNumber,
        String employeeNumber,
        PersonnelPlanOutcomeCode outcomeCode,
        String targetUserStatus,
        List<String> issues,
        List<PersonnelPlannedMutation> plannedMutations
    ) {
        this(
            rowNumber,
            employeeNumber,
            outcomeCode,
            targetUserStatus,
            issues,
            plannedMutations,
            null
        );
    }

    public PersonnelPlan {
        if (employeeNumber == null || employeeNumber.isBlank()) {
            throw new IllegalArgumentException("employeeNumber must not be blank");
        }
        if (outcomeCode == null) {
            throw new IllegalArgumentException("outcomeCode must not be null");
        }
        issues = List.copyOf(issues);
        plannedMutations = List.copyOf(plannedMutations);
    }

    public PersonnelRowDecision decision() {
        return switch (outcomeCode) {
            case CREATE_USER_REQUIRED -> PersonnelRowDecision.CREATE_USER;
            case PLANNED_CHANGES -> PersonnelRowDecision.UPDATE_EXISTING;
            case NO_CHANGE -> PersonnelRowDecision.NO_CHANGE;
            case UNRESOLVED_EMPLOYEE, IDENTITY_MISMATCH, FAIL_CLOSED -> PersonnelRowDecision.FAIL_CLOSED;
        };
    }
}
