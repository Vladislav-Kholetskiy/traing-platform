package com.vladislav.training.platform.integration.personnel.model;

/**
 * Запись данных {@code PersonnelRowIssue}.
 */
public record PersonnelRowIssue(
    Integer rowNumber,
    String columnName,
    PersonnelRowOutcomeCode outcomeCode,
    String message
) {

    public PersonnelRowIssue {
        if (outcomeCode == null) {
            throw new IllegalArgumentException("outcomeCode must not be null");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }

    public boolean fatal() {
        return outcomeCode.fatal();
    }
}
