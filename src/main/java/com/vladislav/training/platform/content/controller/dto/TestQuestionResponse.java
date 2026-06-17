package com.vladislav.training.platform.content.controller.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TestQuestionResponse(
    Long id,
    Long testId,
    Long questionId,
    int displayOrder,
    BigDecimal weight,
    Instant createdAt,
    Instant updatedAt
) {}
