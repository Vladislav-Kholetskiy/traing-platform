package com.vladislav.training.platform.analytics.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Запись данных {@code AnalyticsCampaignAggregate}.
 */
public record AnalyticsCampaignAggregate(
    Long id,
    Long campaignId,
    int recipientSnapshotCount,
    int nonCancelledAssignmentsFromCampaignSnapshot,
    int completedAssignments,
    int overdueAssignments,
    int nonCancelledActivePool,
    int cancelledAssignments,
    BigDecimal coveragePercent,
    BigDecimal overduePercent,
    Instant calculatedAt,
    Instant refreshedAt,
    Instant reconciledAt
) {

    public AnalyticsCampaignAggregate {
        Objects.requireNonNull(campaignId, "campaignId must not be null");
        Objects.requireNonNull(coveragePercent, "coveragePercent must not be null");
        Objects.requireNonNull(overduePercent, "overduePercent must not be null");
        Objects.requireNonNull(calculatedAt, "calculatedAt must not be null");
        Objects.requireNonNull(refreshedAt, "refreshedAt must not be null");
        if (recipientSnapshotCount < 0
            || nonCancelledAssignmentsFromCampaignSnapshot < 0
            || completedAssignments < 0
            || overdueAssignments < 0
            || nonCancelledActivePool < 0
            || cancelledAssignments < 0) {
            throw new IllegalArgumentException("aggregate counts must be non-negative");
        }
    }
}
