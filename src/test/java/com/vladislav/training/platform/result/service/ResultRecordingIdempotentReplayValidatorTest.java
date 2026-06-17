package com.vladislav.training.platform.result.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.result.domain.Result;
import com.vladislav.training.platform.result.domain.ResultOrgContextSnapshot;
import com.vladislav.training.platform.result.domain.ResultScoringSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
/**
 * Проверяет логику валидации в {@code ResultRecordingIdempotentReplay}.
 * Тест страхует правила проверки, на которых держится поведение.
 */
class ResultRecordingIdempotentReplayValidatorTest {

    private final ResultRecordingIdempotentReplayValidator validator =
        new ResultRecordingIdempotentReplayValidator();

    @Test
    void acceptsIdenticalRootFactsReplay() {
        Result existing = assignedResult(501L, new BigDecimal("8.0000"), true, true, true, RECORDED_AT);
        Result replayCandidate = assignedResult(999L, new BigDecimal("8.0000"), true, true, true, RECORDED_AT);

        assertThat(validator.isIdenticalReplay(existing, replayCandidate)).isTrue();
    }

    @Test
    void doesNotTreatRecordedAtDifferenceAsPayloadMismatch() {
        Result existing = assignedResult(501L, new BigDecimal("8.0000"), true, true, true, RECORDED_AT);
        Result replayCandidate = assignedResult(999L, new BigDecimal("8.0000"), true, true, true, RECORDED_AT.plusSeconds(90));

        assertThat(validator.isIdenticalReplay(existing, replayCandidate)).isTrue();
    }

    @Test
    void rejectsReplayWhenAssignmentFactsDiffer() {
        Result existing = assignedResult(501L, new BigDecimal("8.0000"), true, true, true, RECORDED_AT);
        Result replayCandidate = new Result(
            999L,
            9001L,
            3001L,
            AttemptMode.ASSIGNED,
            7002L,
            8002L,
            5002L,
            "Assigned Test",
            existing.scoringSnapshot(),
            existing.withinDeadline(),
            existing.countedInAssignment(),
            existing.completedAt(),
            existing.orgContextSnapshot(),
            existing.snapshotFinalTopicControlFlag(),
            RECORDED_AT.plusSeconds(30)
        );

        assertThat(validator.isIdenticalReplay(existing, replayCandidate)).isFalse();
    }

    @Test
    void rejectsReplayWhenImmutableUserAnchorDiffers() {
        Result existing = assignedResult(501L, new BigDecimal("8.0000"), true, true, true, RECORDED_AT);
        Result replayCandidate = new Result(
            999L,
            9001L,
            3999L,
            AttemptMode.ASSIGNED,
            7001L,
            8001L,
            5001L,
            "Assigned Test",
            existing.scoringSnapshot(),
            existing.withinDeadline(),
            existing.countedInAssignment(),
            existing.completedAt(),
            existing.orgContextSnapshot(),
            existing.snapshotFinalTopicControlFlag(),
            RECORDED_AT.plusSeconds(30)
        );

        assertThat(validator.isIdenticalReplay(existing, replayCandidate)).isFalse();
    }

    @Test
    void rejectsReplayWhenDeadlineCountedOrFinalControlFactsDiffer() {
        Result existing = assignedResult(501L, new BigDecimal("8.0000"), true, true, true, RECORDED_AT);
        Result replayCandidate = assignedResult(999L, new BigDecimal("8.0000"), false, false, false, RECORDED_AT);

        assertThat(validator.isIdenticalReplay(existing, replayCandidate)).isFalse();
    }

    @Test
    void rejectsReplayWhenCanonicalScoringSnapshotDiffers() {
        Result existing = assignedResult(501L, new BigDecimal("8.0000"), true, true, true, RECORDED_AT);
        Result replayCandidate = assignedResult(999L, new BigDecimal("7.0000"), true, true, true, RECORDED_AT);

        assertThat(validator.isIdenticalReplay(existing, replayCandidate)).isFalse();
    }

    private Result assignedResult(
        Long id,
        BigDecimal earnedScore,
        Boolean withinDeadline,
        Boolean countedInAssignment,
        boolean finalControl,
        Instant recordedAt
    ) {
        return new Result(
            id,
            9001L,
            3001L,
            AttemptMode.ASSIGNED,
            7001L,
            8001L,
            5001L,
            "Assigned Test",
            new ResultScoringSnapshot(
                new BigDecimal("70.0000"),
                earnedScore,
                new BigDecimal("10.0000"),
                earnedScore.multiply(new BigDecimal("10.0000")),
                earnedScore.compareTo(new BigDecimal("7.0000")) >= 0,
                "STANDARD",
                "{\"policy\":\"v1\"}"
            ),
            withinDeadline,
            countedInAssignment,
            COMPLETED_AT,
            new ResultOrgContextSnapshot(501L, "/company/ops"),
            finalControl,
            recordedAt
        );
    }

    private static final Instant COMPLETED_AT = Instant.parse("2026-04-19T10:15:00Z");
    private static final Instant RECORDED_AT = Instant.parse("2026-04-19T10:16:00Z");
}
