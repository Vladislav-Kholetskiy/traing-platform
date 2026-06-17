package com.vladislav.training.platform.access.repository;

import com.vladislav.training.platform.access.domain.ManagementRelation;
import java.time.Instant;
import java.util.List;

/**
 * Контракт репозитория {@code ManagementRelationRepository}.
 */
public interface ManagementRelationRepository {

    ManagementRelation findManagementRelationById(Long managementRelationId);

    List<ManagementRelation> findManagementRelationsByUserId(Long userId);

    List<ManagementRelation> findManagementRelationsByOrganizationalUnitId(Long organizationalUnitId);

    List<ManagementRelation> findActiveManagementRelationsByUserId(Long userId, Instant activeAt);

    ManagementRelation saveManagementRelation(ManagementRelation managementRelation);

    void endManagementRelation(Long managementRelationId, Instant validTo);
}
