package com.vladislav.training.platform.integration.domain;
/**
 * Перечисление {@code ImportJobStatus}.
 */
public enum ImportJobStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    COMPLETED_WITH_ERRORS,
    FAILED
}
