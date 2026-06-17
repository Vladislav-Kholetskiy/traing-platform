package com.vladislav.training.platform.analytics.service;

/**
 * Запись данных {@code AnalyticsCampaignAggregateSourceFacts}.
 */
public record AnalyticsCampaignAggregateSourceFacts(
    Long campaignId,
    int recipientSnapshotCount,
    int nonCancelledAssignmentsFromCampaignSnapshot,
    int completedAssignments,
    int overdueAssignments,
    int nonCancelledActivePool,
    int cancelledAssignments
) {
}
