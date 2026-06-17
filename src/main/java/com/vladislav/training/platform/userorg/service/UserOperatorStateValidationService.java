package com.vladislav.training.platform.userorg.service;

import java.time.Instant;
import java.util.List;

/**
 * Контракт сервиса {@code UserOperatorStateValidationService}.
 */
public interface UserOperatorStateValidationService {

    void ensureResultingTemporaryRoleStateConsistent(Long userId, Instant activeAt, List<Long> resultingTemporaryRoleIds);
}

