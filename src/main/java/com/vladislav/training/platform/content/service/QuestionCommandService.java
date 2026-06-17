package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.content.domain.AnswerOption;
import com.vladislav.training.platform.content.domain.Question;

/**
 * Контракт командного сервиса {@code QuestionCommandService}.
 */
public interface QuestionCommandService {

    Question createQuestion(CreateQuestionCommand command);

    Question updateQuestion(Long questionId, UpdateQuestionCommand command);

    AnswerOption createAnswerOption(Long questionId, CreateAnswerOptionCommand command);

    AnswerOption updateAnswerOption(Long questionId, Long answerOptionId, UpdateAnswerOptionCommand command);

    void deleteAnswerOption(Long questionId, Long answerOptionId);
}
