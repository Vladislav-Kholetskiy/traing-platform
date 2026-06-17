package com.vladislav.training.platform.access.service;

import com.vladislav.training.platform.access.domain.EffectiveAccessScope;
import java.time.Instant;

/**
 * Разрешитель {@code EffectiveAccessResolver}.
 */
public interface EffectiveAccessResolver {

    EffectiveAccessScope resolveEffectiveScope(Long userId, Long organizationalUnitId, Instant effectiveAt);
}
