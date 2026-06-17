package com.vladislav.training.platform.result.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code ResultSnapshotAssembler}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class ResultSnapshotAssemblerTest {

    private final ResultSnapshotAssembler assembler = new ResultSnapshotAssembler();

    @Test
    void assemblesImmutableAssignedSnapshotFromTerminalizedAttemptAndExplicitSnapshotFacts() {
        TestAttempt terminalizedAttempt = new TestAttempt(
            101L,
            501L,
            601L,
            701L,
            AttemptMode.ASSIGNED,
            TestAttemptStatus.COMPLETED,
            Instant.parse("2026-04-10T08:00:00Z"),
            Instant.parse("2026-04-10T08:30:00Z"),
            null,
            null,
            Instant.parse("2026-04-10T08:29:00Z"),
            Instant.parse("2026-04-10T08:00:00Z"),
            Instant.parse("2026-04-10T08:30:00Z")
        );
        ResultSnapshotFacts snapshotFacts = new ResultSnapshotFacts(
            801L,
            701L,
            601L,
            "Assigned Test",
            new ResultScoringSnapshot(
                new BigDecimal("80.00"),
                new BigDecimal("9.00"),
                new BigDecimal("10.00"),
                new BigDecimal("90.00"),
                true,
                "STANDARD",
                "{\"threshold\":80}"
            ),
            true,
            true,
            new ResultOrgContextSnapshot(901L, "/company/ops/shift-a"),
            true,
            Instant.parse("2026-04-10T08:31:00Z")
        );

        Result snapshot = assembler.assemble(terminalizedAttempt, snapshotFacts);

        assertThat(snapshot).isEqualTo(new Result(
            null,
            101L,
            501L,
            AttemptMode.ASSIGNED,
            801L,
            701L,
            601L,
            "Assigned Test",
            snapshotFacts.scoringSnapshot(),
            true,
            true,
            Instant.parse("2026-04-10T08:30:00Z"),
            snapshotFacts.orgContextSnapshot(),
            true,
            Instant.parse("2026-04-10T08:31:00Z")
        ));
    }

    @Test
    void assembledSnapshotIsSelfContainedAndDoesNotNeedMutableCurrentStateOverlays() {
        TestAttempt terminalizedAttempt = new TestAttempt(
            102L,
            502L,
            602L,
            null,
            AttemptMode.SELF,
            TestAttemptStatus.ABANDONED,
            Instant.parse("2026-04-11T09:00:00Z"),
            null,
            null,
            Instant.parse("2026-04-11T09:07:00Z"),
            Instant.parse("2026-04-11T09:06:30Z"),
            Instant.parse("2026-04-11T09:00:00Z"),
            Instant.parse("2026-04-11T09:07:00Z")
        );
        ResultSnapshotFacts snapshotFacts = new ResultSnapshotFacts(
            null,
            null,
            602L,
            "Self Test",
            new ResultScoringSnapshot(
                new BigDecimal("70.00"),
                new BigDecimal("2.00"),
                new BigDecimal("10.00"),
                new BigDecimal("20.00"),
                false,
                "SELF_STANDARD",
                "{\"threshold\":70}"
            ),
            null,
            null,
            new ResultOrgContextSnapshot(902L, "/company/self"),
            false,
            Instant.parse("2026-04-11T09:08:00Z")
        );

        Result snapshot = assembler.assemble(terminalizedAttempt, snapshotFacts);

        assertThat(snapshot.testAttemptId()).isEqualTo(102L);
        assertThat(snapshot.userIdSnapshot()).isEqualTo(502L);
        assertThat(snapshot.completedAt()).isEqualTo(Instant.parse("2026-04-11T09:07:00Z"));
        assertThat(snapshot.attemptMode()).isEqualTo(AttemptMode.SELF);
        assertThat(snapshot.assignmentId()).isNull();
        assertThat(snapshot.assignmentTestId()).isNull();
        assertThat(snapshot.testIdSnapshot()).isEqualTo(602L);
        assertThat(snapshot.testNameSnapshot()).isEqualTo("Self Test");
        assertThat(snapshot.withinDeadline()).isNull();
        assertThat(snapshot.countedInAssignment()).isNull();
        assertThat(snapshot.scoringSnapshot()).isSameAs(snapshotFacts.scoringSnapshot());
        assertThat(snapshot.orgContextSnapshot()).isSameAs(snapshotFacts.orgContextSnapshot());
    }

    @Test
    void failsClosedWhenAttemptIsNotTerminalized() {
        TestAttempt activeAttempt = new TestAttempt(
            103L,
            503L,
            603L,
            703L,
            AttemptMode.ASSIGNED,
            TestAttemptStatus.IN_PROGRESS,
            Instant.parse("2026-04-12T10:00:00Z"),
            null,
            null,
            null,
            Instant.parse("2026-04-12T10:04:00Z"),
            Instant.parse("2026-04-12T10:00:00Z"),
            Instant.parse("2026-04-12T10:04:00Z")
        );

        assertThatThrownBy(() -> assembler.assemble(activeAttempt, assignedSnapshotFacts()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("terminalized attempt");
    }

    @Test
    void failsClosedWhenTerminalizedAttemptDoesNotExposeItsTerminalTimestamp() {
        TestAttempt brokenAttempt = new TestAttempt(
            104L,
            504L,
            604L,
            704L,
            AttemptMode.ASSIGNED,
            TestAttemptStatus.EXPIRED,
            Instant.parse("2026-04-13T10:00:00Z"),
            null,
            null,
            null,
            Instant.parse("2026-04-13T10:05:00Z"),
            Instant.parse("2026-04-13T10:00:00Z"),
            Instant.parse("2026-04-13T10:05:00Z")
        );

        assertThatThrownBy(() -> assembler.assemble(brokenAttempt, assignedSnapshotFacts()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("terminal timestamp")
            .hasMessageContaining("EXPIRED");
    }

    private ResultSnapshotFacts assignedSnapshotFacts() {
        return new ResultSnapshotFacts(
            804L,
            704L,
            604L,
            "Assigned Test",
            new ResultScoringSnapshot(
                new BigDecimal("80.00"),
                new BigDecimal("7.00"),
                new BigDecimal("10.00"),
                new BigDecimal("70.00"),
                false,
                "STANDARD",
                "{\"threshold\":80}"
            ),
            false,
            false,
            new ResultOrgContextSnapshot(904L, "/company/ops/shift-b"),
            false,
            Instant.parse("2026-04-13T10:06:00Z")
        );
    }
}
