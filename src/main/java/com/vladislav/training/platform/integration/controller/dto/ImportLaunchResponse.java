package com.vladislav.training.platform.integration.controller.dto;

import com.vladislav.training.platform.integration.domain.ImportJobStatus;
import java.time.Instant;

/**
 * Ответ {@code ImportLaunchResponse}.
 */
public record ImportLaunchResponse(
    Long importJobId,
    ImportJobStatus status,
    Integer totalItemCount,
    Instant createdAt,
    Instant updatedAt
) {
}
