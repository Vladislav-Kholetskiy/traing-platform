package com.vladislav.training.platform.access.repository;

import com.vladislav.training.platform.access.domain.UserAccessArea;
import java.time.Instant;
import java.util.List;

/**
 * Контракт репозитория {@code UserAccessAreaRepository}.
 */
public interface UserAccessAreaRepository {

    UserAccessArea findUserAccessAreaById(Long userAccessAreaId);

    List<UserAccessArea> findUserAccessAreasByUserId(Long userId);

    List<UserAccessArea> findUserAccessAreasByOrganizationalUnitId(Long organizationalUnitId);

    List<UserAccessArea> findActiveUserAccessAreasByUserId(Long userId, Instant activeAt);

    UserAccessArea saveUserAccessArea(UserAccessArea userAccessArea);

    void revokeUserAccessArea(Long userAccessAreaId, Instant validTo);
}
