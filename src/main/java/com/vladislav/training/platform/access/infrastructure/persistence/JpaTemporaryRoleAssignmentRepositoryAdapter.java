package com.vladislav.training.platform.access.infrastructure.persistence;

import com.vladislav.training.platform.access.domain.TemporaryRoleAssignment;
import com.vladislav.training.platform.access.repository.TemporaryRoleAssignmentRepository;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Адаптер репозитория {@code JpaTemporaryRoleAssignmentRepositoryAdapter}.
 */
@Repository
@Transactional(readOnly = true)
public class JpaTemporaryRoleAssignmentRepositoryAdapter implements TemporaryRoleAssignmentRepository {

    private final SpringDataTemporaryRoleAssignmentJpaRepository repository;
    private final AccessMapper mapper;

    public JpaTemporaryRoleAssignmentRepositoryAdapter(
        SpringDataTemporaryRoleAssignmentJpaRepository repository,
        AccessMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public TemporaryRoleAssignment findTemporaryRoleAssignmentById(Long temporaryRoleAssignmentId) {
        return repository.findById(temporaryRoleAssignmentId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException(
                "Temporary role assignment not found by id: " + temporaryRoleAssignmentId
            ));
    }

    @Override
    public List<TemporaryRoleAssignment> findTemporaryRoleAssignmentsByUserId(Long userId) {
        return mapper.toTemporaryRoleAssignments(repository.findAllByUserIdOrderByValidFromDescIdDesc(userId));
    }

    @Override
    public List<TemporaryRoleAssignment> findActiveTemporaryRoleAssignmentsByUserId(Long userId, Instant activeAt) {
        return mapper.toTemporaryRoleAssignments(repository.findActiveByUserId(userId, activeAt));
    }

    @Override
    @Transactional
    public TemporaryRoleAssignment saveTemporaryRoleAssignment(TemporaryRoleAssignment temporaryRoleAssignment) {
        try {
            return mapper.toDomain(repository.save(mapper.toEntity(temporaryRoleAssignment)));
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConstraintViolationException(
                "Failed to persist temporary_role_assignment",
                exception
            );
        }
    }

    @Override
    @Transactional
    public void endTemporaryRoleAssignment(Long temporaryRoleAssignmentId, Instant validTo) {
        TemporaryRoleAssignmentEntity entity = repository.findById(temporaryRoleAssignmentId)
            .orElseThrow(() -> new NotFoundException(
                "Temporary role assignment not found by id: " + temporaryRoleAssignmentId
            ));
        entity.setValidTo(validTo);
        entity.setUpdatedAt(validTo);
        try {
            repository.save(entity);
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConstraintViolationException("Failed to close temporary_role_assignment", exception);
        }
    }
}
