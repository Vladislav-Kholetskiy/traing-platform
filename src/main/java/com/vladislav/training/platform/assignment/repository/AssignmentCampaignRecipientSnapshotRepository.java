package com.vladislav.training.platform.assignment.repository;

import com.vladislav.training.platform.assignment.domain.AssignmentCampaignRecipientSnapshot;
import java.util.List;

/**
 * Контракт репозитория {@code AssignmentCampaignRecipientSnapshotRepository}.
 */
public interface AssignmentCampaignRecipientSnapshotRepository {

    AssignmentCampaignRecipientSnapshot findAssignmentCampaignRecipientSnapshotById(Long recipientSnapshotId);

    List<AssignmentCampaignRecipientSnapshot> findAssignmentCampaignRecipientSnapshotsByCampaignId(Long assignmentCampaignId);

    List<AssignmentCampaignRecipientSnapshot> findAssignmentCampaignRecipientSnapshotsByUserId(Long userId);

    AssignmentCampaignRecipientSnapshot saveAssignmentCampaignRecipientSnapshot(
        AssignmentCampaignRecipientSnapshot assignmentCampaignRecipientSnapshot
    );
}
