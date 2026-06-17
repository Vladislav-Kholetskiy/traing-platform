package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Topic;
import java.util.List;

/**
 * Контракт сервиса чтения {@code TopicQueryService}.
 */
public interface TopicQueryService {

    Topic findTopicById(Long topicId);

    List<Topic> findTopicsByCourseId(Long courseId);

    List<Topic> findTopicsByCourseIdAndStatus(Long courseId, ContentStatus status);
}
