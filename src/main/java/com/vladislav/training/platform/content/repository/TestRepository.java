package com.vladislav.training.platform.content.repository;

import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Test;
import com.vladislav.training.platform.content.domain.TestType;
import java.util.List;
import java.util.Optional;

/**
 * Контракт репозитория {@code TestRepository}.
 */
public interface TestRepository {

    Test findTestById(Long testId);

    Test lockTestById(Long testId);

    List<Test> findTestsByTopicId(Long topicId);

    List<Test> findTestsByTopicIdAndStatus(Long topicId, ContentStatus status);

    List<Test> findTestsByTopicIdAndStatusAndType(Long topicId, ContentStatus status, TestType testType);

    List<Test> findEligibleFinalControlTestsByTopicId(Long topicId);

    Optional<Test> findActiveFinalTestByTopicId(Long topicId);

    Optional<Test> findActiveFinalTestByTopicIdForUpdate(Long topicId);

    boolean existsNonArchivedByTopicId(Long topicId);

    Test saveTest(Test test);
}
