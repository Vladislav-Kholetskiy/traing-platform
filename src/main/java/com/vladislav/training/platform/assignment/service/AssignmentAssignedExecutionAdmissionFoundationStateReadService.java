package com.vladislav.training.platform.assignment.service;

import java.time.Instant;

public interface AssignmentAssignedExecutionAdmissionFoundationStateReadService {

    AssignmentAssignedExecutionAdmissionFoundationState findAssignmentAssignedExecutionAdmissionFoundationState(
        Long actorUserId,
        Long assignmentId,
        Long assignmentTestId
    );

        record AssignmentAssignedExecutionAdmissionFoundationState(
        Long assignmentId,
        Long assignmentTestId,
        Long testId,
        Instant deadlineAt,
        boolean assignmentCancelled,
        boolean assignmentClosed,
        boolean assignmentTestClosed
    ) {
    }
}
