package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.content.domain.Topic;

/**
 * Контракт командного сервиса {@code TopicCommandService}.
 */
public interface TopicCommandService {

    Topic createTopic(CreateTopicCommand command);

    Topic updateTopic(Long topicId, UpdateTopicCommand command);
}
