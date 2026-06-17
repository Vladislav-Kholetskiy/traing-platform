package com.vladislav.training.platform.assignment.infrastructure.persistence;

import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentAdministrativeAction;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import com.vladislav.training.platform.assignment.repository.AssignmentReadRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentSelfScopedReadRepository;
import com.vladislav.training.platform.common.exception.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaAssignmentReadRepositoryAdapter implements AssignmentReadRepository, AssignmentSelfScopedReadRepository {

    private final SpringDataAssignmentJpaRepository repository;
    private final SpringDataAssignmentTestJpaRepository assignmentTestRepository;
    private final SpringDataAssignmentAdministrativeActionJpaRepository assignmentAdministrativeActionRepository;
    private final AssignmentPersistenceMapper mapper;

    public JpaAssignmentReadRepositoryAdapter(
        SpringDataAssignmentJpaRepository repository,
        SpringDataAssignmentTestJpaRepository assignmentTestRepository,
        SpringDataAssignmentAdministrativeActionJpaRepository assignmentAdministrativeActionRepository,
        AssignmentPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.assignmentTestRepository = assignmentTestRepository;
        this.assignmentAdministrativeActionRepository = assignmentAdministrativeActionRepository;
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
    public List<Assignment> findSelfScopedAssignments(Long actorUserId) {
        return mapper.toAssignments(repository.findAllByUserIdOrderByIdAsc(actorUserId));
    }

    @Override
    public Assignment findSelfScopedAssignmentById(Long actorUserId, Long assignmentId) {
        return repository.findByIdAndUserId(assignmentId, actorUserId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException(
                "Assignment not found in self scope: actorUserId=" + actorUserId + ", assignmentId=" + assignmentId
            ));
    }

    @Override
    public List<AssignmentTest> findSelfScopedAssignmentTestsByAssignmentId(Long actorUserId, Long assignmentId) {
        findSelfScopedAssignmentById(actorUserId, assignmentId);
        return mapper.toAssignmentTests(assignmentTestRepository.findAllByAssignmentIdOrderByIdAsc(assignmentId));
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
    public AssignmentTest findAssignmentTestById(Long assignmentTestId) {
        return assignmentTestRepository.findById(assignmentTestId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException("Assignment test not found: " + assignmentTestId));
    }

    @Override
    public List<AssignmentTest> findAssignmentTestsByAssignmentId(Long assignmentId) {
        return mapper.toAssignmentTests(assignmentTestRepository.findAllByAssignmentIdOrderByIdAsc(assignmentId));
    }

    @Override
    public AssignmentTest findAssignmentTestByCountedResultId(Long countedResultId) {
        return assignmentTestRepository.findByCountedResultId(countedResultId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException(
                "Assignment test not found by counted result id: " + countedResultId
            ));
    }

    @Override
    public AssignmentAdministrativeAction findAssignmentAdministrativeActionById(Long assignmentAdministrativeActionId) {
        return assignmentAdministrativeActionRepository.findById(assignmentAdministrativeActionId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException(
                "Assignment administrative action not found: " + assignmentAdministrativeActionId
            ));
    }

    @Override
    public List<AssignmentAdministrativeAction> findAssignmentAdministrativeActionsByAssignmentId(Long assignmentId) {
        return mapper.toAssignmentAdministrativeActions(
            assignmentAdministrativeActionRepository.findAllByAssignmentIdOrderByIdAsc(assignmentId)
        );
    }
}
