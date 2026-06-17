package com.vladislav.training.platform.analytics.query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Объект передачи данных {@code ManagerialDepartmentTopicAnalyticsDto}.
 */
public record ManagerialDepartmentTopicAnalyticsDto(
    Long organizationalUnitIdSnapshot,
    String organizationalUnitName,
    String organizationalPathSnapshot,
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

    public ManagerialDepartmentTopicAnalyticsDto {
        Objects.requireNonNull(organizationalUnitIdSnapshot, "organizationalUnitIdSnapshot must not be null");
        Objects.requireNonNull(organizationalUnitName, "organizationalUnitName must not be null");
        Objects.requireNonNull(organizationalPathSnapshot, "organizationalPathSnapshot must not be null");
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
