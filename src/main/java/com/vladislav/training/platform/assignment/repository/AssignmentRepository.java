package com.vladislav.training.platform.assignment.repository;

import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import java.util.List;

/**
 * Контракт репозитория {@code AssignmentRepository}.
 */
public interface AssignmentRepository {

    Assignment findAssignmentById(Long assignmentId);

    List<Assignment> findAllAssignments();

    List<Assignment> findAssignmentsByCampaignId(Long campaignId);

    List<Assignment> findAssignmentsByUserId(Long userId);

    List<Assignment> findAssignmentsByUserIdAndStatus(Long userId, AssignmentStatus status);

    Assignment findActiveAssignmentByUserIdAndCourseId(Long userId, Long courseId);

    Assignment saveAssignment(Assignment assignment);
}
