package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.assignment.repository.AssignmentAdministrativeActionRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentCampaignReadRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentReadRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentSelfScopedReadRepository;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.result.repository.ResultRepository;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Следит за тем, чтобы {@code AssignmentService} не менялся незаметно.
 * Тест держит под контролем важные договорённости.
 */
class AssignmentServiceRegressionGateTest {

    @Test
    void launchAndAdministrativeOwnerFlowRegressionCoverageRemainsPresent() {
        assertThat(methodNames(AssignmentCampaignCommandServiceImplHappyPathTest.class))
            .contains("launchCreatesCampaignCompositionSnapshotsAssignmentsTestsRefreshAndAudit");
        assertThat(methodNames(AssignmentCampaignCommandServiceImplRejectPathTest.class))
            .contains("emptyRecipientPoolFailsClosed", "duplicateActiveAssignmentFailsClosed");

        assertThat(methodNames(AssignmentAdministrativeActionServiceImplHappyPathTest.class))
            .contains("cancelAssignmentCancelsActiveAssignmentCreatesTypedHistoryRefreshesStatusAndAudits");
        assertThat(methodNames(AssignmentAdministrativeActionServiceImplRejectPathTest.class))
            .contains(
                "admissionDeniedStopsBeforeMutation",
                "assignmentNotFoundRejectsBeforeTypedHistoryOrAudit",
                "cancelOnAlreadyCancelledAssignmentFailsClosed",
                "auditFailurePropagatesInsideTransactionalCancelBoundary"
            );

        assertThat(methodNames(AssignmentAdministrativeActionServiceImplExtendDeadlineHappyPathTest.class))
            .contains(
                "extendDeadlineReopensOverdueAssignmentCreatesTypedHistoryRefreshesStatusAndAudits",
                "extendDeadlineKeepsAssignedStatusForAlreadyActiveAssignmentWhileChangingDeadlineFact"
            );
        assertThat(methodNames(AssignmentAdministrativeActionServiceImplExtendDeadlineRejectPathTest.class))
            .contains(
                "assignmentNotFoundRejectsBeforeTypedHistoryOrAudit",
                "equalDeadlineIsRejectedAsNoOpAdministrativeCommand",
                "overdueAssignmentDoesNotReopenIfNewDeadlineDoesNotRestoreLiveWindow",
                "auditFailurePropagatesInsideTransactionalExtendBoundary"
            );

        assertThat(methodNames(AssignmentAdministrativeActionServiceImplReplaceWithNewValidationTest.class))
            .contains(
                "replaceWithNewRejectsWhenTargetAssignmentDoesNotExist",
                "replacementValidationRejectsMissingCampaignId",
                "replacementValidationRejectsDeadlineEarlierThanAssignedAt",
                "replacementValidationRejectsHiddenSecondActiveAssignmentSemantics"
            );
        assertThat(methodNames(AssignmentAdministrativeActionServiceImplReplaceWithNewMutationTest.class))
            .contains(
                "replaceWithNewCancelsOldCycleCreatesNewCycleRefreshesStatusAndWritesSynchronousAuditCompanion",
                "replaceWithNewFailsClosedWhenNewCyclePersistenceCollides",
                "replacementAdministrativeActionPersistenceFailurePropagatesInsideTransactionalBoundary",
                "replacementAuditFailurePropagatesAndDoesNotLeaveSuccessfulOutwardCompletion"
            );
    }

