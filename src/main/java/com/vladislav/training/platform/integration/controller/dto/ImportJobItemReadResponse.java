package com.vladislav.training.platform.integration.controller.dto;

import com.vladislav.training.platform.integration.domain.ImportItemStatus;
import java.time.Instant;

/**
 * Ответ {@code ImportJobItemReadResponse}.
 */
public record ImportJobItemReadResponse(
    Long id,
    Long importJobId,
    int itemNo,
    String targetEntityType,
    String externalId,
    String employeeNumber,
    ImportItemStatus status,
    String matchedEntityId,
    String errorCode,
    String errorMessage,
    Instant processedAt,
    Instant createdAt,
    Instant updatedAt
) {
}
