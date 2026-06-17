package com.vladislav.training.platform.analytics.controller.dto;

import static org.assertj.core.api.Assertions.assertThat;

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
 * Проверяет договорённости вокруг {@code ExpertQuestionAnalyticsResponse}.
 * Тест помогает сохранить предсказуемое поведение.
 */
class ExpertQuestionAnalyticsResponseContractTest {

    private static final Instant PERIOD_START = Instant.parse("2026-05-01T00:00:00Z");
    private static final Instant PERIOD_END = Instant.parse("2026-05-31T23:59:59Z");
    private static final Instant CALCULATED_AT = Instant.parse("2026-06-01T00:10:00Z");
    private static final Instant REFRESHED_AT = Instant.parse("2026-06-01T00:15:00Z");

    @Test
    void responseKeepsExactExpertAnalyticsPublicShape() {
        ExpertQuestionAnalyticsResponse response = new ExpertQuestionAnalyticsResponse(
            701L,
            PERIOD_START,
            PERIOD_END,
            15,
            8,
            3,
            new BigDecimal("4.7500"),
            CALCULATED_AT,
            REFRESHED_AT
        );

        assertThat(response.questionId()).isEqualTo(701L);
        assertThat(response.averageEarnedScore()).isEqualByComparingTo("4.7500");
        assertThat(componentNames(ExpertQuestionAnalyticsResponse.class))
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
    void responseDoesNotAbsorbContentAuthoringCurrentSupervisionOrActorOverrideFields() {
        assertThat(componentNames(ExpertQuestionAnalyticsResponse.class))
            .doesNotContain(
                "questionText",
                "questionLabel",
                "questionType",
                "contentStatus",
                "assignmentId",
                "assignmentStatus",
                "deadlineAt",
                "managerUserId",
                "organizationalUnitIdSnapshot",
                "organizationalPathSnapshot",
                "actorUserId",
                "expertUserId",
                "targetUserId",
                "scope",
                "scopeOverride"
            );
    }

    @Test
    void responseSourceDoesNotLeakEntitiesMutationOrForeignAnalyticsDtos() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/analytics/controller/dto/ExpertQuestionAnalyticsResponse.java"
        ));

        assertThat(source)
            .doesNotContain("QuestionEntity")
            .doesNotContain("ResultEntity")
            .doesNotContain("AssignmentEntity")
            .doesNotContain("TestAttemptEntity")
            .doesNotContain("AnalyticsQuestionAggregateEntity")
            .doesNotContain("AnalyticsUserTopicAggregateEntity")
            .doesNotContain("AnalyticsDepartmentTopicAggregateEntity")
            .doesNotContain("ManagerialCurrentSupervisionResponse")
            .doesNotContain("ManagerialUserTopicAnalyticsDto")
            .doesNotContain("ManagerialDepartmentTopicAnalyticsDto")
            .doesNotContain("SelfHistoricalResultSummaryDto")
            .doesNotContain("save(")
            .doesNotContain("delete(")
            .doesNotContain("publish")
            .doesNotContain("archive")
            .doesNotContain("rebuild")
            .doesNotContain("refresh(")
            .doesNotContain("recalculate")
            .doesNotContain("recordResult");
    }

    private List<String> componentNames(Class<?> recordType) {
        return Arrays.stream(recordType.getRecordComponents())
            .map(RecordComponent::getName)
            .toList();
    }
}
