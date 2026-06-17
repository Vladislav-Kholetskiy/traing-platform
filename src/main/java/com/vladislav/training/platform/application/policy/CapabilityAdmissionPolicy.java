package com.vladislav.training.platform.application.policy;

/**
 * Интерфейс {@code CapabilityAdmissionPolicy}.
 */
public interface CapabilityAdmissionPolicy {

    void check(CapabilityAdmissionRequest request);
}
