package com.vladislav.training.platform.userorg.infrastructure.persistence;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import com.vladislav.training.platform.userorg.domain.UserRoleAssignment;
import com.vladislav.training.platform.userorg.repository.UserRoleAssignmentRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Адаптер репозитория {@code JpaUserRoleAssignmentRepositoryAdapter}.
 */
@Repository
@Transactional(readOnly = true)
public class JpaUserRoleAssignmentRepositoryAdapter implements UserRoleAssignmentRepository {

    private final SpringDataUserRoleAssignmentJpaRepository repository;
    private final UserOrgMapper mapper;

    public JpaUserRoleAssignmentRepositoryAdapter(
        SpringDataUserRoleAssignmentJpaRepository repository,
        UserOrgMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public UserRoleAssignment findRoleAssignmentById(Long assignmentId) {
        return repository.findById(assignmentId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException("Role assignment not found by id: " + assignmentId));
    }

    @Override
    public List<UserRoleAssignment> findRoleAssignmentsByUserId(Long userId) {
        return mapper.toUserRoleAssignments(repository.findAllByUserIdOrderByValidFromDescIdDesc(userId));
    }

    @Override
    public List<UserRoleAssignment> findRoleAssignmentsByRoleId(Long roleId) {
        return mapper.toUserRoleAssignments(repository.findAllByRoleIdOrderByValidFromDescIdDesc(roleId));
    }

    @Override
    public List<UserRoleAssignment> findActiveRoleAssignmentsByUserId(Long userId, Instant activeAt) {
        return mapper.toUserRoleAssignments(repository.findActiveByUserId(userId, activeAt));
    }

    @Override
    @Transactional
    public UserRoleAssignment saveRoleAssignment(UserRoleAssignment assignment) {
        try {
            return mapper.toDomain(repository.save(mapper.toEntity(assignment)));
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConstraintViolationException("Failed to persist user_role_assignment", exception);
        }
    }

    @Override
    @Transactional
    public void endRoleAssignment(Long assignmentId, Instant validTo) {
        UserRoleAssignmentEntity entity = repository.findById(assignmentId)
            .orElseThrow(() -> new NotFoundException("Role assignment not found by id: " + assignmentId));
        entity.setValidTo(validTo);
        entity.setUpdatedAt(validTo);
        try {
            repository.save(entity);
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConstraintViolationException("Failed to close user_role_assignment", exception);
        }
    }
}
