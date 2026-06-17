package com.vladislav.training.platform.integration.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Null;
/**
 * Запрос {@code ImportReviewRejectRequest}.
 */

@JsonIgnoreProperties(ignoreUnknown = false)
public record ImportReviewRejectRequest(
    String reason,
    @Null Long actorUserId,
    @Null Long initiatedByUserId,
    @Null Long userId
) {
}
