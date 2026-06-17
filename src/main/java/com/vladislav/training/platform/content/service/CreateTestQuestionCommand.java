package com.vladislav.training.platform.content.service;

import java.math.BigDecimal;

/**
 * Команда {@code CreateTestQuestionCommand}.
 */
public record CreateTestQuestionCommand(
    Long questionId,
    int displayOrder,
    BigDecimal weight
) {}
