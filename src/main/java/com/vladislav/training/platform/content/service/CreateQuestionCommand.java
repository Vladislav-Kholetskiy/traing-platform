package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.content.domain.QuestionType;

/**
 * Команда {@code CreateQuestionCommand}.
 */
public record CreateQuestionCommand(
    Long topicId,
    String body,
    QuestionType questionType,
    Integer sortOrder
) {}
