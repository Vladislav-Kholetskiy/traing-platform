package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vladislav.training.platform.analytics.service.AnalyticsRebuildService;
import com.vladislav.training.platform.analytics.service.AnalyticsRefreshService;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
/**
 * Проверяет договорённости вокруг {@code ManagerialCurrentSupervisionQueryService}.
 * Тест помогает сохранить предсказуемое поведение.
 */
class ManagerialCurrentSupervisionQueryServiceContractTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-24T22:15:00Z");

    @Test
    void managerialCurrentSupervisionContractExposesSingleManagerialCurrentReadMethod() throws NoSuchMethodException {
        assertThat(methodNames(ManagerialCurrentSupervisionQueryService.class))
            .containsExactly("findCurrentSupervision");

        Method method = ManagerialCurrentSupervisionQueryService.class.getDeclaredMethod(
            "findCurrentSupervision",
            ManagerialCurrentSupervisionQueryService.ManagerialCurrentSupervisionQuery.class
        );
        assertThat(method.getReturnType()).isEqualTo(List.class);
        assertThat(((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments())
            .containsExactly(ManagerialCurrentSupervisionQueryService.ManagerialCurrentSupervisionRow.class);
    }

    @Test
    void managerialCurrentSupervisionQueryRejectsNullMandatoryFields() {
        assertThatThrownBy(() -> new ManagerialCurrentSupervisionQueryService.ManagerialCurrentSupervisionQuery(
            null,
            FIXED_INSTANT
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("actorUserId must not be null");
        assertThatThrownBy(() -> new ManagerialCurrentSupervisionQueryService.ManagerialCurrentSupervisionQuery(
            101L,
            null
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("effectiveAt must not be null");
    }

    @Test
    void managerialCurrentSupervisionRowKeepsCurrentAssignmentOrientedShape() {
        String userDisplayName = "РРІР°РЅРѕРІ РРІР°РЅ РРІР°РЅРѕРІРёС‡";
        String courseName = "РћС…СЂР°РЅР° С‚СЂСѓРґР°";
        ManagerialCurrentSupervisionQueryService.ManagerialCurrentSupervisionRow row =
            new ManagerialCurrentSupervisionQueryService.ManagerialCurrentSupervisionRow(
                77L,
                101L,
                userDisplayName,
                501L,
                courseName,
                701L,
                FIXED_INSTANT.minusSeconds(3600),
                FIXED_INSTANT.plusSeconds(7200),
                AssignmentStatus.ASSIGNED
            );

        assertThat(row.assignmentId()).isEqualTo(77L);
        assertThat(row.userId()).isEqualTo(101L);
        assertThat(row.userDisplayName()).isEqualTo(userDisplayName);
        assertThat(row.courseId()).isEqualTo(501L);
        assertThat(row.courseName()).isEqualTo(courseName);
        assertThat(row.assignmentTestCount()).isEqualTo(701L);
        assertThat(row.assignmentStatus()).isEqualTo(AssignmentStatus.ASSIGNED);
        assertThat(componentNames(ManagerialCurrentSupervisionQueryService.ManagerialCurrentSupervisionRow.class))
            .containsExactly(
                "assignmentId",
                "userId",
                "userDisplayName",
                "courseId",
                "courseName",
                "assignmentTestCount",
                "assignedAt",
                "deadlineAt",
                "assignmentStatus"
            );
    }

    @Test
    void managerialCurrentSupervisionContractDoesNotAccumulateHistoricalAnalyticsFieldsOrMutationVocabulary()
        throws IOException {
        String source = read("src/main/java/com/vladislav/training/platform/assignment/service/ManagerialCurrentSupervisionQueryService.java");

        assertThat(componentNames(ManagerialCurrentSupervisionQueryService.ManagerialCurrentSupervisionRow.class))
            .doesNotContain(
                "scorePercent",
                "passedRate",
                "averageScore",
                "questionDifficulty",
                "analyticsCalculatedAt",
                "rebuildStatus"
            );
        assertThat(methodNames(ManagerialCurrentSupervisionQueryService.class))
            .doesNotContain("recalculate", "rebuild", "refresh", "command", "mutate");
        assertThat(source)
            .contains("ManagerialCurrentSupervisionQueryService")
            .doesNotContain("mutate");
    }

    @Test
    void managerialCurrentSupervisionContractStaysIndependentFromCommandResultAnalyticsAndAdmissionDependencies()
        throws IOException {
        assertThat(fieldTypeNames(ManagerialCurrentSupervisionQueryService.class))
            .doesNotContain(
                AssignmentCommandService.class.getName(),
                AssignmentStatusRecalculationService.class.getName(),
                ResultRecordingService.class.getName(),
                AnalyticsRefreshService.class.getName(),
                AnalyticsRebuildService.class.getName(),
                CapabilityAdmissionPolicy.class.getName()
            );

        String source = read("src/main/java/com/vladislav/training/platform/assignment/service/ManagerialCurrentSupervisionQueryService.java");
        assertThat(source)
            .doesNotContain("AssignmentCommandService")
            .doesNotContain("AssignmentStatusRecalculationService")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("CapabilityAdmissionPolicy");
    }

    private Set<String> methodNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toUnmodifiableSet());
    }

    private List<String> componentNames(Class<?> type) {
        return Arrays.stream(type.getRecordComponents())
            .map(RecordComponent::getName)
            .toList();
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
