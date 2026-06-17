package com.vladislav.training.platform.testing.admission;

/**
 * Контракт сервиса {@code SelfExecutionAdmissionFoundationStateReadService}.
 */
public interface SelfExecutionAdmissionFoundationStateReadService {

    SelfExecutionAdmissionFoundationState findSelfExecutionAdmissionFoundationState(Long actorUserId, Long testId);

        record SelfExecutionAdmissionFoundationState(Long testId) {
    }
}
