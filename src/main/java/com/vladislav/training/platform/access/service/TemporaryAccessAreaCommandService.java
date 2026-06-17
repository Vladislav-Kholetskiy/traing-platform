package com.vladislav.training.platform.access.service;

import com.vladislav.training.platform.access.domain.TemporaryAccessArea;
import java.time.Instant;
import java.util.List;

/**
 * Контракт командного сервиса {@code TemporaryAccessAreaCommandService}.
 */
public interface TemporaryAccessAreaCommandService {

    TemporaryAccessArea findTemporaryAccessAreaById(Long temporaryAccessAreaId);

    List<TemporaryAccessArea> findTemporaryAccessAreasByUserId(Long userId);

    List<TemporaryAccessArea> findActiveTemporaryAccessAreasByUserId(Long userId, Instant activeAt);

    TemporaryAccessArea saveTemporaryAccessArea(TemporaryAccessArea temporaryAccessArea);

    void endTemporaryAccessArea(Long temporaryAccessAreaId, Instant validTo);

        List<TemporaryAccessArea> closeActiveTemporaryAccessAreasByUserId(Long userId, Instant effectiveAt);
}
