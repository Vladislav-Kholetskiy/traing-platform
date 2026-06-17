package com.vladislav.training.platform.testing.service;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import org.springframework.stereotype.Component;

/**
 * Вспомогательный тип {@code SelfAttemptAdmissionSupport}.
 */
@Component
class SelfAttemptAdmissionSupport {

    private final CapabilityAdmissionPolicy capabilityAdmissionPolicy;
    private final CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;

    SelfAttemptAdmissionSupport(
        CapabilityAdmissionPolicy capabilityAdmissionPolicy,
        CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory
    ) {
        this.capabilityAdmissionPolicy = capabilityAdmissionPolicy;
        this.capabilityAdmissionRequestFactory = capabilityAdmissionRequestFactory;
    }

    CapabilityAdmissionRequest checkSelfAttemptStart(Long testId) {
        CapabilityAdmissionRequest request = capabilityAdmissionRequestFactory.createSelfAttemptStart(testId);
        capabilityAdmissionPolicy.check(request);
        return request;
    }

    CapabilityAdmissionRequest checkSelfAttemptContinue(Long testId) {
        CapabilityAdmissionRequest request = capabilityAdmissionRequestFactory.createSelfAttemptContinue(testId);
        capabilityAdmissionPolicy.check(request);
        return request;
    }
}
