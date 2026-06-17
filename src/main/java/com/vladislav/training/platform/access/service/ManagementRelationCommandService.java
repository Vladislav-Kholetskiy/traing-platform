package com.vladislav.training.platform.access.service;

import com.vladislav.training.platform.access.domain.ManagementRelation;
import java.time.Instant;
import java.util.List;

/**
 * Контракт командного сервиса {@code ManagementRelationCommandService}.
 */
public interface ManagementRelationCommandService {

    ManagementRelation findManagementRelationById(Long managementRelationId);

    List<ManagementRelation> findManagementRelationsByUserId(Long userId);

    List<ManagementRelation> findActiveManagementRelationsByUserId(Long userId, Instant activeAt);

    ManagementRelation saveManagementRelation(ManagementRelation managementRelation);

    void endManagementRelation(Long managementRelationId, Instant validTo);

        List<ManagementRelation> closeActiveManagementRelationsByUserId(Long userId, Instant effectiveAt);
}
