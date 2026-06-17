package com.vladislav.training.platform.testing.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.testing.controller.dto.ActiveAttemptAnswerMutationRequest;
import com.vladislav.training.platform.testing.controller.dto.ActiveAttemptAnswerMutationResponse;
import com.vladislav.training.platform.testing.controller.dto.AssignedAttemptEntryResponse;
import com.vladislav.training.platform.testing.controller.dto.AssignedAttemptSubmitResponse;
import com.vladislav.training.platform.testing.controller.dto.CurrentAttemptResponse;
import com.vladislav.training.platform.testing.controller.dto.SelfAttemptAbandonResponse;
import com.vladislav.training.platform.testing.controller.dto.SelfAttemptEntryResponse;
import com.vladislav.training.platform.testing.controller.dto.SelfAttemptSubmitResponse;
import com.vladislav.training.platform.testing.controller.dto.SelfVisibleTestCatalogEntryResponse;
import com.vladislav.training.platform.testing.controller.dto.SelfVisibleTestResponse;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
/**
 * Собирает набор регрессионных проверок вокруг {@code TestingExternalContractDecompositionHardening}.
 * Такие тесты помогают вовремя заметить незапланированные изменения.
 */
class TestingExternalContractDecompositionHardeningRegressionPackTest {

    private static final List<Class<?>> EXTERNAL_DTO_TYPES = List.of(
        SelfVisibleTestCatalogEntryResponse.class,
        SelfVisibleTestResponse.class,
        CurrentAttemptResponse.class,
        ActiveAttemptAnswerMutationRequest.class,
        ActiveAttemptAnswerMutationResponse.class,
        AssignedAttemptEntryResponse.class,
        SelfAttemptEntryResponse.class,
        AssignedAttemptSubmitResponse.class,
        SelfAttemptSubmitResponse.class,
        SelfAttemptAbandonResponse.class
    );

    @Test
    void dtoLayerDoesNotExposeGenericExecutionRequestOrResponseContracts() {
        assertThat(dtoSimpleNames())
            .containsExactlyInAnyOrder(
                "SelfVisibleTestCatalogEntryResponse",
                "SelfVisibleTestResponse",
                "CurrentAttemptResponse",
                "ActiveAttemptAnswerMutationRequest",
                "ActiveAttemptAnswerMutationResponse",
                "AssignedAttemptEntryResponse",
                "SelfAttemptEntryResponse",
                "AssignedAttemptSubmitResponse",
                "SelfAttemptSubmitResponse",
                "SelfAttemptAbandonResponse"
            )
            .doesNotContain(
                "AttemptRequest",
                "AttemptResponse",
                "ExecutionRequest",
                "ExecutionResponse",
                "AttemptMutationRequest",
                "AttemptCommandRequest",
                "AttemptLifecycleResponse"
            );
    }

    @Test
    void dtoLayerDoesNotUseActionEnumCommandTypeOrModeSwitchAsSubstituteForDedicatedSurfaces() throws Exception {
        String allDtoSources = EXTERNAL_DTO_TYPES.stream()
            .map(this::readDtoSource)
            .collect(Collectors.joining("\n"));

        assertThat(allDtoSources)
            .doesNotContain(
                "actionType",
                "commandType",
                "operation",
                "submissionType",
                "mode=",
                "enum Action",
                "enum CommandType",
                "enum Operation",
                "SELF|ASSIGNED"
            );

        assertThat(recordComponentNames(ActiveAttemptAnswerMutationRequest.class))
            .containsExactly("answerItems")
            .doesNotContain("actionType", "commandType", "operation", "mode", "submissionType");
    }

    @Test
    void dtoContractsRemainSeparatedAcrossReadMutationEntrySubmitAndAbandonSlices() {
        assertThat(recordComponentNames(SelfVisibleTestCatalogEntryResponse.class))
            .containsExactly("id", "courseId", "courseName", "topicId", "topicName", "name", "description", "testType");
        assertThat(recordComponentNames(SelfVisibleTestResponse.class))
            .containsExactly("id", "topicId", "name", "description", "testType", "questions");

        assertThat(recordComponentNames(CurrentAttemptResponse.class))
            .containsExactly(
                "id",
                "userId",
                "testId",
                "assignmentTestId",
                "attemptMode",
                "status",
                "startedAt",
                "completedAt",
                "expiredAt",
                "abandonedAt",
                "lastActivityAt"
            );

        assertThat(recordComponentNames(ActiveAttemptAnswerMutationResponse.class))
            .containsExactly("testAttemptId", "questionId", "status", "lastActivityAt");
        assertThat(recordComponentNames(AssignedAttemptEntryResponse.class))
            .containsExactly("testAttemptId", "assignmentTestId", "testId", "attemptMode", "status", "startedAt", "lastActivityAt");
        assertThat(recordComponentNames(SelfAttemptEntryResponse.class))
            .containsExactly("testAttemptId", "testId", "attemptMode", "status", "startedAt", "lastActivityAt");
        assertThat(recordComponentNames(AssignedAttemptSubmitResponse.class))
            .containsExactly("testAttemptId", "status", "resultId");
        assertThat(recordComponentNames(SelfAttemptSubmitResponse.class))
            .containsExactly("testAttemptId", "resultId");
        assertThat(recordComponentNames(SelfAttemptAbandonResponse.class))
            .containsExactly("testAttemptId");
    }

