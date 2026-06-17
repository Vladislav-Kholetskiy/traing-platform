package com.vladislav.training.platform.access.service;

import com.vladislav.training.platform.access.domain.AccessScopeType;
import java.time.Instant;

/**
 * Фильтр {@code TemporaryAccessAreaAdminFilter}.
 */
public record TemporaryAccessAreaAdminFilter(
    Long userId,
    Long organizationalUnitId,
    AccessScopeType accessScopeType,
    Instant activeAt
) {
}
