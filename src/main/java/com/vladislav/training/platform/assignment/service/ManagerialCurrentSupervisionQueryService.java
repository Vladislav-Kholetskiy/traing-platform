package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Контракт сервиса чтения {@code ManagerialCurrentSupervisionQueryService}.
 */
public interface ManagerialCurrentSupervisionQueryService {

    List<ManagerialCurrentSupervisionRow> findCurrentSupervision(ManagerialCurrentSupervisionQuery query);

    /**
     * Запрос на чтение управленческого списка в заданный момент времени.
     */
    record ManagerialCurrentSupervisionQuery(
        Long actorUserId,
        Instant effectiveAt
    ) {

        public ManagerialCurrentSupervisionQuery {
            Objects.requireNonNull(actorUserId, "actorUserId must not be null");
            Objects.requireNonNull(effectiveAt, "effectiveAt must not be null");
        }
    }

    /**
     * Строка управленческого представления по одному назначению.
     */
    record ManagerialCurrentSupervisionRow(
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

        public ManagerialCurrentSupervisionRow {
            Objects.requireNonNull(assignmentId, "assignmentId must not be null");
            Objects.requireNonNull(userId, "userId must not be null");
            Objects.requireNonNull(userDisplayName, "userDisplayName must not be null");
            Objects.requireNonNull(courseId, "courseId must not be null");
            Objects.requireNonNull(courseName, "courseName must not be null");
            Objects.requireNonNull(assignmentTestCount, "assignmentTestCount must not be null");
            Objects.requireNonNull(assignedAt, "assignedAt must not be null");
            Objects.requireNonNull(assignmentStatus, "assignmentStatus must not be null");
        }
    }
}
