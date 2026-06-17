package com.vladislav.training.platform.userorg.controller.dto;

import java.time.Instant;

/**
 * Ответ {@code RoleResponse}.
 */
public record RoleResponse(
    Long id,
    String code,
    String name,
    String description,
    Instant createdAt,
    Instant updatedAt
) {
}
