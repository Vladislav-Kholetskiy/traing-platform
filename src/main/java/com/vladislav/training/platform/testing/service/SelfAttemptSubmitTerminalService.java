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
 * Контракт сервиса {@code SelfAttemptSubmitTerminalService}.
 */
@Service
@Transactional
public class SelfAttemptSubmitTerminalService {

    private final CriticalCommandAuditSupport criticalCommandAuditSupport;
    private final TestAttemptRepository testAttemptRepository;
    private final UtcClock utcClock;
    private final AttemptTerminalCriticalAuditPayloadFactory auditPayloadFactory =
        new AttemptTerminalCriticalAuditPayloadFactory();

    public SelfAttemptSubmitTerminalService(
        CriticalCommandAuditSupport criticalCommandAuditSupport,
        TestAttemptRepository testAttemptRepository,
        UtcClock utcClock
    ) {
        this.criticalCommandAuditSupport = Objects.requireNonNull(
            criticalCommandAuditSupport,
            "criticalCommandAuditSupport must not be null"
        );
        this.testAttemptRepository = Objects.requireNonNull(testAttemptRepository, "testAttemptRepository must not be null");
        this.utcClock = Objects.requireNonNull(utcClock, "utcClock must not be null");
    }

    public AttemptTerminalizationOutcome submitSelfAttempt(Long actorUserId, Long testAttemptId) {
        TestAttempt activeAttempt = testAttemptRepository.findAndLockTestAttemptByIdAndUserId(testAttemptId, actorUserId);
        requireSubmittableSelfAttempt(activeAttempt, actorUserId);

        Instant now = utcClock.now();
        TestAttempt completedAttempt = testAttemptRepository.saveTestAttempt(new TestAttempt(
            activeAttempt.id(),
            activeAttempt.userId(),
            activeAttempt.testId(),
            null,
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
        return AttemptTerminalizationOutcome.selfNormalSubmit(actorUserId, completedAttempt);
    }

    private void requireSubmittableSelfAttempt(TestAttempt attempt, Long actorUserId) {
        if (attempt.attemptMode() != AttemptMode.SELF || attempt.assignmentTestId() != null) {
            throw new ConflictException("Self submit is not allowed for non-self attempt: " + attempt.id());
        }
        if (!Objects.equals(attempt.userId(), actorUserId)) {
            throw new ConflictException("Self submit is not allowed for foreign attempt: " + attempt.id());
        }
        if (attempt.status() != TestAttemptStatus.STARTED && attempt.status() != TestAttemptStatus.IN_PROGRESS) {
            throw new ConflictException(
                "Self submit is not allowed for non-active attempt: attemptId="
                    + attempt.id()
                    + ", status="
                    + attempt.status()
            );
        }
    }

    private void recordSubmitAudit(Long actorUserId, TestAttempt before, TestAttempt after) {
        AttemptTerminalCriticalAuditCatalog auditCatalog = AttemptTerminalCriticalAuditCatalog.SELF_ATTEMPT_SUBMITTED;
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
                auditPayloadFactory.createSelfSubmitDetails(after.testId())
            )
        );
    }
}
