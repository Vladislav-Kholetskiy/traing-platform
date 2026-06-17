package com.vladislav.training.platform.assignment.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Запись данных {@code AssignmentCampaignRecipientSnapshot}.
 */
public record AssignmentCampaignRecipientSnapshot(
    Long id,
    Long campaignId,
    Long userId,
    Long organizationalUnitIdSnapshot,
    String organizationalPathSnapshot,
    String inclusionBasisCode,
    String employeeNumberSnapshot,
    String fullNameSnapshot,
    Instant capturedAt,
    Instant createdAt
) {

    public AssignmentCampaignRecipientSnapshot {
        Objects.requireNonNull(campaignId, "campaignId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(organizationalUnitIdSnapshot, "organizationalUnitIdSnapshot must not be null");
        Objects.requireNonNull(organizationalPathSnapshot, "organizationalPathSnapshot must not be null");
        Objects.requireNonNull(inclusionBasisCode, "inclusionBasisCode must not be null");
        Objects.requireNonNull(capturedAt, "capturedAt must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
