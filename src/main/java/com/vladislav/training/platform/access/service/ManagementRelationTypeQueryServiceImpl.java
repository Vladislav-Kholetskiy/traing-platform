package com.vladislav.training.platform.access.service;

import com.vladislav.training.platform.access.domain.ManagementRelationType;
import com.vladislav.training.platform.access.repository.ManagementRelationTypeRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса чтения {@code ManagementRelationTypeQueryServiceImpl}.
 */
@Service
@Transactional(readOnly = true)
public class ManagementRelationTypeQueryServiceImpl implements ManagementRelationTypeQueryService {

    private final ManagementRelationTypeRepository managementRelationTypeRepository;

    public ManagementRelationTypeQueryServiceImpl(ManagementRelationTypeRepository managementRelationTypeRepository) {
        this.managementRelationTypeRepository = managementRelationTypeRepository;
    }

    @Override
    public ManagementRelationType findManagementRelationTypeById(Long managementRelationTypeId) {
        return managementRelationTypeRepository.findManagementRelationTypeById(managementRelationTypeId);
    }

    @Override
    public ManagementRelationType findManagementRelationTypeByCode(String managementRelationTypeCode) {
        return managementRelationTypeRepository.findManagementRelationTypeByCode(managementRelationTypeCode);
    }

    @Override
    public List<ManagementRelationType> findAllManagementRelationTypes() {
        return managementRelationTypeRepository.findAllManagementRelationTypes();
    }
}
