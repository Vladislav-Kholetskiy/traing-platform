package com.vladislav.training.platform.assignment.repository;

import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import java.util.List;

/**
 * Контракт репозитория {@code AssignmentSelfScopedReadRepository}.
 */
public interface AssignmentSelfScopedReadRepository {

    List<Assignment> findSelfScopedAssignments(Long actorUserId);

    Assignment findSelfScopedAssignmentById(Long actorUserId, Long assignmentId);

    List<AssignmentTest> findSelfScopedAssignmentTestsByAssignmentId(Long actorUserId, Long assignmentId);
}
