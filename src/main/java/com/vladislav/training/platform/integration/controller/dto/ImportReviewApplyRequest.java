package com.vladislav.training.platform.integration.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
/**
 * Запрос {@code ImportReviewApplyRequest}.
 */

@JsonIgnoreProperties(ignoreUnknown = false)
public record ImportReviewApplyRequest(
    @NotNull Long matchedUserId,
    @Null Long actorUserId,
    @Null Long initiatedByUserId,
    @Null Long userId
) {
}
