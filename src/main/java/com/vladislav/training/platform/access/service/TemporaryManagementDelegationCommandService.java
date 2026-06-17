package com.vladislav.training.platform.access.service;

import com.vladislav.training.platform.access.domain.TemporaryManagementDelegation;
import java.time.Instant;
import java.util.List;

/**
 * Контракт командного сервиса {@code TemporaryManagementDelegationCommandService}.
 */
public interface TemporaryManagementDelegationCommandService {

    TemporaryManagementDelegation findTemporaryManagementDelegationById(Long temporaryManagementDelegationId);

    List<TemporaryManagementDelegation> findTemporaryManagementDelegationsByUserId(Long userId);

    List<TemporaryManagementDelegation> findActiveTemporaryManagementDelegationsByUserId(Long userId, Instant activeAt);

    TemporaryManagementDelegation saveTemporaryManagementDelegation(
        TemporaryManagementDelegation temporaryManagementDelegation
    );

    void endTemporaryManagementDelegation(Long temporaryManagementDelegationId, Instant validTo);

        List<TemporaryManagementDelegation> closeActiveTemporaryManagementDelegationsByUserId(Long userId, Instant effectiveAt);
}
