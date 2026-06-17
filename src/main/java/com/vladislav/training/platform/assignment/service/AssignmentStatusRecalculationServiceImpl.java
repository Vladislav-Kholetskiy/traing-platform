package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.repository.AssignmentRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentTestRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class AssignmentStatusRecalculationServiceImpl implements AssignmentStatusRecalculationService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentTestRepository assignmentTestRepository;
    private final AssignmentStatusDefiningCountedResultFactsReader countedResultFactsReader;

    AssignmentStatusRecalculationServiceImpl(
        AssignmentRepository assignmentRepository,
        AssignmentTestRepository assignmentTestRepository
    ) {
        this(assignmentRepository, assignmentTestRepository, (AssignmentStatusDefiningCountedResultFactsReader) null);
    }

    @Autowired
    AssignmentStatusRecalculationServiceImpl(
        AssignmentRepository assignmentRepository,
        AssignmentTestRepository assignmentTestRepository,
        ObjectProvider<AssignmentStatusDefiningCountedResultFactsReader> countedResultFactsReaderProvider
    ) {
        this(
            assignmentRepository,
            assignmentTestRepository,
            countedResultFactsReaderProvider == null ? null : countedResultFactsReaderProvider.getIfAvailable()
        );
    }

    AssignmentStatusRecalculationServiceImpl(
        AssignmentRepository assignmentRepository,
        AssignmentTestRepository assignmentTestRepository,
        AssignmentStatusDefiningCountedResultFactsReader countedResultFactsReader
    ) {
        this.assignmentRepository = assignmentRepository;
        this.assignmentTestRepository = assignmentTestRepository;
        this.countedResultFactsReader = countedResultFactsReader;
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentStatus recalculateAssignmentStatus(Long assignmentId, Instant effectiveAt) {
        Objects.requireNonNull(assignmentId, "assignmentId must not be null");
        Objects.requireNonNull(effectiveAt, "effectiveAt must not be null");

        Assignment assignment = assignmentRepository.findAssignmentById(assignmentId);
        if (assignment.cancelledAt() != null) {
            return AssignmentStatus.CANCELLED;
        }

        List<com.vladislav.training.platform.assignment.domain.AssignmentTest> assignmentTests =
            assignmentTestRepository.findAssignmentTestsByAssignmentId(assignmentId);
        // counted-result proof for COMPLETED means all assignment tests must be closed by valid counted results that are passed,
        // within deadline, and counted in assignment.
        if (isCompletedByCountedResultProof(assignmentTests)) {
            return AssignmentStatus.COMPLETED;
        }

        if (assignment.deadlineAt().isBefore(effectiveAt)) {
            return AssignmentStatus.OVERDUE;
        }

        return AssignmentStatus.ASSIGNED;
    }

    private boolean isCompletedByCountedResultProof(
        List<com.vladislav.training.platform.assignment.domain.AssignmentTest> assignmentTests
    ) {
        if (countedResultFactsReader == null || assignmentTests.isEmpty()) {
            return false;
        }

        for (com.vladislav.training.platform.assignment.domain.AssignmentTest assignmentTest : assignmentTests) {
            if (!assignmentTest.isClosed() || assignmentTest.countedResultId() == null) {
                return false;
            }

            AssignmentStatusDefiningCountedResultFactsReader.StatusDefiningCountedResultFacts countedResultFacts =
                countedResultFactsReader.findStatusDefiningFactsByCountedResultId(assignmentTest.countedResultId());
            if (!countedResultFacts.passed()
                || !countedResultFacts.withinDeadline()
                || !countedResultFacts.countedInAssignment()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public Assignment refreshAssignmentStatusCache(Long assignmentId, Instant effectiveAt) {
        Objects.requireNonNull(assignmentId, "assignmentId must not be null");
        Objects.requireNonNull(effectiveAt, "effectiveAt must not be null");

        Assignment assignment = assignmentRepository.findAssignmentById(assignmentId);
        AssignmentStatus recalculatedStatus = recalculateAssignmentStatus(assignmentId, effectiveAt);
        Instant materializedClosedAt = materializeClosedAt(assignment, recalculatedStatus, effectiveAt);
        if (assignment.status() == recalculatedStatus
            && Objects.equals(assignment.closedAt(), materializedClosedAt)) {
            return assignment;
        }

        Assignment refreshed = new Assignment(
            assignment.id(),
            assignment.campaignId(),
            assignment.userId(),
            assignment.courseId(),
            recalculatedStatus,
            assignment.assignedAt(),
            assignment.deadlineAt(),
            assignment.cancelledAt(),
            materializedClosedAt,
            assignment.createdAt(),
            effectiveAt
        );
        return assignmentRepository.saveAssignment(refreshed);
    }

    private Instant materializeClosedAt(Assignment assignment, AssignmentStatus recalculatedStatus, Instant effectiveAt) {
        if (recalculatedStatus == AssignmentStatus.COMPLETED) {
            return assignment.closedAt() != null ? assignment.closedAt() : effectiveAt;
        }
        return null;
    }
}
