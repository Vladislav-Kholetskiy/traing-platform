package com.vladislav.training.platform.testing.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Контракт репозитория {@code SpringDataUserAnswerItemJpaRepository}.
 */
public interface SpringDataUserAnswerItemJpaRepository extends JpaRepository<UserAnswerItemEntity, Long> {

    List<UserAnswerItemEntity> findAllByUserAnswerIdOrderByIdAsc(Long userAnswerId);

    void deleteAllByUserAnswerId(Long userAnswerId);
}
