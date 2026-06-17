package com.vladislav.training.platform.content.infrastructure.persistence;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import com.vladislav.training.platform.content.domain.AnswerOption;
import com.vladislav.training.platform.content.repository.AnswerOptionRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Адаптер репозитория {@code JpaAnswerOptionRepositoryAdapter}.
 */
@Repository
@Transactional(readOnly = true)
public class JpaAnswerOptionRepositoryAdapter implements AnswerOptionRepository {
    private final SpringDataAnswerOptionJpaRepository repository; private final ContentMapper mapper;
    public JpaAnswerOptionRepositoryAdapter(SpringDataAnswerOptionJpaRepository repository, ContentMapper mapper){this.repository=repository;this.mapper=mapper;}
    @Override public AnswerOption findAnswerOptionById(Long answerOptionId){ return repository.findById(answerOptionId).map(mapper::toDomain).orElseThrow(() -> new NotFoundException("Answer option not found: "+answerOptionId)); }
    @Override public List<AnswerOption> findAnswerOptionsByQuestionId(Long questionId){ return mapper.toAnswerOptions(repository.findAllByQuestionIdOrderByDisplayOrderAscIdAsc(questionId)); }
    @Override @Transactional public AnswerOption saveAnswerOption(AnswerOption answerOption){ try { return mapper.toDomain(repository.save(mapper.toEntity(answerOption))); } catch (DataIntegrityViolationException ex){ throw new PersistenceConstraintViolationException("Failed to persist answer_option", ex); } }
    @Override @Transactional public void deleteAnswerOption(Long answerOptionId){ repository.deleteById(answerOptionId); }
}
