package com.vladislav.training.platform.analytics.service;

import java.math.BigDecimal;
import java.time.Instant;

public record AnalyticsQuestionAggregateResultSourceRow(
    Long resultId,
    Long userIdSnapshot,
    Long organizationalUnitIdSnapshot,
    String organizationalPathSnapshot,
    String attemptModeSnapshot,
    BigDecimal scorePercent,
    Boolean passed,
    Boolean finalTopicControlSnapshot,
    Long questionOriginalId,
    Long topicIdSnapshot,
    Boolean answeredCorrectly,
    BigDecimal earnedScore,
    BigDecimal maxScore,
    Instant completedAt
) {

    public AnalyticsQuestionAggregateResultSourceRow(
        Long resultId,
        Long userIdSnapshot,
        Long organizationalUnitIdSnapshot,
        String organizationalPathSnapshot,
        String attemptModeSnapshot,
        BigDecimal scorePercent,
        Boolean passed,
        Boolean finalTopicControlSnapshot,
        Long questionOriginalId,
        Boolean answeredCorrectly,
        BigDecimal earnedScore,
        BigDecimal maxScore,
        Instant completedAt
    ) {
        this(
            resultId,
            userIdSnapshot,
            organizationalUnitIdSnapshot,
            organizationalPathSnapshot,
            attemptModeSnapshot,
            scorePercent,
            passed,
            finalTopicControlSnapshot,
            questionOriginalId,
            null,
            answeredCorrectly,
            earnedScore,
            maxScore,
            completedAt
        );
    }
}
