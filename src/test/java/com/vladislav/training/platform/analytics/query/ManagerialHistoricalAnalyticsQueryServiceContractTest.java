package com.vladislav.training.platform.analytics.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
/**
 * Проверяет договорённости вокруг {@code ManagerialHistoricalAnalyticsQueryService}.
 * Тест помогает сохранить предсказуемое поведение.
 */
class ManagerialHistoricalAnalyticsQueryServiceContractTest {

    private static final Instant EFFECTIVE_AT = Instant.parse("2026-04-25T10:00:00Z");
    private static final Instant PERIOD_START = Instant.parse("2026-04-01T00:00:00Z");
    private static final Instant PERIOD_END = Instant.parse("2026-04-30T23:59:59Z");

    @Test
    void interfaceExposesOnlyTwoReadMethods() throws NoSuchMethodException {
        assertThat(methodNames(ManagerialHistoricalAnalyticsQueryService.class))
            .containsExactlyInAnyOrder("findUserTopicAnalytics", "findDepartmentTopicAnalytics");

        Method userTopicMethod = ManagerialHistoricalAnalyticsQueryService.class.getDeclaredMethod(
            "findUserTopicAnalytics",
            ManagerialHistoricalAnalyticsQueryService.ManagerialHistoricalAnalyticsQuery.class
        );
        assertThat(userTopicMethod.getReturnType()).isEqualTo(List.class);
        assertThat(((ParameterizedType) userTopicMethod.getGenericReturnType()).getActualTypeArguments())
            .containsExactly(ManagerialUserTopicAnalyticsDto.class);

        Method departmentTopicMethod = ManagerialHistoricalAnalyticsQueryService.class.getDeclaredMethod(
            "findDepartmentTopicAnalytics",
            ManagerialHistoricalAnalyticsQueryService.ManagerialHistoricalAnalyticsQuery.class
        );
        assertThat(departmentTopicMethod.getReturnType()).isEqualTo(List.class);
        assertThat(((ParameterizedType) departmentTopicMethod.getGenericReturnType()).getActualTypeArguments())
            .containsExactly(ManagerialDepartmentTopicAnalyticsDto.class);
    }

    @Test
    void queryRejectsNullMandatoryFields() {
        assertThatThrownBy(() -> new ManagerialHistoricalAnalyticsQueryService.ManagerialHistoricalAnalyticsQuery(
            null,
            EFFECTIVE_AT,
            PERIOD_START,
            PERIOD_END
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("actorUserId must not be null");

        assertThatThrownBy(() -> new ManagerialHistoricalAnalyticsQueryService.ManagerialHistoricalAnalyticsQuery(
            101L,
            null,
            PERIOD_START,
            PERIOD_END
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("effectiveAt must not be null");

        assertThatThrownBy(() -> new ManagerialHistoricalAnalyticsQueryService.ManagerialHistoricalAnalyticsQuery(
            101L,
            EFFECTIVE_AT,
            null,
            PERIOD_END
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("periodStart must not be null");

        assertThatThrownBy(() -> new ManagerialHistoricalAnalyticsQueryService.ManagerialHistoricalAnalyticsQuery(
            101L,
            EFFECTIVE_AT,
            PERIOD_START,
            null
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("periodEnd must not be null");
    }

    @Test
    void queryRejectsInvalidPeriodRange() {
        assertThatThrownBy(() -> new ManagerialHistoricalAnalyticsQueryService.ManagerialHistoricalAnalyticsQuery(
            101L,
            EFFECTIVE_AT,
            PERIOD_START,
            PERIOD_START
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("periodStart must be before periodEnd");

        assertThatThrownBy(() -> new ManagerialHistoricalAnalyticsQueryService.ManagerialHistoricalAnalyticsQuery(
            101L,
            EFFECTIVE_AT,
            PERIOD_END,
            PERIOD_START
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("periodStart must be before periodEnd");
    }

    @Test
    void queryContractStaysActorSelfAndDoesNotAcceptScopeOverrideSelectors() {
        assertThat(Arrays.stream(
            ManagerialHistoricalAnalyticsQueryService.ManagerialHistoricalAnalyticsQuery.class.getRecordComponents()
        ).map(component -> component.getName()).toList())
            .containsExactly("actorUserId", "effectiveAt", "periodStart", "periodEnd")
            .doesNotContain(
                "managerUserId",
                "targetUserId",
                "scope",
                "scopeOverride",
                "organizationalUnitIds",
                "subtreePaths"
            );
    }

    @Test
    void contractSourceDoesNotContainOperationalQuestionOrMutationVocabulary() throws IOException {
        String source = read(
            "src/main/java/com/vladislav/training/platform/analytics/query/ManagerialHistoricalAnalyticsQueryService.java"
        );

        assertThat(source)
            .doesNotContain("assignmentId")
            .doesNotContain("assignmentTestId")
            .doesNotContain("assignmentStatus")
            .doesNotContain("deadlineAt")
            .doesNotContain("ManagerialCurrentSupervision")
            .doesNotContain("questionId")
            .doesNotContain("correctCount")
            .doesNotContain("incorrectCount")
            .doesNotContain("averageEarnedScore")
            .doesNotContain("rebuild")
            .doesNotContain("refresh")
            .doesNotContain("recalculate")
            .doesNotContain("recordResult")
            .doesNotContain("save(")
            .doesNotContain("delete(")
            .doesNotContain("flush(");
    }

    @Test
    void contractMustNotDependOnForbiddenServicesOrAdmissionDependencies() throws IOException {
        assertThat(fieldTypeNames(ManagerialHistoricalAnalyticsQueryService.class))
            .doesNotContain(
                "com.vladislav.training.platform.analytics.service.AnalyticsRefreshService",
                "com.vladislav.training.platform.analytics.service.AnalyticsRebuildService",
                "com.vladislav.training.platform.result.service.ResultRecordingService",
                "com.vladislav.training.platform.assignment.service.AssignmentStatusRecalculationService",
                "com.vladislav.training.platform.content.service.QuestionCommandService",
                "com.vladislav.training.platform.content.service.QuestionLifecycleService",
                "com.vladislav.training.platform.content.service.QuestionQueryService",
                "com.vladislav.training.platform.access.repository.ManagementRelationRepository",
                "com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy"
            );

        String source = read(
            "src/main/java/com/vladislav/training/platform/analytics/query/ManagerialHistoricalAnalyticsQueryService.java"
        );
        assertThat(source)
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("AssignmentStatusRecalculationService")
            .doesNotContain("QuestionCommandService")
            .doesNotContain("QuestionLifecycleService")
            .doesNotContain("QuestionQueryService")
            .doesNotContain("ManagementRelationRepository")
            .doesNotContain("CapabilityAdmissionPolicy");
    }

    private Set<String> methodNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toUnmodifiableSet());
    }

    private Set<String> fieldTypeNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .map(field -> field.getType().getName())
            .collect(Collectors.toUnmodifiableSet());
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
