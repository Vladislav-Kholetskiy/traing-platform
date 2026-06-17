package com.vladislav.training.platform.integration.personnel.support;

/**
 * Запись данных {@code PersonnelWorkbookLimits}.
 */
public record PersonnelWorkbookLimits(int maxRows) {

    public PersonnelWorkbookLimits {
        if (maxRows <= 0) {
            throw new IllegalArgumentException("maxRows must be positive");
        }
    }
}
