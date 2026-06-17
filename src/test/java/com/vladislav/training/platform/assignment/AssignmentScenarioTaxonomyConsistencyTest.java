package com.vladislav.training.platform.assignment;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.assignment.service.AssignmentAdministrativeActionService;
import com.vladislav.training.platform.assignment.service.AssignmentCampaignCommandService;
import com.vladislav.training.platform.assignment.service.AssignmentCriticalAuditCatalog;
import com.vladislav.training.platform.assignment.service.AssignmentCriticalAuditPlanner;
import com.vladislav.training.platform.assignment.service.AssignmentCriticalAuditPlannerImpl;
import com.vladislav.training.platform.assignment.service.AssignmentSelfScopedQueryService;
import com.vladislav.training.platform.assignment.service.AssignmentStatusRecalculationService;
import com.vladislav.training.platform.assignment.service.LaunchAssignmentCampaignCommand;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code AssignmentScenarioTaxonomyConsistency}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class AssignmentScenarioTaxonomyConsistencyTest {

    @Test
    void launchVocabularyStaysLaunchOrientedAndDoesNotDriftIntoCampaignCrud() throws IOException {
        assertThat(methodNames(AssignmentCampaignCommandService.class))
            .containsExactly("launchAssignmentCampaign")
            .doesNotContain(
                "createAssignmentCampaign",
                "recordRecipientSnapshot",
                "updateAssignmentCampaign",
                "addCourseToCampaign",
                "removeCourseFromCampaign",
                "patchAssignmentCampaign",
                "manageCampaign"
            );

        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/service/AssignmentCampaignCommandService.java"))
            .contains("SCN-06")
            .contains("launch contour")
            .contains("immutable recipient snapshot")
            .contains("not a generic")
            .contains("campaign CRUD")
            .contains("launch-only owner entrypoint");

        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/service/LaunchAssignmentCampaignCommand.java"))
            .contains("Typed owner command for {@code SCN-06} campaign launch")
            .contains("launch-only")
            .contains("generic campaign CRUD");

        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/controller/AssignmentCampaignLaunchController.java"))
            .contains("campaign/launch command surface")
            .contains("generic")
            .contains("campaign CRUD")
            .contains("mutable campaign rewrite");
    }

    @Test
    void administrativeVocabularyStaysTypedAndDoesNotDegradeIntoPatchOrOverrideSurface() throws IOException {
        assertThat(methodNames(AssignmentAdministrativeActionService.class))
            .containsExactlyInAnyOrder("cancelAssignment", "extendAssignmentDeadline", "replaceWithNewAssignment")
            .doesNotContain(
                "patchAssignment",
                "updateAssignment",
                "markAssignmentCompleted",
                "overrideAssignmentResult",
                "changeAssignee",
                "editAssignmentTestSet",
                "reopenCompletedAssignment",
                "reopenCancelledAssignment"
            );

        assertThat(nestedTypeNames(AssignmentAdministrativeActionService.class))
            .containsExactlyInAnyOrder(
                "CancelAssignmentCommand",
                "ExtendAssignmentDeadlineCommand",
                "ReplaceWithNewAssignmentCommand"
            );

        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/service/AssignmentAdministrativeActionService.java"))
            .contains("typed administrative action contour")
            .contains("generic patch semantics")
            .contains("assignee drift")
            .contains("assignment-test rewrite");

        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/controller/AssignmentAdministrativeActionController.java"))
            .contains("typed administrative actions")
            .contains("PATCH assignment")
            .contains("Manual status patching")
            .contains("assignment-test rewrite")
            .contains("/cancel/{assignmentId}")
            .contains("/deadline-extend/{assignmentId}")
            .contains("/replace-with-new/{assignmentId}");

        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/controller/dto/CancelAssignmentRequest.java"))
            .contains("Typed request carrier for the cancel administrative action")
            .contains("generic assignment patch semantics");
        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/controller/dto/ExtendAssignmentDeadlineRequest.java"))
            .contains("Typed request carrier for the deadline-extend administrative action")
            .contains("generic update semantics");
        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/controller/dto/ReplaceAssignmentWithNewRequest.java"))
            .contains("replace-with-new administrative action")
            .contains("provenance anchor")
            .contains("whole-assignment rewrite shape");
    }

    @Test
    void statusVocabularyStaysInternalRecalculationContourAndNotManualStatusManagement() throws IOException {
        assertThat(methodNames(AssignmentStatusRecalculationService.class))
            .containsExactlyInAnyOrder("recalculateAssignmentStatus", "refreshAssignmentStatusCache")
            .doesNotContain(
                "updateAssignmentStatus",
                "patchAssignmentStatus",
                "manageAssignmentStatus",
                "markAssignmentCompleted"
            );

        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/service/AssignmentStatusRecalculationService.java"))
            .contains("SCN-07")
            .contains("materialized {@code assignment.status} cache")
            .contains("not a user-facing")
            .contains("manual patch helper")
            .contains("scheduler-owned business truth");

        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/service/AssignmentStatusRecalculationServiceImpl.java"))
            .contains("counted-result proof for COMPLETED")
            .contains("all assignment tests must be closed by valid counted results that are passed")
            .contains("within deadline, and counted in assignment")
            .contains("return AssignmentStatus.COMPLETED;")
            .doesNotContain("TestAttempt.status");

        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/package-info.java"))
            .contains("materialized cache column only")
            .contains("internal recalculation")
            .contains("status editing");
    }

    @Test
    void auditVocabularyStaysSynchronousCompanionAndNotOwnerHistorySubstitute() throws IOException {
        assertThat(AssignmentCriticalAuditCatalog.CAMPAIGN_LAUNCH.auditEntityType()).isEqualTo("assignment_campaign");
        assertThat(AssignmentCriticalAuditCatalog.CAMPAIGN_LAUNCH.anchorsCampaignRoot()).isTrue();
        assertThat(AssignmentCriticalAuditCatalog.CAMPAIGN_LAUNCH.requiresSynchronousCompanion()).isTrue();

        assertThat(AssignmentCriticalAuditCatalog.ASSIGNMENT_CANCEL.auditEntityType()).isEqualTo("assignment");
        assertThat(AssignmentCriticalAuditCatalog.ASSIGNMENT_CANCEL.anchorsAssignmentRoot()).isTrue();
        assertThat(AssignmentCriticalAuditCatalog.ASSIGNMENT_DEADLINE_EXTEND.auditEntityType()).isEqualTo("assignment");
        assertThat(AssignmentCriticalAuditCatalog.ASSIGNMENT_REPLACE_WITH_NEW.auditEntityType()).isEqualTo("assignment");

        AssignmentCriticalAuditPlanner companion = new AssignmentCriticalAuditPlannerImpl();
        AssignmentCriticalAuditPlanner.AuditPlan launchPlan = companion.planLaunchAudit(11L);
        AssignmentCriticalAuditPlanner.AuditPlan adminPlan =
            companion.planAdministrativeAudit(
                com.vladislav.training.platform.application.policy.CapabilityOperationCode.ASSIGNMENT_CANCEL,
                21L
            );

        assertThat(launchPlan.requiresSynchronousCommandBoundary()).isTrue();
        assertThat(launchPlan.allowsAsyncBestEffortTail()).isFalse();
        assertThat(launchPlan.substitutesOwnerHistory()).isFalse();
        assertThat(adminPlan.requiresSynchronousCommandBoundary()).isTrue();
        assertThat(adminPlan.allowsAsyncBestEffortTail()).isFalse();
        assertThat(adminPlan.substitutesOwnerHistory()).isFalse();

        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/service/AssignmentCriticalAuditCatalog.java"))
            .contains("launch-side audit is anchored to {@code assignment_campaign}")
            .contains("typed administrative-action audit is anchored to {@code assignment}")
            .contains("synchronous companion foundation")
            .contains("does not turn {@code assignment_administrative_action} into a substitute");

        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/service/AssignmentCriticalAuditPlanner.java"))
            .contains("mandatory synchronous audit companion")
            .contains("launch-side and typed administrative companion embedding")
            .contains("does not reinterpret")
            .contains("assignment_administrative_action")
            .contains("same command boundary");
    }

    @Test
    void crossLayerTaxonomyStaysConsistentAcrossServiceControllerDtoAndPackageDocs() throws IOException {
        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/service/package-info.java"))
            .contains("package com.vladislav.training.platform.assignment.service")
            .doesNotContain("generic campaign management");

        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/controller/package-info.java"))
            .contains("package com.vladislav.training.platform.assignment.controller");

        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/controller/dto/package-info.java"))
            .contains("package com.vladislav.training.platform.assignment.controller.dto");

        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/infrastructure/persistence/package-info.java"))
            .contains("package com.vladislav.training.platform.assignment.infrastructure.persistence")
            .doesNotContain("readiness only");

        assertThat(read("src/main/java/com/vladislav/training/platform/audit/service/package-info.java"))
            .contains("package com.vladislav.training.platform.audit.service");
    }

    @Test
    void selfScopedReadVocabularyStaysActorBoundAndDoesNotReintroduceBroadSubjectReads() throws IOException {
        assertThat(methodNames(AssignmentSelfScopedQueryService.class))
            .containsExactlyInAnyOrder(
                "findSelfAssignments",
                "findSelfAssignmentById",
                "findAssignedLearningContext",
                "findAssignedMaterialContent",
                "findAssignedTestContext"
            )
            .doesNotContain(
                "findAssignmentsByUserId",
                "findAssignmentsForUser",
                "findAssignmentByUserId",
                "launchAssignmentCampaign",
                "cancelAssignment",
                "refreshAssignmentStatusCache"
            );

        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/service/AssignmentSelfScopedQueryService.java"))
            .contains("actor-bound")
            .contains("trusted upstream actor identity")
            .contains("dedicated self-scoped")
            .doesNotContain("subjectUserId")
            .contains("does not authorize generic assignment read API semantics");
    }

    private Set<String> methodNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
            .map(method -> method.getName())
            .collect(Collectors.toUnmodifiableSet());
    }

    private Set<String> nestedTypeNames(Class<?> type) {
        return Stream.of(type.getDeclaredClasses())
            .map(Class::getSimpleName)
            .collect(Collectors.toUnmodifiableSet());
    }

    private String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}




