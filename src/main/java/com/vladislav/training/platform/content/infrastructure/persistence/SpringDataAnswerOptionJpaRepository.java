package com.vladislav.training.platform.content.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Контракт репозитория {@code SpringDataAnswerOptionJpaRepository}.
 */
public interface SpringDataAnswerOptionJpaRepository extends JpaRepository<AnswerOptionEntity, Long> {
    List<AnswerOptionEntity> findAllByQuestionIdOrderByDisplayOrderAscIdAsc(Long questionId);
}
