package com.vladislav.training.platform.testing.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Запись данных {@code UserAnswerItem}.
 */
public record UserAnswerItem(
    Long id,
    Long userAnswerId,
    Long answerOptionId,
    Long leftAnswerOptionId,
    Long rightAnswerOptionId,
    Integer userOrderPosition,
    Instant createdAt,
    Instant updatedAt
) {

    public UserAnswerItem {
        Objects.requireNonNull(userAnswerId, "userAnswerId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}
