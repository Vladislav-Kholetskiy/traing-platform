package com.vladislav.training.platform.access.service;

import java.time.Instant;
import java.util.List;

/**
 * Контракт сервиса {@code TemporaryRoleAssignmentReadService}.
 */
public interface TemporaryRoleAssignmentReadService {

    List<Long> findActiveTemporaryRoleIdsByUserId(Long userId, Instant activeAt);
}
