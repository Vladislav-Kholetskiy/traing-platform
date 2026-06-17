package com.vladislav.training.platform.content.repository;

import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Topic;
import java.util.List;

/**
 * Контракт репозитория {@code TopicRepository}.
 */
public interface TopicRepository {

    Topic findTopicById(Long topicId);

    List<Topic> findTopicsByCourseId(Long courseId);

    List<Topic> findTopicsByCourseIdAndStatus(Long courseId, ContentStatus status);

    boolean existsNonArchivedByCourseId(Long courseId);

    Topic saveTopic(Topic topic);
}
