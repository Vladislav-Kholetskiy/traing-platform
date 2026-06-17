package com.vladislav.training.platform.integration.personnel.model;

import java.util.List;

/**
 * Запись данных {@code PersonnelWorkbookParseResult}.
 */
public record PersonnelWorkbookParseResult(
    List<PersonnelRow> rows,
    List<PersonnelRowIssue> issues
) {

    public PersonnelWorkbookParseResult {
        rows = List.copyOf(rows);
        issues = List.copyOf(issues);
    }

    public boolean hasFatalIssues() {
        return issues.stream().anyMatch(PersonnelRowIssue::fatal);
    }
}
