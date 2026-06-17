package com.vladislav.training.platform.content.repository;

import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Question;
import java.util.Collection;
import java.util.List;

/**
 * Контракт репозитория {@code QuestionRepository}.
 */
public interface QuestionRepository {

    Question findQuestionById(Long questionId);

    List<Question> findQuestionsByTopicId(Long topicId);

    List<Question> findQuestionsByTopicIdAndStatus(Long topicId, ContentStatus status);

    List<Question> findQuestionsByIds(Collection<Long> questionIds);

    boolean existsNonArchivedByTopicId(Long topicId);

    Question saveQuestion(Question question);
}
