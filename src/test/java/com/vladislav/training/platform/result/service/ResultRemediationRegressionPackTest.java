package com.vladislav.training.platform.result.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * Собирает набор регрессионных проверок вокруг {@code ResultRemediation}.
 * Такие тесты помогают вовремя заметить незапланированные изменения.
 */
class ResultRemediationRegressionPackTest {

    private static final String STEP_6_TARGET_REASON =
        "Step 6 target test: enable after canonical result scoring evaluator is implemented";
    private static final String STEP_7_TARGET_REASON =
        "Step 7 target test: enable after canonical assignment counted result policy is implemented";

    @Test
    void countedAssignmentHandoffUsesDedicatedCanonicalValidityGateInsteadOfLocalBroadShortcut() throws IOException {
        String source = read("src/main/java/com/vladislav/training/platform/result/service/ResultRecordingServiceImpl.java");
        String gateSource = read(
            "src/main/java/com/vladislav/training/platform/result/service/CountedAssignmentResultValidator.java"
        );

        assertThat(source)
            .contains("countedAssignmentResultValidityGate.allowsAssignmentCountedHandoff(")
            .doesNotContain("private void handoffValidCountedAssignmentResultIfEligible(Result canonicalResult)")
            .doesNotContain("acceptCanonicalResult(");
        assertThat(gateSource)
            .contains("!Boolean.TRUE.equals(materializedResult.countedInAssignment())")
            .contains("!Boolean.TRUE.equals(materializedResult.withinDeadline())")
            .contains("materializedResult.scoringSnapshot() != null && materializedResult.scoringSnapshot().passed()");
    }

    @Test
    void stageSevenCountedPolicyTargetTestsAreActiveAfterCanonicalPolicyRemediation() throws IOException {
        String source = read("src/test/java/com/vladislav/training/platform/result/service/ResultRecordingServiceImplTest.java");

        assertThat(source)
            .contains("STEP_7_TARGET_DISABLED_REASON")
            .contains(STEP_7_TARGET_REASON)
            .doesNotContain("@Disabled(STEP_7_TARGET_DISABLED_REASON)")
            .contains("newlyMaterializedNonCountedAssignedResultDoesNotTriggerDownstreamHandoff")
            .contains("lateAssignedResultDoesNotInvokeCountedHandoff")
            .contains("failedAssignedResultDoesNotInvokeCountedHandoff")
            .contains("selfResultWillNeverBeCountedInAssignment")
            .contains("passedAssignedResultWithinDeadlineWillBeCountedOnlyAfterSuccessfulResultMaterialization")
            .contains("countedHandoffMustNotBeDerivedFromTerminalAttemptStatus")
            .contains("countedDecisionMatrixWillRequireAssignedPassedWithinDeadlineResult")
            .contains("assertThat(savedResult.countedInAssignment()).isNull()")
            .contains("assertThat(savedResult.withinDeadline()).isNull()")
            .contains("assertThat(savedResult.scoringSnapshot().passed()).isFalse()")
            .doesNotContain("newlyMaterializedOverdueAssignedCountedResultDoesNotTriggerDownstreamHandoff")
            .doesNotContain("newlyMaterializedFailedAssignedCountedResultDoesNotTriggerDownstreamHandoff");
    }

    @Test
    void selfResultProviderAndServiceProofsRemainPresent() throws IOException {
        String providerTestSource = read("src/test/java/com/vladislav/training/platform/result/service/ResultRecordingSnapshotFactsProviderTest.java");
        String serviceTestSource = read("src/test/java/com/vladislav/training/platform/result/service/ResultRecordingServiceImplTest.java");

        assertThat(providerTestSource)
            .contains("selfRootFactsComeFromCanonicalSelfOrgSnapshotSource")
            .contains("selfRootFactsFailClosedWhenCanonicalOrgContextSourceDoesNotExist");
        assertThat(serviceTestSource)
            .contains("selfResultRecordingMaterializesRootSnapshotFieldsFromRealProviderFacts");
    }

