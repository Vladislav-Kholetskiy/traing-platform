package com.vladislav.training.platform.access.repository;

import com.vladislav.training.platform.access.domain.TemporaryManagementDelegation;
import java.time.Instant;
import java.util.List;

/**
 * Контракт репозитория {@code TemporaryManagementDelegationRepository}.
 */
public interface TemporaryManagementDelegationRepository {

    TemporaryManagementDelegation findTemporaryManagementDelegationById(Long temporaryManagementDelegationId);

    List<TemporaryManagementDelegation> findTemporaryManagementDelegationsByUserId(Long userId);

    List<TemporaryManagementDelegation> findTemporaryManagementDelegationsByOrganizationalUnitId(Long organizationalUnitId);

    List<TemporaryManagementDelegation> findActiveTemporaryManagementDelegationsByUserId(Long userId, Instant activeAt);

    TemporaryManagementDelegation saveTemporaryManagementDelegation(
        TemporaryManagementDelegation temporaryManagementDelegation
    );

    void endTemporaryManagementDelegation(Long temporaryManagementDelegationId, Instant validTo);
}
