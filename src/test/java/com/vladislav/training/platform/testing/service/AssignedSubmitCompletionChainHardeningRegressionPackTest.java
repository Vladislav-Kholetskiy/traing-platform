package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.testing.admission.AssignedAttemptSubmitAdmissionFoundationStateReadService;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Собирает набор регрессионных проверок вокруг {@code AssignedSubmitCompletionChainHardening}.
 * Такие тесты помогают вовремя заметить незапланированные изменения.
 */
@ExtendWith(MockitoExtension.class)
class AssignedSubmitCompletionChainHardeningRegressionPackTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-20T15:00:00Z");
    private static final Long ACTOR_USER_ID = 101L;

    @Mock
    private AssignedAttemptSubmitTerminalService assignedAttemptSubmitTerminalService;
    @Mock
    private ResultRecordingService resultRecordingService;
    @Mock
    private CriticalCommandAuditSupport criticalCommandAuditSupport;
    @Mock
    private CapabilityAdmissionPolicy capabilityAdmissionPolicy;
    @Mock
    private CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;
    @Mock
    private AssignedAttemptSubmitAdmissionFoundationStateReadService foundationStateReadService;
    @Mock
    private SelfAttemptSubmitSequencingService selfAttemptSubmitOrchestrationFacade;

    @Test
    void assignedSubmitChainKeepsDedicatedAssignedOnlyApplicationAndOrchestrationVocabulary() throws IOException {
        assertThat(publicMethodNames(AssignedAttemptSubmissionService.class))
            .containsExactly("submitAssignedAttempt")
            .doesNotContain("submitAttempt", "submitSelfAttempt", "abandonSelfAttempt", "completeEverythingAfterSubmit");

        assertThat(publicMethodNames(AssignedAttemptSubmitSequencingService.class))
            .containsExactly("submitAssignedAttempt")
            .doesNotContain(
                "submitAttempt",
                "submitSelfAttempt",
                "abandonSelfAttempt",
                "completeAssignmentFromSubmit",
                "finalizeAttemptAndAssignment",
                "universalSubmitPipeline"
            );

        String assignedEntrySource = read("src/main/java/com/vladislav/training/platform/testing/service/AssignedAttemptSubmissionService.java");
        String assignedFacadeSource = read(
            "src/main/java/com/vladislav/training/platform/testing/service/AssignedAttemptSubmitSequencingService.java"
        );

        assertThat(assignedEntrySource + assignedFacadeSource)
            .doesNotContain(
                "ExecutionCompletionService",
                "AttemptSubmitManager",
                "UniversalResultPipeline",
                "AssignmentCompletionCoordinator",
                "completeEverythingAfterSubmit",
                "finalizeAttemptAndAssignment"
            );
    }

    @Test
    void assignedSubmitChainRunsOnlyThroughAssignedTerminalThenResultOwner() {
        AssignedAttemptSubmitSequencingService facade = new AssignedAttemptSubmitSequencingService(
            assignedAttemptSubmitTerminalService,
            resultRecordingService
        );
        AssignedAttemptSubmissionService entry = new AssignedAttemptSubmissionService(
            facade,
            criticalCommandAuditSupport,
            capabilityAdmissionPolicy,
            capabilityAdmissionRequestFactory,
            foundationStateReadService
        );
        TestAttempt terminalizedAttempt = completedAssignedAttempt(9001L);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(ACTOR_USER_ID);
        when(foundationStateReadService.findAssignedAttemptSubmitAdmissionFoundationState(ACTOR_USER_ID, 7001L))
            .thenReturn(new AssignedAttemptSubmitAdmissionFoundationStateReadService.AssignedAttemptSubmitAdmissionFoundationState(
                7001L,
                77L,
                701L
            ));
        when(capabilityAdmissionRequestFactory.createAssignedAttemptSubmit(ACTOR_USER_ID, 77L, 701L))
            .thenReturn(new CapabilityAdmissionRequest(
                ACTOR_USER_ID,
                "TESTING_ASSIGNED_ATTEMPT_SUBMIT",
                com.vladislav.training.platform.application.policy.CapabilityTargetEntityType.ASSIGNMENT_TEST,
                701L,
                com.vladislav.training.platform.application.policy.CapabilityAdmissionPayload.Empty.INSTANCE,
                FIXED_INSTANT
            ));
        when(assignedAttemptSubmitTerminalService.submitAssignedAttempt(ACTOR_USER_ID, 701L, 7001L))
            .thenReturn(AttemptTerminalizationOutcome.assignedNormalSubmit(ACTOR_USER_ID, terminalizedAttempt));
        when(resultRecordingService.recordResult(9001L)).thenReturn(8001L);

        AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome outcome = entry.submitAssignedAttempt(7001L);

        assertThat(outcome.attemptId()).isEqualTo(9001L);
        assertThat(outcome.terminalStatus()).isEqualTo(TestAttemptStatus.COMPLETED);
        assertThat(outcome.recordedResult()).isEqualTo(8001L);
        InOrder inOrder = inOrder(criticalCommandAuditSupport, assignedAttemptSubmitTerminalService, resultRecordingService);
        inOrder.verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        inOrder.verify(assignedAttemptSubmitTerminalService).submitAssignedAttempt(ACTOR_USER_ID, 701L, 7001L);
        inOrder.verify(resultRecordingService).recordResult(9001L);
        verifyNoInteractions(selfAttemptSubmitOrchestrationFacade);
    }

    @Test
    void assignedSubmitApplicationAndOrchestrationDoNotPullSelfPathOrAssignmentOwnerWritesDirectly() {
        assertThat(fieldTypes(AssignedAttemptSubmissionService.class))
            .containsExactlyInAnyOrder(
                AssignedAttemptSubmitSequencingService.class,
                CriticalCommandAuditSupport.class,
                CapabilityAdmissionPolicy.class,
                CapabilityAdmissionRequestFactory.class,
                AssignedAttemptSubmitAdmissionFoundationStateReadService.class
            )
            .doesNotContain(
                SelfAttemptSubmitSequencingService.class,
                SelfAttemptAbandonSequencingService.class,
                ResultRecordingService.class
            );

        assertThat(fieldTypes(AssignedAttemptSubmitSequencingService.class))
            .containsExactlyInAnyOrder(
                AssignedAttemptSubmitTerminalService.class,
                ResultRecordingService.class
            )
            .doesNotContain(
                SelfAttemptSubmitTerminalService.class,
                SelfAttemptSubmitSequencingService.class,
                SelfAttemptAbandonSequencingService.class
            );

        assertThat(fieldTypes(AssignedAttemptSubmitTerminalService.class))
            .doesNotContain(
                SelfAttemptSubmitTerminalService.class,
                SelfAttemptAbandonTerminalService.class,
                com.vladislav.training.platform.assignment.service.AssignmentCommandService.class,
                com.vladislav.training.platform.assignment.repository.AssignmentRepository.class,
                com.vladislav.training.platform.assignment.repository.AssignmentTestRepository.class
            );
    }

    @Test
    void resultOwnerContinuesAssignedCompletionOnlyThroughAllowedAssignmentHandoff() throws IOException {
        String sequencingSource = read(
            "src/main/java/com/vladislav/training/platform/testing/service/AssignedAttemptSubmitSequencingService.java"
        );
        String resultRecordingSource = read(
            "src/main/java/com/vladislav/training/platform/result/service/ResultRecordingServiceImpl.java"
        );

        assertThat(sequencingSource)
            .doesNotContain("assignmentCountedResultHandoffService.acceptValidCountedAssignmentResult(")
            .doesNotContain(
                "saveAssignment(",
                "saveAssignmentTest(",
                "closeAssignmentTestWithCountedResult(",
                "refreshAssignmentStatusCache("
            );

        assertThat(resultRecordingSource)
            .doesNotContain(
                "saveAssignment(",
                "saveAssignmentTest(",
                "closeAssignmentTestWithCountedResult(",
                "refreshAssignmentStatusCache(",
                "completeAssignmentFromSubmit",
                "finalizeAttemptAndAssignment"
            );
    }

    @Test
    void assignmentSideContinuationRemainsOwnerLocalAfterCanonicalResultMaterialization() throws IOException {
        String handoffSource = read(
            "src/main/java/com/vladislav/training/platform/assignment/service/AssignmentCountedResultHandoffServiceImpl.java"
        );
        String commandSource = read(
            "src/main/java/com/vladislav/training/platform/assignment/service/AssignmentCommandServiceImpl.java"
        );
        String statusSource = read(
            "src/main/java/com/vladislav/training/platform/assignment/service/AssignmentStatusRecalculationServiceImpl.java"
        );

        assertThat(handoffSource)
            .contains("assignmentCommandService.closeAssignmentTestWithCountedResult")
            .doesNotContain(
                "saveAssignmentTest(",
                "saveAssignment(",
                "refreshAssignmentStatusCache(",
                "completeEverythingAfterSubmit",
                "universalSubmitPipeline"
            );

        assertThat(commandSource)
            .contains("refreshAssignmentStatusCache")
            .doesNotContain(
                "ResultRecordingService",
                "AssignedAttemptSubmitSequencingService",
                "SelfAttemptSubmitSequencingService",
                "completeAssignmentFromSubmit"
            );

        assertThat(statusSource)
            .contains("counted-result proof for COMPLETED")
            .doesNotContain(
                "TestAttemptStatus.COMPLETED",
                "AssignedAttemptSubmitSequencingService",
                "completeEverythingAfterSubmit"
            );
    }

    @Test
    void assignedSubmitCompletionChainDoesNotTreatTestingStatusAsAssignmentCompletionTruthOrPullApiLayer() throws IOException {
        String terminalSource = read(
            "src/main/java/com/vladislav/training/platform/testing/service/AssignedAttemptSubmitTerminalService.java"
        );
        String facadeSource = read(
            "src/main/java/com/vladislav/training/platform/testing/service/AssignedAttemptSubmitSequencingService.java"
        );
        String statusSource = read(
            "src/main/java/com/vladislav/training/platform/assignment/service/AssignmentStatusRecalculationServiceImpl.java"
        );

        assertThat(terminalSource + facadeSource)
            .doesNotContain(
                "saveAssignment(",
                "saveAssignmentTest(",
                "AssignmentCommandService",
                "AssignedAttemptSubmitController",
                "SelfAttemptSubmitController",
                "completeAssignmentFromSubmit"
            );

        assertThat(statusSource)
            .doesNotContain(
                "attempt.status() == TestAttemptStatus.COMPLETED",
                "test_attempt.status = COMPLETED",
                "AssignedAttemptSubmitController",
                "SelfAttemptSubmitController"
            );
    }

    private List<String> publicMethodNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
            .filter(method -> Modifier.isPublic(method.getModifiers()))
            .map(Method::getName)
            .toList();
    }

    private List<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .map(Field::getType)
            .toList();
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }

    private TestAttempt completedAssignedAttempt(Long attemptId) {
        return new TestAttempt(
            attemptId,
            111L,
            511L,
            711L,
            AttemptMode.ASSIGNED,
            TestAttemptStatus.COMPLETED,
            FIXED_INSTANT.minusSeconds(600),
            FIXED_INSTANT,
            null,
            null,
            FIXED_INSTANT,
            FIXED_INSTANT.minusSeconds(600),
            FIXED_INSTANT
        );
    }
}
