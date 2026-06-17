package com.vladislav.training.platform.assignment.infrastructure.persistence;

import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.repository.AssignmentRepository;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaAssignmentRepositoryAdapter implements AssignmentRepository {

    private final SpringDataAssignmentJpaRepository repository;
    private final AssignmentPersistenceMapper mapper;

    public JpaAssignmentRepositoryAdapter(
        SpringDataAssignmentJpaRepository repository,
        AssignmentPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Assignment findAssignmentById(Long assignmentId) {
        return repository.findById(assignmentId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException("Assignment not found: " + assignmentId));
    }

    @Override
    public List<Assignment> findAllAssignments() {
        return mapper.toAssignments(repository.findAllByOrderByIdAsc());
    }

    @Override
    public List<Assignment> findAssignmentsByCampaignId(Long campaignId) {
        return mapper.toAssignments(repository.findAllByCampaignIdOrderByIdAsc(campaignId));
    }

    @Override
    public List<Assignment> findAssignmentsByUserId(Long userId) {
        return mapper.toAssignments(repository.findAllByUserIdOrderByIdAsc(userId));
    }

    @Override
    public List<Assignment> findAssignmentsByUserIdAndStatus(Long userId, AssignmentStatus status) {
        return mapper.toAssignments(repository.findAllByUserIdAndStatusOrderByIdAsc(userId, status));
    }

    @Override
    public Assignment findActiveAssignmentByUserIdAndCourseId(Long userId, Long courseId) {
        return repository.findFirstByUserIdAndCourseIdAndCancelledAtIsNullAndClosedAtIsNullOrderByIdDesc(
            userId,
            courseId
        ).map(mapper::toDomain).orElse(null);
    }

    @Override
    @Transactional
    public Assignment saveAssignment(Assignment assignment) {
        try {
            return mapper.toDomain(repository.save(mapper.toEntity(assignment)));
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConstraintViolationException("Failed to persist assignment", exception);
        }
    }
}
