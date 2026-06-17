package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.testing.controller.AssignedAttemptSubmitController;
import com.vladislav.training.platform.testing.controller.dto.AssignedAttemptSubmitResponse;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
/**
 * Собирает набор регрессионных проверок вокруг {@code AssignedSubmitOutcomeContractHardening}.
 * Такие тесты помогают вовремя заметить незапланированные изменения.
 */
class AssignedSubmitOutcomeContractHardeningRegressionPackTest {

    @Test
    void terminalLayerRemainsVerdictDrivenAndFreeFromResultOrCountedOwnership() throws IOException {
        String source = read("src/main/java/com/vladislav/training/platform/testing/service/AssignedAttemptSubmitTerminalService.java");

        int refreshIndex = source.indexOf("refreshAssignedAttemptStatusCacheWithVerdict(");
        int statusExpiredIndex = source.indexOf("activeAttempt.status() == TestAttemptStatus.EXPIRED", refreshIndex);
        int verdictIndex = source.indexOf("refreshResult.expiredByThisRefresh()", statusExpiredIndex);
        int genericValidationIndex = source.indexOf("requireSubmittableAssignedAttempt(activeAttempt, actorUserId);", verdictIndex);

        assertThat(refreshIndex).isGreaterThanOrEqualTo(0);
        assertThat(statusExpiredIndex).isGreaterThan(refreshIndex);
        assertThat(verdictIndex)
            
            .isGreaterThan(statusExpiredIndex);
        assertThat(genericValidationIndex)
            
            .isGreaterThan(verdictIndex);

        assertThat(source)
            .contains("already expired attempt")
            .doesNotContain("ResultRecordingService")
            .doesNotContain(".recordResult(")
            .doesNotContain("AssignmentCountedResultHandoffService");
    }

    @Test
    void sequencingLayerRemainsExplicitFailClosedAndNonNull() throws IOException {
        String source = read("src/main/java/com/vladislav/training/platform/testing/service/AssignedAttemptSubmitSequencingService.java");

        int expiredBranchIndex = source.indexOf("AssignedAttemptSubmitOutcome.expired(");
        int recordResultIndex = source.indexOf("resultRecordingService.recordResult(");
        int completedBranchIndex = source.indexOf("AssignedAttemptSubmitOutcome.completed(");

        assertThat(expiredBranchIndex).isGreaterThanOrEqualTo(0);
        assertThat(recordResultIndex)
            
            .isGreaterThan(expiredBranchIndex);
        assertThat(completedBranchIndex)
            
            .isGreaterThan(recordResultIndex);

        assertThat(source)
            .contains("AssignedAttemptSubmitOutcome")
            .contains("unsupported terminal status")
            .contains("TestAttemptStatus.COMPLETED")
            .contains("TestAttemptStatus.EXPIRED")
            .contains("IllegalArgumentException")
            .doesNotContain("return null");
    }

    @Test
    void submitOutcomeCarrierAllowsOnlyCompletedWithResultOrExpiredWithoutResult() {
        AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome completed =
            AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome.completed(9001L, 7001L);
        AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome expired =
            AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome.expired(9002L);

        assertThat(completed.attemptId()).isEqualTo(9001L);
        assertThat(completed.terminalStatus()).isEqualTo(TestAttemptStatus.COMPLETED);
        assertThat(completed.recordedResult()).isEqualTo(7001L);
        assertThat(completed.completedWithResult()).isTrue();
        assertThat(completed.expiredWithoutResult()).isFalse();

        assertThat(expired.attemptId()).isEqualTo(9002L);
        assertThat(expired.terminalStatus()).isEqualTo(TestAttemptStatus.EXPIRED);
        assertThat(expired.recordedResult()).isNull();
        assertThat(expired.expiredWithoutResult()).isTrue();
        assertThat(expired.completedWithResult()).isFalse();

        assertThatThrownBy(() -> new AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome(
            9003L,
            TestAttemptStatus.COMPLETED,
            null
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome(
            9004L,
            TestAttemptStatus.EXPIRED,
            7004L
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome(
            9005L,
            TestAttemptStatus.IN_PROGRESS,
            null
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome(
            9006L,
            TestAttemptStatus.ABANDONED,
            null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void submissionEntryAndControllerRemainOutcomeDrivenThinPerimeters() throws IOException {
        assertThat(publicMethodReturnType(AssignedAttemptSubmissionService.class, "submitAssignedAttempt", Long.class))
            .isEqualTo(AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome.class);
        assertThat(fieldTypes(AssignedAttemptSubmissionService.class))
            .doesNotContain(ResultRecordingService.class);

        String entrySource = read("src/main/java/com/vladislav/training/platform/testing/service/AssignedAttemptSubmissionService.java");
        int capabilityIndex = entrySource.indexOf("capabilityAdmissionPolicy.check(");
        int sequencingIndex = entrySource.indexOf("assignedAttemptSubmitSequencingService.submitAssignedAttempt(");
        assertThat(capabilityIndex).isGreaterThanOrEqualTo(0);
        assertThat(sequencingIndex).isGreaterThan(capabilityIndex);
        assertThat(entrySource)
            .contains("AssignedAttemptSubmitOutcome")
            .doesNotContain("ResultRecordingService")
            .doesNotContain(".recordResult(");

        assertThat(recordComponentNames(AssignedAttemptSubmitResponse.class))
            .containsExactly("testAttemptId", "status", "resultId");

        String controllerSource = read("src/main/java/com/vladislav/training/platform/testing/controller/AssignedAttemptSubmitController.java");
        assertThat(controllerSource)
            .contains("outcome.attemptId()")
            .contains("outcome.terminalStatus()")
            .contains("outcome.recordedResult()")
            .doesNotContain("new AssignedAttemptSubmitResponse(testAttemptId,")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("AssignmentCountedResultHandoffService")
            .doesNotContain(".recordResult(");
    }

    @Test
    void controllerObservableResponsesRemainExplicitForCompletedAndExpiredOutcomes() throws Exception {
        AssignedAttemptSubmissionService service = org.mockito.Mockito.mock(AssignedAttemptSubmissionService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AssignedAttemptSubmitController(service)).build();

        when(service.submitAssignedAttempt(9001L))
            .thenReturn(AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome.completed(9100L, 7001L));
        when(service.submitAssignedAttempt(9002L))
            .thenReturn(AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome.expired(9200L));

        mockMvc.perform(post("/api/v1/assigned-attempt-submissions/attempts/9001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.testAttemptId").value(9100))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.resultId").value(7001));

        mockMvc.perform(post("/api/v1/assigned-attempt-submissions/attempts/9002"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.testAttemptId").value(9200))
            .andExpect(jsonPath("$.status").value("EXPIRED"))
            .andExpect(jsonPath("$.resultId").isEmpty());

        verify(service).submitAssignedAttempt(9001L);
        verify(service).submitAssignedAttempt(9002L);
        verifyNoMoreInteractions(service);
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }

    private List<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .map(Field::getType)
            .toList();
    }

    private Class<?> publicMethodReturnType(Class<?> type, String methodName, Class<?>... parameterTypes) {
        try {
            Method method = type.getDeclaredMethod(methodName, parameterTypes);
            return method.getReturnType();
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("Missing method: " + methodName, exception);
        }
    }

    private List<String> recordComponentNames(Class<?> recordType) {
        return Arrays.stream(recordType.getRecordComponents())
            .map(RecordComponent::getName)
            .toList();
    }
}
