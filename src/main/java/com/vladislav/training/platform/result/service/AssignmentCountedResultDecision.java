package com.vladislav.training.platform.result.service;

/**
 * Запись данных {@code AssignmentCountedResultDecision}.
 */
record AssignmentCountedResultDecision(
    boolean countedInAssignment,
    AssignmentCountedResultDecisionReason reason
) {
}
