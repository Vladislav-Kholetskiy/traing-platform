package com.vladislav.training.platform.testing.service;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import org.springframework.stereotype.Component;

/**
 * Вспомогательный тип {@code AssignedAttemptAnswerMutationAdmissionSupport}.
 */
@Component
class AssignedAttemptAnswerMutationAdmissionSupport {

    private final CapabilityAdmissionPolicy capabilityAdmissionPolicy;
    private final CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;

    AssignedAttemptAnswerMutationAdmissionSupport(
        CapabilityAdmissionPolicy capabilityAdmissionPolicy,
        CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory
    ) {
        this.capabilityAdmissionPolicy = capabilityAdmissionPolicy;
        this.capabilityAdmissionRequestFactory = capabilityAdmissionRequestFactory;
    }

    CapabilityAdmissionRequest checkAssignedAnswerMutation(Long actorUserId, Long assignmentId, Long assignmentTestId) {
        CapabilityAdmissionRequest request = capabilityAdmissionRequestFactory.createAssignedAnswerMutation(
            actorUserId,
            assignmentId,
            assignmentTestId
        );
        capabilityAdmissionPolicy.check(request);
        return request;
    }
}
