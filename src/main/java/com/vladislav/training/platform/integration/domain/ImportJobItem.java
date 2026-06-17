package com.vladislav.training.platform.integration.domain;

import java.time.Instant;
import java.util.Objects;
/**
 * Запись данных {@code ImportJobItem}.
 */
public record ImportJobItem(
    Long id,
    Long importJobId,
    int itemNo,
    String targetEntityType,
    String externalId,
    String employeeNumber,
    ImportItemStatus status,
    String matchedEntityId,
    String payload,
    String errorCode,
    String errorMessage,
    Instant processedAt,
    Instant createdAt,
    Instant updatedAt
) {

    public ImportJobItem {
        Objects.requireNonNull(importJobId, "importJobId must not be null");
        Objects.requireNonNull(targetEntityType, "targetEntityType must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (itemNo < 0) {
            throw new IllegalArgumentException("itemNo must be non-negative");
        }
        if (targetEntityType.isBlank()) {
            throw new IllegalArgumentException("targetEntityType must not be blank");
        }
        if ((status == ImportItemStatus.APPLIED
            || status == ImportItemStatus.NO_CHANGE
            || status == ImportItemStatus.FAILED
            || status == ImportItemStatus.REQUIRES_REVIEW)
            && processedAt == null) {
            throw new IllegalArgumentException("processedAt must not be null for terminal item status");
        }
    }
}
