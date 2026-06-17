package com.vladislav.training.platform.testing.infrastructure.persistence;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import com.vladislav.training.platform.testing.domain.UserAnswer;
import com.vladislav.training.platform.testing.repository.UserAnswerRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Адаптер репозитория {@code JpaUserAnswerRepositoryAdapter}.
 */
@Repository
@Transactional(readOnly = true)
public class JpaUserAnswerRepositoryAdapter implements UserAnswerRepository {

    private final SpringDataUserAnswerJpaRepository repository;
    private final TestingPersistenceMapper mapper;

    public JpaUserAnswerRepositoryAdapter(
        SpringDataUserAnswerJpaRepository repository,
        TestingPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public UserAnswer findUserAnswerById(Long userAnswerId) {
        return repository.findById(userAnswerId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException("User answer not found: " + userAnswerId));
    }

    @Override
    public List<UserAnswer> findUserAnswersByTestAttemptId(Long testAttemptId) {
        return mapper.toUserAnswers(repository.findAllByTestAttemptIdOrderByIdAsc(testAttemptId));
    }

    @Override
    public UserAnswer findUserAnswerByTestAttemptIdAndQuestionId(Long testAttemptId, Long questionId) {
        return repository.findByTestAttemptIdAndQuestionId(testAttemptId, questionId)
            .map(mapper::toDomain)
            .orElse(null);
    }

    @Override
    @Transactional
    public UserAnswer saveUserAnswer(UserAnswer userAnswer) {
        try {
            return mapper.toDomain(repository.save(mapper.toEntity(userAnswer)));
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConstraintViolationException("Failed to persist user_answer", exception);
        }
    }
}
