package com.vladislav.training.platform.access.controller.dto;

import com.vladislav.training.platform.access.domain.AccessScopeType;
import java.time.Instant;

/**
 * Ответ {@code TemporaryAccessAreaResponse}.
 */
public record TemporaryAccessAreaResponse(
    Long id,
    Long userId,
    Long organizationalUnitId,
    AccessScopeType accessScopeType,
    Instant validFrom,
    Instant validTo,
    Instant createdAt,
    Instant updatedAt
) {
}
