package com.vladislav.training.platform.result.query;

import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.result.domain.ResultQuestionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Объект передачи данных {@code SelfHistoricalResultReviewDto}.
 */
record SelfHistoricalResultReviewDto(
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
    List<SelfHistoricalResultReviewQuestionDto> questions
) {

    record SelfHistoricalResultReviewQuestionDto(
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
        List<SelfHistoricalResultReviewOptionDto> answerOptions
    ) {
    }

    record SelfHistoricalResultReviewOptionDto(
        Long resultAnswerOptionSnapshotId,
        Long answerOptionOriginalId,
        String body,
        Integer displayOrder,
        boolean correctAtSnapshot,
        boolean selectedByUser
    ) {
    }
}
