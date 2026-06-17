package com.vladislav.training.platform.result.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.result.domain.ResultScoringSnapshot;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code AssignmentCountedResultPolicy}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class AssignmentCountedResultPolicyTest {

    private AssignmentCountedResultPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new AssignmentCountedResultPolicy();
    }

    @Test
    void decidesCountedForAssignedFinalControlPassedWithinDeadline() {
        AssignmentCountedResultDecision decision = policy.decide(true, scoringSnapshot(true), true);

        assertThat(decision.countedInAssignment()).isTrue();
        assertThat(decision.reason()).isEqualTo(AssignmentCountedResultDecisionReason.COUNTED);
    }

    @Test
    void rejectsFailedAssignedFinalControlResult() {
        AssignmentCountedResultDecision decision = policy.decide(true, scoringSnapshot(false), true);

        assertThat(decision.countedInAssignment()).isFalse();
        assertThat(decision.reason()).isEqualTo(AssignmentCountedResultDecisionReason.FAILED);
    }

    @Test
    void rejectsLateAssignedFinalControlResult() {
        AssignmentCountedResultDecision decision = policy.decide(true, scoringSnapshot(true), false);

        assertThat(decision.countedInAssignment()).isFalse();
        assertThat(decision.reason()).isEqualTo(AssignmentCountedResultDecisionReason.OUTSIDE_DEADLINE);
    }

    @Test
    void rejectsNonFinalControlAssignedResult() {
        AssignmentCountedResultDecision decision = policy.decide(false, scoringSnapshot(true), true);

        assertThat(decision.countedInAssignment()).isFalse();
        assertThat(decision.reason()).isEqualTo(AssignmentCountedResultDecisionReason.NOT_FINAL_CONTROL);
    }

    @Test
    void rejectsSelfResultForAssignmentCounting() {
        AssignmentCountedResultDecision decision = policy.decide(false, scoringSnapshot(true), true);

        assertThat(decision.countedInAssignment()).isFalse();
        assertThat(decision.reason()).isEqualTo(AssignmentCountedResultDecisionReason.NOT_FINAL_CONTROL);
    }

    private ResultScoringSnapshot scoringSnapshot(boolean passed) {
        return new ResultScoringSnapshot(
            new BigDecimal("80.0000"),
            new BigDecimal(passed ? "8.0000" : "6.0000"),
            new BigDecimal("10.0000"),
            new BigDecimal(passed ? "80.0000" : "60.0000"),
            passed,
            "STANDARD",
            "{\"source\":\"policy-test\"}"
        );
    }
}
