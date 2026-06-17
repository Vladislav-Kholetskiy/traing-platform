package com.vladislav.training.platform.result.domain;

import java.time.Instant;
import java.util.Objects;
/**
 * Запись данных {@code ResultAnswerOptionSnapshot}.
 */
public record ResultAnswerOptionSnapshot(
    Long id,
    Long resultQuestionSnapshotId,
    Long answerOptionOriginalId,
    String body,
    int displayOrder,
    boolean isCorrectAtSnapshot,
    boolean isSelectedByUser,
    Instant createdAt
) {

    public ResultAnswerOptionSnapshot {
        Objects.requireNonNull(resultQuestionSnapshotId, "resultQuestionSnapshotId must not be null");
        Objects.requireNonNull(body, "body must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
