package com.vladislav.training.platform.testing.service;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.testing.admission.AssignedAttemptSubmitAdmissionFoundationStateReadService;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Контракт сервиса {@code AssignedAttemptSubmissionService}.
 */
@Service
@Transactional
public class AssignedAttemptSubmissionService {

    private final AssignedAttemptSubmitSequencingService assignedAttemptSubmitSequencingService;
    private final CriticalCommandAuditSupport criticalCommandAuditSupport;
    private final CapabilityAdmissionPolicy capabilityAdmissionPolicy;
    private final CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;
    private final AssignedAttemptSubmitAdmissionFoundationStateReadService foundationStateReadService;

    public AssignedAttemptSubmissionService(
        AssignedAttemptSubmitSequencingService assignedAttemptSubmitSequencingService,
        CriticalCommandAuditSupport criticalCommandAuditSupport,
        CapabilityAdmissionPolicy capabilityAdmissionPolicy,
        CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory,
        AssignedAttemptSubmitAdmissionFoundationStateReadService foundationStateReadService
    ) {
        this.assignedAttemptSubmitSequencingService = Objects.requireNonNull(
            assignedAttemptSubmitSequencingService,
            "assignedAttemptSubmitSequencingService must not be null"
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

    public AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome submitAssignedAttempt(Long testAttemptId) {
        Long actorUserId = criticalCommandAuditSupport.resolveInteractiveActorUserId();
        var foundationState = requireFoundation(actorUserId, testAttemptId);
        capabilityAdmissionPolicy.check(capabilityAdmissionRequestFactory.createAssignedAttemptSubmit(
            actorUserId,
            foundationState.assignmentId(),
            foundationState.assignmentTestId()
        ));
        return assignedAttemptSubmitSequencingService.submitAssignedAttempt(
            actorUserId,
            foundationState.assignmentTestId(),
            testAttemptId
        );
    }

    private AssignedAttemptSubmitAdmissionFoundationStateReadService.AssignedAttemptSubmitAdmissionFoundationState
    requireFoundation(Long actorUserId, Long testAttemptId) {
        var foundationState = foundationStateReadService.findAssignedAttemptSubmitAdmissionFoundationState(
            actorUserId,
            testAttemptId
        );
        if (foundationState == null) {
            throw new NotFoundException("Assigned submit foundation not found: testAttemptId=" + testAttemptId);
        }
        return foundationState;
    }
}
