package com.vladislav.training.platform.testing.admission;

/**
 * Контракт сервиса {@code SelfAttemptTerminalAdmissionFoundationStateReadService}.
 */
public interface SelfAttemptTerminalAdmissionFoundationStateReadService {

    SelfAttemptTerminalAdmissionFoundationState findSelfAttemptTerminalAdmissionFoundationState(
        Long actorUserId,
        Long testAttemptId
    );

    record SelfAttemptTerminalAdmissionFoundationState(
        Long testAttemptId,
        Long testId
    ) {
    }
}