    @Test
    void stageSixScoringTargetTestsAreActiveAfterCanonicalEvaluatorRemediation() throws IOException {
        String providerTestSource = read("src/test/java/com/vladislav/training/platform/result/service/ResultRecordingSnapshotFactsProviderTest.java");
        String subordinateSource = read(
            "src/test/java/com/vladislav/training/platform/result/service/ResultRecordingSubordinateSnapshotMaterializerTest.java"
        );

        assertThat(providerTestSource)
            .contains("STEP_6_TARGET_DISABLED_REASON")
            .contains(STEP_6_TARGET_REASON)
            .doesNotContain("@Disabled(STEP_6_TARGET_DISABLED_REASON)")
            .contains("rootResultEarnedScoreWillEqualSumOfCanonicalQuestionEarnedScores")
            .contains("rootAndQuestionSnapshotsWillUseSameCanonicalScoringDecision")
            .contains("scoringTargetsDoNotDeriveAssignmentCountedSemantics")
            .contains("isGreaterThan(BigDecimal.ZERO.setScale(4))")
            .contains("isLessThan(observation.rootFacts().scoringSnapshot().maxScore())");
        assertThat(subordinateSource)
            .contains("STEP_6_TARGET_DISABLED_REASON")
            .contains(STEP_6_TARGET_REASON)
            .doesNotContain("@Disabled(STEP_6_TARGET_DISABLED_REASON)")
            .contains("canonicalEvaluatorWillAssignPartialCreditForMultipleChoiceAnswers")
            .contains("canonicalEvaluatorWillAssignPartialCreditForMatchingAnswers")
            .contains("canonicalEvaluatorWillAssignPartialCreditForOrderingAnswers")
            .contains("canonicalEvaluatorWillRejectDuplicatePersistedMatchingPairs")
            .contains("canonicalEvaluatorWillRejectDuplicatePersistedOrderingFacts")
            .contains("isLessThan(savedSnapshot.maxScore())")
            .contains(".hasMessageContaining(\"matching\")")
            .contains(".hasMessageContaining(\"ordering\")");
    }

    @Test
    void duplicateAnswerStateRegressionProofsRemainFailClosedAcrossMutationAndResultRecording() throws IOException {
        String mutationSource = read(
            "src/test/java/com/vladislav/training/platform/testing/service/ActiveAttemptAnswerMutationServiceTest.java"
        );
        String providerSource = read(
            "src/test/java/com/vladislav/training/platform/result/service/ResultRecordingSnapshotFactsProviderTest.java"
        );
        String serviceSource = read(
            "src/test/java/com/vladislav/training/platform/result/service/ResultRecordingServiceImplTest.java"
        );
        String subordinateSource = read(
            "src/test/java/com/vladislav/training/platform/result/service/ResultRecordingSubordinateSnapshotMaterializerTest.java"
        );
        String materializerSource = read(
            "src/main/java/com/vladislav/training/platform/result/service/ResultRecordingSubordinateSnapshotMaterializer.java"
        );

        assertThat(mutationSource)
            .contains("rejectsDuplicateChoiceAnswerItemsWithoutAnyAnswerWriteOrSilentDeduplication")
            .contains("rejectsDuplicateMatchingExactPairsWithoutAnyAnswerWriteOrSilentDeduplication")
            .contains("rejectsDuplicateMatchingLeftWithoutAnyAnswerWriteOrSilentDeduplication")
            .contains("rejectsDuplicateMatchingRightSideOwnershipWithoutAnyAnswerWrite")
            .contains("rejectsDuplicateOrderingAnswerOptionWithoutAnyAnswerWriteOrSilentDeduplication")
            .contains("rejectsDuplicateOrderingUserPositionWithoutAnyAnswerWriteOrSilentDeduplication")
            .contains("verify(userAnswerItemRepository, never()).saveUserAnswerItem(any(UserAnswerItem.class))")
            .contains(".isInstanceOf(ConflictException.class)");

        assertThat(providerSource)
            .contains("providerRejectsDuplicatePersistedChoiceRowsInsteadOfSilentSetDeduplication")
            .contains("providerRejectsDuplicatePersistedMatchingLeftStateInsteadOfSilentDeduplication")
            .contains("providerRejectsDuplicatePersistedMatchingRightStateInsteadOfSilentDeduplication")
            .contains("providerRejectsDuplicatePersistedMatchingExactPairStateInsteadOfSilentDeduplication")
            .contains("providerRejectsDuplicatePersistedOrderingAnswerOptionStateInsteadOfSilentDeduplication")
            .contains("providerRejectsDuplicatePersistedOrderingUserPositionStateInsteadOfSilentDeduplication")
            .contains(".isInstanceOf(ConflictException.class)");

        assertThat(serviceSource)
            .contains("snapshotFactsProviderFailurePreventsResultSaveMaterializationAuditAndHandoff")
            .contains("verify(resultRepository, never()).saveResult(any(Result.class))");

        assertThat(subordinateSource)
            .contains("persistedAnswerFactMaterializationMustNotUseSilentDedupMergeFunctions");
        assertThat(materializerSource).doesNotContain("(left, right) -> left");
    }

