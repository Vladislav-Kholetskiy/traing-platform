package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import com.vladislav.training.platform.assignment.repository.AssignmentRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentTestRepository;
import com.vladislav.training.platform.common.exception.NotFoundException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@ConditionalOnBean({AssignmentRepository.class, AssignmentTestRepository.class})
class AssignmentAssignedExecutionAdmissionFoundationStateReadServiceImpl
    implements AssignmentAssignedExecutionAdmissionFoundationStateReadService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentTestRepository assignmentTestRepository;

    AssignmentAssignedExecutionAdmissionFoundationStateReadServiceImpl(
        AssignmentRepository assignmentRepository,
        AssignmentTestRepository assignmentTestRepository
    ) {
        this.assignmentRepository = assignmentRepository;
        this.assignmentTestRepository = assignmentTestRepository;
    }

    @Override
    public AssignmentAssignedExecutionAdmissionFoundationState findAssignmentAssignedExecutionAdmissionFoundationState(
        Long actorUserId,
        Long assignmentId,
        Long assignmentTestId
    ) {
        requireId(actorUserId, "actorUserId");
        requireId(assignmentId, "assignmentId");
        requireId(assignmentTestId, "assignmentTestId");

        Assignment assignment = findAssignmentOrNull(assignmentId);
        if (assignment == null || !assignment.userId().equals(actorUserId)) {
            return null;
        }

        AssignmentTest assignmentTest = findAssignmentTestOrNull(assignmentTestId);
        if (assignmentTest == null || !assignmentId.equals(assignmentTest.assignmentId())) {
            return null;
        }

        return new AssignmentAssignedExecutionAdmissionFoundationState(
            assignment.id(),
            assignmentTest.id(),
            assignmentTest.testId(),
            assignment.deadlineAt(),
            assignment.cancelledAt() != null,
            assignment.closedAt() != null,
            assignmentTest.isClosed()
        );
    }

    private Long requireId(Long value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return value;
    }

    private Assignment findAssignmentOrNull(Long assignmentId) {
        try {
            return assignmentRepository.findAssignmentById(assignmentId);
        } catch (NotFoundException exception) {
            return null;
        }
    }

    private AssignmentTest findAssignmentTestOrNull(Long assignmentTestId) {
        try {
            return assignmentTestRepository.findAssignmentTestById(assignmentTestId);
        } catch (NotFoundException exception) {
            return null;
        }
    }
}
