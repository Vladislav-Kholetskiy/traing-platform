package com.vladislav.training.platform.result.query;

import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.result.domain.ResultQuestionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Контракт сервиса чтения {@code SelfHistoricalResultReviewQueryService}.
 */
public interface SelfHistoricalResultReviewQueryService {

    SelfHistoricalResultReviewReadModel findSelfHistoricalResultReview(SelfHistoricalResultReviewQuery query);

    record SelfHistoricalResultReviewQuery(
        Long actorUserId,
        Long resultId
    ) {
    }

    record SelfHistoricalResultReviewReadModel(
        Long resultId,
        Instant recordedAt,
        Long testAttemptId,
        Long testId,
        String testName,
        BigDecimal scorePercent,
        BigDecimal score,
        boolean passed,
        AttemptMode attemptMode,
        Long assignmentId,
        List<SelfHistoricalResultReviewQuestionReadModel> questions
    ) {
    }

    record SelfHistoricalResultReviewQuestionReadModel(
        Long resultQuestionSnapshotId,
        Long questionOriginalId,
        String body,
        ResultQuestionType questionType,
        Integer displayOrder,
        BigDecimal earnedScore,
        BigDecimal maxScore,
        boolean correct,
        String evaluationNote,
        String correctAnswerSnapshot,
        String userAnswerSnapshot,
        List<SelfHistoricalResultReviewOptionReadModel> answerOptions
    ) {
    }

    record SelfHistoricalResultReviewOptionReadModel(
        Long resultAnswerOptionSnapshotId,
        Long answerOptionOriginalId,
        String body,
        Integer displayOrder,
        boolean correctAtSnapshot,
        boolean selectedByUser
    ) {
    }
}
