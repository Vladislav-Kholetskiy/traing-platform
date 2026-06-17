package com.vladislav.training.platform.analytics.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет договорённости вокруг {@code ExpertQuestionAnalyticsQueryService}.
 * Тест помогает сохранить предсказуемое поведение.
 */
class ExpertQuestionAnalyticsQueryServiceContractTest {

    private static final Instant EFFECTIVE_AT = Instant.parse("2026-04-25T10:00:00Z");
    private static final Instant PERIOD_START = Instant.parse("2026-04-01T00:00:00Z");
    private static final Instant PERIOD_END = Instant.parse("2026-04-30T23:59:59Z");
    private static final Path CONTRACT_SOURCE = Path.of(
        "src/main/java/com/vladislav/training/platform/analytics/query/ExpertQuestionAnalyticsQueryService.java"
    );

    @Test
    void interfaceExposesExactlyOneReadMethodReturningExpertQuestionAnalyticsDtoList() throws NoSuchMethodException {
        Method[] declaredMethods = ExpertQuestionAnalyticsQueryService.class.getDeclaredMethods();

        assertThat(declaredMethods).hasSize(1);
        assertThat(declaredMethods[0].getName()).isEqualTo("findQuestionAnalytics");
        assertThat(declaredMethods[0].getParameterTypes())
            .containsExactly(ExpertQuestionAnalyticsQueryService.ExpertQuestionAnalyticsQuery.class);
        assertThat(declaredMethods[0].getReturnType()).isEqualTo(List.class);
        assertThat(((ParameterizedType) declaredMethods[0].getGenericReturnType()).getActualTypeArguments())
            .containsExactly(ExpertQuestionAnalyticsDto.class);
    }

    @Test
    void queryRecordKeepsExactContractShapeAndAcceptsValidValues() {
        ExpertQuestionAnalyticsQueryService.ExpertQuestionAnalyticsQuery query =
            new ExpertQuestionAnalyticsQueryService.ExpertQuestionAnalyticsQuery(
                101L,
                EFFECTIVE_AT,
                PERIOD_START,
                PERIOD_END
            );

        assertThat(query.actorUserId()).isEqualTo(101L);
        assertThat(query.effectiveAt()).isEqualTo(EFFECTIVE_AT);
        assertThat(query.periodStart()).isEqualTo(PERIOD_START);
        assertThat(query.periodEnd()).isEqualTo(PERIOD_END);
        assertThat(Arrays.stream(
            ExpertQuestionAnalyticsQueryService.ExpertQuestionAnalyticsQuery.class.getRecordComponents()
        ).map(RecordComponent::getName).toList())
            .containsExactly("actorUserId", "effectiveAt", "periodStart", "periodEnd");
    }

    @Test
    void queryRecordRejectsNullMandatoryFields() {
        assertThatThrownBy(() -> new ExpertQuestionAnalyticsQueryService.ExpertQuestionAnalyticsQuery(
            null,
            EFFECTIVE_AT,
            PERIOD_START,
            PERIOD_END
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("actorUserId must not be null");

        assertThatThrownBy(() -> new ExpertQuestionAnalyticsQueryService.ExpertQuestionAnalyticsQuery(
            101L,
            null,
            PERIOD_START,
            PERIOD_END
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("effectiveAt must not be null");

        assertThatThrownBy(() -> new ExpertQuestionAnalyticsQueryService.ExpertQuestionAnalyticsQuery(
            101L,
            EFFECTIVE_AT,
            null,
            PERIOD_END
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("periodStart must not be null");

        assertThatThrownBy(() -> new ExpertQuestionAnalyticsQueryService.ExpertQuestionAnalyticsQuery(
            101L,
            EFFECTIVE_AT,
            PERIOD_START,
            null
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("periodEnd must not be null");
    }

    @Test
    void queryRecordRejectsNonStrictPeriods() {
        assertThatThrownBy(() -> new ExpertQuestionAnalyticsQueryService.ExpertQuestionAnalyticsQuery(
            101L,
            EFFECTIVE_AT,
            PERIOD_START,
            PERIOD_START
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("periodStart must be strictly before periodEnd");

        assertThatThrownBy(() -> new ExpertQuestionAnalyticsQueryService.ExpertQuestionAnalyticsQuery(
            101L,
            EFFECTIVE_AT,
            PERIOD_END,
            PERIOD_START
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("periodStart must be strictly before periodEnd");
    }

    @Test
    void contractSourceRejectsManagerialSupervisionAuthoringAndExecutionDrift() throws IOException {
        String source = Files.readString(CONTRACT_SOURCE);

        assertThat(source)
            .contains("interface ExpertQuestionAnalyticsQueryService")
            .contains("findQuestionAnalytics")
            .doesNotContain("ManagerialUserTopicAnalyticsDto")
            .doesNotContain("ManagerialDepartmentTopicAnalyticsDto")
            .doesNotContain("ManagerialHistoricalAnalyticsQueryService")
            .doesNotContain("ManagerialHistoricalAnalyticsQuery")
            .doesNotContain("userId")
            .doesNotContain("organizationalUnitIdSnapshot")
            .doesNotContain("organizationalPathSnapshot")
            .doesNotContain("assignmentId")
            .doesNotContain("assignmentTestId")
            .doesNotContain("assignmentStatus")
            .doesNotContain("deadlineAt")
            .doesNotContain("QuestionCommandService")
            .doesNotContain("QuestionLifecycleService")
            .doesNotContain("QuestionQueryService")
            .doesNotContain("com.vladislav.training.platform.analytics.service.AnalyticsQueryService")
            .doesNotContain("import com.vladislav.training.platform.analytics.service.AnalyticsQueryService")
            .doesNotContain("AnalyticsQueryService analyticsQueryService")
            .doesNotContain("AnalyticsQueryService.class")
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
            .doesNotContain("flush(")
            .doesNotContain("ExpertQuestionAnalyticsQueryServiceImpl");
    }
}
