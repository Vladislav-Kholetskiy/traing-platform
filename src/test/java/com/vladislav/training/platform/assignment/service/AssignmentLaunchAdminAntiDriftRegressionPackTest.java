package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.assignment.domain.AssignmentAdministrativeActionType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Собирает набор регрессионных проверок вокруг {@code AssignmentLaunchAdminAntiDrift}.
 * Такие тесты помогают вовремя заметить незапланированные изменения.
 */
class AssignmentLaunchAdminAntiDriftRegressionPackTest {

    @Test
    void scn06LaunchContourStaysClosedAgainstMutableCampaignCrudDrift() {
        assertThat(methodNames(AssignmentCampaignCommandService.class))
            .containsExactly("launchAssignmentCampaign")
            .doesNotContain(
                "createAssignmentCampaign",
                "recordRecipientSnapshot",
                "updateAssignmentCampaign",
                "addCourseToCampaign",
                "removeCourseFromCampaign",
                "rewriteCampaignComposition"
            );
        assertThat(publicMethodNames(AssignmentCampaignCommandServiceImpl.class))
            .containsExactly("launchAssignmentCampaign")
            .doesNotContain(
                "persistLaunchCampaignRoot",
                "persistLaunchRecipientSnapshot",
                "createAssignmentCampaign",
                "recordRecipientSnapshot",
                "updateAssignmentCampaign"
            );
        assertThat(methodNames(AssignmentCampaignAntiMutableRegressionTest.class))
            .contains(
                "commandSurfaceStaysLaunchOnlyWithoutStandaloneMutableCampaignEntrypoints",
                "implementationDoesNotExposePublicBypassAroundLaunchOnlySemantics"
            );
    }

    @Test
    void scn22AdministrativeContourStaysClosedAgainstGenericPatchAssignmentDrift() {
        assertThat(methodNames(AssignmentAdministrativeActionService.class))
            .containsExactlyInAnyOrder("cancelAssignment", "extendAssignmentDeadline", "replaceWithNewAssignment")
            .doesNotContain(
                "patchAssignment",
                "updateAssignment",
                "changeAssignee",
                "changeCourse",
                "editAssignmentTests",
                "rewriteAssignment"
            );
        assertThat(Arrays.stream(AssignmentAdministrativeActionService.class.getDeclaredClasses())
            .map(Class::getSimpleName))
            .containsExactlyInAnyOrder(
                "CancelAssignmentCommand",
                "ExtendAssignmentDeadlineCommand",
                "ReplaceWithNewAssignmentCommand"
            );
        assertThat(Arrays.stream(
            AssignmentAdministrativeActionService.ReplaceWithNewAssignmentCommand.class.getRecordComponents()
        ).map(component -> component.getName()))
            .containsExactly("assignmentId", "campaignId", "newCycleDeadlineAt", "note")
            .doesNotContain("userId", "courseId", "status", "assignmentTests", "replacementAssignment");
        assertThat(methodNames(AssignmentAdministrativeActionServiceBoundaryTest.class))
            .contains("administrativeContourRemainsTypedAndDoesNotDegradeIntoGenericPatchSurface");
    }

    @Test
    void onlyCanonicalAdministrativeActionTypesRemainAllowed() {
        assertThat(Arrays.stream(AssignmentAdministrativeActionType.values()))
            .containsExactly(
                AssignmentAdministrativeActionType.CANCEL_ASSIGNMENT,
                AssignmentAdministrativeActionType.EXTEND_DEADLINE,
                AssignmentAdministrativeActionType.REPLACE_WITH_NEW_ASSIGNMENT
            );
        assertThat(methodNames(AssignmentAdministrativeActionServiceImplHappyPathTest.class))
            .contains("cancelAssignmentCancelsActiveAssignmentCreatesTypedHistoryRefreshesStatusAndAudits");
        assertThat(methodNames(AssignmentAdministrativeActionServiceImplExtendDeadlineHappyPathTest.class))
            .contains("extendDeadlineReopensOverdueAssignmentCreatesTypedHistoryRefreshesStatusAndAudits");
        assertThat(methodNames(AssignmentAdministrativeActionServiceImplReplaceWithNewMutationTest.class))
            .contains("replaceWithNewCancelsOldCycleCreatesNewCycleRefreshesStatusAndWritesSynchronousAuditCompanion");
    }

    @Test
    void replacementRegressionGuardsKeepAssigneeAndCourseInvariant() {
        assertThat(methodNames(AssignmentAdministrativeActionServiceImplReplaceWithNewValidationTest.class))
            .contains(
                "replacementValidationRejectsAttemptToChangeAssignee",
                "replacementValidationRejectsAttemptToChangeCourse",
                "canonicalReplacementIntentPreservesTypedCycleSemanticsWithoutGenericRewriteDrift"
            );
        assertThat(methodNames(AssignmentAdministrativeActionServiceImplReplaceWithNewMutationTest.class))
            .contains(
                "replaceWithNewCancelsOldCycleCreatesNewCycleRefreshesStatusAndWritesSynchronousAuditCompanion",
                "replaceWithNewAllowsOverdueTargetThroughCanonicalTypedPath"
            );
    }

    @Test
    void replacementRegressionGuardsPreserveProvenanceAndExplicitDeadlineRule() {
        assertThat(methodNames(AssignmentAdministrativeActionServiceImplReplaceWithNewValidationTest.class))
            .contains(
                "replacementValidationRejectsMissingCampaignId",
                "replacementValidationRejectsAdHocCampaignProvenanceChoice",
                "replacementValidationRejectsMissingNewCycleDeadlineSemantics",
                "replacementValidationAllowsCurrentTargetAsTheOnlyActiveAssignment"
            );
        assertThat(methodNames(AssignmentAdministrativeActionServiceImplReplaceWithNewMutationTest.class))
            .contains(
                "replaceWithNewCancelsOldCycleCreatesNewCycleRefreshesStatusAndWritesSynchronousAuditCompanion",
                "replaceWithNewAllowsOverdueTargetThroughCanonicalTypedPath"
            );
        assertThat(methodNames(AssignmentCampaignCommandServiceImplHappyPathTest.class))
            .contains("launchCreatesCampaignCompositionSnapshotsAssignmentsTestsRefreshAndAudit");
        assertThat(methodNames(AssignmentAdministrativeActionServiceImplHappyPathTest.class))
            .contains("cancelAssignmentCancelsActiveAssignmentCreatesTypedHistoryRefreshesStatusAndAudits");
        assertThat(methodNames(AssignmentAdministrativeActionServiceImplExtendDeadlineHappyPathTest.class))
            .contains(
                "extendDeadlineReopensOverdueAssignmentCreatesTypedHistoryRefreshesStatusAndAudits",
                "extendDeadlineKeepsAssignedStatusForAlreadyActiveAssignmentWhileChangingDeadlineFact"
            );
    }

    private Set<String> methodNames(Class<?> type) {
        return Stream.of(type.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toUnmodifiableSet());
    }

    private Set<String> publicMethodNames(Class<?> type) {
        return Stream.of(type.getDeclaredMethods())
            .filter(method -> Modifier.isPublic(method.getModifiers()))
            .map(Method::getName)
            .collect(Collectors.toUnmodifiableSet());
    }
}
