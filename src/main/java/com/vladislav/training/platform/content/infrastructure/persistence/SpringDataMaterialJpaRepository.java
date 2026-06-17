package com.vladislav.training.platform.content.infrastructure.persistence;

import com.vladislav.training.platform.content.domain.ContentStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Контракт репозитория {@code SpringDataMaterialJpaRepository}.
 */
public interface SpringDataMaterialJpaRepository extends JpaRepository<MaterialEntity, Long> {
    List<MaterialEntity> findAllByTopicIdOrderBySortOrderAscIdAsc(Long topicId);
    List<MaterialEntity> findAllByTopicIdAndStatusOrderBySortOrderAscIdAsc(Long topicId, ContentStatus status);
    boolean existsByTopicIdAndStatusNot(Long topicId, ContentStatus status);
}
