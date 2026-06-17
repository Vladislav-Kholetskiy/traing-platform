package com.vladislav.training.platform.analytics.query;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Контракт сервиса чтения {@code ExpertQuestionAnalyticsQueryService}.
 */
public interface ExpertQuestionAnalyticsQueryService {

    List<ExpertQuestionAnalyticsDto> findQuestionAnalytics(ExpertQuestionAnalyticsQuery query);

    /**
     * Параметры чтения экспертной аналитики по вопросам за период.
     */
    record ExpertQuestionAnalyticsQuery(
        Long actorUserId,
        Instant effectiveAt,
        Instant periodStart,
        Instant periodEnd
    ) {

        public ExpertQuestionAnalyticsQuery {
            Objects.requireNonNull(actorUserId, "actorUserId must not be null");
            Objects.requireNonNull(effectiveAt, "effectiveAt must not be null");
            Objects.requireNonNull(periodStart, "periodStart must not be null");
            Objects.requireNonNull(periodEnd, "periodEnd must not be null");
            if (!periodStart.isBefore(periodEnd)) {
                throw new IllegalArgumentException("periodStart must be strictly before periodEnd");
            }
        }
    }
}
