package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Собирает набор регрессионных проверок вокруг {@code AssignmentStatusPreviewAuditAntiDrift}.
 * Такие тесты помогают вовремя заметить незапланированные изменения.
 */
class AssignmentStatusPreviewAuditAntiDriftRegressionPackTest {

    @Test
    void statusContourStaysMaterializedCacheRatherThanOwnerTruth() {
        assertThat(methodNames(AssignmentStatusRecalculationServiceImplTest.class))
            .contains(
                "completedWhenAllAssignmentTestsHaveCanonicalCountedResultProof",
                "refreshCachePromotesAssignmentToCompletedWhenCanonicalProofAppears",
                "refreshCacheMaterializesCompletedClosureWhenStatusAlreadyCompletedButClosedAtIsMissing",
                "refreshCacheClearsStaleClosedAtWhenAssignmentReturnsToNonTerminalMaterializedState",
                "refreshCacheClearsStaleClosedAtForCancelledMaterializationWithoutCreatingManualPatchPath",
                "implementationStillDoesNotUseTestAttemptStatusShortcutOrCallSiteIntegration"
            );
        assertThat(methodNames(AssignmentStatusDefiningCountedResultFactsReaderBoundaryTest.class))
            .contains(
                "countedResultFactsReaderContractRemainsNarrowAndOwnerScoped",
                "statusRecalculationImplementationIsNotYetWidenedIntoResultFacadeOrAttemptStatusShortcut"
            );
    }

    @Test
    void schedulerAndReconciliationDoNotBecomeOwnerTruth() {
        assertThat(methodNames(loadTestClass(
            "com.vladislav.training.platform.assignment.infrastructure.scheduler.AssignmentStatusRecalculationSchedulerTest"
        )))
            .contains(
                "describeSchedulerReportsRuntimeMaterializedOptInScheduler",
                "triggerRecalculationDelegatesToBoundedBatchPass"
            );
        assertThat(methodNames(AssignmentReadinessGateTest.class))
            .contains("skeletonAndAuditCarriersRemainPresentAsCanonicalImplementationTargets");
        assertThat(methodNames(loadTestClass(
            "com.vladislav.training.platform.assignment.AssignmentScenarioTaxonomyConsistencyTest"
        )))
            .contains("statusVocabularyStaysInternalRecalculationContourAndNotManualStatusManagement");
    }

    @Test
    void previewContourStaysSeparatePreLaunchComputeReadRatherThanLaunchSubstitute() {
        assertThat(methodNames(AssignmentCampaignPreviewServiceImplTest.class))
            .contains(
                "previewRecipientPoolComputesFromCurrentTargetingCandidatesWithoutPersistenceArtifacts",
                "previewRecipientPoolStaysEphemeralWhenTargetingBasisIsArchived",
                "previewRecipientPoolRejectsUnsupportedTargetingBasisBeforeAnyPersistenceLikeFlowAppears",
                "previewExcludesNonOperatorRecipientRejectedByMandatoryLaunchEligibility"
            );
        assertThat(methodNames(AssignmentReadinessGateTest.class))
            .contains("queryPolicyClosureRemainsCarrierCompleteAndPreviewSeparatedFromPostLaunchRead");
    }

    @Test
    void previewLaunchParityRemainsGuardedOnTargetingAndEligibilitySemantics() {
        assertThat(methodNames(AssignmentCampaignTargetingSupportTest.class))
            .contains(
                "previewAndLaunchAcceptTheSameSupportedBasisSet",
                "previewAndLaunchRejectTheSameUnsupportedBasisFailClosed"
            );
        assertThat(methodNames(MandatoryAssignmentRecipientEligibilityServiceTest.class))
            .contains("seamReturnsSameCanonicalIneligibilityForPreviewFilteringAndLaunchFailClosed");
        assertThat(methodNames(AssignmentCampaignPreviewServiceImplTest.class))
            .contains(
                "previewExcludesUserWithoutValidActivePrimaryHomeUnit",
                "previewExcludesUserWithInvalidHomeUnitSemanticsForMandatoryAssignmentContour",
                "previewAndLaunchStayAlignedOnCriticalMandatoryEligibilitySemantics"
            );
    }

    @Test
    void auditContourStaysSynchronousCompanionWithCanonicalAnchorsAndUnifiedActorDiscipline() {
        assertThat(AssignmentCriticalAuditCatalog.CAMPAIGN_LAUNCH.auditEntityType()).isEqualTo("assignment_campaign");
        assertThat(AssignmentCriticalAuditCatalog.CAMPAIGN_LAUNCH.anchorsCampaignRoot()).isTrue();
        assertThat(AssignmentCriticalAuditCatalog.ASSIGNMENT_CANCEL.auditEntityType()).isEqualTo("assignment");
        assertThat(AssignmentCriticalAuditCatalog.ASSIGNMENT_CANCEL.anchorsAssignmentRoot()).isTrue();
        assertThat(AssignmentCriticalAuditCatalog.ASSIGNMENT_DEADLINE_EXTEND.auditEntityType()).isEqualTo("assignment");
        assertThat(AssignmentCriticalAuditCatalog.ASSIGNMENT_REPLACE_WITH_NEW.auditEntityType()).isEqualTo("assignment");

        assertThat(methodNames(AssignmentCriticalAuditCatalogTest.class))
            .contains(
                "assignmentCriticalAuditCatalogFixesMandatoryAnchorsAndCoverage",
                "criticalAuditCatalogDoesNotMixAuditWithOwnerHistoryOrReadSide"
            );
        assertThat(methodNames(AssignmentCriticalAuditPlannerImplTest.class))
            .contains(
                "launchCompanionPlanFixesCampaignRootAndSynchronousCompanionSemantics",
                "administrativeCompanionPlanDifferentiatesTypedAdministrativeFamilies"
            );
        assertThat(methodNames(AssignmentAdministrativeActionServiceBoundaryTest.class))
            .contains("criticalAssignmentCommandsUseSingleInteractiveActorResolutionDiscipline");
        assertThat(methodNames(AssignmentCampaignCommandServiceImplHappyPathTest.class))
            .contains("launchCreatesCampaignCompositionSnapshotsAssignmentsTestsRefreshAndAudit");
        assertThat(methodNames(AssignmentCampaignCommandServiceImplRejectPathTest.class))
            .contains("missingInteractiveAuditActorFailsClosedWithoutFallbackOnLaunch");
    }

    private Set<String> methodNames(Class<?> type) {
        return Stream.of(type.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toUnmodifiableSet());
    }

    private Class<?> loadTestClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("Expected regression coverage class is missing: " + className, exception);
        }
    }
}