    @Test
    void queryStatusAndAuditRegressionCoverageRemainsPresent() {
        assertThat(methodNames(AssignmentCampaignQueryServiceImplTest.class))
            .contains(
                "recipientSnapshotDetailReadUsesPostLaunchCampaignContourAndReturnsHistoricalSnapshot",
                "recipientSnapshotCampaignReadUsesPersistedSnapshotContourWithoutPreviewRecompute",
                "recipientSnapshotUserReadReturnsPersistedHistoricalSnapshotsAndAllowsEmptyResult"
            );
        assertThat(methodNames(AssignmentQueryServiceImplTest.class))
            .contains(
                "assignmentRootListReadByCampaignUsesAssignmentContourThroughCanonicalPipeline",
                "assignmentRootListReadByUserReturnsPersistedAssignmentsAndAllowsEmptyResult",
                "assignmentRootListReadByUserAndStatusUsesPersistedAssignmentRootFacts",
                "activeAssignmentReadByUserAndCourseUsesAssignmentContourAndReturnsCurrentActiveRoot",
                "assignmentTestDetailReadUsesPersistedSubordinateContour",
                "assignmentAdministrativeActionsByAssignmentIdUseTypedHistoryContour"
            );

        assertThat(methodNames(AssignmentStatusRecalculationServiceImplTest.class))
            .contains(
                "cancelledHasHighestPriorityEvenWhenCompletionProofExists",
                "completedWhenAllAssignmentTestsHaveCanonicalCountedResultProof",
                "missingCountedResultIdPreventsCompleted",
                "failedCountedResultPreventsCompleted",
                "countedResultOutsideDeadlinePreventsCompleted",
                "countedResultNotCountedInAssignmentPreventsCompleted",
                "overdueWhenDeadlinePassedAndCompletionIsIncomplete",
                "assignedWhenDeadlineNotPassedAndCompletionIsIncomplete",
                "refreshCacheIsIdempotentWhenStatusAndCompletedClosureAlreadyMatchCanonicalProof",
                "implementationStillDoesNotUseTestAttemptStatusShortcutOrCallSiteIntegration"
            );
        assertThat(methodNames(AssignmentStatusDefiningCountedResultFactsReaderBoundaryTest.class))
            .contains(
                "countedResultFactsReaderContractRemainsNarrowAndOwnerScoped",
                "statusRecalculationImplementationIsNotYetWidenedIntoResultFacadeOrAttemptStatusShortcut"
            );

        assertThat(methodNames(AssignmentCriticalAuditCatalogTest.class))
            .contains(
                "assignmentCriticalAuditCatalogFixesMandatoryAnchorsAndCoverage",
                "criticalAuditCatalogDoesNotMixAuditWithOwnerHistoryOrReadSide"
            );
        assertThat(methodNames(AssignmentCriticalAuditPlannerImplTest.class))
            .contains(
                "launchCompanionPlanFixesCampaignRootAndSynchronousCompanionSemantics",
                "administrativeCompanionPlanDifferentiatesTypedAdministrativeFamilies",
                "readinessSupportDoesNotAdvertiseSpringRuntimeOrAsyncBehavior"
            );
    }

    @Test
    void boundaryAndTaxonomyRegressionGuardsRemainPresentForAssignmentClosure() {
        assertThat(methodNames(AssignmentCampaignContractHardeningTest.class))
            .contains(
                "campaignCommandServiceNoLongerExposesMutableCrudLikeMethods",
                "campaignRepositoriesDoNotExposeExplicitHistoricalRewriteMethods"
            );
        assertThat(methodNames(AssignmentCampaignAntiMutableRegressionTest.class))
            .contains(
                "commandSurfaceStaysLaunchOnlyWithoutStandaloneMutableCampaignEntrypoints",
                "implementationDoesNotExposePublicBypassAroundLaunchOnlySemantics",
                "launchDocumentationAndServiceImplementationStayHistoricalRatherThanCrudLike",
                "existingLaunchAndReadSmokeCoverageRemainsCompatibleWithAntiDriftGuard"
            );
        assertThat(methodNames(AssignmentAdministrativeActionServiceBoundaryTest.class))
            .contains(
                "administrativeContourRemainsTypedAndDoesNotDegradeIntoGenericPatchSurface",
                "statusRecalculationIsWiredOnlyIntoAssignmentOwnerCommandContours"
            );
        assertThat(methodNames(AssignmentReadinessGateTest.class))
            .contains(
                "commandPolicyClosureRemainsCarrierCompleteAndWiredThroughCanonicalAdmissionPath",
                "queryPolicyClosureRemainsCarrierCompleteAndPreviewSeparatedFromPostLaunchRead",
                "dangerousDriftAndHalfWiredSurfaceGuardsRemainFailClosed",
                "skeletonAndAuditCarriersRemainPresentAsCanonicalImplementationTargets"
            );
        assertThat(methodNames(AssignmentSelfScopedQueryServiceContractTest.class))
            .contains(
                "selfScopedContractExposesSingleActorBoundReadPathForListDetailAndLearningContext",
                "selfScopedContractDoesNotExposeArbitrarySubjectSelectorShape",
                "selfScopedContractDoesNotMixLaunchAdministrativeOrStatusVocabulary"
            );
        assertThat(methodNames(AssignmentSelfScopedQueryServiceImplTest.class))
            .contains(
                "selfScopedListChecksPolicyBeforeUsingSelfScopedRepository",
                "selfScopedDetailChecksPolicyBeforeUsingSelfScopedRepository",
                "deniedSelfScopedReadStopsBeforeRepositoryAccess",
                "assignedLearningContextReusesAssignmentAnchoredDetailPolicyPathAndAddsSubordinateTests",
                "deniedAssignedLearningContextStopsBeforeRepositoryAccess",
                "selfScopedDetailPropagatesOwnerSafeNotFoundFromSelfScopedRepositoryPath"
            );
        assertThat(methodNames(loadTestClass(
            "com.vladislav.training.platform.assignment.AssignmentBoundedContextConsistencyTest"
        ))).isNotEmpty();
        assertThat(methodNames(loadTestClass(
            "com.vladislav.training.platform.assignment.AssignmentScenarioTaxonomyConsistencyTest"
        )))
            .contains(
                "launchVocabularyStaysLaunchOrientedAndDoesNotDriftIntoCampaignCrud",
                "administrativeVocabularyStaysTypedAndDoesNotDegradeIntoPatchOrOverrideSurface",
                "selfScopedReadVocabularyStaysActorBoundAndDoesNotReintroduceBroadSubjectReads",
                "statusVocabularyStaysInternalRecalculationContourAndNotManualStatusManagement",
                "auditVocabularyStaysSynchronousCompanionAndNotOwnerHistorySubstitute"
            );
    }

