package com.vladislav.training.platform.assignment.repository;

import com.vladislav.training.platform.assignment.domain.AssignmentCampaign;
import java.util.List;

/**
 * Контракт репозитория {@code AssignmentCampaignRepository}.
 */
public interface AssignmentCampaignRepository {

    AssignmentCampaign findAssignmentCampaignById(Long assignmentCampaignId);

    List<AssignmentCampaign> findAllAssignmentCampaigns();

    List<AssignmentCampaign> findAssignmentCampaignsBySourceType(String sourceType);

    AssignmentCampaign saveAssignmentCampaign(AssignmentCampaign assignmentCampaign);
}
