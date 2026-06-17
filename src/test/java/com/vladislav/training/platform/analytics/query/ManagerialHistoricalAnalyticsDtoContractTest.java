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
 * Проверяет договорённости вокруг {@code ManagerialHistoricalAnalyticsDto}.
 * Тест помогает сохранить предсказуемое поведение.
 */
class ManagerialHistoricalAnalyticsDtoContractTest {

    private static final Instant PERIOD_START = Instant.parse("2026-04-01T00:00:00Z");
    private static final Instant PERIOD_END = Instant.parse("2026-04-30T23:59:59Z");
    private static final Instant CALCULATED_AT = Instant.parse("2026-05-01T00:10:00Z");
    private static final Instant REFRESHED_AT = Instant.parse("2026-05-01T00:15:00Z");
    private static final BigDecimal AVERAGE_SCORE_PERCENT = new BigDecimal("84.2500");
    private static final BigDecimal PASS_RATE_PERCENT = new BigDecimal("91.5000");

    @Test
    void userTopicDtoKeepsHistoricalAggregateShape() {
        ManagerialUserTopicAnalyticsDto dto = new ManagerialUserTopicAnalyticsDto(
            101L,
            "EMP-101",
            "Иванов Иван Иванович",
            501L,
            "Охрана труда",
            PERIOD_START,
            PERIOD_END,
            AVERAGE_SCORE_PERCENT,
            PASS_RATE_PERCENT,
            12,
            3,
            CALCULATED_AT,
            REFRESHED_AT
        );

        assertThat(dto.userId()).isEqualTo(101L);
        assertThat(dto.userEmployeeNumber()).isEqualTo("EMP-101");
        assertThat(dto.userDisplayName()).isEqualTo("Иванов Иван Иванович");
        assertThat(dto.topicId()).isEqualTo(501L);
        assertThat(dto.topicName()).isEqualTo("Охрана труда");
        assertThat(dto.averageScorePercent()).isEqualByComparingTo("84.2500");
        assertThat(dto.passRatePercent()).isEqualByComparingTo("91.5000");
        assertThat(componentNames(ManagerialUserTopicAnalyticsDto.class))
            .containsExactly(
                "userId",
                "userEmployeeNumber",
                "userDisplayName",
                "topicId",
                "topicName",
                "periodStart",
                "periodEnd",
                "averageScorePercent",
                "passRatePercent",
                "attemptCount",
                "errorCount",
                "calculatedAt",
                "refreshedAt"
            )
            .doesNotContain(
                "assignmentId",
                "assignmentTestId",
                "assignmentStatus",
                "deadlineAt",
                "questionId",
                "correctCount",
                "incorrectCount",
                "averageEarnedScore",
                "questionDifficulty"
            );
    }

    @Test
    void departmentTopicDtoKeepsHistoricalAggregateShape() {
        ManagerialDepartmentTopicAnalyticsDto dto = new ManagerialDepartmentTopicAnalyticsDto(
            42L,
            "Установка 42",
            "/company/division/department",
            501L,
            "Пожарная безопасность",
            PERIOD_START,
            PERIOD_END,
            AVERAGE_SCORE_PERCENT,
            PASS_RATE_PERCENT,
            25,
            4,
            CALCULATED_AT,
            REFRESHED_AT
        );

        assertThat(dto.organizationalUnitIdSnapshot()).isEqualTo(42L);
        assertThat(dto.organizationalUnitName()).isEqualTo("Установка 42");
        assertThat(dto.organizationalPathSnapshot()).isEqualTo("/company/division/department");
        assertThat(dto.topicId()).isEqualTo(501L);
        assertThat(dto.topicName()).isEqualTo("Пожарная безопасность");
        assertThat(componentNames(ManagerialDepartmentTopicAnalyticsDto.class))
            .containsExactly(
                "organizationalUnitIdSnapshot",
                "organizationalUnitName",
                "organizationalPathSnapshot",
                "topicId",
                "topicName",
                "periodStart",
                "periodEnd",
                "averageScorePercent",
                "passRatePercent",
                "attemptCount",
                "errorCount",
                "calculatedAt",
                "refreshedAt"
            )
            .doesNotContain(
                "assignmentId",
                "assignmentTestId",
                "assignmentStatus",
                "deadlineAt",
                "questionId",
                "correctCount",
                "incorrectCount",
                "averageEarnedScore",
                "questionDifficulty"
            );
    }

