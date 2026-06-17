package com.vladislav.training.platform.content.repository;

import com.vladislav.training.platform.content.domain.TestQuestion;
import java.util.List;

/**
 * Контракт репозитория {@code TestQuestionRepository}.
 */
public interface TestQuestionRepository {

    TestQuestion findTestQuestionById(Long testQuestionId);

    List<TestQuestion> findTestQuestionsByTestId(Long testId);

    boolean existsPublishedTestUsingQuestion(Long questionId);

    TestQuestion saveTestQuestion(TestQuestion testQuestion);

    void deleteTestQuestion(Long testQuestionId);
}
