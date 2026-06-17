package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.assignment.domain.Assignment;
import java.util.List;

/**
 * Контракт сервиса чтения {@code AssignmentSelfScopedQueryService}.
 */
public interface AssignmentSelfScopedQueryService {

    List<Assignment> findSelfAssignments(Long actorUserId);

    Assignment findSelfAssignmentById(Long actorUserId, Long assignmentId);

    AssignedLearningContext findAssignedLearningContext(Long actorUserId, Long assignmentId);

    AssignedTestContext findAssignedTestContext(Long actorUserId, Long assignmentId, Long assignmentTestId);

    AssignedMaterialContent findAssignedMaterialContent(Long actorUserId, Long assignmentId, Long materialId);
}
