package com.vladislav.training.platform.integration.personnel.service;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import java.util.Objects;
import org.springframework.stereotype.Service;
/**
 * Контракт сервиса {@code PersonnelImportAdmissionService}.
 */

@Service
public class PersonnelImportAdmissionService {

    private final CapabilityAdmissionPolicy capabilityAdmissionPolicy;

    public PersonnelImportAdmissionService(CapabilityAdmissionPolicy capabilityAdmissionPolicy) {
        this.capabilityAdmissionPolicy = Objects.requireNonNull(
            capabilityAdmissionPolicy,
            "capabilityAdmissionPolicy must not be null"
        );
    }

    public void checkDryRunAdmission(CapabilityAdmissionRequest request) {
        capabilityAdmissionPolicy.check(request);
    }

    public void checkApplyAdmission(CapabilityAdmissionRequest request) {
        capabilityAdmissionPolicy.check(request);
    }
}
