package com.vladislav.training.platform.testing.controller.dto;

import com.vladislav.training.platform.content.domain.AnswerOptionRole;
import com.vladislav.training.platform.content.domain.QuestionType;
import com.vladislav.training.platform.content.domain.TestType;
import java.math.BigDecimal;
import java.util.List;

/**
 * Ответ {@code SelfVisibleTestResponse}.
 */
public record SelfVisibleTestResponse(
    Long id,
    Long topicId,
    String name,
    String description,
    TestType testType,
    List<SelfVisibleQuestionResponse> questions
) {

    public record SelfVisibleQuestionResponse(
        Long id,
        String body,
        QuestionType questionType,
        int displayOrder,
        BigDecimal weight,
        List<SelfVisibleAnswerOptionResponse> answerOptions
    ) {
    }

    public record SelfVisibleAnswerOptionResponse(
        Long id,
        String body,
        AnswerOptionRole answerOptionRole,
        int displayOrder
    ) {
    }
}
