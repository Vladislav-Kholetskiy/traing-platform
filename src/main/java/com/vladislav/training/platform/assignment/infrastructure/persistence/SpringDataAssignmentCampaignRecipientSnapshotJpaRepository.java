package com.vladislav.training.platform.assignment.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataAssignmentCampaignRecipientSnapshotJpaRepository
    extends JpaRepository<AssignmentCampaignRecipientSnapshotEntity, Long> {

    List<AssignmentCampaignRecipientSnapshotEntity> findAllByCampaignIdOrderByIdAsc(Long campaignId);

    List<AssignmentCampaignRecipientSnapshotEntity> findAllByUserIdOrderByIdAsc(Long userId);
}
