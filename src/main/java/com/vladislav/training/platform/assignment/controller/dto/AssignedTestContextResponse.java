package com.vladislav.training.platform.assignment.controller.dto;

import com.vladislav.training.platform.content.domain.AnswerOptionRole;
import com.vladislav.training.platform.content.domain.QuestionType;
import java.util.List;

public record AssignedTestContextResponse(
    Long assignmentId,
    Long assignmentTestId,
    Long testId,
    String testName,
    List<AssignedTestQuestionResponse> questions
) {

    public record AssignedTestQuestionResponse(
        Long questionId,
        String body,
        QuestionType questionType,
        int displayOrder,
        List<AssignedTestAnswerOptionResponse> answerOptions
    ) {
    }

    public record AssignedTestAnswerOptionResponse(
        Long answerOptionId,
        String body,
        AnswerOptionRole answerOptionRole,
        int displayOrder
    ) {
    }
}
