package com.vladislav.training.platform.access.infrastructure.persistence;

import com.vladislav.training.platform.access.domain.ManagementRelationType;
import com.vladislav.training.platform.access.repository.ManagementRelationTypeRepository;
import com.vladislav.training.platform.common.exception.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Адаптер репозитория {@code JpaManagementRelationTypeRepositoryAdapter}.
 */
@Repository
@Transactional(readOnly = true)
public class JpaManagementRelationTypeRepositoryAdapter implements ManagementRelationTypeRepository {

    private final SpringDataManagementRelationTypeJpaRepository repository;
    private final AccessMapper mapper;

    public JpaManagementRelationTypeRepositoryAdapter(
        SpringDataManagementRelationTypeJpaRepository repository,
        AccessMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public ManagementRelationType findManagementRelationTypeById(Long managementRelationTypeId) {
        return repository.findById(managementRelationTypeId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException(
                "Management relation type not found by id: " + managementRelationTypeId
            ));
    }

    @Override
    public ManagementRelationType findManagementRelationTypeByCode(String managementRelationTypeCode) {
        return repository.findByCode(managementRelationTypeCode)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException(
                "Management relation type not found by code: " + managementRelationTypeCode
            ));
    }

    @Override
    public List<ManagementRelationType> findAllManagementRelationTypes() {
        return mapper.toManagementRelationTypes(repository.findAllByOrderByCodeAsc());
    }
}
