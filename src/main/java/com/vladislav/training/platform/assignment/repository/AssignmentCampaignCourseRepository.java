package com.vladislav.training.platform.assignment.repository;

import com.vladislav.training.platform.assignment.domain.AssignmentCampaignCourse;
import java.util.List;

/**
 * Контракт репозитория {@code AssignmentCampaignCourseRepository}.
 */
public interface AssignmentCampaignCourseRepository {

    AssignmentCampaignCourse findAssignmentCampaignCourseById(Long assignmentCampaignCourseId);

    List<AssignmentCampaignCourse> findAssignmentCampaignCoursesByCampaignId(Long assignmentCampaignId);

    AssignmentCampaignCourse saveAssignmentCampaignCourse(AssignmentCampaignCourse assignmentCampaignCourse);
}
