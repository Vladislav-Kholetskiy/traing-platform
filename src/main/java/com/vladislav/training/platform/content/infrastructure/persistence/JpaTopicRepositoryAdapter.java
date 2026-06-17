package com.vladislav.training.platform.content.infrastructure.persistence;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Topic;
import com.vladislav.training.platform.content.repository.TopicRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Адаптер репозитория {@code JpaTopicRepositoryAdapter}.
 */
@Repository
@Transactional(readOnly = true)
public class JpaTopicRepositoryAdapter implements TopicRepository {
    private final SpringDataTopicJpaRepository repository; private final ContentMapper mapper;
    public JpaTopicRepositoryAdapter(SpringDataTopicJpaRepository repository, ContentMapper mapper){this.repository=repository;this.mapper=mapper;}
    @Override public Topic findTopicById(Long topicId){ return repository.findById(topicId).map(mapper::toDomain).orElseThrow(() -> new NotFoundException("Topic not found: "+topicId)); }
    @Override public List<Topic> findTopicsByCourseId(Long courseId){ return mapper.toTopics(repository.findAllByCourseIdOrderBySortOrderAscIdAsc(courseId)); }
    @Override public List<Topic> findTopicsByCourseIdAndStatus(Long courseId, ContentStatus status){ return mapper.toTopics(repository.findAllByCourseIdAndStatusOrderBySortOrderAscIdAsc(courseId, status)); }
    @Override public boolean existsNonArchivedByCourseId(Long courseId){ return repository.existsByCourseIdAndStatusNot(courseId, ContentStatus.ARCHIVED); }
    @Override @Transactional public Topic saveTopic(Topic topic){ try { return mapper.toDomain(repository.save(mapper.toEntity(topic))); } catch (DataIntegrityViolationException ex){ throw new PersistenceConstraintViolationException("Failed to persist topic", ex); } }
}
