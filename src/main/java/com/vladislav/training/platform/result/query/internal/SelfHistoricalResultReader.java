package com.vladislav.training.platform.result.query.internal;

import com.vladislav.training.platform.common.model.AttemptMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
/**
 * Читатель {@code SelfHistoricalResultReader}.
 */
public interface SelfHistoricalResultReader {

    List<SelfHistoricalResultReadRow> findSelfHistoricalResultRows(SelfHistoricalResultReadCriteria criteria);

    record SelfHistoricalResultReadCriteria(
        Long actorUserId
    ) {
    }

    record SelfHistoricalResultReadRow(
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
}

