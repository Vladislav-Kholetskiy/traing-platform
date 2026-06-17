package com.vladislav.training.platform.testing.infrastructure.persistence;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import com.vladislav.training.platform.testing.repository.TestAttemptRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Адаптер репозитория {@code JpaTestAttemptRepositoryAdapter}.
 */
@Repository
@Transactional(readOnly = true)
public class JpaTestAttemptRepositoryAdapter implements TestAttemptRepository {

    private static final List<TestAttemptStatus> ACTIVE_STATUSES = List.of(
        TestAttemptStatus.STARTED,
        TestAttemptStatus.IN_PROGRESS
    );

    private final SpringDataTestAttemptJpaRepository repository;
    private final TestingPersistenceMapper mapper;

    public JpaTestAttemptRepositoryAdapter(
        SpringDataTestAttemptJpaRepository repository,
        TestingPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public TestAttempt findTestAttemptById(Long testAttemptId) {
        return repository.findById(testAttemptId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException("Test attempt not found: " + testAttemptId));
    }

    @Override
    public List<TestAttempt> findTestAttemptsByUserId(Long userId) {
        return mapper.toTestAttempts(repository.findAllByUserIdOrderByIdAsc(userId));
    }

    @Override
    public List<TestAttempt> findTestAttemptsByAssignmentTestId(Long assignmentTestId) {
        return mapper.toTestAttempts(repository.findAllByAssignmentTestIdOrderByIdAsc(assignmentTestId));
    }

    @Override
    public List<TestAttempt> findTestAttemptsByUserIdAndTestId(Long userId, Long testId) {
        return mapper.toTestAttempts(repository.findAllByUserIdAndTestIdOrderByIdAsc(userId, testId));
    }

    @Override
    public TestAttempt findActiveAssignedAttemptForActor(Long userId, Long assignmentTestId) {
        return repository.findByUserIdAndAssignmentTestIdAndAttemptModeAndStatusIn(
            userId,
            assignmentTestId,
            AttemptMode.ASSIGNED,
            ACTIVE_STATUSES
        )
            .map(mapper::toDomain)
            .orElse(null);
    }

    @Override
    public TestAttempt findActiveSelfAttempt(Long userId, Long testId) {
        return repository.findByUserIdAndTestIdAndAttemptModeAndStatusIn(
            userId,
            testId,
            AttemptMode.SELF,
            ACTIVE_STATUSES
        ).map(mapper::toDomain).orElse(null);
    }

    @Override
    @Transactional
    public TestAttempt findAndLockTestAttemptById(Long testAttemptId) {
        return repository.findByIdForUpdate(testAttemptId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException("Test attempt not found: " + testAttemptId));
    }

    @Override
    @Transactional
    public TestAttempt findAndLockTestAttemptByIdAndUserId(Long testAttemptId, Long userId) {
        return repository.findByIdAndUserIdForUpdate(testAttemptId, userId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException("Test attempt not found: " + testAttemptId));
    }

    @Override
    @Transactional
    public TestAttempt findAndLockAssignedAttemptByIdAndUserIdAndAssignmentTestId(
        Long testAttemptId,
        Long userId,
        Long assignmentTestId
    ) {
        return repository.findByIdAndUserIdAndAssignmentTestIdForUpdate(testAttemptId, userId, assignmentTestId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException("Test attempt not found: " + testAttemptId));
    }

    @Override
    @Transactional
    public TestAttempt findAndLockActiveAssignedAttemptForActor(Long userId, Long assignmentTestId) {
        return repository.findByUserIdAndAssignmentTestIdAndStatusInForUpdate(userId, assignmentTestId, ACTIVE_STATUSES)
            .map(mapper::toDomain)
            .orElse(null);
    }

    @Override
    @Transactional
    public TestAttempt findAndLockActiveSelfAttempt(Long userId, Long testId) {
        return repository.findByUserIdAndTestIdAndAttemptModeAndStatusInForUpdate(
            userId,
            testId,
            AttemptMode.SELF,
            ACTIVE_STATUSES
        ).map(mapper::toDomain).orElse(null);
    }

    @Override
    @Transactional
    public TestAttempt saveTestAttempt(TestAttempt testAttempt) {
        try {
            return mapper.toDomain(repository.save(mapper.toEntity(testAttempt)));
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConstraintViolationException("Failed to persist test_attempt", exception);
        }
    }
}
