package com.vladislav.training.platform.integration.personnel.model;

import java.util.List;

/**
 * Запись данных {@code PersonnelApplyResult}.
 */
public record PersonnelApplyResult(List<PersonnelApplyRowResult> rows) {

    public PersonnelApplyResult {
        rows = List.copyOf(rows);
    }
}
