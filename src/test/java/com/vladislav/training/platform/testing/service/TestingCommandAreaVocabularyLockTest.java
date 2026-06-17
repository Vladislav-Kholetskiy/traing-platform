package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
/**
 * Фиксирует словарь и смысловые границы вокруг {@code TestingCommandArea}.
 * Это помогает не расползтись именам и договорённостям.
 */
class TestingCommandAreaVocabularyLockTest {

    @Test
    void testingServicePackageFixesScenarioSplitWithoutGenericExecutionDrift() throws IOException {
        String packageInfo = read("src/main/java/com/vladislav/training/platform/testing/service/package-info.java");

        assertThat(packageInfo)
            .contains("assigned execution entry ({@code SCN-01})")
            .contains("self execution entry ({@code SCN-02})")
            .contains("terminal lifecycle transitions owned by {@code test_attempt} ({@code SCN-08})")
            .contains("handoff into immutable result recording")
            .contains("not a single generic execution surface")
            .contains("submit does not replace immutable result recording");
    }

    @Test
    void scenarioSpecificSubmitAbandonAndRecoveryContractsKeepDistinctVocabulary() throws IOException {
        String recalculationContract = read(
            "src/main/java/com/vladislav/training/platform/testing/service/AttemptStatusRecalculationService.java"
        );
        String assignedSubmitSequencingSource = read(
            "src/main/java/com/vladislav/training/platform/testing/service/AssignedAttemptSubmitSequencingService.java"
        );

        assertThat(Files.exists(Path.of("src/main/java/com/vladislav/training/platform/testing/service/AttemptCommandService.java")))
            .isFalse();
        assertThat(Files.exists(Path.of("src/main/java/com/vladislav/training/platform/testing/service/AttemptLifecycleService.java")))
            .isFalse();
        assertThat(Files.exists(Path.of("src/main/java/com/vladislav/training/platform/testing/service/AttemptAnswerService.java")))
            .isFalse();
        assertThat(Files.exists(Path.of("src/main/java/com/vladislav/training/platform/testing/service/AttemptQueryService.java")))
            .isFalse();

        assertThat(Files.exists(Path.of("src/main/java/com/vladislav/training/platform/testing/service/AttemptSubmissionService.java")))
            .isFalse();
        assertThat(methodNames(AssignedAttemptEntryService.class))
            .containsExactly("enterAssignedAttempt")
            .doesNotContain("startAttempt", "resumeAttempt", "submitAttempt", "recordResult");
        assertThat(methodNames(SelfAttemptEntryService.class))
            .containsExactly("startOrContinueSelfAttempt")
            .doesNotContain("startAttempt", "resumeAttempt", "submitAttempt", "recordResult");
        assertThat(methodNames(AssignedAttemptSubmissionService.class))
            .containsExactly("submitAssignedAttempt")
            .doesNotContain("submitAttempt", "submitSelfAttempt", "abandonSelfAttempt");
        assertThat(methodNames(SelfAttemptSubmitSequencingService.class))
            .containsExactly("submitSelfAttempt")
            .doesNotContain("submitAttempt", "submitAssignedAttempt", "abandonSelfAttempt");
        assertThat(methodNames(SelfAttemptAbandonSequencingService.class))
            .containsExactly("abandonSelfAttempt")
            .doesNotContain("submitAttempt", "submitAssignedAttempt", "submitSelfAttempt");

        assertThat(methodNames(AttemptStatusRecalculationService.class))
            .containsExactlyInAnyOrder(
                "recalculateAttemptStatus",
                "refreshAttemptStatusCache",
                "refreshAttemptStatusCacheWithVerdict",
                "refreshAssignedAttemptStatusCacheWithVerdict"
            )
            .doesNotContain("completeAttempt", "submitAttempt", "recordResult");
        assertThat(recalculationContract)
            .contains("Recovery and reconciliation contract")
            .contains("does not replace owner command semantics")
            .contains("does not become a manual completion surface");
        assertThat(assignedSubmitSequencingSource)
            .doesNotContain("return null")
            .contains("AssignedAttemptSubmitOutcome")
            .contains("TestAttemptStatus.COMPLETED")
            .contains("TestAttemptStatus.EXPIRED")
            .contains("IllegalArgumentException")
            .contains("supports only COMPLETED with recordedResult or EXPIRED without recordedResult");
    }

    @Test
    void assignedEntryVocabularyMustNotKeepStartOrContinueAsCanonicalOperationName() throws IOException {
        String assignedEntrySource = read(
            "src/main/java/com/vladislav/training/platform/testing/service/AssignedAttemptEntryService.java"
        );

        assertThat(methodNames(AssignedAttemptEntryService.class))
            .doesNotContain("startOrContinueAssignedAttempt");
        assertThat(assignedEntrySource)
            .doesNotContain("startOrContinueAssignedAttempt")
            .doesNotContain("start/continue semantics")
            .contains("enterAssignedAttempt(")
            .contains("checkAssignedAttemptContinue(");
    }

    private Set<String> methodNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
            .filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
            .map(Method::getName)
            .collect(Collectors.toUnmodifiableSet());
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
