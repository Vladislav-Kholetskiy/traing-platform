package com.vladislav.training.platform.content.service;

import java.math.BigDecimal;

/**
 * Команда {@code UpdateTestQuestionCommand}.
 */
public record UpdateTestQuestionCommand(
    Long questionId,
    int displayOrder,
    BigDecimal weight
) {}
