package com.vladislav.training.platform.application.query;

import com.vladislav.training.platform.access.service.AccessReadScope;
import java.time.Instant;
import java.util.Set;

/**
 * Контракт сервиса {@code AccessScopeProjectionService}.
 */
public interface AccessScopeProjectionService {

    Set<Long> resolveOrganizationalUnitIds(AccessReadScope scope);

    Set<Long> resolveVisibleUserIds(AccessReadScope scope, Instant effectiveAt);
}
