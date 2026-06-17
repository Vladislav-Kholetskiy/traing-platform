package com.vladislav.training.platform.access.service;

import com.vladislav.training.platform.access.domain.AccessScopeType;
import java.time.Instant;

/**
 * Фильтр {@code UserAccessAreaAdminFilter}.
 */
public record UserAccessAreaAdminFilter(
    Long userId,
    Long organizationalUnitId,
    AccessScopeType accessScopeType,
    Instant activeAt
) {
}
