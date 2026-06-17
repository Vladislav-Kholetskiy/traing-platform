package com.vladislav.training.platform.access.infrastructure.persistence;

import com.vladislav.training.platform.access.domain.ManagementRelation;
import com.vladislav.training.platform.access.repository.ManagementRelationRepository;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Адаптер репозитория {@code JpaManagementRelationRepositoryAdapter}.
 */
@Repository
@Transactional(readOnly = true)
public class JpaManagementRelationRepositoryAdapter implements ManagementRelationRepository {

    private final SpringDataManagementRelationJpaRepository repository;
    private final AccessMapper mapper;

    public JpaManagementRelationRepositoryAdapter(
        SpringDataManagementRelationJpaRepository repository,
        AccessMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public ManagementRelation findManagementRelationById(Long managementRelationId) {
        return repository.findById(managementRelationId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException(
                "Management relation not found by id: " + managementRelationId
            ));
    }

    @Override
    public List<ManagementRelation> findManagementRelationsByUserId(Long userId) {
        return mapper.toManagementRelations(repository.findAllByUserIdOrderByValidFromDescIdDesc(userId));
    }

    @Override
    public List<ManagementRelation> findManagementRelationsByOrganizationalUnitId(Long organizationalUnitId) {
        return mapper.toManagementRelations(
            repository.findAllByOrganizationalUnitIdOrderByValidFromDescIdDesc(organizationalUnitId)
        );
    }

    @Override
    public List<ManagementRelation> findActiveManagementRelationsByUserId(Long userId, Instant activeAt) {
        return mapper.toManagementRelations(repository.findActiveByUserId(userId, activeAt));
    }

    @Override
    @Transactional
    public ManagementRelation saveManagementRelation(ManagementRelation managementRelation) {
        try {
            return mapper.toDomain(repository.save(mapper.toEntity(managementRelation)));
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConstraintViolationException("Failed to persist management_relation", exception);
        }
    }

    @Override
    @Transactional
    public void endManagementRelation(Long managementRelationId, Instant validTo) {
        ManagementRelationEntity entity = repository.findById(managementRelationId)
            .orElseThrow(() -> new NotFoundException(
                "Management relation not found by id: " + managementRelationId
            ));
        entity.setValidTo(validTo);
        entity.setUpdatedAt(validTo);
        try {
            repository.save(entity);
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConstraintViolationException("Failed to close management_relation", exception);
        }
    }
}
