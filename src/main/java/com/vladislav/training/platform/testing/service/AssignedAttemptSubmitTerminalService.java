package com.vladislav.training.platform.testing.service;

import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.exception.ConflictException;
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
 * Контракт сервиса {@code AssignedAttemptSubmitTerminalService}.
 */
@Service
@Transactional
public class AssignedAttemptSubmitTerminalService {

    private final AttemptStatusRecalculationService attemptStatusRecalculationService;
    private final CriticalCommandAuditSupport criticalCommandAuditSupport;
    private final TestAttemptRepository testAttemptRepository;
    private final UtcClock utcClock;
    private final AttemptTerminalCriticalAuditPayloadFactory auditPayloadFactory =
        new AttemptTerminalCriticalAuditPayloadFactory();

    public AssignedAttemptSubmitTerminalService(
        AttemptStatusRecalculationService attemptStatusRecalculationService,
        CriticalCommandAuditSupport criticalCommandAuditSupport,
        TestAttemptRepository testAttemptRepository,
        UtcClock utcClock
    ) {
        this.attemptStatusRecalculationService = Objects.requireNonNull(
            attemptStatusRecalculationService,
            "attemptStatusRecalculationService must not be null"
        );
        this.criticalCommandAuditSupport = Objects.requireNonNull(
            criticalCommandAuditSupport,
            "criticalCommandAuditSupport must not be null"
        );
        this.testAttemptRepository = Objects.requireNonNull(testAttemptRepository, "testAttemptRepository must not be null");
        this.utcClock = Objects.requireNonNull(utcClock, "utcClock must not be null");
    }

    public AttemptTerminalizationOutcome submitAssignedAttempt(Long actorUserId, Long assignmentTestId, Long testAttemptId) {
        Instant now = utcClock.now();
        AttemptStatusRecalculationService.AttemptStatusRefreshResult refreshResult =
            attemptStatusRecalculationService.refreshAssignedAttemptStatusCacheWithVerdict(
                actorUserId,
                assignmentTestId,
                testAttemptId,
                now
            );
        TestAttempt activeAttempt = refreshResult.refreshedAttempt();
        if (activeAttempt.status() == TestAttemptStatus.EXPIRED) {
            if (refreshResult.expiredByThisRefresh()) {
                recordExpiredSubmitAudit(
                    actorUserId,
                    preRefreshAttempt(activeAttempt, refreshResult.previousStatus()),
                    activeAttempt
                );
                return AttemptTerminalizationOutcome.expiredByRefresh(actorUserId, activeAttempt);
            }
            throw new ConflictException(
                "Assigned submit is not allowed for already expired attempt: attemptId="
                    + activeAttempt.id()
                    + ", status="
                    + activeAttempt.status()
            );
        }
        requireSubmittableAssignedAttempt(activeAttempt, actorUserId);

        TestAttempt completedAttempt = testAttemptRepository.saveTestAttempt(new TestAttempt(
            activeAttempt.id(),
            activeAttempt.userId(),
            activeAttempt.testId(),
            activeAttempt.assignmentTestId(),
            activeAttempt.attemptMode(),
            TestAttemptStatus.COMPLETED,
            activeAttempt.startedAt(),
            now,
            null,
            null,
            now,
            activeAttempt.createdAt(),
            now
        ));
        recordSubmitAudit(actorUserId, activeAttempt, completedAttempt);
        return AttemptTerminalizationOutcome.assignedNormalSubmit(actorUserId, completedAttempt);
    }

    private void requireSubmittableAssignedAttempt(TestAttempt attempt, Long actorUserId) {
        if (attempt.attemptMode() != AttemptMode.ASSIGNED || attempt.assignmentTestId() == null) {
            throw new ConflictException("Assigned submit is not allowed for non-assigned attempt: " + attempt.id());
        }
        if (!Objects.equals(attempt.userId(), actorUserId)) {
            throw new ConflictException("Assigned submit is not allowed for foreign attempt: " + attempt.id());
        }
        if (attempt.status() != TestAttemptStatus.STARTED && attempt.status() != TestAttemptStatus.IN_PROGRESS) {
            throw new ConflictException(
                "Assigned submit is not allowed for non-active attempt: attemptId="
                    + attempt.id()
                    + ", status="
                    + attempt.status()
            );
        }
    }

    private void recordSubmitAudit(Long actorUserId, TestAttempt before, TestAttempt after) {
        AttemptTerminalCriticalAuditCatalog auditCatalog = AttemptTerminalCriticalAuditCatalog.ASSIGNED_ATTEMPT_SUBMITTED;
        criticalCommandAuditSupport.recordAudit(
            actorUserId,
            auditCatalog.auditEventType(),
            auditCatalog.auditEntityType(),
            after.id(),
            auditPayloadFactory.payloadBefore(before),
            auditPayloadFactory.payloadAfter(after),
            criticalCommandAuditSupport.buildAuditContext(
                "Testing",
                auditCatalog.operationCode(),
                auditPayloadFactory.createAssignedSubmitDetails(after.assignmentTestId(), after.testId())
            )
        );
    }

    private void recordExpiredSubmitAudit(Long actorUserId, TestAttempt before, TestAttempt after) {
        AttemptTerminalCriticalAuditCatalog auditCatalog = AttemptTerminalCriticalAuditCatalog.ASSIGNED_ATTEMPT_EXPIRED;
        criticalCommandAuditSupport.recordAudit(
            actorUserId,
            auditCatalog.auditEventType(),
            auditCatalog.auditEntityType(),
            after.id(),
            auditPayloadFactory.payloadBefore(before),
            auditPayloadFactory.payloadAfter(after),
            criticalCommandAuditSupport.buildAuditContext(
                "Testing",
                auditCatalog.operationCode(),
                auditPayloadFactory.createAssignedSubmitExpiredDetails(after.assignmentTestId(), after.testId())
            )
        );
    }

    private TestAttempt preRefreshAttempt(TestAttempt expiredAttempt, TestAttemptStatus previousStatus) {
        return new TestAttempt(
            expiredAttempt.id(),
            expiredAttempt.userId(),
            expiredAttempt.testId(),
            expiredAttempt.assignmentTestId(),
            expiredAttempt.attemptMode(),
            previousStatus,
            expiredAttempt.startedAt(),
            null,
            null,
            null,
            expiredAttempt.lastActivityAt(),
            expiredAttempt.createdAt(),
            expiredAttempt.createdAt()
        );
    }
}
