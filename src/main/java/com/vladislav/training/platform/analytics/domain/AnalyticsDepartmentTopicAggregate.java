package com.vladislav.training.platform.analytics.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Запись данных {@code AnalyticsDepartmentTopicAggregate}.
 */
public record AnalyticsDepartmentTopicAggregate(
    Long id,
    Long organizationalUnitIdSnapshot,
    String organizationalPathSnapshot,
    Long topicId,
    Instant periodStart,
    Instant periodEnd,
    BigDecimal averageScorePercent,
    BigDecimal passRatePercent,
    int attemptCount,
    int errorCount,
    Instant calculatedAt,
    Instant refreshedAt,
    Instant reconciledAt
) {

    public AnalyticsDepartmentTopicAggregate {
        Objects.requireNonNull(organizationalUnitIdSnapshot, "organizationalUnitIdSnapshot must not be null");
        Objects.requireNonNull(organizationalPathSnapshot, "organizationalPathSnapshot must not be null");
        Objects.requireNonNull(topicId, "topicId must not be null");
        Objects.requireNonNull(periodStart, "periodStart must not be null");
        Objects.requireNonNull(periodEnd, "periodEnd must not be null");
        Objects.requireNonNull(averageScorePercent, "averageScorePercent must not be null");
        Objects.requireNonNull(passRatePercent, "passRatePercent must not be null");
        Objects.requireNonNull(calculatedAt, "calculatedAt must not be null");
        Objects.requireNonNull(refreshedAt, "refreshedAt must not be null");
        if (organizationalPathSnapshot.isBlank()) {
            throw new IllegalArgumentException("organizationalPathSnapshot must not be blank");
        }
        if (!periodEnd.isAfter(periodStart)) {
            throw new IllegalArgumentException("periodEnd must be after periodStart");
        }
        if (attemptCount < 0 || errorCount < 0) {
            throw new IllegalArgumentException("aggregate counts must be non-negative");
        }
    }
}