    @Test
    void dtoRecordsRejectNullMandatoryFields() {
        assertThatThrownBy(() -> new ManagerialUserTopicAnalyticsDto(
            null,
            "EMP-101",
            "Иванов Иван Иванович",
            501L,
            "Охрана труда",
            PERIOD_START,
            PERIOD_END,
            AVERAGE_SCORE_PERCENT,
            PASS_RATE_PERCENT,
            12,
            3,
            CALCULATED_AT,
            REFRESHED_AT
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("userId must not be null");

        assertThatThrownBy(() -> new ManagerialUserTopicAnalyticsDto(
            101L,
            "EMP-101",
            "Иванов Иван Иванович",
            501L,
            "Охрана труда",
            PERIOD_START,
            PERIOD_END,
            null,
            PASS_RATE_PERCENT,
            12,
            3,
            CALCULATED_AT,
            REFRESHED_AT
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("averageScorePercent must not be null");

        assertThatThrownBy(() -> new ManagerialDepartmentTopicAnalyticsDto(
            42L,
            "Установка 42",
            null,
            501L,
            "Пожарная безопасность",
            PERIOD_START,
            PERIOD_END,
            AVERAGE_SCORE_PERCENT,
            PASS_RATE_PERCENT,
            25,
            4,
            CALCULATED_AT,
            REFRESHED_AT
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("organizationalPathSnapshot must not be null");

        assertThatThrownBy(() -> new ManagerialDepartmentTopicAnalyticsDto(
            42L,
            "Установка 42",
            "/company/division/department",
            501L,
            "Пожарная безопасность",
            PERIOD_START,
            PERIOD_END,
            AVERAGE_SCORE_PERCENT,
            PASS_RATE_PERCENT,
            25,
            4,
            null,
            REFRESHED_AT
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("calculatedAt must not be null");
    }

    @Test
    void dtoSourcesStayIndependentFromOperationalQuestionAndImplementationDependencies() throws IOException {
        String userTopicSource = read(
            "src/main/java/com/vladislav/training/platform/analytics/query/ManagerialUserTopicAnalyticsDto.java"
        );
        String departmentTopicSource = read(
            "src/main/java/com/vladislav/training/platform/analytics/query/ManagerialDepartmentTopicAnalyticsDto.java"
        );

        assertThat(userTopicSource)
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("AssignmentStatusRecalculationService")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("QuestionCommandService")
            .doesNotContain("QuestionLifecycleService")
            .doesNotContain("QuestionQueryService")
            .doesNotContain("Controller")
            .doesNotContain("ServiceImpl")
            .doesNotContain("assignmentId")
            .doesNotContain("assignmentTestId")
            .doesNotContain("assignmentStatus")
            .doesNotContain("deadlineAt")
            .doesNotContain("questionId")
            .doesNotContain("correctCount")
            .doesNotContain("incorrectCount")
            .doesNotContain("averageEarnedScore")
            .doesNotContain("questionDifficulty");

        assertThat(departmentTopicSource)
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("AssignmentStatusRecalculationService")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("QuestionCommandService")
            .doesNotContain("QuestionLifecycleService")
            .doesNotContain("QuestionQueryService")
            .doesNotContain("Controller")
            .doesNotContain("ServiceImpl")
            .doesNotContain("assignmentId")
            .doesNotContain("assignmentTestId")
            .doesNotContain("assignmentStatus")
            .doesNotContain("deadlineAt")
            .doesNotContain("questionId")
            .doesNotContain("correctCount")
            .doesNotContain("incorrectCount")
            .doesNotContain("averageEarnedScore")
            .doesNotContain("questionDifficulty");
    }

    private List<String> componentNames(Class<?> recordType) {
        return Arrays.stream(recordType.getRecordComponents())
            .map(RecordComponent::getName)
            .toList();
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