    @Test
    void serviceApplicationContoursRemainClosedAgainstCrudPatchScn20AndAuditDrift() throws IOException {
        assertThat(methodNames(AssignmentCampaignCommandService.class))
            .contains("launchAssignmentCampaign")
            .doesNotContain("updateAssignmentCampaign", "patchAssignmentCampaign", "removeCourseFromCampaign");
        assertThat(methodNames(AssignmentAdministrativeActionService.class))
            .containsExactlyInAnyOrder("cancelAssignment", "extendAssignmentDeadline", "replaceWithNewAssignment")
            .doesNotContain("patchAssignment", "updateAssignment", "changeAssignee", "editAssignmentTests");

        assertThat(fieldTypes(AssignmentCampaignCommandServiceImpl.class))
            .contains(AssignmentStatusRecalculationService.class)
            .doesNotContain(AssignmentCampaignQueryService.class, AssignmentQueryService.class);
        assertThat(fieldTypes(AssignmentAdministrativeActionServiceImpl.class))
            .contains(
                AssignmentAdministrativeActionRepository.class,
                AssignmentStatusRecalculationService.class
            )
            .doesNotContain(AssignmentCampaignQueryService.class, AssignmentQueryService.class);
        assertThat(fieldTypes(AssignmentQueryServiceImpl.class))
            .contains(AssignmentReadRepository.class)
            .doesNotContain(AssignmentStatusRecalculationService.class, ResultRepository.class);
        assertThat(fieldTypes(AssignmentSelfScopedQueryServiceImpl.class))
            .contains(
                AssignmentSelfScopedReadRepository.class,
                AccessSpecificationPolicy.class,
                AccessPolicyQueryContextResolver.class
            )
            .doesNotContain(AssignmentReadRepository.class, AssignmentStatusRecalculationService.class, ResultRepository.class);
        assertThat(fieldTypes(AssignmentCampaignQueryServiceImpl.class))
            .contains(AssignmentCampaignReadRepository.class)
            .doesNotContain(AssignmentStatusRecalculationService.class, ResultRepository.class);
        assertThat(fieldTypes(AssignmentStatusRecalculationServiceImpl.class))
            .contains(AssignmentStatusDefiningCountedResultFactsReader.class)
            .doesNotContain(CriticalCommandAuditSupport.class, AssignmentCriticalAuditPlanner.class);

        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/service/AssignmentStatusRecalculationServiceImpl.java"))
            .contains("counted-result proof for COMPLETED")
            .doesNotContain("TestAttempt.status")
            .doesNotContain("recordAudit");
        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/service/AssignmentAdministrativeActionService.java"))
            .contains("typed administrative action contour")
            .contains("status refresh/reconciliation belongs to")
            .contains("generic patch semantics");
        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/service/AssignmentQueryServiceImpl.java"))
            .doesNotContain("SCN-20")
            .doesNotContain("reporting")
            .doesNotContain("audit trail");
        assertThat(read("src/main/java/com/vladislav/training/platform/assignment/service/AssignmentSelfScopedQueryServiceImpl.java"))
            .contains("resolveActorSelfScope")
            .contains("AssignmentSelfScopedReadRepository")
            .doesNotContain("AssignmentReadRepository");
    }

    private Set<String> methodNames(Class<?> type) {
        return Stream.of(type.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toUnmodifiableSet());
    }

    private Set<Class<?>> fieldTypes(Class<?> type) {
        return Stream.of(type.getDeclaredFields())
            .filter(field -> !Modifier.isStatic(field.getModifiers()))
            .map(Field::getType)
            .collect(Collectors.toUnmodifiableSet());
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }

    private Class<?> loadTestClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("Expected regression coverage class is missing: " + className, exception);
        }
    }
}


