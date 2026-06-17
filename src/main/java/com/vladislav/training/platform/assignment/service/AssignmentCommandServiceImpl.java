package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import com.vladislav.training.platform.assignment.repository.AssignmentRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentTestRepository;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.NotFoundException;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class AssignmentCommandServiceImpl implements AssignmentCommandService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentTestRepository assignmentTestRepository;
    private final AssignmentStatusRecalculationService assignmentStatusRecalculationService;

    AssignmentCommandServiceImpl(
        AssignmentRepository assignmentRepository,
        AssignmentTestRepository assignmentTestRepository,
        AssignmentStatusRecalculationService assignmentStatusRecalculationService
    ) {
        this.assignmentRepository = assignmentRepository;
        this.assignmentTestRepository = assignmentTestRepository;
        this.assignmentStatusRecalculationService = assignmentStatusRecalculationService;
    }

    @Override
    public Assignment createAssignment(Assignment assignment) {
        Objects.requireNonNull(assignment, "assignment must not be null");
        return assignmentRepository.saveAssignment(assignment);
    }

    @Override
    public AssignmentTest createAssignmentTest(AssignmentTest assignmentTest) {
        Objects.requireNonNull(assignmentTest, "assignmentTest must not be null");
        return assignmentTestRepository.saveAssignmentTest(assignmentTest);
    }

    @Override
    public AssignmentTest closeAssignmentTestWithCountedResult(Long assignmentTestId, Long countedResultId, Instant closedAt) {
        Objects.requireNonNull(assignmentTestId, "assignmentTestId must not be null");
        Objects.requireNonNull(countedResultId, "countedResultId must not be null");
        Objects.requireNonNull(closedAt, "closedAt must not be null");

        AssignmentTest existing = assignmentTestRepository.findAssignmentTestById(assignmentTestId);
        if (existing == null) {
            throw new NotFoundException("Assignment test not found: assignmentTestId=" + assignmentTestId);
        }
        if (existing.countedResultId() != null && !Objects.equals(existing.countedResultId(), countedResultId)) {
            throw new ConflictException("Assignment test is already closed by counted result: " + assignmentTestId);
        }

        if (existing.isClosed() && existing.countedResultId() != null && existing.closedAt() != null) {
            assignmentStatusRecalculationService.refreshAssignmentStatusCache(existing.assignmentId(), closedAt);
            return existing;
        }

        if (existing.isClosed() && existing.countedResultId() == null) {
            throw new ConflictException("Assignment test is already closed by counted result: " + assignmentTestId);
        }

        AssignmentTest closed = new AssignmentTest(
            existing.id(),
            existing.assignmentId(),
            existing.testId(),
            existing.assignmentTestRole(),
            existing.countedResultId() != null ? existing.countedResultId() : countedResultId,
            closedAt,
            true,
            existing.createdAt(),
            closedAt
        );
        AssignmentTest saved = assignmentTestRepository.saveAssignmentTest(closed);
        assignmentStatusRecalculationService.refreshAssignmentStatusCache(saved.assignmentId(), closedAt);
        return saved;
    }
}
