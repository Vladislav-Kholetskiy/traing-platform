package com.vladislav.training.platform.integration.personnel.controller.dto;

import java.util.List;

/**
 * Ответ {@code PersonnelDryRunResponse}.
 */
public record PersonnelDryRunResponse(
    List<PersonnelDryRunRowResult> rows
) {
}
