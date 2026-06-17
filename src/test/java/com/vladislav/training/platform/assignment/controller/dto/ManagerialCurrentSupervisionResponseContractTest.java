package com.vladislav.training.platform.assignment.controller.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет договорённости вокруг {@code ManagerialCurrentSupervisionResponse}.
 * Тест помогает сохранить предсказуемое поведение.
 */
class ManagerialCurrentSupervisionResponseContractTest {

    private static final Instant ASSIGNED_AT = Instant.parse("2026-05-01T10:00:00Z");
    private static final Instant DEADLINE_AT = Instant.parse("2026-05-15T10:00:00Z");

    @Test
    void responseKeepsCurrentSupervisionDashboardShape() {
        String userDisplayName = "РРІР°РЅРѕРІ РРІР°РЅ РРІР°РЅРѕРІРёС‡";
        String courseName = "РћС…СЂР°РЅР° С‚СЂСѓРґР°";
        ManagerialCurrentSupervisionResponse response = new ManagerialCurrentSupervisionResponse(
            101L,
            201L,
            userDisplayName,
            301L,
            courseName,
            5L,
            ASSIGNED_AT,
            DEADLINE_AT,
            AssignmentStatus.ASSIGNED
        );

        assertThat(response.assignmentId()).isEqualTo(101L);
        assertThat(response.userId()).isEqualTo(201L);
        assertThat(response.userDisplayName()).isEqualTo(userDisplayName);
        assertThat(response.courseId()).isEqualTo(301L);
        assertThat(response.courseName()).isEqualTo(courseName);
        assertThat(response.assignmentTestCount()).isEqualTo(5L);
        assertThat(response.assignedAt()).isEqualTo(ASSIGNED_AT);
        assertThat(response.deadlineAt()).isEqualTo(DEADLINE_AT);
        assertThat(response.assignmentStatus()).isEqualTo(AssignmentStatus.ASSIGNED);
        assertThat(componentNames(ManagerialCurrentSupervisionResponse.class))
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
    void responseDoesNotAbsorbHistoricalAnalyticsExpertDiagnosticsOrActorOverrideFields() {
        assertThat(componentNames(ManagerialCurrentSupervisionResponse.class))
            .doesNotContain(
                "periodStart",
                "periodEnd",
                "averageScorePercent",
                "passRatePercent",
                "attemptCount",
                "errorCount",
                "questionId",
                "correctCount",
                "incorrectCount",
                "averageEarnedScore",
                "actorUserId",
                "managerUserId",
                "targetUserId",
                "scope",
                "scopeOverride",
                "rebuildStatus",
                "refreshRequestedAt"
            );
    }

    @Test
    void responseSourceDoesNotLeakPersistenceMutationOrForeignScenarioTypes() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/assignment/controller/dto/ManagerialCurrentSupervisionResponse.java"
        ));

        assertThat(source)
            .contains("AssignmentStatus")
            .doesNotContain("AssignmentEntity")
            .doesNotContain("AssignmentTestEntity")
            .doesNotContain("ResultEntity")
            .doesNotContain("TestAttemptEntity")
            .doesNotContain("AnalyticsUserTopicAggregateEntity")
            .doesNotContain("AnalyticsDepartmentTopicAggregateEntity")
            .doesNotContain("AnalyticsQuestionAggregateEntity")
            .doesNotContain("ManagerialUserTopicAnalyticsDto")
            .doesNotContain("ManagerialDepartmentTopicAnalyticsDto")
            .doesNotContain("ExpertQuestionAnalyticsResponse")
            .doesNotContain("ExpertQuestionAnalyticsDto")
            .doesNotContain("save(")
            .doesNotContain("delete(")
            .doesNotContain("publish")
            .doesNotContain("archive")
            .doesNotContain("rebuild")
            .doesNotContain("refresh(")
            .doesNotContain("recordResult");
    }

    private List<String> componentNames(Class<?> recordType) {
        return Arrays.stream(recordType.getRecordComponents())
            .map(RecordComponent::getName)
            .toList();
    }
}
