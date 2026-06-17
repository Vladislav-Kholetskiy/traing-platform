package com.vladislav.training.platform.result.domain;

import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import java.time.Instant;
import java.util.Objects;
/**
 * Класс {@code ResultSnapshotAssembler}.
 */
public final class ResultSnapshotAssembler {

    public Result assemble(TestAttempt terminalizedAttempt, ResultSnapshotFacts snapshotFacts) {
        Objects.requireNonNull(terminalizedAttempt, "terminalizedAttempt must not be null");
        Objects.requireNonNull(snapshotFacts, "snapshotFacts must not be null");

        return new Result(
            null,
            terminalizedAttempt.id(),
            terminalizedAttempt.userId(),
            terminalizedAttempt.attemptMode(),
            snapshotFacts.assignmentId(),
            snapshotFacts.assignmentTestId(),
            snapshotFacts.testIdSnapshot(),
            snapshotFacts.testNameSnapshot(),
            snapshotFacts.scoringSnapshot(),
            snapshotFacts.withinDeadline(),
            snapshotFacts.countedInAssignment(),
            terminalInstantOf(terminalizedAttempt),
            snapshotFacts.orgContextSnapshot(),
            snapshotFacts.snapshotFinalTopicControlFlag(),
            snapshotFacts.recordedAt()
        );
    }

    private Instant terminalInstantOf(TestAttempt attempt) {
        return switch (attempt.status()) {
            case COMPLETED -> requiredTerminalInstant(attempt.completedAt(), attempt.status());
            case EXPIRED -> requiredTerminalInstant(attempt.expiredAt(), attempt.status());
            case ABANDONED -> requiredTerminalInstant(attempt.abandonedAt(), attempt.status());
            case STARTED, IN_PROGRESS -> throw new IllegalArgumentException(
                "Result snapshot assembly requires a terminalized attempt"
            );
        };
    }

    private Instant requiredTerminalInstant(Instant terminalInstant, TestAttemptStatus status) {
        if (terminalInstant == null) {
            throw new IllegalArgumentException(
                "Terminalized attempt must expose terminal timestamp for status " + status
            );
        }
        return terminalInstant;
    }
}
