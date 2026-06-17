package com.vladislav.training.platform.testing.service;

import com.vladislav.training.platform.content.domain.AnswerOptionRole;
import com.vladislav.training.platform.content.domain.QuestionType;
import com.vladislav.training.platform.content.domain.TestType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Запись данных {@code SelfVisibleTestReadModel}.
 */
public record SelfVisibleTestReadModel(
    Long id,
    Long topicId,
    String name,
    String description,
    TestType testType,
    List<SelfVisibleQuestionReadModel> questions
) {

    public SelfVisibleTestReadModel {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(topicId, "topicId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(testType, "testType must not be null");
        Objects.requireNonNull(questions, "questions must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }

    public record SelfVisibleQuestionReadModel(
        Long id,
        String body,
        QuestionType questionType,
        int displayOrder,
        BigDecimal weight,
        List<SelfVisibleAnswerOptionReadModel> answerOptions
    ) {

        public SelfVisibleQuestionReadModel {
            Objects.requireNonNull(id, "id must not be null");
            Objects.requireNonNull(body, "body must not be null");
            Objects.requireNonNull(questionType, "questionType must not be null");
            Objects.requireNonNull(weight, "weight must not be null");
            Objects.requireNonNull(answerOptions, "answerOptions must not be null");
            if (body.isBlank()) {
                throw new IllegalArgumentException("body must not be blank");
            }
            if (displayOrder < 0) {
                throw new IllegalArgumentException("displayOrder must be non-negative");
            }
            if (weight.signum() <= 0) {
                throw new IllegalArgumentException("weight must be positive");
            }
        }
    }

    public record SelfVisibleAnswerOptionReadModel(
        Long id,
        String body,
        AnswerOptionRole answerOptionRole,
        int displayOrder
    ) {

        public SelfVisibleAnswerOptionReadModel {
            Objects.requireNonNull(id, "id must not be null");
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
