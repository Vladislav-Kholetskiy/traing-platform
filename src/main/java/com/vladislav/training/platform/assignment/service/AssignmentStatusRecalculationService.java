package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import java.time.Instant;

/**
 * Контракт сервиса {@code AssignmentStatusRecalculationService}.
 */
public interface AssignmentStatusRecalculationService {

    AssignmentStatus recalculateAssignmentStatus(Long assignmentId, Instant effectiveAt);

    Assignment refreshAssignmentStatusCache(Long assignmentId, Instant effectiveAt);
}
