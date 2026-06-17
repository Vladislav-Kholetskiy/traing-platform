package com.vladislav.training.platform.result.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.common.model.AttemptMode;
import java.math.BigDecimal;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет договорённости вокруг {@code SelfHistoricalResultSummaryDto}.
 * Тест помогает сохранить предсказуемое поведение.
 */
class SelfHistoricalResultSummaryDtoContractTest {

    private static final Instant FIXED_RECORDED_AT = Instant.parse("2026-04-24T16:30:00Z");

    @Test
    void selfHistoricalResultSummaryDtoKeepsFrontendUsefulImmutableSelfHistoryShape() {
        SelfHistoricalResultSummaryDto dto = new SelfHistoricalResultSummaryDto(
            7001L,
            FIXED_RECORDED_AT,
            9101L,
            501L,
            "Final Test",
            new BigDecimal("85.0000"),
            new BigDecimal("17.0000"),
            true,
            AttemptMode.ASSIGNED,
            3001L
        );

        assertThat(dto.resultId()).isEqualTo(7001L);
        assertThat(dto.recordedAt()).isEqualTo(FIXED_RECORDED_AT);
        assertThat(dto.testAttemptId()).isEqualTo(9101L);
        assertThat(dto.testId()).isEqualTo(501L);
        assertThat(dto.testName()).isEqualTo("Final Test");
        assertThat(dto.scorePercent()).isEqualByComparingTo("85.0000");
        assertThat(dto.score()).isEqualByComparingTo("17.0000");
        assertThat(dto.passed()).isTrue();
        assertThat(dto.attemptMode()).isEqualTo(AttemptMode.ASSIGNED);
        assertThat(dto.assignmentId()).isEqualTo(3001L);
        assertThat(componentNames(SelfHistoricalResultSummaryDto.class))
            .containsExactly(
                "resultId",
                "recordedAt",
                "testAttemptId",
                "testId",
                "testName",
                "scorePercent",
                "score",
                "passed",
                "attemptMode",
                "assignmentId"
            );
    }

    @Test
    void selfHistoricalResultSummaryDtoDoesNotAccumulateInternalScoringAttemptRecoveryManagerOrAnalyticsFields() {
        assertThat(componentNames(SelfHistoricalResultSummaryDto.class))
            .doesNotContain(
                "attemptStatus",
                "currentAttemptId",
                "isCorrect",
                "correctAnswers",
                "scoringPolicyCode",
                "scoringPolicySnapshot",
                "thresholdPercent",
                "maxScore",
                "assignmentStatus",
                "managerUserId",
                "organizationalUnitCurrentName",
                "questionDifficulty",
                "rebuildStatus"
            );
    }

    private List<String> componentNames(Class<?> type) {
        return Arrays.stream(type.getRecordComponents())
            .map(RecordComponent::getName)
            .toList();
    }
}
