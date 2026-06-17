package com.vladislav.training.platform.integration.domain;

import java.time.Instant;
import java.util.Objects;
/**
 * Запись данных {@code ImportJob}.
 */
public record ImportJob(
    Long id,
    String sourceType,
    String sourceRef,
    Long initiatedByUserId,
    ImportJobStatus status,
    String payload,
    Instant startedAt,
    Instant completedAt,
    Integer totalItemCount,
    Integer processedItemCount,
    Integer appliedItemCount,
    Integer failedItemCount,
    Integer requiresReviewItemCount,
    Instant createdAt,
    Instant updatedAt
) {

    public ImportJob {
        Objects.requireNonNull(sourceType, "sourceType must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (sourceType.isBlank()) {
            throw new IllegalArgumentException("sourceType must not be blank");
        }
        if ((status == ImportJobStatus.COMPLETED
            || status == ImportJobStatus.COMPLETED_WITH_ERRORS
            || status == ImportJobStatus.FAILED)
            && completedAt == null) {
            throw new IllegalArgumentException("completedAt must not be null for terminal status");
        }
        if (totalItemCount != null && totalItemCount < 0) {
            throw new IllegalArgumentException("totalItemCount must be non-negative");
        }
        if (processedItemCount != null && processedItemCount < 0) {
            throw new IllegalArgumentException("processedItemCount must be non-negative");
        }
        if (appliedItemCount != null && appliedItemCount < 0) {
            throw new IllegalArgumentException("appliedItemCount must be non-negative");
        }
        if (failedItemCount != null && failedItemCount < 0) {
            throw new IllegalArgumentException("failedItemCount must be non-negative");
        }
        if (requiresReviewItemCount != null && requiresReviewItemCount < 0) {
            throw new IllegalArgumentException("requiresReviewItemCount must be non-negative");
        }
        if (totalItemCount != null && processedItemCount != null && processedItemCount > totalItemCount) {
            throw new IllegalArgumentException("processedItemCount must not exceed totalItemCount");
        }
        if (processedItemCount != null
            && (coalesce(appliedItemCount) + coalesce(failedItemCount) + coalesce(requiresReviewItemCount)) > processedItemCount) {
            throw new IllegalArgumentException("terminal counters must not exceed processedItemCount");
        }
    }

    private static int coalesce(Integer value) {
        return value == null ? 0 : value;
    }
}