    @Test
    void replayCompletenessProofsRemainPresentAcrossRootQuestionAndAnswerOptionLayers() throws IOException {
        String serviceSource = read(
            "src/test/java/com/vladislav/training/platform/result/service/ResultRecordingServiceImplTest.java"
        );
        String validatorSource = read(
            "src/test/java/com/vladislav/training/platform/result/service/ResultRecordingIdempotentReplayValidatorTest.java"
        );

        assertThat(validatorSource)
            .contains("acceptsIdenticalRootFactsReplay")
            .contains("rejectsReplayWhenAssignmentFactsDiffer")
            .contains("rejectsReplayWhenDeadlineCountedOrFinalControlFactsDiffer")
            .contains("rejectsReplayWhenCanonicalScoringSnapshotDiffers");

        assertThat(serviceSource)
            .contains("existingCanonicalCountedResultUsesIdempotentHandoffReplayWithoutNewResultMaterialization")
            .contains("secondRecordingForSameAttemptWithDifferentPayloadFailsClosedWithoutOverwrite")
            .contains("questionSnapshotStructuralDriftDoesNotPassIdempotentReplay")
            .contains("semanticQuestionSnapshotDriftDoesNotPassIdempotentReplay")
            .contains("incompleteChildSnapshotAggregateDoesNotPassIdempotentReplay")
            .contains("extraPersistedQuestionSnapshotDoesNotPassIdempotentReplay")
            .contains("answerOptionSemanticDriftDoesNotPassIdempotentReplay")
            .contains("answerOptionQuestionSnapshotBindingDriftDoesNotPassIdempotentReplay")
            .contains("missingExpectedAnswerOptionSnapshotDoesNotPassIdempotentReplay")
            .contains("extraPersistedAnswerOptionSnapshotDoesNotPassIdempotentReplay")
            .contains("verify(resultRepository, never()).saveResult(any(Result.class))")
            .contains("verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any())")
            .contains("verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any())");
    }

    @Test
    void testingLockInventoryKeepsHistoricalStageZeroNotesWhileMarkingCurrentRuntimeTruth() throws IOException {
        String inventory = read("docs/testing-test-lock-inventory.md");
        String step03 = read("docs/testing-step-0-3-read-test-tightening.md");
        String step04 = read("docs/testing-step-0-4-result-test-tightening.md");
        String step05 = read("docs/testing-step-0-5-counted-test-tightening.md");
        String step06 = read("docs/testing-step-0-6-stage-zero-closure.md");

        assertThat(Path.of("docs/testing-step-0-3-read-test-tightening.md")).exists();
        assertThat(Path.of("docs/testing-step-0-4-result-test-tightening.md")).exists();
        assertThat(Path.of("docs/testing-step-0-5-counted-test-tightening.md")).exists();
        assertThat(Path.of("docs/testing-step-0-6-stage-zero-closure.md")).exists();

        assertThat(step03).contains("# testing contour Step 0.3 Read Test Tightening");
        assertThat(step04)
            .contains("# testing contour Step 0.4 Result Test Tightening")
            .contains(STEP_6_TARGET_REASON);
        assertThat(step05)
            .contains("# testing contour Step 0.5 Counted Test Tightening")
            .contains(STEP_7_TARGET_REASON);
        assertThat(step06)
            .contains("# testing contour Step 0.6 Stage Zero Closure")
            .contains("Step 0.6 closes only the preparatory Stage 0 test layer")
            .contains("Step 3")
            .contains("Step 4")
            .contains("Step 6")
            .contains("Step 7")
            .doesNotContain("production remediation is complete")
            .doesNotContain("testing contour is ready");

        assertThat(inventory)
            .contains("## Current testing contour truth")
            .contains("Stage 12 regression sweep and runtime readiness are already confirmed by green testing contour targeted packs.")
            .contains("Stage 6 scoring target tests and Stage 7 counted-policy target tests are active and green")
            .contains("Stage 13 is a docs/package-info/Javadoc/meta-lock cleanup step only")
            .contains("analytics contour self-history, managerial analytics, and expert analytics remain outside testing contour scope.")
            .contains("## Historical inventory note")
            .contains("historical triage snapshot")
            .contains("must not be read as current runtime blockers")
            .contains("## Historical triage summary")
            .contains("## Historical rewrite queue snapshot")
            .contains("## Historical final note")
            .contains("## Step 0.3 closure note")
            .contains("## Step 0.4 closure note")
            .contains("## Step 0.5 closure note")
            .contains("## Step 0.6 closure note")
            .contains("TestingCommandContourVocabularyLockTest")
            .contains("ActiveAttemptAnswerMutationServiceTest")
            .contains("ResultRecordingSnapshotFactsProviderTest")
            .contains("ResultRecordingSubordinateSnapshotMaterializerTest")
            .contains("AssignmentCountedResultHandoffServiceImplTest")
            .contains("AssignmentStatusDefiningCountedResultFactsReaderAdapterTest")
            .contains("AssignmentStatusRecalculationServiceImplTest")
            .contains("AssignmentAssignedCompletionSchemaRegressionPackTest")
            .doesNotContain("newlyMaterializedOverdueAssignedCountedResultDoesNotTriggerDownstreamHandoff")
            .doesNotContain("newlyMaterializedFailedAssignedCountedResultDoesNotTriggerDownstreamHandoff");
    }

    @Test
    void selfSubmitSequencingFailureProofRemainsPresent() throws IOException {
        String source = read("src/test/java/com/vladislav/training/platform/testing/service/SelfAttemptSubmitSequencingServiceTest.java");

        assertThat(source)
            .contains("rejectsNonRecordableSelfTerminalizationOutcomeWithoutRecordingResult")
            .contains("requires recordable terminalization outcome")
            .contains("verifyNoInteractions(resultRecordingService");
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}

