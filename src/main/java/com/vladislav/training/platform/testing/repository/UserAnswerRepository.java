package com.vladislav.training.platform.testing.repository;

import com.vladislav.training.platform.testing.domain.UserAnswer;
import java.util.List;

/**
 * Контракт репозитория {@code UserAnswerRepository}.
 */
public interface UserAnswerRepository {

    UserAnswer findUserAnswerById(Long userAnswerId);

    List<UserAnswer> findUserAnswersByTestAttemptId(Long testAttemptId);

    UserAnswer findUserAnswerByTestAttemptIdAndQuestionId(Long testAttemptId, Long questionId);

    UserAnswer saveUserAnswer(UserAnswer userAnswer);
}
