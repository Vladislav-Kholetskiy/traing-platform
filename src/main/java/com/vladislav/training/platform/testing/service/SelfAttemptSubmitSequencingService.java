package com.vladislav.training.platform.testing.service;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.testing.admission.SelfAttemptTerminalAdmissionFoundationStateReadService;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Контракт сервиса {@code SelfAttemptSubmitSequencingService}.
 */
@Service
@Transactional
public class SelfAttemptSubmitSequencingService {

    private final SelfAttemptSubmitTerminalService selfAttemptSubmitTerminalService;
    private final ResultRecordingService resultRecordingService;
    private final CriticalCommandAuditSupport criticalCommandAuditSupport;
    private final CapabilityAdmissionPolicy capabilityAdmissionPolicy;
    private final CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;
    private final SelfAttemptTerminalAdmissionFoundationStateReadService foundationStateReadService;

    public SelfAttemptSubmitSequencingService(
        SelfAttemptSubmitTerminalService selfAttemptSubmitTerminalService,
        ResultRecordingService resultRecordingService,
        CriticalCommandAuditSupport criticalCommandAuditSupport,
        CapabilityAdmissionPolicy capabilityAdmissionPolicy,
        CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory,
        SelfAttemptTerminalAdmissionFoundationStateReadService foundationStateReadService
    ) {
        this.selfAttemptSubmitTerminalService = Objects.requireNonNull(
            selfAttemptSubmitTerminalService,
            "selfAttemptSubmitTerminalService must not be null"
        );
        this.resultRecordingService = Objects.requireNonNull(
            resultRecordingService,
            "resultRecordingService must not be null"
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

    public Long submitSelfAttempt(Long testAttemptId) {
        Long actorUserId = criticalCommandAuditSupport.resolveInteractiveActorUserId();
        var foundationState = requireFoundation(actorUserId, testAttemptId);
        capabilityAdmissionPolicy.check(capabilityAdmissionRequestFactory.createSelfAttemptSubmit(
            actorUserId,
            foundationState.testId()
        ));
        AttemptTerminalizationOutcome terminalizationOutcome =
            selfAttemptSubmitTerminalService.submitSelfAttempt(actorUserId, testAttemptId);
        if (!terminalizationOutcome.resultRecordable()) {
            throw new IllegalStateException(
                "Self submit sequencing requires recordable terminalization outcome: attemptId="
                    + terminalizationOutcome.attemptId()
                    + ", status="
                    + terminalizationOutcome.terminalStatus()
                    + ", reason="
                    + terminalizationOutcome.reason()
            );
        }
        return resultRecordingService.recordResult(terminalizationOutcome.attemptId());
    }

    private SelfAttemptTerminalAdmissionFoundationStateReadService.SelfAttemptTerminalAdmissionFoundationState
    requireFoundation(Long actorUserId, Long testAttemptId) {
        var foundationState = foundationStateReadService.findSelfAttemptTerminalAdmissionFoundationState(
            actorUserId,
            testAttemptId
        );
        if (foundationState == null) {
            throw new NotFoundException("Self submit foundation not found: testAttemptId=" + testAttemptId);
        }
        return foundationState;
    }
}
