package com.vladislav.training.platform.assignment.controller.dto;

import java.time.Instant;

public record AssignmentCampaignRecipientSnapshotResponse(
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
}
