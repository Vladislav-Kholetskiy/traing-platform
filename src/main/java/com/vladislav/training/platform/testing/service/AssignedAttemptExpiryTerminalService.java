package com.vladislav.training.platform.testing.service;

import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.audit.service.SystemActorResolver;
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
 * Контракт сервиса {@code AssignedAttemptExpiryTerminalService}.
 */
@Service
@Transactional
public class AssignedAttemptExpiryTerminalService {

    private final CriticalCommandAuditSupport criticalCommandAuditSupport;
    private final SystemActorResolver systemActorResolver;
    private final TestAttemptRepository testAttemptRepository;
    private final UtcClock utcClock;
    private final AttemptTerminalCriticalAuditPayloadFactory auditPayloadFactory =
        new AttemptTerminalCriticalAuditPayloadFactory();

    public AssignedAttemptExpiryTerminalService(
        CriticalCommandAuditSupport criticalCommandAuditSupport,
        SystemActorResolver systemActorResolver,
        TestAttemptRepository testAttemptRepository,
        UtcClock utcClock
    ) {
        this.criticalCommandAuditSupport = Objects.requireNonNull(
            criticalCommandAuditSupport,
            "criticalCommandAuditSupport must not be null"
        );
        this.systemActorResolver = Objects.requireNonNull(systemActorResolver, "systemActorResolver must not be null");
        this.testAttemptRepository = Objects.requireNonNull(testAttemptRepository, "testAttemptRepository must not be null");
        this.utcClock = Objects.requireNonNull(utcClock, "utcClock must not be null");
    }

    public TestAttempt expireAssignedAttempt(Long testAttemptId) {
        TestAttempt activeAttempt = testAttemptRepository.findAndLockTestAttemptById(testAttemptId);
        requireExpirableAssignedAttempt(activeAttempt);

        Instant now = utcClock.now();
        Long actorUserId = criticalCommandAuditSupport.resolveSystemActorUserId(systemActorResolver);
        TestAttempt expiredAttempt = testAttemptRepository.saveTestAttempt(new TestAttempt(
            activeAttempt.id(),
            activeAttempt.userId(),
            activeAttempt.testId(),
            activeAttempt.assignmentTestId(),
            activeAttempt.attemptMode(),
            TestAttemptStatus.EXPIRED,
            activeAttempt.startedAt(),
            null,
            now,
            null,
            activeAttempt.lastActivityAt(),
            activeAttempt.createdAt(),
            now
        ));
        recordExpiryAudit(actorUserId, activeAttempt, expiredAttempt);
        return AttemptTerminalizationOutcome.assignedExplicitExpiry(actorUserId, expiredAttempt).terminalizedAttempt();
    }

    private void requireExpirableAssignedAttempt(TestAttempt attempt) {
        if (attempt.attemptMode() != AttemptMode.ASSIGNED || attempt.assignmentTestId() == null) {
            throw new ConflictException("Assigned expiry is not allowed for non-assigned attempt: " + attempt.id());
        }
        if (attempt.status() != TestAttemptStatus.STARTED && attempt.status() != TestAttemptStatus.IN_PROGRESS) {
            throw new ConflictException(
                "Assigned expiry is not allowed for non-active attempt: attemptId="
                    + attempt.id()
                    + ", status="
                    + attempt.status()
            );
        }
    }

    private void recordExpiryAudit(Long actorUserId, TestAttempt before, TestAttempt after) {
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
                auditPayloadFactory.createAssignedExpiryDetails(after.assignmentTestId(), after.testId())
            )
        );
    }
}
