package com.vladislav.training.platform.result.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.result.domain.Result;
import com.vladislav.training.platform.result.domain.ResultOrgContextSnapshot;
import com.vladislav.training.platform.result.domain.ResultScoringSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
/**
 * Проверяет логику валидации в {@code CountedAssignmentResult}.
 * Тест страхует правила проверки, на которых держится поведение.
 */
class CountedAssignmentResultValidatorTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-23T09:00:00Z");

    private CountedAssignmentResultValidityGate gate;

    @BeforeEach
    void setUp() {
        gate = new CountedAssignmentResultValidator();
    }

    @Test
    void allowsOnlyValidCountedAssignedResult() {
        assertThat(gate.allowsAssignmentCountedHandoff(validCountedAssignedResult())).isTrue();
    }

    @Test
    void rejectsSelfResult() {
        assertThat(gate.allowsAssignmentCountedHandoff(selfResult())).isFalse();
    }

    @Test
    void rejectsNonCountedAssignedResult() {
        assertThat(gate.allowsAssignmentCountedHandoff(assignedResult(false, true, true))).isFalse();
    }

    @Test
    void rejectsOverdueAssignedResult() {
        assertThat(gate.allowsAssignmentCountedHandoff(assignedResult(true, false, true))).isFalse();
    }

    @Test
    void rejectsFailedAssignedResult() {
        assertThat(gate.allowsAssignmentCountedHandoff(assignedResult(true, true, false))).isFalse();
    }

    private Result validCountedAssignedResult() {
        return assignedResult(true, true, true);
    }

    private Result assignedResult(boolean countedInAssignment, boolean withinDeadline, boolean passed) {
        return new Result(
            701L,
            801L,
            1801L,
            AttemptMode.ASSIGNED,
            901L,
            1001L,
            1101L,
            "Assigned Test",
            new ResultScoringSnapshot(
                new BigDecimal(passed ? "80.00" : "60.00"),
                new BigDecimal(passed ? "8.00" : "6.00"),
                new BigDecimal("10.00"),
                new BigDecimal(passed ? "80.00" : "60.00"),
                passed,
                "DEFAULT_POLICY",
                "{\"policy\":\"v1\"}"
            ),
            withinDeadline,
            countedInAssignment,
            FIXED_INSTANT.minusSeconds(60),
            new ResultOrgContextSnapshot(901L, "/company/ops"),
            true,
            FIXED_INSTANT
        );
    }

    private Result selfResult() {
        return new Result(
            702L,
            802L,
            1802L,
            AttemptMode.SELF,
            null,
            null,
            1102L,
            "Self Test",
            new ResultScoringSnapshot(
                new BigDecimal("80.00"),
                new BigDecimal("8.00"),
                new BigDecimal("10.00"),
                new BigDecimal("80.00"),
                true,
                "SELF_POLICY",
                "{\"policy\":\"self\"}"
            ),
            null,
            null,
            FIXED_INSTANT.minusSeconds(60),
            new ResultOrgContextSnapshot(902L, "/company/self"),
            false,
            FIXED_INSTANT
        );
    }
}


