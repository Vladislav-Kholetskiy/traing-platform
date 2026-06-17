package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Test;
import com.vladislav.training.platform.content.domain.TestQuestion;
import java.util.List;
import java.util.Optional;

/**
 * Контракт сервиса чтения {@code TestQueryService}.
 */
public interface TestQueryService {

    Test findTestById(Long testId);

    List<Test> findTestsByTopicId(Long topicId);

    List<Test> findTestsByTopicIdAndStatus(Long topicId, ContentStatus status);

    Optional<Test> findActiveFinalTestByTopicId(Long topicId);

    List<Test> findEligibleFinalControlTestsByTopicId(Long topicId);

    TestQuestion findTestQuestionById(Long testQuestionId);

    List<TestQuestion> findTestQuestionsByTestId(Long testId);
}
