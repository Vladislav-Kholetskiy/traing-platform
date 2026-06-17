package com.vladislav.training.platform.testing.service;

import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import com.vladislav.training.platform.assignment.repository.AssignmentRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentTestRepository;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import com.vladislav.training.platform.testing.repository.TestAttemptRepository;
import java.time.Instant;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса {@code AttemptStatusRecalculationServiceImpl}.
 */
@Service
@Transactional
@ConditionalOnBean({TestAttemptRepository.class, AssignmentRepository.class, AssignmentTestRepository.class})
class AttemptStatusRecalculationServiceImpl implements AttemptStatusRecalculationService {

    private final TestAttemptRepository testAttemptRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentTestRepository assignmentTestRepository;

    AttemptStatusRecalculationServiceImpl(
        TestAttemptRepository testAttemptRepository,
        AssignmentRepository assignmentRepository,
        AssignmentTestRepository assignmentTestRepository
    ) {
        this.testAttemptRepository = Objects.requireNonNull(testAttemptRepository, "testAttemptRepository must not be null");
        this.assignmentRepository = Objects.requireNonNull(assignmentRepository, "assignmentRepository must not be null");
        this.assignmentTestRepository = Objects.requireNonNull(
            assignmentTestRepository,
            "assignmentTestRepository must not be null"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TestAttemptStatus recalculateAttemptStatus(Long testAttemptId, Instant effectiveAt) {
        Objects.requireNonNull(testAttemptId, "testAttemptId must not be null");
        Objects.requireNonNull(effectiveAt, "effectiveAt must not be null");

        TestAttempt attempt = testAttemptRepository.findTestAttemptById(testAttemptId);
        return recalculateStatus(attempt, effectiveAt);
    }

    @Override
    public TestAttempt refreshAttemptStatusCache(Long testAttemptId, Instant effectiveAt) {
        Objects.requireNonNull(testAttemptId, "testAttemptId must not be null");
        Objects.requireNonNull(effectiveAt, "effectiveAt must not be null");

        TestAttempt lockedAttempt = testAttemptRepository.findAndLockTestAttemptById(testAttemptId);
        TestAttemptStatus recalculatedStatus = recalculateStatus(lockedAttempt, effectiveAt);
        if (recalculatedStatus == lockedAttempt.status()) {
            return lockedAttempt;
        }

        if (recalculatedStatus != TestAttemptStatus.EXPIRED) {
            throw new ConflictException(
                "Attempt status recalculation does not materialize unsupported transition: attemptId="
                    + testAttemptId
                    + ", targetStatus="
                    + recalculatedStatus
            );
        }

        TestAttempt refreshedAttempt = new TestAttempt(
            lockedAttempt.id(),
            lockedAttempt.userId(),
            lockedAttempt.testId(),
            lockedAttempt.assignmentTestId(),
            lockedAttempt.attemptMode(),
            TestAttemptStatus.EXPIRED,
            lockedAttempt.startedAt(),
            null,
            effectiveAt,
            null,
            lockedAttempt.lastActivityAt(),
            lockedAttempt.createdAt(),
            effectiveAt
        );
        return testAttemptRepository.saveTestAttempt(refreshedAttempt);
    }

    @Override
    public TestAttempt refreshAttemptStatusCache(Long actorUserId, Long testAttemptId, Instant effectiveAt) {
        return refreshAttemptStatusCacheWithVerdict(actorUserId, testAttemptId, effectiveAt).refreshedAttempt();
    }

    @Override
    public AttemptStatusRefreshResult refreshAttemptStatusCacheWithVerdict(Long actorUserId, Long testAttemptId, Instant effectiveAt) {
        Objects.requireNonNull(actorUserId, "actorUserId must not be null");
        Objects.requireNonNull(testAttemptId, "testAttemptId must not be null");
        Objects.requireNonNull(effectiveAt, "effectiveAt must not be null");

        TestAttempt lockedAttempt = testAttemptRepository.findAndLockTestAttemptByIdAndUserId(testAttemptId, actorUserId);
        return refreshVerdictFromLockedAttempt(lockedAttempt, testAttemptId, effectiveAt);
    }

    @Override
    public AttemptStatusRefreshResult refreshAssignedAttemptStatusCacheWithVerdict(
        Long actorUserId,
        Long assignmentTestId,
        Long testAttemptId,
        Instant effectiveAt
    ) {
        Objects.requireNonNull(actorUserId, "actorUserId must not be null");
        Objects.requireNonNull(assignmentTestId, "assignmentTestId must not be null");
        Objects.requireNonNull(testAttemptId, "testAttemptId must not be null");
        Objects.requireNonNull(effectiveAt, "effectiveAt must not be null");

        TestAttempt lockedAttempt = testAttemptRepository.findAndLockAssignedAttemptByIdAndUserIdAndAssignmentTestId(
            testAttemptId,
            actorUserId,
            assignmentTestId
        );
        requireAssignedRefreshLookupAttempt(lockedAttempt, assignmentTestId);
        return refreshVerdictFromLockedAttempt(lockedAttempt, testAttemptId, effectiveAt);
    }

    private AttemptStatusRefreshResult refreshVerdictFromLockedAttempt(
        TestAttempt lockedAttempt,
        Long testAttemptId,
        Instant effectiveAt
    ) {
        TestAttemptStatus recalculatedStatus = recalculateStatus(lockedAttempt, effectiveAt);
        if (recalculatedStatus == lockedAttempt.status()) {
            return new AttemptStatusRefreshResult(lockedAttempt, lockedAttempt.status(), false);
        }

        if (recalculatedStatus != TestAttemptStatus.EXPIRED) {
            throw new ConflictException(
                "Attempt status recalculation does not materialize unsupported transition: attemptId="
                    + testAttemptId
                    + ", targetStatus="
                    + recalculatedStatus
            );
        }

        TestAttempt refreshedAttempt = new TestAttempt(
            lockedAttempt.id(),
            lockedAttempt.userId(),
            lockedAttempt.testId(),
            lockedAttempt.assignmentTestId(),
            lockedAttempt.attemptMode(),
            TestAttemptStatus.EXPIRED,
            lockedAttempt.startedAt(),
            null,
            effectiveAt,
            null,
            lockedAttempt.lastActivityAt(),
            lockedAttempt.createdAt(),
            effectiveAt
        );
        return new AttemptStatusRefreshResult(
            testAttemptRepository.saveTestAttempt(refreshedAttempt),
            lockedAttempt.status(),
            true
        );
    }

    private void requireAssignedRefreshLookupAttempt(TestAttempt attempt, Long assignmentTestId) {
        if (attempt.attemptMode() != AttemptMode.ASSIGNED) {
            throw new ConflictException("Assigned refresh requires ASSIGNED attempt mode: " + attempt.id());
        }
        if (!Objects.equals(attempt.assignmentTestId(), assignmentTestId)) {
            throw new ConflictException("Assigned refresh requires matching assignmentTestId: " + attempt.id());
        }
    }

    private TestAttemptStatus recalculateStatus(TestAttempt attempt, Instant effectiveAt) {
        if (attempt.status() == TestAttemptStatus.COMPLETED
            || attempt.status() == TestAttemptStatus.EXPIRED
            || attempt.status() == TestAttemptStatus.ABANDONED) {
            return attempt.status();
        }
        if (attempt.attemptMode() == AttemptMode.SELF) {
            requireConsistentSelfAttempt(attempt);
            return attempt.status();
        }
        if (attempt.attemptMode() == AttemptMode.ASSIGNED) {
            return recalculateAssignedStatus(attempt, effectiveAt);
        }
        throw new ConflictException("Attempt status recalculation encountered unsupported mode: " + attempt.attemptMode());
    }

    private TestAttemptStatus recalculateAssignedStatus(TestAttempt attempt, Instant effectiveAt) {
        AssignmentTest assignmentTest = assignmentTestRepository.findAssignmentTestById(requireAssignmentTestId(attempt));
        Assignment assignment = assignmentRepository.findAssignmentById(assignmentTest.assignmentId());
        requireConsistentAssignedAttempt(attempt, assignment, assignmentTest);

        if (assignment.cancelledAt() != null
            || assignment.closedAt() != null
            || assignment.deadlineAt().isBefore(effectiveAt)
            || assignmentTest.isClosed()) {
            return TestAttemptStatus.EXPIRED;
        }
        return attempt.status();
    }

    private void requireConsistentSelfAttempt(TestAttempt attempt) {
        if (attempt.assignmentTestId() != null) {
            throw new ConflictException("Self attempt must not carry assignment anchor in status recalculation: " + attempt.id());
        }
    }

    private void requireConsistentAssignedAttempt(TestAttempt attempt, Assignment assignment, AssignmentTest assignmentTest) {
        if (attempt.assignmentTestId() == null) {
            throw new ConflictException("Assigned attempt is missing assignment anchor in status recalculation: " + attempt.id());
        }
        if (!Objects.equals(assignment.userId(), attempt.userId())) {
            throw new ConflictException("Assigned attempt user does not match assignment owner: " + attempt.id());
        }
        if (!Objects.equals(assignmentTest.id(), attempt.assignmentTestId())) {
            throw new ConflictException("Assigned attempt anchor does not match assignment_test: " + attempt.id());
        }
        if (!Objects.equals(assignmentTest.testId(), attempt.testId())) {
            throw new ConflictException("Assigned attempt test does not match assignment_test: " + attempt.id());
        }
    }

    private Long requireAssignmentTestId(TestAttempt attempt) {
        if (attempt.assignmentTestId() == null) {
            throw new ConflictException("Assigned attempt is missing assignmentTestId: " + attempt.id());
        }
        return attempt.assignmentTestId();
    }
}
