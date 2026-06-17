package com.vladislav.training.platform.access.repository;

import com.vladislav.training.platform.access.domain.ManagementRelationType;
import java.util.List;

/**
 * Контракт репозитория {@code ManagementRelationTypeRepository}.
 */
public interface ManagementRelationTypeRepository {

    ManagementRelationType findManagementRelationTypeById(Long managementRelationTypeId);

    ManagementRelationType findManagementRelationTypeByCode(String managementRelationTypeCode);

    List<ManagementRelationType> findAllManagementRelationTypes();
}
