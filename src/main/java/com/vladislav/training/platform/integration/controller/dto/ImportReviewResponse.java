package com.vladislav.training.platform.integration.controller.dto;

import com.vladislav.training.platform.integration.domain.ImportItemStatus;
import java.time.Instant;

/**
 * Ответ {@code ImportReviewResponse}.
 */
public record ImportReviewResponse(
    Long itemId,
    Long importJobId,
    ImportItemStatus status,
    String matchedEntityId,
    String errorCode,
    String errorMessage,
    Instant processedAt,
    Instant updatedAt
) {
}
