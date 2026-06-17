package com.vladislav.training.platform.assignment.controller.dto;

import java.time.Instant;

/**
 * Ответ {@code AssignmentCampaignResponse}.
 */
public record AssignmentCampaignResponse(
    Long id,
    String name,
    String description,
    String sourceType,
    String sourceRef,
    String sourceNameSnapshot,
    Instant createdAt,
    Instant updatedAt
) {
}
