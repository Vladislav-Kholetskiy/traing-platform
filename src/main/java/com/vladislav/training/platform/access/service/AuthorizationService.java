package com.vladislav.training.platform.access.service;

import com.vladislav.training.platform.access.domain.AccessDecision;
import com.vladislav.training.platform.access.domain.AccessScopeType;
import java.time.Instant;

/**
 * Контракт сервиса {@code AuthorizationService}.
 */
public interface AuthorizationService {

    AccessDecision decideAccess(Long userId, AccessScopeType targetType, Long organizationalUnitId, Instant effectiveAt);
}
