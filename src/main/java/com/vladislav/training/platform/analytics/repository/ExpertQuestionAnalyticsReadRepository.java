package com.vladislav.training.platform.analytics.repository;

import com.vladislav.training.platform.access.service.AccessReadScope;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public interface ExpertQuestionAnalyticsReadRepository {

    List<ExpertQuestionAnalyticsReadRow> findQuestionAnalyticsRows(ExpertQuestionAnalyticsReadCriteria criteria);

    record ExpertQuestionAnalyticsReadCriteria(
        AccessReadScope accessReadScope,
        Instant periodStart,
        Instant periodEnd
    ) {

        public ExpertQuestionAnalyticsReadCriteria {
            Objects.requireNonNull(accessReadScope, "accessReadScope must not be null");
            Objects.requireNonNull(periodStart, "periodStart must not be null");
            Objects.requireNonNull(periodEnd, "periodEnd must not be null");
            if (!periodStart.isBefore(periodEnd)) {
                throw new IllegalArgumentException("periodStart must be strictly before periodEnd");
            }
        }
    }

    record ExpertQuestionAnalyticsReadRow(
        Long questionId,
        Instant periodStart,
        Instant periodEnd,
        Integer attemptCount,
        Integer correctCount,
        Integer incorrectCount,
        BigDecimal averageEarnedScore,
        Instant calculatedAt,
        Instant refreshedAt
    ) {

        public ExpertQuestionAnalyticsReadRow {
            Objects.requireNonNull(questionId, "questionId must not be null");
            Objects.requireNonNull(periodStart, "periodStart must not be null");
            Objects.requireNonNull(periodEnd, "periodEnd must not be null");
            Objects.requireNonNull(attemptCount, "attemptCount must not be null");
            Objects.requireNonNull(correctCount, "correctCount must not be null");
            Objects.requireNonNull(incorrectCount, "incorrectCount must not be null");
            Objects.requireNonNull(averageEarnedScore, "averageEarnedScore must not be null");
            Objects.requireNonNull(calculatedAt, "calculatedAt must not be null");
            Objects.requireNonNull(refreshedAt, "refreshedAt must not be null");
            if (!periodStart.isBefore(periodEnd)) {
                throw new IllegalArgumentException("periodStart must be strictly before periodEnd");
            }
        }
    }
}
