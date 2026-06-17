package com.vladislav.training.platform.access.service;

import com.vladislav.training.platform.access.domain.UserAccessArea;
import java.time.Instant;
import java.util.List;

/**
 * Контракт командного сервиса {@code UserAccessAreaCommandService}.
 */
public interface UserAccessAreaCommandService {

    UserAccessArea findUserAccessAreaById(Long userAccessAreaId);

    List<UserAccessArea> findUserAccessAreasByUserId(Long userId);

    List<UserAccessArea> findActiveUserAccessAreasByUserId(Long userId, Instant activeAt);

    UserAccessArea saveUserAccessArea(UserAccessArea userAccessArea);

    void revokeUserAccessArea(Long userAccessAreaId, Instant validTo);

        List<UserAccessArea> closeActiveUserAccessAreasByUserId(Long userId, Instant effectiveAt);
}
