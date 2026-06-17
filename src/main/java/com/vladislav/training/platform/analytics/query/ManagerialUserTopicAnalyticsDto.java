package com.vladislav.training.platform.analytics.query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Объект передачи данных {@code ManagerialUserTopicAnalyticsDto}.
 */
public record ManagerialUserTopicAnalyticsDto(
    Long userId,
    String userEmployeeNumber,
    String userDisplayName,
    Long topicId,
    String topicName,
    Instant periodStart,
    Instant periodEnd,
    BigDecimal averageScorePercent,
    BigDecimal passRatePercent,
    Integer attemptCount,
    Integer errorCount,
    Instant calculatedAt,
    Instant refreshedAt
) {

    public ManagerialUserTopicAnalyticsDto {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(userEmployeeNumber, "userEmployeeNumber must not be null");
        Objects.requireNonNull(userDisplayName, "userDisplayName must not be null");
        Objects.requireNonNull(topicId, "topicId must not be null");
        Objects.requireNonNull(topicName, "topicName must not be null");
        Objects.requireNonNull(periodStart, "periodStart must not be null");
        Objects.requireNonNull(periodEnd, "periodEnd must not be null");
        Objects.requireNonNull(averageScorePercent, "averageScorePercent must not be null");
        Objects.requireNonNull(passRatePercent, "passRatePercent must not be null");
        Objects.requireNonNull(attemptCount, "attemptCount must not be null");
        Objects.requireNonNull(errorCount, "errorCount must not be null");
        Objects.requireNonNull(calculatedAt, "calculatedAt must not be null");
        Objects.requireNonNull(refreshedAt, "refreshedAt must not be null");
    }
}
