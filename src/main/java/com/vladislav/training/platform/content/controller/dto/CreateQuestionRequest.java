package com.vladislav.training.platform.content.controller.dto;

import com.vladislav.training.platform.content.domain.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateQuestionRequest(
    @NotNull Long topicId,
    @NotBlank String body,
    @NotNull QuestionType questionType,
    @PositiveOrZero Integer sortOrder
) {}
