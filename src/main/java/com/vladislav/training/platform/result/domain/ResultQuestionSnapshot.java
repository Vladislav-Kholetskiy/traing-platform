package com.vladislav.training.platform.result.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Запись данных {@code ResultQuestionSnapshot}.
 */
public record ResultQuestionSnapshot(
    Long id,
    Long resultId,
    Long questionOriginalId,
    Long topicIdSnapshot,
    String body,
    ResultQuestionType questionType,
    int displayOrder,
    BigDecimal weight,
    String correctAnswerSnapshot,
    String userAnswerSnapshot,
    BigDecimal earnedScore,
    BigDecimal maxScore,
    boolean isCorrect,
    String evaluationNote,
    Instant createdAt
) {

    public ResultQuestionSnapshot(
        Long id,
        Long resultId,
        Long questionOriginalId,
        String body,
        ResultQuestionType questionType,
        int displayOrder,
        BigDecimal weight,
        String correctAnswerSnapshot,
        String userAnswerSnapshot,
        BigDecimal earnedScore,
        BigDecimal maxScore,
        boolean isCorrect,
        String evaluationNote,
        Instant createdAt
    ) {
        this(
            id,
            resultId,
            questionOriginalId,
            null,
            body,
            questionType,
            displayOrder,
            weight,
            correctAnswerSnapshot,
            userAnswerSnapshot,
            earnedScore,
            maxScore,
            isCorrect,
            evaluationNote,
            createdAt
        );
    }

    public ResultQuestionSnapshot {
        Objects.requireNonNull(resultId, "resultId must not be null");
        Objects.requireNonNull(body, "body must not be null");
        Objects.requireNonNull(questionType, "questionType must not be null");
        Objects.requireNonNull(weight, "weight must not be null");
        Objects.requireNonNull(correctAnswerSnapshot, "correctAnswerSnapshot must not be null");
        Objects.requireNonNull(userAnswerSnapshot, "userAnswerSnapshot must not be null");
        Objects.requireNonNull(earnedScore, "earnedScore must not be null");
        Objects.requireNonNull(maxScore, "maxScore must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
