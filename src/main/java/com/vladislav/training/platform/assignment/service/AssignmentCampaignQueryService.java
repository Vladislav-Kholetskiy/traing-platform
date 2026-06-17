package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.assignment.domain.AssignmentCampaign;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaignCourse;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaignRecipientSnapshot;
import java.util.List;

/**
 * Контракт сервиса чтения {@code AssignmentCampaignQueryService}.
 */
public interface AssignmentCampaignQueryService {

    AssignmentCampaign findAssignmentCampaignById(Long assignmentCampaignId);

    List<AssignmentCampaign> findAllAssignmentCampaigns();

    List<AssignmentCampaign> findAssignmentCampaignsBySourceType(String sourceType);

    AssignmentCampaignCourse findAssignmentCampaignCourseById(Long assignmentCampaignCourseId);

    List<AssignmentCampaignCourse> findAssignmentCampaignCoursesByCampaignId(Long assignmentCampaignId);

    AssignmentCampaignRecipientSnapshot findAssignmentCampaignRecipientSnapshotById(Long recipientSnapshotId);

    List<AssignmentCampaignRecipientSnapshot> findAssignmentCampaignRecipientSnapshotsByCampaignId(Long assignmentCampaignId);

    List<AssignmentCampaignRecipientSnapshot> findAssignmentCampaignRecipientSnapshotsByUserId(Long userId);
}
