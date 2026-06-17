package com.vladislav.training.platform.analytics.query;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Контракт сервиса чтения {@code ManagerialHistoricalAnalyticsQueryService}.
 */
public interface ManagerialHistoricalAnalyticsQueryService {

    List<ManagerialUserTopicAnalyticsDto> findUserTopicAnalytics(ManagerialHistoricalAnalyticsQuery query);

    List<ManagerialDepartmentTopicAnalyticsDto> findDepartmentTopicAnalytics(ManagerialHistoricalAnalyticsQuery query);

    /**
     * Параметры чтения управленческой исторической аналитики.
     */
    record ManagerialHistoricalAnalyticsQuery(
        Long actorUserId,
        Instant effectiveAt,
        Instant periodStart,
        Instant periodEnd
    ) {

        public ManagerialHistoricalAnalyticsQuery {
            Objects.requireNonNull(actorUserId, "actorUserId must not be null");
            Objects.requireNonNull(effectiveAt, "effectiveAt must not be null");
            Objects.requireNonNull(periodStart, "periodStart must not be null");
            Objects.requireNonNull(periodEnd, "periodEnd must not be null");
            if (!periodStart.isBefore(periodEnd)) {
                throw new IllegalArgumentException("periodStart must be before periodEnd");
            }
        }
    }
}
