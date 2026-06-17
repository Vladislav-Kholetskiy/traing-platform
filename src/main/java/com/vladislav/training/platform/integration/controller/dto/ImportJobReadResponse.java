package com.vladislav.training.platform.integration.controller.dto;

import com.vladislav.training.platform.integration.domain.ImportJobStatus;
import java.time.Instant;

/**
 * Ответ {@code ImportJobReadResponse}.
 */
public record ImportJobReadResponse(
    Long id,
    String sourceType,
    String sourceRef,
    ImportJobStatus status,
    Integer totalItemCount,
    Integer processedItemCount,
    Integer appliedItemCount,
    Integer failedItemCount,
    Integer requiresReviewItemCount,
    Instant startedAt,
    Instant completedAt,
    Instant createdAt,
    Instant updatedAt
) {
}
