package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.assignment.domain.AssignmentCampaignRecipientSnapshot;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
class AssignmentCampaignRecipientSnapshotCaptureContract {

    AssignmentCampaignRecipientSnapshot capture(
        Long campaignId,
        Long userId,
        Long organizationalUnitIdSnapshot,
        String organizationalPathSnapshot,
        String employeeNumberSnapshot,
        String fullNameSnapshot,
        String inclusionBasisCode,
        Instant capturedAt
    ) {
        Objects.requireNonNull(campaignId, "campaignId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(organizationalUnitIdSnapshot, "organizationalUnitIdSnapshot must not be null");
        Objects.requireNonNull(organizationalPathSnapshot, "organizationalPathSnapshot must not be null");
        Objects.requireNonNull(inclusionBasisCode, "inclusionBasisCode must not be null");
        Objects.requireNonNull(capturedAt, "capturedAt must not be null");

        return new AssignmentCampaignRecipientSnapshot(
            null,
            campaignId,
            userId,
            organizationalUnitIdSnapshot,
            organizationalPathSnapshot,
            inclusionBasisCode,
            employeeNumberSnapshot,
            fullNameSnapshot,
            capturedAt,
            capturedAt
        );
    }
}
