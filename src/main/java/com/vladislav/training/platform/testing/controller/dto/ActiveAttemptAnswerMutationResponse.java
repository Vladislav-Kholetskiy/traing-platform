package com.vladislav.training.platform.testing.controller.dto;

import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import java.time.Instant;

/**
 * Ответ {@code ActiveAttemptAnswerMutationResponse}.
 */
public record ActiveAttemptAnswerMutationResponse(
    Long testAttemptId,
    Long questionId,
    TestAttemptStatus status,
    Instant lastActivityAt
) {
}
