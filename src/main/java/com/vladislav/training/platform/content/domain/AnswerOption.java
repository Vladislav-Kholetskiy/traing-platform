package com.vladislav.training.platform.content.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Запись данных {@code AnswerOption}.
 */
public record AnswerOption(
    Long id,
    Long questionId,
    String body,
    AnswerOptionRole answerOptionRole,
    Boolean isCorrect,
    int displayOrder,
    String pairingKey,
    Integer canonicalOrderPosition,
    Instant createdAt,
    Instant updatedAt
) {

    public AnswerOption {
        Objects.requireNonNull(questionId, "questionId must not be null");
        Objects.requireNonNull(body, "body must not be null");
        Objects.requireNonNull(answerOptionRole, "answerOptionRole must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (body.isBlank()) {
            throw new IllegalArgumentException("body must not be blank");
        }
        if (displayOrder < 0) {
            throw new IllegalArgumentException("displayOrder must be non-negative");
        }
        if (canonicalOrderPosition != null && canonicalOrderPosition < 0) {
            throw new IllegalArgumentException("canonicalOrderPosition must be non-negative");
        }
        if (answerOptionRole == AnswerOptionRole.CHOICE_OPTION && isCorrect == null) {
            throw new IllegalArgumentException("isCorrect is required for CHOICE_OPTION");
        }
        if (answerOptionRole != AnswerOptionRole.CHOICE_OPTION && isCorrect != null) {
            throw new IllegalArgumentException("isCorrect is allowed only for CHOICE_OPTION");
        }
        if (pairingKey != null && answerOptionRole != AnswerOptionRole.MATCH_LEFT && answerOptionRole != AnswerOptionRole.MATCH_RIGHT) {
            throw new IllegalArgumentException("pairingKey is allowed only for matching answer options");
        }
        if (canonicalOrderPosition != null && answerOptionRole != AnswerOptionRole.ORDER_ITEM) {
            throw new IllegalArgumentException("canonicalOrderPosition is allowed only for ORDER_ITEM");
        }
    }
}
