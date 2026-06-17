package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.content.domain.QuestionType;

/**
 * Команда {@code UpdateQuestionCommand}.
 */
public record UpdateQuestionCommand(
    String body,
    QuestionType questionType,
    Integer sortOrder
) {}
