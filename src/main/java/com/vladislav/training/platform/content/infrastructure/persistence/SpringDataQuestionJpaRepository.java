package com.vladislav.training.platform.content.infrastructure.persistence;

import com.vladislav.training.platform.content.domain.ContentStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Контракт репозитория {@code SpringDataQuestionJpaRepository}.
 */
public interface SpringDataQuestionJpaRepository extends JpaRepository<QuestionEntity, Long> {
    List<QuestionEntity> findAllByTopicIdOrderBySortOrderAscIdAsc(Long topicId);
    List<QuestionEntity> findAllByTopicIdAndStatusOrderBySortOrderAscIdAsc(Long topicId, ContentStatus status);
    List<QuestionEntity> findAllByIdIn(Collection<Long> ids);
    boolean existsByTopicIdAndStatusNot(Long topicId, ContentStatus status);
}
