package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.content.domain.AnswerOptionRole;
import com.vladislav.training.platform.content.domain.QuestionType;
import java.util.List;
import java.util.Objects;

public record AssignedTestContext(
    Long assignmentId,
    Long assignmentTestId,
    Long testId,
    String testName,
    List<AssignedTestQuestionContext> questions
) {

    public AssignedTestContext {
        Objects.requireNonNull(assignmentId, "assignmentId must not be null");
        Objects.requireNonNull(assignmentTestId, "assignmentTestId must not be null");
        Objects.requireNonNull(testId, "testId must not be null");
        Objects.requireNonNull(testName, "testName must not be null");
        Objects.requireNonNull(questions, "questions must not be null");
        if (testName.isBlank()) {
            throw new IllegalArgumentException("testName must not be blank");
        }
        questions = List.copyOf(questions);
    }

    public record AssignedTestQuestionContext(
        Long questionId,
        String body,
        QuestionType questionType,
        int displayOrder,
        List<AssignedTestAnswerOptionContext> answerOptions
    ) {

        public AssignedTestQuestionContext {
            Objects.requireNonNull(questionId, "questionId must not be null");
            Objects.requireNonNull(body, "body must not be null");
            Objects.requireNonNull(questionType, "questionType must not be null");
            Objects.requireNonNull(answerOptions, "answerOptions must not be null");
            if (body.isBlank()) {
                throw new IllegalArgumentException("body must not be blank");
            }
            if (displayOrder < 0) {
                throw new IllegalArgumentException("displayOrder must be non-negative");
            }
            answerOptions = List.copyOf(answerOptions);
        }
    }

    public record AssignedTestAnswerOptionContext(
        Long answerOptionId,
        String body,
        AnswerOptionRole answerOptionRole,
        int displayOrder
    ) {

        public AssignedTestAnswerOptionContext {
            Objects.requireNonNull(answerOptionId, "answerOptionId must not be null");
            Objects.requireNonNull(body, "body must not be null");
            Objects.requireNonNull(answerOptionRole, "answerOptionRole must not be null");
            if (body.isBlank()) {
                throw new IllegalArgumentException("body must not be blank");
            }
            if (displayOrder < 0) {
                throw new IllegalArgumentException("displayOrder must be non-negative");
            }
        }
    }
}
