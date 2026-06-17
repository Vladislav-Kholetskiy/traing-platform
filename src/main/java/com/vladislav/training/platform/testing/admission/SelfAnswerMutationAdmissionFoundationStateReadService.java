package com.vladislav.training.platform.testing.admission;

/**
 * Контракт сервиса {@code SelfAnswerMutationAdmissionFoundationStateReadService}.
 */
public interface SelfAnswerMutationAdmissionFoundationStateReadService {

    SelfAnswerMutationAdmissionFoundationState findSelfAnswerMutationAdmissionFoundationState(Long actorUserId, Long testAttemptId);

    record SelfAnswerMutationAdmissionFoundationState(
        Long testAttemptId,
        Long testId
    ) {
    }
}
