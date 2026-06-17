package com.vladislav.training.platform.testing.admission;

/**
 * Контракт сервиса {@code AssignedAnswerMutationAdmissionFoundationStateReadService}.
 */
public interface AssignedAnswerMutationAdmissionFoundationStateReadService {

    AssignedAnswerMutationAdmissionFoundationState findAssignedAnswerMutationAdmissionFoundationState(
        Long actorUserId,
        Long testAttemptId
    );

    record AssignedAnswerMutationAdmissionFoundationState(
        Long testAttemptId,
        Long assignmentId,
        Long assignmentTestId
    ) {
    }
}
