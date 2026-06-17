package com.vladislav.training.platform.testing.service;

import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.assignment.service.AssignmentAssignedExecutionAdmissionFoundationStateReadService;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import com.vladislav.training.platform.testing.repository.TestAttemptRepository;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Контракт сервиса {@code AssignedAttemptEntryService}.
 */
@Service
@Transactional
public class AssignedAttemptEntryService {

    private final AssignmentAssignedExecutionAdmissionFoundationStateReadService foundationStateReadService;
    private final AssignedAttemptAdmissionSupport assignedAttemptAdmissionSupport;
    private final TestAttemptRepository testAttemptRepository;
    private final CriticalCommandAuditSupport criticalCommandAuditSupport;
    private final UtcClock utcClock;
    private final AssignedAttemptEntryCriticalAuditPayloadFactory auditPayloadFactory =
        new AssignedAttemptEntryCriticalAuditPayloadFactory();

    public AssignedAttemptEntryService(
        AssignmentAssignedExecutionAdmissionFoundationStateReadService foundationStateReadService,
        AssignedAttemptAdmissionSupport assignedAttemptAdmissionSupport,
        TestAttemptRepository testAttemptRepository,
        CriticalCommandAuditSupport criticalCommandAuditSupport,
        UtcClock utcClock
    ) {
        this.foundationStateReadService = Objects.requireNonNull(
            foundationStateReadService,
            "foundationStateReadService must not be null"
        );
        this.assignedAttemptAdmissionSupport = Objects.requireNonNull(
            assignedAttemptAdmissionSupport,
            "assignedAttemptAdmissionSupport must not be null"
        );
        this.testAttemptRepository = Objects.requireNonNull(testAttemptRepository, "testAttemptRepository must not be null");
        this.criticalCommandAuditSupport = Objects.requireNonNull(
            criticalCommandAuditSupport,
            "criticalCommandAuditSupport must not be null"
        );
        this.utcClock = Objects.requireNonNull(utcClock, "utcClock must not be null");
    }

    public TestAttempt enterAssignedAttempt(Long assignmentId, Long assignmentTestId) {
        Long actorUserId = criticalCommandAuditSupport.resolveInteractiveActorUserId();
        AssignmentAssignedExecutionAdmissionFoundationStateReadService.AssignmentAssignedExecutionAdmissionFoundationState
            foundationState = requireAssignedFoundation(actorUserId, assignmentId, assignmentTestId);
        requireContinuationAllowed(foundationState, assignmentId, assignmentTestId);

        TestAttempt existingActiveAttempt = testAttemptRepository.findAndLockActiveAssignedAttemptForActor(
            actorUserId,
            assignmentTestId
        );
        if (existingActiveAttempt != null) {
            TestAttempt consistentAttempt = requireConsistentAssignedAttempt(
                existingActiveAttempt,
                actorUserId,
                assignmentTestId,
                foundationState.testId()
            );
            checkAssignedAttemptContinue(assignmentId, assignmentTestId);
            Instant effectiveAt = utcClock.now();
            requireDeadlineAllowed(foundationState, assignmentId, assignmentTestId, effectiveAt);
            return consistentAttempt;
        }
        assignedAttemptAdmissionSupport.checkAssignedAttemptStart(assignmentId, assignmentTestId);
        Instant effectiveAt = utcClock.now();
        requireDeadlineAllowed(foundationState, assignmentId, assignmentTestId, effectiveAt);
        TestAttempt createdAttempt = testAttemptRepository.saveTestAttempt(newAssignedAttempt(
            actorUserId,
            foundationState.testId(),
            assignmentTestId,
            effectiveAt
        ));
        recordCreateAudit(actorUserId, assignmentId, createdAttempt);
        return createdAttempt;
    }

    private void checkAssignedAttemptContinue(Long assignmentId, Long assignmentTestId) {
        assignedAttemptAdmissionSupport.checkAssignedAttemptContinue(assignmentId, assignmentTestId);
    }