    @Test
    void noSingleResponseDtoPlaysReadEntrySubmitAndAbandonRolesSimultaneously() {
        assertThat(recordComponentNames(CurrentAttemptResponse.class))
            .contains("completedAt", "expiredAt", "abandonedAt")
            .doesNotContain("resultId");

        assertThat(recordComponentNames(AssignedAttemptEntryResponse.class))
            .contains("assignmentTestId", "attemptMode", "status")
            .doesNotContain("completedAt", "expiredAt", "abandonedAt", "resultId");

        assertThat(recordComponentNames(SelfAttemptEntryResponse.class))
            .contains("attemptMode", "status")
            .doesNotContain("assignmentTestId", "completedAt", "expiredAt", "abandonedAt", "resultId");

        assertThat(recordComponentNames(AssignedAttemptSubmitResponse.class))
            .containsExactly("testAttemptId", "status", "resultId");
        assertThat(recordComponentNames(SelfAttemptSubmitResponse.class))
            .containsExactly("testAttemptId", "resultId");
        assertThat(recordComponentNames(SelfAttemptAbandonResponse.class))
            .containsExactly("testAttemptId");
    }

    @Test
    void dtoLayerDoesNotPullAssignmentOrResultOwnershipSemanticsIntoTestingExternalContracts() throws Exception {
        String dtoSources = EXTERNAL_DTO_TYPES.stream()
            .map(this::readDtoSource)
            .collect(Collectors.joining("\n"));

        assertThat(dtoSources)
            .doesNotContain(
                "AssignmentStatus",
                "countedResultId",
                "closedAt",
                "isClosed",
                "cancelledAt",
                "ResultRecordingService",
                "AssignmentCountedResultHandoffService",
                "AssignmentCommandService"
            );

        assertThat(recordComponentNames(AssignedAttemptSubmitResponse.class))
            .containsExactly("testAttemptId", "status", "resultId");
        assertThat(recordComponentNames(SelfAttemptSubmitResponse.class))
            .containsExactly("testAttemptId", "resultId");
    }

    @Test
    void dtoPackageDescriptionAndTypesDoNotDriftIntoGenericExecutionGrammar() throws Exception {
        String packageInfo = Files.readString(
            Path.of("src/main/java/com/vladislav/training/platform/testing/controller/dto/package-info.java")
        );

        assertThat(packageInfo)
            .contains("not a home for attempt lifecycle payloads")
            .doesNotContain(
                "generic execution grammar",
                "AttemptRequest",
                "AttemptResponse",
                "ExecutionResponse"
            );

        assertThat(methodNames(SelfVisibleTestResponse.class))
            .doesNotContain("actionType", "commandType", "operation", "submissionType");
        assertThat(methodNames(CurrentAttemptResponse.class))
            .doesNotContain("actionType", "commandType", "operation", "submissionType");
        assertThat(methodNames(AssignedAttemptEntryResponse.class))
            .doesNotContain("actionType", "commandType", "operation", "submissionType");
        assertThat(methodNames(SelfAttemptEntryResponse.class))
            .doesNotContain("actionType", "commandType", "operation", "submissionType");
    }

    private Set<String> dtoSimpleNames() {
        return EXTERNAL_DTO_TYPES.stream()
            .map(Class::getSimpleName)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<String> recordComponentNames(Class<?> recordType) {
        return Arrays.stream(recordType.getRecordComponents())
            .map(RecordComponent::getName)
            .toList();
    }

    private Set<String> methodNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String readDtoSource(Class<?> dtoType) {
        String path = "src/main/java/com/vladislav/training/platform/testing/controller/dto/" + dtoType.getSimpleName() + ".java";
        try {
            return Files.readString(Path.of(path));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Failed to read dto source: " + path, exception);
        }
    }
}
