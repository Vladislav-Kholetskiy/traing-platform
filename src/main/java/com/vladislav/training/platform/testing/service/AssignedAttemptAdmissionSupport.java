package com.vladislav.training.platform.testing.service;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import org.springframework.stereotype.Component;

/**
 * Вспомогательный тип {@code AssignedAttemptAdmissionSupport}.
 */
@Component
class AssignedAttemptAdmissionSupport {

    private final CapabilityAdmissionPolicy capabilityAdmissionPolicy;
    private final CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;

    AssignedAttemptAdmissionSupport(
        CapabilityAdmissionPolicy capabilityAdmissionPolicy,
        CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory
    ) {
        this.capabilityAdmissionPolicy = capabilityAdmissionPolicy;
        this.capabilityAdmissionRequestFactory = capabilityAdmissionRequestFactory;
    }

    CapabilityAdmissionRequest checkAssignedAttemptStart(Long assignmentId, Long assignmentTestId) {
        CapabilityAdmissionRequest request = capabilityAdmissionRequestFactory.createAssignedAttemptStart(
            assignmentId,
            assignmentTestId
        );
        capabilityAdmissionPolicy.check(request);
        return request;
    }

    CapabilityAdmissionRequest checkAssignedAttemptContinue(Long assignmentId, Long assignmentTestId) {
        CapabilityAdmissionRequest request = capabilityAdmissionRequestFactory.createAssignedAttemptContinue(
            assignmentId,
            assignmentTestId
        );
        capabilityAdmissionPolicy.check(request);
        return request;
    }
}
