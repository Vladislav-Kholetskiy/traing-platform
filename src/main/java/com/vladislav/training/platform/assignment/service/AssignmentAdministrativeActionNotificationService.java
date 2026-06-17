package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.assignment.domain.Assignment;
import java.time.Instant;

interface AssignmentAdministrativeActionNotificationService {

    void createCancellationNotifications(
        Assignment assignmentBefore,
        Assignment assignmentAfter,
        Instant occurredAt
    );

    void createDeadlineExtendedNotifications(
        Assignment assignmentBefore,
        Assignment assignmentAfter,
        Instant occurredAt
    );

    void createReplacementNotifications(
        Assignment assignmentBefore,
        Assignment cancelledAssignmentAfter,
        Assignment replacementAssignmentAfter,
        Instant occurredAt
    );
}
