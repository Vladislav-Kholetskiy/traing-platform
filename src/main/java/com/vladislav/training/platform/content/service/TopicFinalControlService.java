package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.content.domain.Test;
import java.util.Optional;

/**
 * Контракт сервиса {@code TopicFinalControlService}.
 */
public interface TopicFinalControlService {

    void assignActiveFinalTest(Long topicId, Long testId);

    void replaceActiveFinalTest(Long topicId, Long testId);

    void clearActiveFinalTest(Long topicId);

    Optional<Test> findActiveFinalTestByTopicId(Long topicId);
}
