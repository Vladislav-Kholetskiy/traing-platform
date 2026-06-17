package com.vladislav.training.platform.result.domain;

import java.math.BigDecimal;
import java.util.Objects;
/**
 * Запись данных {@code ResultScoringSnapshot}.
 */
public record ResultScoringSnapshot(
    BigDecimal thresholdPercent,
    BigDecimal earnedScore,
    BigDecimal maxScore,
    BigDecimal scorePercent,
    boolean passed,
    String scoringPolicyCode,
    String scoringPolicySnapshot
) {

    public ResultScoringSnapshot {
        Objects.requireNonNull(thresholdPercent, "thresholdPercent must not be null");
        Objects.requireNonNull(earnedScore, "earnedScore must not be null");
        Objects.requireNonNull(maxScore, "maxScore must not be null");
        Objects.requireNonNull(scorePercent, "scorePercent must not be null");
        Objects.requireNonNull(scoringPolicyCode, "scoringPolicyCode must not be null");
        Objects.requireNonNull(scoringPolicySnapshot, "scoringPolicySnapshot must not be null");
    }
}
