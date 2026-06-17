package com.vladislav.training.platform.testing.admission;

/**
 * Контракт сервиса {@code SelfAttemptEntryFoundationStateReadService}.
 */
public interface SelfAttemptEntryFoundationStateReadService {

    SelfAttemptEntryFoundationState findSelfAttemptEntryFoundationState(Long actorUserId, Long testId);

        record SelfAttemptEntryFoundationState(Long testId) {
    }
}
