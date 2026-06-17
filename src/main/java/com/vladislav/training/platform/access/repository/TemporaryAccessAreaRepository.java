package com.vladislav.training.platform.access.repository;

import com.vladislav.training.platform.access.domain.TemporaryAccessArea;
import java.time.Instant;
import java.util.List;

/**
 * Контракт репозитория {@code TemporaryAccessAreaRepository}.
 */
public interface TemporaryAccessAreaRepository {

    TemporaryAccessArea findTemporaryAccessAreaById(Long temporaryAccessAreaId);

    List<TemporaryAccessArea> findTemporaryAccessAreasByUserId(Long userId);

    List<TemporaryAccessArea> findTemporaryAccessAreasByOrganizationalUnitId(Long organizationalUnitId);

    List<TemporaryAccessArea> findActiveTemporaryAccessAreasByUserId(Long userId, Instant activeAt);

    TemporaryAccessArea saveTemporaryAccessArea(TemporaryAccessArea temporaryAccessArea);

    void endTemporaryAccessArea(Long temporaryAccessAreaId, Instant validTo);
}
