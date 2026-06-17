package com.vladislav.training.platform.content.controller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record SaveTestQuestionRequest(
    @NotNull Long questionId,
    @PositiveOrZero int displayOrder,
    @NotNull @Positive BigDecimal weight
) {}
