package com.vladislav.training.platform.integration.personnel.controller.dto;

import java.util.List;

/**
 * Ответ {@code PersonnelApplyResponse}.
 */
public record PersonnelApplyResponse(List<PersonnelApplyRowResult> rows) {

    public PersonnelApplyResponse {
        rows = List.copyOf(rows);
    }
}
