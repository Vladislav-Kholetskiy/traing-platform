package com.vladislav.training.platform.analytics.repository;

import com.vladislav.training.platform.access.service.ManagerialReadScope;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Контракт репозитория {@code ManagerialHistoricalAnalyticsReadRepository}.
 */
public interface ManagerialHistoricalAnalyticsReadRepository {

    List<ManagerialUserTopicAnalyticsReadRow> findUserTopicRows(ManagerialHistoricalAnalyticsReadCriteria criteria);

    List<ManagerialDepartmentTopicAnalyticsReadRow> findDepartmentTopicRows(ManagerialHistoricalAnalyticsReadCriteria criteria);

    record ManagerialHistoricalAnalyticsReadCriteria(
        ManagerialReadScope managerialReadScope,
        Instant periodStart,
        Instant periodEnd
    ) {

        public ManagerialHistoricalAnalyticsReadCriteria {
            Objects.requireNonNull(managerialReadScope, "managerialReadScope must not be null");
            Objects.requireNonNull(periodStart, "periodStart must not be null");
            Objects.requireNonNull(periodEnd, "periodEnd must not be null");
            if (!periodStart.isBefore(periodEnd)) {
                throw new IllegalArgumentException("periodStart must be before periodEnd");
            }
        }
    }

    record ManagerialUserTopicAnalyticsReadRow(
        Long userId,
        String employeeNumber,
        String lastName,
        String firstName,
        String middleName,
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

        public ManagerialUserTopicAnalyticsReadRow {
            Objects.requireNonNull(userId, "userId must not be null");
            Objects.requireNonNull(employeeNumber, "employeeNumber must not be null");
            Objects.requireNonNull(lastName, "lastName must not be null");
            Objects.requireNonNull(firstName, "firstName must not be null");
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

    record ManagerialDepartmentTopicAnalyticsReadRow(
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

        public ManagerialDepartmentTopicAnalyticsReadRow {
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
}
