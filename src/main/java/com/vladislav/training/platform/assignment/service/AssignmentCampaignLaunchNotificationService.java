package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.assignment.domain.AssignmentCampaign;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaignRecipientSnapshot;
import java.time.Instant;
import java.util.List;

/**
 * Контракт сервиса {@code AssignmentCampaignLaunchNotificationService}.
 */
interface AssignmentCampaignLaunchNotificationService {

    void createLaunchNotifications(
        AssignmentCampaign campaign,
        LaunchAssignmentCampaignCommand command,
        List<AssignmentCampaignRecipientSnapshot> recipientSnapshots,
        Instant launchedAt
    );
}
