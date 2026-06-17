package com.vladislav.training.platform.testing.service;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import org.springframework.stereotype.Component;

/**
 * Вспомогательный тип {@code SelfAttemptAnswerMutationAdmissionSupport}.
 */
@Component
class SelfAttemptAnswerMutationAdmissionSupport {

    private final CapabilityAdmissionPolicy capabilityAdmissionPolicy;
    private final CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;

    SelfAttemptAnswerMutationAdmissionSupport(
        CapabilityAdmissionPolicy capabilityAdmissionPolicy,
        CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory
    ) {
        this.capabilityAdmissionPolicy = capabilityAdmissionPolicy;
        this.capabilityAdmissionRequestFactory = capabilityAdmissionRequestFactory;
    }

    CapabilityAdmissionRequest checkSelfAnswerMutation(Long actorUserId, Long testId) {
        CapabilityAdmissionRequest request = capabilityAdmissionRequestFactory.createSelfAnswerMutation(actorUserId, testId);
        capabilityAdmissionPolicy.check(request);
        return request;
    }
}
