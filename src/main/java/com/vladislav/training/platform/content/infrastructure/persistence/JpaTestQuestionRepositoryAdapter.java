package com.vladislav.training.platform.content.infrastructure.persistence;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import com.vladislav.training.platform.content.domain.TestQuestion;
import com.vladislav.training.platform.content.repository.TestQuestionRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Адаптер репозитория {@code JpaTestQuestionRepositoryAdapter}.
 */
@Repository
@Transactional(readOnly = true)
public class JpaTestQuestionRepositoryAdapter implements TestQuestionRepository {
    private final SpringDataTestQuestionJpaRepository repository; private final ContentMapper mapper;
    public JpaTestQuestionRepositoryAdapter(SpringDataTestQuestionJpaRepository repository, ContentMapper mapper){this.repository=repository;this.mapper=mapper;}
    @Override public TestQuestion findTestQuestionById(Long testQuestionId){ return repository.findById(testQuestionId).map(mapper::toDomain).orElseThrow(() -> new NotFoundException("TestQuestion not found: "+testQuestionId)); }
    @Override public List<TestQuestion> findTestQuestionsByTestId(Long testId){ return mapper.toTestQuestions(repository.findAllByTestIdOrderByDisplayOrderAscIdAsc(testId)); }
    @Override public boolean existsPublishedTestUsingQuestion(Long questionId){ return repository.existsByQuestionIdAndTestStatus(questionId, ContentStatus.PUBLISHED); }
    @Override @Transactional public TestQuestion saveTestQuestion(TestQuestion testQuestion){ try { return mapper.toDomain(repository.save(mapper.toEntity(testQuestion))); } catch (DataIntegrityViolationException ex){ throw new PersistenceConstraintViolationException("Failed to persist test_question", ex); } }
    @Override @Transactional public void deleteTestQuestion(Long testQuestionId){ repository.deleteById(testQuestionId); }
}
