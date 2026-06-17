package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.content.domain.Test;
import com.vladislav.training.platform.content.domain.TestQuestion;

/**
 * Контракт командного сервиса {@code TestCommandService}.
 */
public interface TestCommandService {

    Test createTest(CreateTestCommand command);

    Test updateTest(Long testId, UpdateTestCommand command);

    TestQuestion createTestQuestion(Long testId, CreateTestQuestionCommand command);

    TestQuestion updateTestQuestion(Long testId, Long testQuestionId, UpdateTestQuestionCommand command);

    void deleteTestQuestion(Long testId, Long testQuestionId);
}
