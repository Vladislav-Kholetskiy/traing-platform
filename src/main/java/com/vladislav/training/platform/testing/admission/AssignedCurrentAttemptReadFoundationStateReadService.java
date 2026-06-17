package com.vladislav.training.platform.testing.admission;

/**
 * Контракт сервиса {@code AssignedCurrentAttemptReadFoundationStateReadService}.
 */
public interface AssignedCurrentAttemptReadFoundationStateReadService {

    AssignedCurrentAttemptReadFoundationState findAssignedCurrentAttemptReadFoundationState(
        Long actorUserId,
        Long assignmentId,
        Long assignmentTestId
    );

    record AssignedCurrentAttemptReadFoundationState(
        Long assignmentId,
        Long assignmentTestId
    ) {
    }
}
