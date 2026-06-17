package com.vladislav.training.platform.testing.controller.dto;

/**
 * Ответ {@code SelfAttemptSubmitResponse}.
 */
public record SelfAttemptSubmitResponse(
    Long testAttemptId,
    Long resultId
) {
}
