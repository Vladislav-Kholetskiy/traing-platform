package com.vladislav.training.platform.userorg.infrastructure.persistence;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment;
import com.vladislav.training.platform.userorg.repository.UserOrganizationAssignmentRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Адаптер репозитория {@code JpaUserOrganizationAssignmentRepositoryAdapter}.
 */
@Repository
@Transactional(readOnly = true)
public class JpaUserOrganizationAssignmentRepositoryAdapter implements UserOrganizationAssignmentRepository {

    private final SpringDataUserOrganizationAssignmentJpaRepository repository;
    private final UserOrgMapper mapper;

    public JpaUserOrganizationAssignmentRepositoryAdapter(
        SpringDataUserOrganizationAssignmentJpaRepository repository,
        UserOrgMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public UserOrganizationAssignment findOrganizationAssignmentById(Long assignmentId) {
        return repository.findById(assignmentId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException("Organization assignment not found by id: " + assignmentId));
    }

    @Override
    public List<UserOrganizationAssignment> findOrganizationAssignmentsByUserId(Long userId) {
        return mapper.toUserOrganizationAssignments(repository.findAllByUserIdOrderByValidFromDescIdDesc(userId));
    }

    @Override
    public List<UserOrganizationAssignment> findOrganizationAssignmentsByUnitId(Long organizationalUnitId) {
        return mapper.toUserOrganizationAssignments(
            repository.findAllByOrganizationalUnitIdOrderByValidFromDescIdDesc(organizationalUnitId)
        );
    }

    @Override
    public List<UserOrganizationAssignment> findOrganizationAssignmentsByType(OrganizationAssignmentType assignmentType) {
        return mapper.toUserOrganizationAssignments(repository.findAllByAssignmentTypeOrderByValidFromDescIdDesc(assignmentType));
    }

    @Override
    public List<UserOrganizationAssignment> findActiveOrganizationAssignmentsByUserId(Long userId, Instant activeAt) {
        return mapper.toUserOrganizationAssignments(repository.findActiveByUserId(userId, activeAt));
    }

    @Override
    @Transactional
    public UserOrganizationAssignment saveOrganizationAssignment(UserOrganizationAssignment assignment) {
        try {
            return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(assignment)));
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConstraintViolationException(
                "Failed to persist user_organization_assignment",
                exception
            );
        }
    }

    @Override
    @Transactional
    public void endOrganizationAssignment(Long assignmentId, Instant validTo) {
        UserOrganizationAssignmentEntity entity = repository.findById(assignmentId)
            .orElseThrow(() -> new NotFoundException("Organization assignment not found by id: " + assignmentId));
        entity.setValidTo(validTo);
        entity.setUpdatedAt(validTo);
        try {
            repository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConstraintViolationException(
                "Failed to close user_organization_assignment",
                exception
            );
        }
    }
}
