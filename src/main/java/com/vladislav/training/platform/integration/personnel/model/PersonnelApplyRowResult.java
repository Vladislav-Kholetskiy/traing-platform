package com.vladislav.training.platform.integration.personnel.model;

import java.util.List;

/**
 * Запись данных {@code PersonnelApplyRowResult}.
 */
public record PersonnelApplyRowResult(
    Integer rowNumber,
    String employeeNumber,
    String outcomeCode,
    String decision,
    String targetUserStatus,
    List<String> issues,
    List<String> appliedMutationTypes,
    Long createdUserId
) {

    public PersonnelApplyRowResult(
        Integer rowNumber,
        String employeeNumber,
        String outcomeCode,
        String targetUserStatus,
        List<String> issues,
        List<String> appliedMutationTypes
    ) {
        this(
            rowNumber,
            employeeNumber,
            outcomeCode,
            legacyDecision(outcomeCode),
            targetUserStatus,
            issues,
            appliedMutationTypes,
            null
        );
    }

    public PersonnelApplyRowResult {
        if (outcomeCode == null || outcomeCode.isBlank()) {
            throw new IllegalArgumentException("outcomeCode must not be blank");
        }
        if (decision == null || decision.isBlank()) {
            throw new IllegalArgumentException("decision must not be blank");
        }
        issues = List.copyOf(issues);
        appliedMutationTypes = List.copyOf(appliedMutationTypes);
    }

    private static String legacyDecision(String outcomeCode) {
        if ("NO_CHANGE".equals(outcomeCode)) {
            return PersonnelRowDecision.NO_CHANGE.name();
        }
        if ("SUCCESS".equals(outcomeCode)) {
            return PersonnelRowDecision.UPDATE_EXISTING.name();
        }
        return PersonnelRowDecision.FAIL_CLOSED.name();
    }
}
