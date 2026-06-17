package com.vladislav.training.platform.access.controller.dto;

import java.time.Instant;

/**
 * Ответ {@code ManagementRelationTypeResponse}.
 */
public record ManagementRelationTypeResponse(
    Long id,
    String code,
    String name,
    String description,
    Instant createdAt,
    Instant updatedAt
) {
}
