package com.vladislav.training.platform.analytics.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет договорённости вокруг {@code ExpertQuestionAnalyticsDto}.
 * Тест помогает сохранить предсказуемое поведение.
 */
class ExpertQuestionAnalyticsDtoContractTest {

    private static final Instant PERIOD_START = Instant.parse("2026-05-01T00:00:00Z");
    private static final Instant PERIOD_END = Instant.parse("2026-05-31T23:59:59Z");
    private static final Instant CALCULATED_AT = Instant.parse("2026-06-01T00:10:00Z");
    private static final Instant REFRESHED_AT = Instant.parse("2026-06-01T00:15:00Z");
    private static final BigDecimal AVERAGE_EARNED_SCORE = new BigDecimal("4.7500");

    @Test
    void dtoKeepsExactQuestionAggregateShape() {
        ExpertQuestionAnalyticsDto dto = new ExpertQuestionAnalyticsDto(
            701L,
            PERIOD_START,
            PERIOD_END,
            15,
            8,
            3,
            AVERAGE_EARNED_SCORE,
            CALCULATED_AT,
            REFRESHED_AT
        );

        assertThat(dto.questionId()).isEqualTo(701L);
        assertThat(dto.averageEarnedScore()).isEqualByComparingTo("4.7500");
        assertThat(componentNames(ExpertQuestionAnalyticsDto.class))
            .containsExactly(
                "questionId",
                "periodStart",
                "periodEnd",
                "attemptCount",
                "correctCount",
                "incorrectCount",
                "averageEarnedScore",
                "calculatedAt",
                "refreshedAt"
            );
    }

    @Test
    void dtoRejectsNullMandatoryFields() {
        assertThatThrownBy(() -> new ExpertQuestionAnalyticsDto(
            null,
            PERIOD_START,
            PERIOD_END,
            15,
            8,
            3,
            AVERAGE_EARNED_SCORE,
            CALCULATED_AT,
            REFRESHED_AT
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("questionId must not be null");

        assertThatThrownBy(() -> new ExpertQuestionAnalyticsDto(
            701L,
            null,
            PERIOD_END,
            15,
            8,
            3,
            AVERAGE_EARNED_SCORE,
            CALCULATED_AT,
            REFRESHED_AT
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("periodStart must not be null");

        assertThatThrownBy(() -> new ExpertQuestionAnalyticsDto(
            701L,
            PERIOD_START,
            PERIOD_END,
            15,
            8,
            3,
            null,
            CALCULATED_AT,
            REFRESHED_AT
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("averageEarnedScore must not be null");

        assertThatThrownBy(() -> new ExpertQuestionAnalyticsDto(
            701L,
            PERIOD_START,
            PERIOD_END,
            15,
            8,
            3,
            AVERAGE_EARNED_SCORE,
            CALCULATED_AT,
            null
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("refreshedAt must not be null");
    }

    @Test
    void dtoSourceRejectsManagerialCurrentSupervisionContentAndExecutionDrift() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/analytics/query/ExpertQuestionAnalyticsDto.java"
        ));

        assertThat(source)
            .doesNotContain("ManagerialUserTopicAnalyticsDto")
            .doesNotContain("ManagerialDepartmentTopicAnalyticsDto")
            .doesNotContain("userId")
            .doesNotContain("organizationalUnitIdSnapshot")
            .doesNotContain("organizationalPathSnapshot")
            .doesNotContain("assignmentId")
            .doesNotContain("assignmentTestId")
            .doesNotContain("assignmentStatus")
            .doesNotContain("QuestionCommandService")
            .doesNotContain("QuestionLifecycleService")
            .doesNotContain("QuestionQueryService")
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("AssignmentStatusRecalculationService")
            .doesNotContain("CapabilityAdmissionPolicy")
            .doesNotContain("AccessSpecificationPolicy")
            .doesNotContain("EntityManager")
            .doesNotContain("Controller")
            .doesNotContain("rebuild")
            .doesNotContain("refresh(")
            .doesNotContain("recalculate")
            .doesNotContain("recordResult")
            .doesNotContain("save(")
            .doesNotContain("delete(")
            .doesNotContain("flush(");
    }

    private List<String> componentNames(Class<?> recordType) {
        return Arrays.stream(recordType.getRecordComponents())
            .map(RecordComponent::getName)
            .toList();
    }
}
