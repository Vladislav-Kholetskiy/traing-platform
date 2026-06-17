package com.vladislav.training.platform.content.infrastructure.persistence;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Question;
import com.vladislav.training.platform.content.repository.QuestionRepository;
import java.util.Collection;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Адаптер репозитория {@code JpaQuestionRepositoryAdapter}.
 */
@Repository
@Transactional(readOnly = true)
public class JpaQuestionRepositoryAdapter implements QuestionRepository {
    private final SpringDataQuestionJpaRepository repository; private final ContentMapper mapper;
    public JpaQuestionRepositoryAdapter(SpringDataQuestionJpaRepository repository, ContentMapper mapper){this.repository=repository;this.mapper=mapper;}
    @Override public Question findQuestionById(Long questionId){ return repository.findById(questionId).map(mapper::toDomain).orElseThrow(() -> new NotFoundException("Question not found: "+questionId)); }
    @Override public List<Question> findQuestionsByTopicId(Long topicId){ return mapper.toQuestions(repository.findAllByTopicIdOrderBySortOrderAscIdAsc(topicId)); }
    @Override public List<Question> findQuestionsByTopicIdAndStatus(Long topicId, ContentStatus status){ return mapper.toQuestions(repository.findAllByTopicIdAndStatusOrderBySortOrderAscIdAsc(topicId, status)); }
    @Override public List<Question> findQuestionsByIds(Collection<Long> questionIds){ return mapper.toQuestions(repository.findAllByIdIn(questionIds)); }
    @Override public boolean existsNonArchivedByTopicId(Long topicId){ return repository.existsByTopicIdAndStatusNot(topicId, ContentStatus.ARCHIVED); }
    @Override @Transactional public Question saveQuestion(Question question){ try { return mapper.toDomain(repository.save(mapper.toEntity(question))); } catch (DataIntegrityViolationException ex){ throw new PersistenceConstraintViolationException("Failed to persist question", ex); } }
}
