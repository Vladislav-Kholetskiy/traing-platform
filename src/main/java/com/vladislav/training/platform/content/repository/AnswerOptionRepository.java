package com.vladislav.training.platform.content.repository;

import com.vladislav.training.platform.content.domain.AnswerOption;
import java.util.List;

/**
 * Контракт репозитория {@code AnswerOptionRepository}.
 */
public interface AnswerOptionRepository {

    AnswerOption findAnswerOptionById(Long answerOptionId);

    List<AnswerOption> findAnswerOptionsByQuestionId(Long questionId);

    AnswerOption saveAnswerOption(AnswerOption answerOption);

    void deleteAnswerOption(Long answerOptionId);
}
