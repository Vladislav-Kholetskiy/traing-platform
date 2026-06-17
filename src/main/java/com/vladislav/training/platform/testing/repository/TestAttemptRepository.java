package com.vladislav.training.platform.testing.repository;

import com.vladislav.training.platform.testing.domain.TestAttempt;
import java.util.List;

/**
 * Контракт репозитория {@code TestAttemptRepository}.
 */
public interface TestAttemptRepository {

    TestAttempt findTestAttemptById(Long testAttemptId);

    List<TestAttempt> findTestAttemptsByUserId(Long userId);

    List<TestAttempt> findTestAttemptsByAssignmentTestId(Long assignmentTestId);

    List<TestAttempt> findTestAttemptsByUserIdAndTestId(Long userId, Long testId);

    TestAttempt findActiveAssignedAttemptForActor(Long userId, Long assignmentTestId);

    TestAttempt findActiveSelfAttempt(Long userId, Long testId);

    TestAttempt findAndLockTestAttemptById(Long testAttemptId);

    TestAttempt findAndLockTestAttemptByIdAndUserId(Long testAttemptId, Long userId);

    TestAttempt findAndLockAssignedAttemptByIdAndUserIdAndAssignmentTestId(Long testAttemptId, Long userId, Long assignmentTestId);

    TestAttempt findAndLockActiveAssignedAttemptForActor(Long userId, Long assignmentTestId);

    TestAttempt findAndLockActiveSelfAttempt(Long userId, Long testId);

    TestAttempt saveTestAttempt(TestAttempt testAttempt);
}
