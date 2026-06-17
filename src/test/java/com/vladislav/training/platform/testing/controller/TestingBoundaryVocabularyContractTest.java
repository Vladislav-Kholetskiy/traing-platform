package com.vladislav.training.platform.testing.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.application.policy.CapabilityOperationCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет договорённости вокруг {@code TestingBoundaryVocabulary}.
 * Тест помогает сохранить предсказуемое поведение.
 */
class TestingBoundaryVocabularyContractTest {

    private static final Path TESTING_CONTROLLER_DIR = Path.of(
        "src/main/java/com/vladislav/training/platform/testing/controller"
    );
    private static final Path RESULT_QUERY_DIR = Path.of(
        "src/main/java/com/vladislav/training/platform/result/query"
    );

    @Test
    void accessReadContourExposesCanonicalTestingAndPreparedReadContours() {
        assertThat(EnumSetView.namesOf(AccessReadArea.class))
            .contains(
                "ASSIGNED_CURRENT_ATTEMPT",
                "SELF_CURRENT_ATTEMPT",
                "SELF_VISIBLE_TESTING",
                "SELF_RESULT_HISTORY",
                "MANAGERIAL_CURRENT_SUPERVISION",
                "MANAGERIAL_HISTORICAL_ANALYTICS",
                "EXPERT_QUESTION_ANALYTICS"
            );
    }

    @Test
    void capabilityOperationCodeExposesCanonicalTestingOperationsWithoutGenericDrift() {
        assertThat(EnumSetView.namesOf(CapabilityOperationCode.class))
            .contains(
                "TESTING_ASSIGNED_ATTEMPT_START",
                "TESTING_ASSIGNED_ATTEMPT_CONTINUE",
                "TESTING_SELF_ATTEMPT_START",
                "TESTING_SELF_ATTEMPT_CONTINUE",
                "TESTING_ASSIGNED_ANSWER_MUTATION",
                "TESTING_SELF_ANSWER_MUTATION",
                "TESTING_ASSIGNED_ATTEMPT_SUBMIT",
                "TESTING_SELF_ATTEMPT_SUBMIT",
                "TESTING_SELF_ATTEMPT_ABANDON"
            )
            .doesNotContain(
                "TESTING_ASSIGNED_ATTEMPT_ABANDON",
                "ATTEMPT_MUTATE",
                "EXECUTION_UPDATE",
                "SAVE_EXECUTION_STATE",
                "COMPLETE_ATTEMPT_GENERIC"
            );
    }

    @Test
    void noGenericAttemptPatch() throws IOException {
        assertThat(controllerSimpleNames())
            .doesNotContain("AttemptPatchController", "AttemptController", "ExecutionController");
        assertThat(controllerSources(TESTING_CONTROLLER_DIR))
            .doesNotContain("@PatchMapping", "PatchMapping", "/api/v1/attempts", "/api/v1/execution");
    }

    @Test
    void noGenericResultPatch() throws IOException {
        assertThat(controllerSources(RESULT_QUERY_DIR))
            .doesNotContain("@PatchMapping", "PatchMapping", "/api/v1/results/{id}", "patchResult", "updateResult");
    }

    @Test
    void noGenericExternalAnswerMutationController() {
        assertThat(controllerSimpleNames())
            .contains("SelfAttemptAnswerMutationController", "AssignedAttemptAnswerMutationController")
            .doesNotContain("AnswerMutationController", "ExternalAnswerMutationController", "AttemptAnswerController");
    }

    @Test
    void noGenericExecutionFacadeExposedToControllers() throws IOException {
        assertThat(controllerSources(TESTING_CONTROLLER_DIR))
            .doesNotContain(
                "ExecutionFacade ",
                "ExecutionManager ",
                "AttemptManager ",
                " AttemptCommandService",
                " AttemptAnswerService",
                " AttemptLifecycleService",
                " AttemptSubmissionService",
                " AttemptQueryService",
                "import com.vladislav.training.platform.testing.service.AttemptCommandService;",
                "import com.vladislav.training.platform.testing.service.AttemptAnswerService;",
                "import com.vladislav.training.platform.testing.service.AttemptLifecycleService;",
                "import com.vladislav.training.platform.testing.service.AttemptSubmissionService;",
                "import com.vladislav.training.platform.testing.service.AttemptQueryService;"
            );
    }

    @Test
    void noTestingControllerWritesAssignmentOwnerState() throws IOException {
        assertThat(controllerSources(TESTING_CONTROLLER_DIR))
            .doesNotContain(
                "AssignmentCommandService",
                "AssignmentCampaignCommandService",
                "AssignmentCountedResultHandoffService",
                "CriticalCommandAuditSupport",
                "ResultRecordingService"
            );
    }

    private Set<String> controllerSimpleNames() {
        try (Stream<Path> paths = Files.list(TESTING_CONTROLLER_DIR)) {
            return paths
                .filter(path -> path.toString().endsWith(".java"))
                .map(path -> path.getFileName().toString().replace(".java", ""))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to list controller sources", exception);
        }
    }

    private String controllerSources(Path directory) throws IOException {
        try (Stream<Path> paths = Files.list(directory)) {
            return paths
                .filter(path -> path.toString().endsWith(".java"))
                .sorted()
                .map(this::read)
                .collect(Collectors.joining("\n"));
        }
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read source file: " + path, exception);
        }
    }

    private static final class EnumSetView {

        private EnumSetView() {
        }

        private static <E extends Enum<E>> List<String> namesOf(Class<E> enumType) {
            return Arrays.stream(enumType.getEnumConstants())
                .map(Enum::name)
                .toList();
        }
    }
}
