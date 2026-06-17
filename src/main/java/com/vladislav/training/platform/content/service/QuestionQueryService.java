package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.content.domain.AnswerOption;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Question;
import java.util.List;

/**
 * Контракт сервиса чтения {@code QuestionQueryService}.
 */
public interface QuestionQueryService {

    Question findQuestionById(Long questionId);

    List<Question> findQuestionsByTopicId(Long topicId);

    List<Question> findQuestionsByTopicIdAndStatus(Long topicId, ContentStatus status);

    AnswerOption findAnswerOptionById(Long answerOptionId);

    List<AnswerOption> findAnswerOptionsByQuestionId(Long questionId);
}
