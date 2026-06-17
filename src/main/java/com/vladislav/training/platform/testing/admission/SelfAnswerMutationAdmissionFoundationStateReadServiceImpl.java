package com.vladislav.training.platform.testing.admission;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.repository.TestAttemptRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса {@code SelfAnswerMutationAdmissionFoundationStateReadServiceImpl}.
 */
@Service
@Transactional(readOnly = true)
@ConditionalOnBean(TestAttemptRepository.class)
class SelfAnswerMutationAdmissionFoundationStateReadServiceImpl
    implements SelfAnswerMutationAdmissionFoundationStateReadService {

    private final TestAttemptRepository testAttemptRepository;

    SelfAnswerMutationAdmissionFoundationStateReadServiceImpl(TestAttemptRepository testAttemptRepository) {
        this.testAttemptRepository = testAttemptRepository;
    }

    @Override
    public SelfAnswerMutationAdmissionFoundationState findSelfAnswerMutationAdmissionFoundationState(
        Long actorUserId,
        Long testAttemptId
    ) {
        requireId(actorUserId, "actorUserId");
        requireId(testAttemptId, "testAttemptId");

        TestAttempt attempt = findAttemptOrNull(testAttemptId);
        if (attempt == null
            || attempt.attemptMode() != AttemptMode.SELF
            || attempt.assignmentTestId() != null
            || !actorUserId.equals(attempt.userId())) {
            return null;
        }
        return new SelfAnswerMutationAdmissionFoundationState(attempt.id(), attempt.testId());
    }

    private Long requireId(Long value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return value;
    }

    private TestAttempt findAttemptOrNull(Long testAttemptId) {
        try {
            return testAttemptRepository.findTestAttemptById(testAttemptId);
        } catch (NotFoundException exception) {
            return null;
        }
    }
}
