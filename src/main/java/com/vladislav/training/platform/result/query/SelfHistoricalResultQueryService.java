package com.vladislav.training.platform.result.query;

import com.vladislav.training.platform.common.model.AttemptMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Контракт сервиса чтения {@code SelfHistoricalResultQueryService}.
 */
public interface SelfHistoricalResultQueryService {

    List<SelfHistoricalResultReadModel> findSelfHistoricalResults(SelfHistoricalResultQuery query);

    record SelfHistoricalResultQuery(
        Long actorUserId
    ) {
    }

    record SelfHistoricalResultReadModel(
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
