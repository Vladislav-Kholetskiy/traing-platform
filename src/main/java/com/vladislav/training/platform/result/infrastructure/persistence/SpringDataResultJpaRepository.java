package com.vladislav.training.platform.result.infrastructure.persistence;

import com.vladislav.training.platform.common.model.AttemptMode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Контракт репозитория {@code SpringDataResultJpaRepository}.
 */
public interface SpringDataResultJpaRepository extends JpaRepository<ResultEntity, Long> {

    interface ResultHistorySummaryRowView {

        Long getResultId();

        java.time.Instant getRecordedAt();

        Long getTestAttemptId();

        Long getTestId();

        String getTestName();

        BigDecimal getScorePercent();

        BigDecimal getScore();

        boolean isPassed();

        AttemptMode getAttemptMode();

        Long getAssignmentId();
    }

    Optional<ResultEntity> findByTestAttemptId(Long testAttemptId);

    List<ResultEntity> findAllByUserIdSnapshotOrderByCompletedAtDescIdDesc(Long userIdSnapshot);

    @Query(
        """
            select
                r.id as resultId,
                r.completedAt as recordedAt,
                r.testAttemptId as testAttemptId,
                r.testIdSnapshot as testId,
                r.testNameSnapshot as testName,
                r.scorePercent as scorePercent,
                r.earnedScore as score,
                r.passed as passed,
                r.attemptMode as attemptMode,
                r.assignmentId as assignmentId
            from ResultEntity r
            where r.userIdSnapshot = :userIdSnapshot
            order by r.completedAt desc, r.id desc
            """
    )
    List<ResultHistorySummaryRowView> findResultHistorySummaryRowsByUserIdSnapshotOrderByCompletedAtDescIdDesc(
        @Param("userIdSnapshot") Long userIdSnapshot
    );

    List<ResultEntity> findAllByAssignmentIdOrderByIdAsc(Long assignmentId);

    List<ResultEntity> findAllByAssignmentTestIdOrderByIdAsc(Long assignmentTestId);
}
