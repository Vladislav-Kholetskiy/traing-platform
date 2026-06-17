package com.vladislav.training.platform.assignment.repository;

import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentAdministrativeAction;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import java.util.List;

/**
 * Контракт репозитория {@code AssignmentReadRepository}.
 */
public interface AssignmentReadRepository {

    Assignment findAssignmentById(Long assignmentId);

    List<Assignment> findAllAssignments();

    List<Assignment> findAssignmentsByCampaignId(Long campaignId);

    List<Assignment> findAssignmentsByUserId(Long userId);

    List<Assignment> findAssignmentsByUserIdAndStatus(Long userId, AssignmentStatus status);

    Assignment findActiveAssignmentByUserIdAndCourseId(Long userId, Long courseId);

    AssignmentTest findAssignmentTestById(Long assignmentTestId);

    List<AssignmentTest> findAssignmentTestsByAssignmentId(Long assignmentId);

    AssignmentTest findAssignmentTestByCountedResultId(Long countedResultId);

    AssignmentAdministrativeAction findAssignmentAdministrativeActionById(Long assignmentAdministrativeActionId);

    List<AssignmentAdministrativeAction> findAssignmentAdministrativeActionsByAssignmentId(Long assignmentId);
}
