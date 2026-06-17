package com.vladislav.training.platform.content.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Запись данных {@code Question}.
 */
public record Question(
    Long id,
    Long topicId,
    String body,
    QuestionType questionType,
    ContentStatus status,
    Integer sortOrder,
    Instant createdAt,
    Instant updatedAt
) {

    public Question {
        Objects.requireNonNull(topicId, "topicId must not be null");
        Objects.requireNonNull(body, "body must not be null");
        Objects.requireNonNull(questionType, "questionType must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (body.isBlank()) {
            throw new IllegalArgumentException("body must not be blank");
        }
        if (sortOrder != null && sortOrder < 0) {
            throw new IllegalArgumentException("sortOrder must be non-negative");
        }
    }
}
