package com.vladislav.training.platform.content.infrastructure.persistence;

import com.vladislav.training.platform.content.domain.ContentStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Контракт репозитория {@code SpringDataTopicJpaRepository}.
 */
public interface SpringDataTopicJpaRepository extends JpaRepository<TopicEntity, Long> {
    List<TopicEntity> findAllByCourseIdOrderBySortOrderAscIdAsc(Long courseId);
    List<TopicEntity> findAllByCourseIdAndStatusOrderBySortOrderAscIdAsc(Long courseId, ContentStatus status);
    boolean existsByCourseIdAndStatusNot(Long courseId, ContentStatus status);
}
