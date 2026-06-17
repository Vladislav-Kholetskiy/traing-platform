package com.vladislav.training.platform.integration.personnel.model;

/**
 * Перечисление {@code PersonnelRowOutcomeCode}.
 */
public enum PersonnelRowOutcomeCode {
    BUSINESS_SHEET_COUNT_INVALID(true),
    DUPLICATE_HEADER(true),
    MISSING_REQUIRED_HEADER(true),
    UNEXPECTED_HEADER(true),
    FORMULA_CELL_NOT_ALLOWED(true),
    DUPLICATE_EMPLOYEE_NUMBER(true),
    ROW_COUNT_LIMIT_EXCEEDED(true),
    INVALID_DATE_VALUE(false),
    PARTIAL_TEMPORARY_BLOCK(false),
    UNSUPPORTED_EMPLOYMENT_STATUS(false);

    private final boolean fatal;

    PersonnelRowOutcomeCode(boolean fatal) {
        this.fatal = fatal;
    }

    public boolean fatal() {
        return fatal;
    }
}
