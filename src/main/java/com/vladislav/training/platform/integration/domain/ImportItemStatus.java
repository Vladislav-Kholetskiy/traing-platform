package com.vladislav.training.platform.integration.domain;
/**
 * Перечисление {@code ImportItemStatus}.
 */
public enum ImportItemStatus {
    PENDING,
    PROCESSING,
    APPLIED,
    NO_CHANGE,
    FAILED,
    REQUIRES_REVIEW
}
