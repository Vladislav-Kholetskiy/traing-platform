package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Собирает набор регрессионных проверок вокруг {@code TestingSubmissionSurfaceDecomposition}.
 * Такие тесты помогают вовремя заметить незапланированные изменения.
 */
class TestingSubmissionSurfaceDecompositionRegressionPackTest {

    @Test
    void genericTestingWideSubmissionContractNoLongerExists() {
        assertThat(Files.exists(Path.of(
            "src/main/java/com/vladislav/training/platform/testing/service/AttemptSubmissionService.java"
        ))).isFalse();
    }

    @Test
    void submitAndAbandonSurfacesStaySplitIntoDedicatedAssignedSelfAndSelfAbandonContours() {
        assertThat(publicMethodNames(AssignedAttemptSubmissionService.class))
            .containsExactly("submitAssignedAttempt")
            .doesNotContain("submitAttempt", "submitSelfAttempt", "abandonSelfAttempt");
        assertThat(publicMethodNames(SelfAttemptSubmitSequencingService.class))
            .containsExactly("submitSelfAttempt")
            .doesNotContain("submitAttempt", "submitAssignedAttempt", "abandonSelfAttempt");
        assertThat(publicMethodNames(SelfAttemptAbandonSequencingService.class))
            .containsExactly("abandonSelfAttempt")
            .doesNotContain("submitAttempt", "submitAssignedAttempt", "submitSelfAttempt");
    }

    @Test
    void entryReadAnswerAndTerminalContoursDoNotRecoverGenericSubmissionSemantics() {
        assertThat(publicMethodNames(AssignedAttemptEntryService.class))
            .doesNotContain("submitAttempt", "submitAssignedAttempt", "submitSelfAttempt");
        assertThat(publicMethodNames(SelfAttemptEntryService.class))
            .doesNotContain("submitAttempt", "submitAssignedAttempt", "submitSelfAttempt");
        assertThat(publicMethodNames(ActiveAttemptOwnerLocalReadService.class))
            .doesNotContain("submitAttempt", "submitAssignedAttempt", "submitSelfAttempt");
        assertThat(publicMethodNames(ActiveAttemptAnswerMutationService.class))
            .doesNotContain("submitAttempt", "submitAssignedAttempt", "submitSelfAttempt");
        assertThat(publicMethodNames(AssignedAttemptSubmitTerminalService.class))
            .doesNotContain("submitAttempt", "submitSelfAttempt", "abandonSelfAttempt");
        assertThat(publicMethodNames(SelfAttemptSubmitTerminalService.class))
            .doesNotContain("submitAttempt", "submitAssignedAttempt", "abandonSelfAttempt");
        assertThat(publicMethodNames(SelfAttemptAbandonTerminalService.class))
            .doesNotContain("submitAttempt", "submitAssignedAttempt", "submitSelfAttempt");
    }

    @Test
    void sourceLayerDoesNotReintroduceGenericSubmissionVocabulary() throws IOException {
        String assignedSubmitSource = read("src/main/java/com/vladislav/training/platform/testing/service/AssignedAttemptSubmissionService.java");
        String selfSubmitSource = read("src/main/java/com/vladislav/training/platform/testing/service/SelfAttemptSubmitSequencingService.java");
        String selfAbandonSource = read("src/main/java/com/vladislav/training/platform/testing/service/SelfAttemptAbandonSequencingService.java");

        assertThat(assignedSubmitSource + selfSubmitSource + selfAbandonSource)
            .doesNotContain(
                "submitAttempt(",
                "interface AttemptSubmissionService",
                "universal submit",
                "generic submission",
                "AttemptSubmitManager"
            );
    }

    private List<String> publicMethodNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
            .filter(method -> Modifier.isPublic(method.getModifiers()))
            .map(Method::getName)
            .toList();
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