    private AssignmentAssignedExecutionAdmissionFoundationStateReadService.AssignmentAssignedExecutionAdmissionFoundationState
    requireAssignedFoundation(Long actorUserId, Long assignmentId, Long assignmentTestId) {
        var foundationState = foundationStateReadService.findAssignmentAssignedExecutionAdmissionFoundationState(
            actorUserId,
            assignmentId,
            assignmentTestId
        );
        if (foundationState == null) {
            throw new NotFoundException(
                "Assigned execution foundation not found: assignmentId=" + assignmentId + ", assignmentTestId=" + assignmentTestId
            );
        }
        return foundationState;
    }

    private void requireContinuationAllowed(
        AssignmentAssignedExecutionAdmissionFoundationStateReadService.AssignmentAssignedExecutionAdmissionFoundationState foundationState,
        Long assignmentId,
        Long assignmentTestId
    ) {
        if (foundationState.assignmentCancelled()) {
            throw new ConflictException(
                "Assigned attempt continuation is not allowed for cancelled assignment: assignmentId=" + assignmentId
            );
        }
        if (foundationState.assignmentClosed()) {
            throw new ConflictException(
                "Assigned attempt continuation is not allowed for closed assignment: assignmentId=" + assignmentId
            );
        }
        if (foundationState.assignmentTestClosed()) {
            throw new ConflictException(
                "Assigned attempt continuation is not allowed for closed assignment_test: assignmentTestId=" + assignmentTestId
            );
        }
    }

    private void requireDeadlineAllowed(
        AssignmentAssignedExecutionAdmissionFoundationStateReadService.AssignmentAssignedExecutionAdmissionFoundationState foundationState,
        Long assignmentId,
        Long assignmentTestId,
        Instant now
    ) {
        if (now.isAfter(foundationState.deadlineAt())) {
            throw new ConflictException(
                "Assigned attempt continuation is not allowed after deadline: assignmentId=" + assignmentId
                    + ", assignmentTestId=" + assignmentTestId
            );
        }
    }

    private TestAttempt requireConsistentAssignedAttempt(
        TestAttempt activeAttempt,
        Long actorUserId,
        Long assignmentTestId,
        Long expectedTestId
    ) {
        if (activeAttempt.attemptMode() != AttemptMode.ASSIGNED
            || !Objects.equals(activeAttempt.userId(), actorUserId)
            || !Objects.equals(activeAttempt.assignmentTestId(), assignmentTestId)
            || !Objects.equals(activeAttempt.testId(), expectedTestId)) {
            throw new ConflictException(
                "Active assigned attempt is inconsistent with assignment anchor: assignmentTestId=" + assignmentTestId
            );
        }
        return activeAttempt;
    }

    private TestAttempt newAssignedAttempt(Long actorUserId, Long testId, Long assignmentTestId, Instant now) {
        return new TestAttempt(
            null,
            actorUserId,
            testId,
            assignmentTestId,
            AttemptMode.ASSIGNED,
            TestAttemptStatus.STARTED,
            now,
            null,
            null,
            null,
            now,
            now,
            now
        );
    }

    private void recordCreateAudit(Long actorUserId, Long assignmentId, TestAttempt createdAttempt) {
        AssignedAttemptEntryCriticalAuditCatalog auditCatalog =
            AssignedAttemptEntryCriticalAuditCatalog.ASSIGNED_ATTEMPT_STARTED;
        criticalCommandAuditSupport.recordAudit(
            actorUserId,
            auditCatalog.auditEventType(),
            auditCatalog.auditEntityType(),
            createdAttempt.id(),
            null,
            auditPayloadFactory.payloadAfter(createdAttempt, assignmentId),
            criticalCommandAuditSupport.buildAuditContext(
                "Testing",
                auditCatalog.operationCode(),
                auditPayloadFactory.createDetails(assignmentId, createdAttempt.assignmentTestId(), createdAttempt.testId())
            )
        );
    }
}
