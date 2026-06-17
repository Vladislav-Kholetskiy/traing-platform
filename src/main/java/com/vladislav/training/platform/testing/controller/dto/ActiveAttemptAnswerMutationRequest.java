package com.vladislav.training.platform.testing.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Запрос {@code ActiveAttemptAnswerMutationRequest}.
 */
public record ActiveAttemptAnswerMutationRequest(
    @NotNull List<@Valid ActiveAttemptAnswerItemRequest> answerItems
) {

    public record ActiveAttemptAnswerItemRequest(
        Long answerOptionId,
        Long leftAnswerOptionId,
        Long rightAnswerOptionId,
        Integer userOrderPosition
    ) {
    }
}
