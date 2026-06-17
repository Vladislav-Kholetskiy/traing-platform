package com.vladislav.training.platform.result.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.result.query.internal.SelfHistoricalResultReader.SelfHistoricalResultReadRow;
import com.vladislav.training.platform.result.query.internal.SelfHistoricalResultReader.SelfHistoricalResultReadCriteria;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет чтение данных в {@code JpaSelfHistoricalResult}.
 * Тест держит под контролем выборку и состав возвращаемых данных.
 */
@ExtendWith(MockitoExtension.class)
class JpaSelfHistoricalResultReaderTest {

    private static final Instant FIRST_COMPLETED_AT = Instant.parse("2026-04-20T08:00:00Z");
    private static final Instant SECOND_COMPLETED_AT = Instant.parse("2026-04-21T09:30:00Z");

    @Mock
    private SpringDataResultJpaRepository repository;

    @Test
    void findSelfHistoricalResultRowsReadsByActorUserIdSnapshotAndMapsRepositoryRows() {
        JpaSelfHistoricalResultReader seam = new JpaSelfHistoricalResultReader(repository);
        when(repository.findResultHistorySummaryRowsByUserIdSnapshotOrderByCompletedAtDescIdDesc(101L))
            .thenReturn(List.of(
                historyRow(7002L, 9102L, 502L, "Assigned Test", new BigDecimal("80.0000"), new BigDecimal("8.0000"), true, AttemptMode.ASSIGNED, 3001L, SECOND_COMPLETED_AT),
                historyRow(7001L, 9101L, 501L, "Self Test", new BigDecimal("40.0000"), new BigDecimal("4.0000"), false, AttemptMode.SELF, null, FIRST_COMPLETED_AT)
            ));

        List<SelfHistoricalResultReadRow> rows = seam.findSelfHistoricalResultRows(new SelfHistoricalResultReadCriteria(101L));

        assertThat(rows).containsExactly(
            new SelfHistoricalResultReadRow(
                7002L,
                SECOND_COMPLETED_AT,
                9102L,
                502L,
                "Assigned Test",
                new BigDecimal("80.0000"),
                new BigDecimal("8.0000"),
                true,
                AttemptMode.ASSIGNED,
                3001L
            ),
            new SelfHistoricalResultReadRow(
                7001L,
                FIRST_COMPLETED_AT,
                9101L,
                501L,
                "Self Test",
                new BigDecimal("40.0000"),
                new BigDecimal("4.0000"),
                false,
                AttemptMode.SELF,
                null
            )
        );
        verify(repository).findResultHistorySummaryRowsByUserIdSnapshotOrderByCompletedAtDescIdDesc(101L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void findSelfHistoricalResultRowsReturnsEmptyListWhenRepositoryReturnsNoRows() {
        JpaSelfHistoricalResultReader seam = new JpaSelfHistoricalResultReader(repository);
        when(repository.findResultHistorySummaryRowsByUserIdSnapshotOrderByCompletedAtDescIdDesc(101L))
            .thenReturn(List.of());

        List<SelfHistoricalResultReadRow> rows = seam.findSelfHistoricalResultRows(new SelfHistoricalResultReadCriteria(101L));

        assertThat(rows).isEmpty();
        verify(repository).findResultHistorySummaryRowsByUserIdSnapshotOrderByCompletedAtDescIdDesc(101L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void seamDoesNotDependOnAttemptOrCurrentAttemptRecoveryPaths() throws IOException {
        String seamSource = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/result/infrastructure/persistence/JpaSelfHistoricalResultReader.java"
        ));

        assertThat(seamSource)
            .doesNotContain("TestAttemptEntity")
            .doesNotContain("TestAttemptRepository")
            .doesNotContain("SpringDataTestAttemptJpaRepository")
            .doesNotContain("JpaTestAttemptRepositoryAdapter")
            .doesNotContain("SelfCurrentAttemptReadService")
            .doesNotContain("AssignedCurrentAttemptReadService")
            .doesNotContain("ActiveAttemptOwnerLocalReadService")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("findCurrentAssignedAttemptForActor(")
            .doesNotContain("findCurrentSelfAttemptForActor(")
            .doesNotContain("findActiveAssignedAttemptForActor(")
            .doesNotContain("findActiveSelfAttempt(")
            .doesNotContain("AttemptMode.SELF")
            .contains("findResultHistorySummaryRowsByUserIdSnapshotOrderByCompletedAtDescIdDesc");
    }

    private SpringDataResultJpaRepository.ResultHistorySummaryRowView historyRow(
        Long resultId,
        Long testAttemptId,
        Long testId,
        String testName,
        BigDecimal scorePercent,
        BigDecimal score,
        boolean passed,
        AttemptMode attemptMode,
        Long assignmentId,
        Instant recordedAt
    ) {
        return new SpringDataResultJpaRepository.ResultHistorySummaryRowView() {
            @Override
            public Long getResultId() {
                return resultId;
            }

            @Override
            public Instant getRecordedAt() {
                return recordedAt;
            }

            @Override
            public Long getTestAttemptId() {
                return testAttemptId;
            }

            @Override
            public Long getTestId() {
                return testId;
            }

            @Override
            public String getTestName() {
                return testName;
            }

            @Override
            public BigDecimal getScorePercent() {
                return scorePercent;
            }

            @Override
            public BigDecimal getScore() {
                return score;
            }

            @Override
            public boolean isPassed() {
                return passed;
            }

            @Override
            public AttemptMode getAttemptMode() {
                return attemptMode;
            }

            @Override
            public Long getAssignmentId() {
                return assignmentId;
            }
        };
    }
}


