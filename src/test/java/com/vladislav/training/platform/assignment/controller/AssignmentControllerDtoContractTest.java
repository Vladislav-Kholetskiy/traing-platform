package com.vladislav.training.platform.assignment.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.assignment.controller.dto.AssignedLearningContextResponse;
import com.vladislav.training.platform.assignment.controller.dto.AssignmentCampaignResponse;
import com.vladislav.training.platform.assignment.controller.dto.AssignmentCampaignRecipientSnapshotResponse;
import com.vladislav.training.platform.assignment.controller.dto.AssignmentAdministrativeActionResponse;
import com.vladislav.training.platform.assignment.controller.dto.AssignmentResponse;
import com.vladislav.training.platform.assignment.controller.dto.AssignmentTestResponse;
import com.vladislav.training.platform.assignment.controller.dto.CancelAssignmentRequest;
import com.vladislav.training.platform.assignment.controller.dto.ExtendAssignmentDeadlineRequest;
import com.vladislav.training.platform.assignment.controller.dto.LaunchAssignmentCampaignRequest;
import com.vladislav.training.platform.assignment.controller.dto.ReplaceAssignmentWithNewRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет договорённости вокруг {@code AssignmentControllerDto}.
 * Тест помогает сохранить предсказуемое поведение.
 */
class AssignmentControllerDtoContractTest {

    @Test
    void launchRequestRemainsScenarioOrientedAndDoesNotExpressGenericCampaignCrud() throws NoSuchFieldException {
        assertThat(LaunchAssignmentCampaignRequest.class.isRecord()).isTrue();
        assertThat(componentNames(LaunchAssignmentCampaignRequest.class))
            .contains(
                "name",
                "description",
                "sourceType",
                "sourceRef",
                "sourceNameSnapshot",
                "courseIds",
                "targeting",
                "deadlinePolicy"
            )
            .doesNotContain("id", "campaignId", "status", "createdAt", "updatedAt", "assignmentTests", "recipientSnapshots");
        assertThat(field(LaunchAssignmentCampaignRequest.class, "name").isAnnotationPresent(NotBlank.class)).isTrue();
        assertThat(field(LaunchAssignmentCampaignRequest.class, "sourceType").isAnnotationPresent(NotBlank.class)).isTrue();
        assertThat(field(LaunchAssignmentCampaignRequest.class, "courseIds").isAnnotationPresent(NotEmpty.class)).isTrue();
        assertThat(field(LaunchAssignmentCampaignRequest.class, "targeting").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(field(LaunchAssignmentCampaignRequest.class, "deadlinePolicy").isAnnotationPresent(NotNull.class)).isTrue();
    }

    @Test
    void administrativeRequestsRemainTypedAndDoNotDegenerateIntoGenericPatchShape() throws NoSuchFieldException {
        assertThat(componentNames(CancelAssignmentRequest.class))
            .contains("note")
            .doesNotContain("status", "userId", "courseId", "assignmentTests", "assigneeId");

        assertThat(componentNames(ExtendAssignmentDeadlineRequest.class))
            .contains("newDeadlineAt", "note")
            .doesNotContain("status", "userId", "courseId", "assignmentTests", "assigneeId");
        assertThat(field(ExtendAssignmentDeadlineRequest.class, "newDeadlineAt").isAnnotationPresent(NotNull.class)).isTrue();

        assertThat(componentNames(ReplaceAssignmentWithNewRequest.class))
            .contains("campaignId", "newCycleDeadlineAt", "note")
            .doesNotContain("occurredAt")
            .doesNotContain("status", "userId", "courseId", "assignmentTests", "assigneeId");
        assertThat(field(ReplaceAssignmentWithNewRequest.class, "campaignId").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(field(ReplaceAssignmentWithNewRequest.class, "campaignId").isAnnotationPresent(Positive.class)).isTrue();
        assertThat(field(ReplaceAssignmentWithNewRequest.class, "newCycleDeadlineAt").isAnnotationPresent(NotNull.class)).isTrue();
    }

    @Test
    void responsesRemainNarrowRootLevelCarriersWithoutPortalOrExecutionPayloads() {
        assertThat(componentNames(AssignmentResponse.class))
            .contains("id", "campaignId", "userId", "courseId", "status", "assignedAt", "deadlineAt")
            .doesNotContain(
                "assignmentTests",
                "recipientSnapshots",
                "administrativeActions",
                "testAttempts",
                "countedResults",
                "entitlements",
                "courseContent"
            );

        assertThat(componentNames(AssignmentCampaignResponse.class))
            .contains("id", "name", "description", "sourceType", "sourceRef", "sourceNameSnapshot")
            .doesNotContain(
                "courseIds",
                "recipientSnapshots",
                "assignments",
                "assignmentTests",
                "administrativeActions",
                "entitlements",
                "courseContent"
            );

        assertThat(componentNames(AssignmentCampaignRecipientSnapshotResponse.class))
            .contains(
                "id",
                "campaignId",
                "userId",
                "organizationalUnitIdSnapshot",
                "organizationalPathSnapshot",
                "inclusionBasisCode"
            )
            .doesNotContain(
                "assignments",
                "assignmentTests",
                "administrativeActions",
                "entitlements",
                "previewRecipients",
                "learningState"
            );

        assertThat(componentNames(AssignmentTestResponse.class))
            .contains("id", "assignmentId", "testId", "assignmentTestRole", "countedResultId")
            .doesNotContain(
                "attempts",
                "questions",
                "results",
                "entitlements",
                "learningState",
                "courseContent"
            );

        assertThat(componentNames(AssignedLearningContextResponse.class))
            .containsExactlyInAnyOrder(
                "assignment",
                "assignmentTests",
                "publishedCourse",
                "publishedTopics",
                "publishedMaterials"
            )
            .doesNotContain(
                "testAttempts",
                "answers",
                "results",
                "executionState",
                "startUrl",
                "submissionState"
            );

        assertThat(componentNames(AssignmentAdministrativeActionResponse.class))
            .contains("id", "assignmentId", "actionType", "occurredAt", "note")
            .doesNotContain(
                "auditEventId",
                "actorUserId",
                "payloadBefore",
                "payloadAfter",
                "learningState",
                "reportingMetadata"
            );
    }

    private Set<String> componentNames(Class<?> recordType) {
        return Stream.of(recordType.getRecordComponents())
            .map(component -> component.getName())
            .collect(Collectors.toUnmodifiableSet());
    }

    private Field field(Class<?> type, String name) throws NoSuchFieldException {
        return type.getDeclaredField(name);
    }
}
