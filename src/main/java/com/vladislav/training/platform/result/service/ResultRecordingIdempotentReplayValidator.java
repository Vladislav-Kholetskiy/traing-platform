package com.vladislav.training.platform.result.service;

import com.vladislav.training.platform.result.domain.Result;
import com.vladislav.training.platform.result.domain.ResultScoringSnapshot;
import java.math.BigDecimal;
import java.util.Objects;
import org.springframework.stereotype.Component;
/**
 * Проверка {@code ResultRecordingIdempotentReplayValidator}.
 */
@Component
class ResultRecordingIdempotentReplayValidator {

    boolean isIdenticalReplay(Result existingResult, Result replayCandidate) {
        Objects.requireNonNull(existingResult, "existingResult must not be null");
        Objects.requireNonNull(replayCandidate, "replayCandidate must not be null");

        return Objects.equals(existingResult.testAttemptId(), replayCandidate.testAttemptId())
            && Objects.equals(existingResult.userIdSnapshot(), replayCandidate.userIdSnapshot())
            && existingResult.attemptMode() == replayCandidate.attemptMode()
            && Objects.equals(existingResult.assignmentId(), replayCandidate.assignmentId())
            && Objects.equals(existingResult.assignmentTestId(), replayCandidate.assignmentTestId())
            && sameScoringSnapshot(existingResult.scoringSnapshot(), replayCandidate.scoringSnapshot())
            && Objects.equals(existingResult.withinDeadline(), replayCandidate.withinDeadline())
            && Objects.equals(existingResult.countedInAssignment(), replayCandidate.countedInAssignment())
            && Objects.equals(existingResult.completedAt(), replayCandidate.completedAt())
            && Objects.equals(existingResult.orgContextSnapshot(), replayCandidate.orgContextSnapshot())
            && existingResult.snapshotFinalTopicControlFlag() == replayCandidate.snapshotFinalTopicControlFlag();
    }

    private boolean sameScoringSnapshot(ResultScoringSnapshot existing, ResultScoringSnapshot replayCandidate) {
        return comparable(existing.thresholdPercent(), replayCandidate.thresholdPercent())
            && comparable(existing.earnedScore(), replayCandidate.earnedScore())
            && comparable(existing.maxScore(), replayCandidate.maxScore())
            && comparable(existing.scorePercent(), replayCandidate.scorePercent())
            && existing.passed() == replayCandidate.passed()
            && Objects.equals(existing.scoringPolicyCode(), replayCandidate.scoringPolicyCode())
            && Objects.equals(existing.scoringPolicySnapshot(), replayCandidate.scoringPolicySnapshot());
    }

    private boolean comparable(BigDecimal left, BigDecimal right) {
        return left.compareTo(right) == 0;
    }
}
