package com.vladislav.training.platform.integration.personnel.controller.dto;

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
        this(rowNumber, employeeNumber, outcomeCode, null, targetUserStatus, issues, appliedMutationTypes, null);
    }

    public PersonnelApplyRowResult {
        issues = List.copyOf(issues);
        appliedMutationTypes = List.copyOf(appliedMutationTypes);
    }
}
