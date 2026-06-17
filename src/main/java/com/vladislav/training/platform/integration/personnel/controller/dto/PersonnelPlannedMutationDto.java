package com.vladislav.training.platform.integration.personnel.controller.dto;

/**
 * Объект передачи данных {@code PersonnelPlannedMutationDto}.
 */
public record PersonnelPlannedMutationDto(
    String mutationType,
    String targetRef,
    String detail
) {
}
