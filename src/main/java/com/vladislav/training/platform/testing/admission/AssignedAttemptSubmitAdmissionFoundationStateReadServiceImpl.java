package com.vladislav.training.platform.testing.admission;

import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import com.vladislav.training.platform.assignment.repository.AssignmentTestRepository;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.repository.TestAttemptRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса {@code AssignedAttemptSubmitAdmissionFoundationStateReadServiceImpl}.
 */
@Service
@Transactional(readOnly = true)
@ConditionalOnBean({TestAttemptRepository.class, AssignmentTestRepository.class})
class AssignedAttemptSubmitAdmissionFoundationStateReadServiceImpl
    implements AssignedAttemptSubmitAdmissionFoundationStateReadService {

    private final TestAttemptRepository testAttemptRepository;
    private final AssignmentTestRepository assignmentTestRepository;

    AssignedAttemptSubmitAdmissionFoundationStateReadServiceImpl(
        TestAttemptRepository testAttemptRepository,
        AssignmentTestRepository assignmentTestRepository
    ) {
        this.testAttemptRepository = testAttemptRepository;
        this.assignmentTestRepository = assignmentTestRepository;
    }

    @Override
    public AssignedAttemptSubmitAdmissionFoundationState findAssignedAttemptSubmitAdmissionFoundationState(
        Long actorUserId,
        Long testAttemptId
    ) {
        requireId(actorUserId, "actorUserId");
        requireId(testAttemptId, "testAttemptId");

        TestAttempt attempt = findAttemptOrNull(testAttemptId);
        if (attempt == null
            || attempt.attemptMode() != AttemptMode.ASSIGNED
            || attempt.assignmentTestId() == null
            || !actorUserId.equals(attempt.userId())) {
            return null;
        }

        AssignmentTest assignmentTest = findAssignmentTestOrNull(attempt.assignmentTestId());
        if (assignmentTest == null) {
            return null;
        }

        return new AssignedAttemptSubmitAdmissionFoundationState(
            attempt.id(),
            assignmentTest.assignmentId(),
            assignmentTest.id()
        );
    }

    private Long requireId(Long value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return value;
    }

    private TestAttempt findAttemptOrNull(Long testAttemptId) {
        try {
            return testAttemptRepository.findTestAttemptById(testAttemptId);
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
