package com.vladislav.training.platform.integration.personnel.controller.dto;

import java.util.List;

/**
 * Запись данных {@code PersonnelDryRunRowResult}.
 */
public record PersonnelDryRunRowResult(
    int rowNumber,
    String employeeNumber,
    String outcomeCode,
    String decision,
    String targetUserStatus,
    List<String> issues,
    List<PersonnelPlannedMutationDto> plannedMutations
) {
}
