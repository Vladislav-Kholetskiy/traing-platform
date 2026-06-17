package com.vladislav.training.platform.assignment.repository;

import com.vladislav.training.platform.access.service.ManagerialReadScope;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Контракт репозитория {@code ManagerialCurrentSupervisionReadRepository}.
 */
public interface ManagerialCurrentSupervisionReadRepository {

    List<ManagerialCurrentSupervisionReadRow> findCurrentSupervisionRows(ManagerialCurrentSupervisionReadCriteria criteria);

    /**
     * Критерии выборки для управленческого чтения.
     */
    record ManagerialCurrentSupervisionReadCriteria(
        ManagerialReadScope managerialReadScope
    ) {

        public ManagerialCurrentSupervisionReadCriteria {
            Objects.requireNonNull(managerialReadScope, "managerialReadScope must not be null");
        }
    }

    /**
     * Строка результата для управленческого списка назначений.
     */
    record ManagerialCurrentSupervisionReadRow(
        Long assignmentId,
        Long userId,
        String userDisplayName,
        Long courseId,
        String courseName,
        Long assignmentTestCount,
        Instant assignedAt,
        Instant deadlineAt,
        AssignmentStatus assignmentStatus
    ) {

        public ManagerialCurrentSupervisionReadRow {
            Objects.requireNonNull(assignmentId, "assignmentId must not be null");
            Objects.requireNonNull(userId, "userId must not be null");
            Objects.requireNonNull(userDisplayName, "userDisplayName must not be null");
            Objects.requireNonNull(courseId, "courseId must not be null");
            Objects.requireNonNull(courseName, "courseName must not be null");
            Objects.requireNonNull(assignmentTestCount, "assignmentTestCount must not be null");
            Objects.requireNonNull(assignedAt, "assignedAt must not be null");
            Objects.requireNonNull(deadlineAt, "deadlineAt must not be null");
            Objects.requireNonNull(assignmentStatus, "assignmentStatus must not be null");
        }
    }
}
