package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.assignment.domain.AssignmentCampaign;

/**
 * Контракт командного сервиса {@code AssignmentCampaignCommandService}.
 */
public interface AssignmentCampaignCommandService {

    AssignmentCampaign launchAssignmentCampaign(LaunchAssignmentCampaignCommand command);
}

