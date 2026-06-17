package com.vladislav.training.platform.testing.infrastructure.persistence;

import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Контракт репозитория {@code SpringDataTestAttemptJpaRepository}.
 */
public interface SpringDataTestAttemptJpaRepository extends JpaRepository<TestAttemptEntity, Long> {

    List<TestAttemptEntity> findAllByUserIdOrderByIdAsc(Long userId);

    List<TestAttemptEntity> findAllByAssignmentTestIdOrderByIdAsc(Long assignmentTestId);

    List<TestAttemptEntity> findAllByUserIdAndTestIdOrderByIdAsc(Long userId, Long testId);

    Optional<TestAttemptEntity> findByUserIdAndAssignmentTestIdAndAttemptModeAndStatusIn(
        Long userId,
        Long assignmentTestId,
        AttemptMode attemptMode,
        Collection<TestAttemptStatus> statuses
    );

    Optional<TestAttemptEntity> findByUserIdAndTestIdAndAttemptModeAndStatusIn(
        Long userId,
        Long testId,
        AttemptMode attemptMode,
        Collection<TestAttemptStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ta from TestAttemptEntity ta where ta.id = :testAttemptId")
    Optional<TestAttemptEntity> findByIdForUpdate(@Param("testAttemptId") Long testAttemptId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select ta from TestAttemptEntity ta
        where ta.id = :testAttemptId
          and ta.userId = :userId
        """)
    Optional<TestAttemptEntity> findByIdAndUserIdForUpdate(
        @Param("testAttemptId") Long testAttemptId,
        @Param("userId") Long userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select ta from TestAttemptEntity ta
        where ta.id = :testAttemptId
          and ta.userId = :userId
          and ta.assignmentTestId = :assignmentTestId
          and ta.attemptMode = com.vladislav.training.platform.common.model.AttemptMode.ASSIGNED
        """)
    Optional<TestAttemptEntity> findByIdAndUserIdAndAssignmentTestIdForUpdate(
        @Param("testAttemptId") Long testAttemptId,
        @Param("userId") Long userId,
        @Param("assignmentTestId") Long assignmentTestId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select ta from TestAttemptEntity ta
        where ta.userId = :userId
          and ta.assignmentTestId = :assignmentTestId
          and ta.attemptMode = com.vladislav.training.platform.common.model.AttemptMode.ASSIGNED
          and ta.status in :statuses
        """)
    Optional<TestAttemptEntity> findByUserIdAndAssignmentTestIdAndStatusInForUpdate(
        @Param("userId") Long userId,
        @Param("assignmentTestId") Long assignmentTestId,
        @Param("statuses") Collection<TestAttemptStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select ta from TestAttemptEntity ta
        where ta.userId = :userId
          and ta.testId = :testId
          and ta.attemptMode = :attemptMode
          and ta.status in :statuses
        """)
    Optional<TestAttemptEntity> findByUserIdAndTestIdAndAttemptModeAndStatusInForUpdate(
        @Param("userId") Long userId,
        @Param("testId") Long testId,
        @Param("attemptMode") AttemptMode attemptMode,
        @Param("statuses") Collection<TestAttemptStatus> statuses
    );
}
