package com.vladislav.training.platform.testing.service;

import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.testing.admission.SelfAttemptEntryFoundationStateReadService;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import com.vladislav.training.platform.testing.repository.TestAttemptRepository;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Контракт сервиса {@code SelfAttemptEntryService}.
 */
@Service
@Transactional
public class SelfAttemptEntryService {

    private final SelfAttemptEntryFoundationStateReadService foundationStateReadService;
    private final SelfAttemptAdmissionSupport selfAttemptAdmissionSupport;
    private final TestAttemptRepository testAttemptRepository;
    private final CriticalCommandAuditSupport criticalCommandAuditSupport;
    private final UtcClock utcClock;
    private final SelfAttemptEntryCriticalAuditPayloadFactory auditPayloadFactory =
        new SelfAttemptEntryCriticalAuditPayloadFactory();

    public SelfAttemptEntryService(
        SelfAttemptEntryFoundationStateReadService foundationStateReadService,
        SelfAttemptAdmissionSupport selfAttemptAdmissionSupport,
        TestAttemptRepository testAttemptRepository,
        CriticalCommandAuditSupport criticalCommandAuditSupport,
        UtcClock utcClock
    ) {
        this.foundationStateReadService = Objects.requireNonNull(
            foundationStateReadService,
            "foundationStateReadService must not be null"
        );
        this.selfAttemptAdmissionSupport = Objects.requireNonNull(
            selfAttemptAdmissionSupport,
            "selfAttemptAdmissionSupport must not be null"
        );
        this.testAttemptRepository = Objects.requireNonNull(testAttemptRepository, "testAttemptRepository must not be null");
        this.criticalCommandAuditSupport = Objects.requireNonNull(
            criticalCommandAuditSupport,
            "criticalCommandAuditSupport must not be null"
        );
        this.utcClock = Objects.requireNonNull(utcClock, "utcClock must not be null");
    }

    public TestAttempt startOrContinueSelfAttempt(Long testId) {
        Long actorUserId = criticalCommandAuditSupport.resolveInteractiveActorUserId();
        SelfAttemptEntryFoundationStateReadService.SelfAttemptEntryFoundationState foundationState =
            requireSelfFoundation(actorUserId, testId);

        TestAttempt existingActiveAttempt = testAttemptRepository.findAndLockActiveSelfAttempt(actorUserId, testId);
        if (existingActiveAttempt != null) {
            TestAttempt consistentAttempt = requireConsistentSelfAttempt(existingActiveAttempt, actorUserId, foundationState.testId());
            selfAttemptAdmissionSupport.checkSelfAttemptContinue(testId);
            return consistentAttempt;
        }

        selfAttemptAdmissionSupport.checkSelfAttemptStart(testId);
        TestAttempt createdAttempt = testAttemptRepository.saveTestAttempt(
            newSelfAttempt(actorUserId, foundationState.testId(), utcClock.now())
        );
        recordCreateAudit(actorUserId, createdAttempt);
        return createdAttempt;
    }

    private SelfAttemptEntryFoundationStateReadService.SelfAttemptEntryFoundationState requireSelfFoundation(
        Long actorUserId,
        Long testId
    ) {
        var foundationState = foundationStateReadService.findSelfAttemptEntryFoundationState(actorUserId, testId);
        if (foundationState == null) {
            throw new NotFoundException("Self execution foundation not found: testId=" + testId);
        }
        return foundationState;
    }

    private TestAttempt requireConsistentSelfAttempt(TestAttempt activeAttempt, Long actorUserId, Long expectedTestId) {
        if (activeAttempt.attemptMode() != AttemptMode.SELF
            || activeAttempt.assignmentTestId() != null
            || !Objects.equals(activeAttempt.userId(), actorUserId)
            || !Objects.equals(activeAttempt.testId(), expectedTestId)) {
            throw new ConflictException(
                "Active self attempt is inconsistent with self anchor: testId=" + expectedTestId
            );
        }
        return activeAttempt;
    }

    private TestAttempt newSelfAttempt(Long actorUserId, Long testId, Instant now) {
        return new TestAttempt(
            null,
            actorUserId,
            testId,
            null,
            AttemptMode.SELF,
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

    private void recordCreateAudit(Long actorUserId, TestAttempt createdAttempt) {
        SelfAttemptEntryCriticalAuditCatalog auditCatalog = SelfAttemptEntryCriticalAuditCatalog.SELF_ATTEMPT_STARTED;
        criticalCommandAuditSupport.recordAudit(
            actorUserId,
            auditCatalog.auditEventType(),
            auditCatalog.auditEntityType(),
            createdAttempt.id(),
            null,
            auditPayloadFactory.payloadAfter(createdAttempt),
            criticalCommandAuditSupport.buildAuditContext(
                "Testing",
                auditCatalog.operationCode(),
                auditPayloadFactory.createDetails(createdAttempt.testId())
            )
        );
    }
}
