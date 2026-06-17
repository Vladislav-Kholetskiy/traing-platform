package com.vladislav.training.platform.assignment.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Запись данных {@code AssignmentCampaignCourse}.
 */
public record AssignmentCampaignCourse(
    Long id,
    Long campaignId,
    Long courseId,
    Instant createdAt,
    Instant updatedAt
) {

    public AssignmentCampaignCourse {
        Objects.requireNonNull(campaignId, "campaignId must not be null");
        Objects.requireNonNull(courseId, "courseId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}
