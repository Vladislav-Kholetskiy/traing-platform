package com.vladislav.training.platform.testing.admission;

/**
 * Контракт сервиса {@code AssignedAttemptSubmitAdmissionFoundationStateReadService}.
 */
public interface AssignedAttemptSubmitAdmissionFoundationStateReadService {

    AssignedAttemptSubmitAdmissionFoundationState findAssignedAttemptSubmitAdmissionFoundationState(
        Long actorUserId,
        Long testAttemptId
    );

    record AssignedAttemptSubmitAdmissionFoundationState(
        Long testAttemptId,
        Long assignmentId,
        Long assignmentTestId
    ) {
    }
}
