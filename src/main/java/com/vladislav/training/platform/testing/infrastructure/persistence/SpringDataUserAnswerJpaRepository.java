package com.vladislav.training.platform.testing.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Контракт репозитория {@code SpringDataUserAnswerJpaRepository}.
 */
public interface SpringDataUserAnswerJpaRepository extends JpaRepository<UserAnswerEntity, Long> {

    List<UserAnswerEntity> findAllByTestAttemptIdOrderByIdAsc(Long testAttemptId);

    Optional<UserAnswerEntity> findByTestAttemptIdAndQuestionId(Long testAttemptId, Long questionId);
}
