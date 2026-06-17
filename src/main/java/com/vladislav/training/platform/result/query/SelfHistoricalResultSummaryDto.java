package com.vladislav.training.platform.result.query;

import com.vladislav.training.platform.common.model.AttemptMode;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Объект передачи данных {@code SelfHistoricalResultSummaryDto}.
 */
record SelfHistoricalResultSummaryDto(
    Long resultId,
    Instant recordedAt,
    Long testAttemptId,
    Long testId,
    String testName,
    BigDecimal scorePercent,
    BigDecimal score,
    boolean passed,
    AttemptMode attemptMode,
    Long assignmentId
) {
}
