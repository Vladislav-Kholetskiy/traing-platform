package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.content.domain.Topic;

/**
 * Контракт сервиса {@code TopicLifecycleService}.
 */
public interface TopicLifecycleService {

    Topic publish(Long topicId);

    Topic archive(Long topicId);
}
