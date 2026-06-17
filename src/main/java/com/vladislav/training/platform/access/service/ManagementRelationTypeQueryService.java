package com.vladislav.training.platform.access.service;

import com.vladislav.training.platform.access.domain.ManagementRelationType;
import java.util.List;

/**
 * Контракт сервиса чтения {@code ManagementRelationTypeQueryService}.
 */
public interface ManagementRelationTypeQueryService {

    ManagementRelationType findManagementRelationTypeById(Long managementRelationTypeId);

    ManagementRelationType findManagementRelationTypeByCode(String managementRelationTypeCode);

    List<ManagementRelationType> findAllManagementRelationTypes();
}
