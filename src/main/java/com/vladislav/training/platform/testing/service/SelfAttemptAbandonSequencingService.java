package com.vladislav.training.platform.testing.service;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.testing.admission.SelfAttemptTerminalAdmissionFoundationStateReadService;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Контракт сервиса {@code SelfAttemptAbandonSequencingService}.
 */
@Service
@Transactional
public class SelfAttemptAbandonSequencingService {

    private final SelfAttemptAbandonTerminalService selfAttemptAbandonTerminalService;
    private final CriticalCommandAuditSupport criticalCommandAuditSupport;
    private final CapabilityAdmissionPolicy capabilityAdmissionPolicy;
    private final CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;
    private final SelfAttemptTerminalAdmissionFoundationStateReadService foundationStateReadService;

    public SelfAttemptAbandonSequencingService(
        SelfAttemptAbandonTerminalService selfAttemptAbandonTerminalService,
        CriticalCommandAuditSupport criticalCommandAuditSupport,
        CapabilityAdmissionPolicy capabilityAdmissionPolicy,
        CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory,
        SelfAttemptTerminalAdmissionFoundationStateReadService foundationStateReadService
    ) {
        this.selfAttemptAbandonTerminalService = Objects.requireNonNull(
            selfAttemptAbandonTerminalService,
            "selfAttemptAbandonTerminalService must not be null"
        );
        this.criticalCommandAuditSupport = Objects.requireNonNull(
            criticalCommandAuditSupport,
            "criticalCommandAuditSupport must not be null"
        );
        this.capabilityAdmissionPolicy = Objects.requireNonNull(
            capabilityAdmissionPolicy,
            "capabilityAdmissionPolicy must not be null"
        );
        this.capabilityAdmissionRequestFactory = Objects.requireNonNull(
            capabilityAdmissionRequestFactory,
            "capabilityAdmissionRequestFactory must not be null"
        );
        this.foundationStateReadService = Objects.requireNonNull(
            foundationStateReadService,
            "foundationStateReadService must not be null"
        );
    }

    public Long abandonSelfAttempt(Long testAttemptId) {
        Long actorUserId = criticalCommandAuditSupport.resolveInteractiveActorUserId();
        var foundationState = requireFoundation(actorUserId, testAttemptId);
        capabilityAdmissionPolicy.check(capabilityAdmissionRequestFactory.createSelfAttemptAbandon(
            actorUserId,
            foundationState.testId()
        ));
        AttemptTerminalizationOutcome terminalizationOutcome =
            selfAttemptAbandonTerminalService.abandonSelfAttempt(actorUserId, testAttemptId);
        requireAbandonOutcome(terminalizationOutcome);
        return terminalizationOutcome.attemptId();
    }

    private SelfAttemptTerminalAdmissionFoundationStateReadService.SelfAttemptTerminalAdmissionFoundationState
    requireFoundation(Long actorUserId, Long testAttemptId) {
        var foundationState = foundationStateReadService.findSelfAttemptTerminalAdmissionFoundationState(
            actorUserId,
            testAttemptId
        );
        if (foundationState == null) {
            throw new NotFoundException("Self abandon foundation not found: testAttemptId=" + testAttemptId);
        }
        return foundationState;
    }

    private void requireAbandonOutcome(AttemptTerminalizationOutcome terminalizationOutcome) {
        if (terminalizationOutcome.terminalStatus() != TestAttemptStatus.ABANDONED) {
            throw new IllegalStateException(
                "Self abandon sequencing requires ABANDONED terminal status: attemptId="
                    + terminalizationOutcome.attemptId()
                    + ", status="
                    + terminalizationOutcome.terminalStatus()
            );
        }
        if (terminalizationOutcome.resultRecordable()) {
            throw new IllegalStateException(
                "Self abandon sequencing requires non-recordable terminalization outcome: attemptId="
                    + terminalizationOutcome.attemptId()
            );
        }
    }
}
