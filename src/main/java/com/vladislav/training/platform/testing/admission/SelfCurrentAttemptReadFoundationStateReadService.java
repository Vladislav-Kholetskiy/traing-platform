package com.vladislav.training.platform.testing.admission;

/**
 * Контракт сервиса {@code SelfCurrentAttemptReadFoundationStateReadService}.
 */
public interface SelfCurrentAttemptReadFoundationStateReadService {

    SelfCurrentAttemptReadFoundationState findSelfCurrentAttemptReadFoundationState(Long actorUserId, Long testId);

    record SelfCurrentAttemptReadFoundationState(Long testId) {
    }
}
