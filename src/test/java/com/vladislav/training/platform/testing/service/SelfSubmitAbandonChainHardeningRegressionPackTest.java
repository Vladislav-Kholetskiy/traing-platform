package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.assignment.service.AssignmentCountedResultHandoffService;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.testing.admission.SelfAttemptTerminalAdmissionFoundationStateReadService;
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
 * Собирает набор регрессионных проверок вокруг {@code SelfSubmitAbandonChainHardening}.
 * Такие тесты помогают вовремя заметить незапланированные изменения.
 */
@ExtendWith(MockitoExtension.class)
class SelfSubmitAbandonChainHardeningRegressionPackTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-20T16:00:00Z");
    private static final Long ACTOR_USER_ID = 101L;

    @Mock
    private SelfAttemptSubmitTerminalService selfAttemptSubmitTerminalService;
    @Mock
    private SelfAttemptAbandonTerminalService selfAttemptAbandonTerminalService;
    @Mock
    private ResultRecordingService resultRecordingService;
    @Mock
    private CriticalCommandAuditSupport criticalCommandAuditSupport;
    @Mock
    private CapabilityAdmissionPolicy capabilityAdmissionPolicy;
    @Mock
    private CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;
    @Mock
    private SelfAttemptTerminalAdmissionFoundationStateReadService foundationStateReadService;
    @Mock
    private AssignmentCountedResultHandoffService assignmentCountedResultHandoffService;
    @Mock
    private AssignedAttemptSubmitTerminalService assignedAttemptSubmitTerminalService;
    @Mock
    private AssignedAttemptSubmitSequencingService assignedAttemptSubmitOrchestrationFacade;

    @Test
    void selfSubmitAndSelfAbandonVocabulariesRemainDedicatedWithoutGenericMerge() throws IOException {
        assertThat(publicMethodNames(SelfAttemptSubmitSequencingService.class))
            .containsExactly("submitSelfAttempt")
            .doesNotContain(
                "submitAttempt",
                "submitAssignedAttempt",
                "abandonSelfAttempt",
                "completeEverything",
                "finalizeAttemptAndAssignment",
                "universalSubmitPipeline"
            );

        assertThat(publicMethodNames(SelfAttemptAbandonSequencingService.class))
            .containsExactly("abandonSelfAttempt")
            .doesNotContain(
                "abandonAttempt",
                "submitSelfAttempt",
                "submitAssignedAttempt",
                "executionCompletionCoordinator",
                "attemptLifecycleManager"
            );

        String selfSubmitSource = read("src/main/java/com/vladislav/training/platform/testing/service/SelfAttemptSubmitSequencingService.java");
        String selfAbandonSource = read(
            "src/main/java/com/vladislav/training/platform/testing/service/SelfAttemptAbandonSequencingService.java"
        );

        assertThat(selfSubmitSource + selfAbandonSource)
            .doesNotContain(
                "submitAttempt",
                "abandonAttempt",
                "completeEverything",
                "finalizeAttemptAndAssignment",
                "universalSubmitPipeline",
                "executionCompletionCoordinator",
                "attemptLifecycleManager"
            );
    }

    @Test
    void selfSubmitChainRunsOnlyThroughSelfTerminalThenResultOwner() {
        SelfAttemptSubmitSequencingService facade = new SelfAttemptSubmitSequencingService(
            selfAttemptSubmitTerminalService,
            resultRecordingService,
            criticalCommandAuditSupport,
            capabilityAdmissionPolicy,
            capabilityAdmissionRequestFactory,
            foundationStateReadService
        );
        TestAttempt terminalizedAttempt = completedSelfAttempt(9001L);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(ACTOR_USER_ID);
        when(foundationStateReadService.findSelfAttemptTerminalAdmissionFoundationState(ACTOR_USER_ID, 7001L))
            .thenReturn(new SelfAttemptTerminalAdmissionFoundationStateReadService.SelfAttemptTerminalAdmissionFoundationState(
                7001L,
                501L
            ));
        when(capabilityAdmissionRequestFactory.createSelfAttemptSubmit(ACTOR_USER_ID, 501L))
            .thenReturn(new CapabilityAdmissionRequest(
                ACTOR_USER_ID,
                "TESTING_SELF_ATTEMPT_SUBMIT",
                com.vladislav.training.platform.application.policy.CapabilityTargetEntityType.TEST,
                501L,
                com.vladislav.training.platform.application.policy.CapabilityAdmissionPayload.Empty.INSTANCE,
                FIXED_INSTANT
            ));
        when(selfAttemptSubmitTerminalService.submitSelfAttempt(ACTOR_USER_ID, 7001L))
            .thenReturn(AttemptTerminalizationOutcome.selfNormalSubmit(ACTOR_USER_ID, terminalizedAttempt));
        when(resultRecordingService.recordResult(9001L)).thenReturn(8001L);

        Long resultId = facade.submitSelfAttempt(7001L);

        assertThat(resultId).isEqualTo(8001L);
        InOrder inOrder = inOrder(criticalCommandAuditSupport, selfAttemptSubmitTerminalService, resultRecordingService);
        inOrder.verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        inOrder.verify(selfAttemptSubmitTerminalService).submitSelfAttempt(ACTOR_USER_ID, 7001L);
        inOrder.verify(resultRecordingService).recordResult(9001L);
        verifyNoInteractions(
            assignmentCountedResultHandoffService,
            assignedAttemptSubmitTerminalService,
            assignedAttemptSubmitOrchestrationFacade
        );
    }

    @Test
    void selfAbandonChainRunsOnlyThroughSelfAbandonTerminal() {
        SelfAttemptAbandonSequencingService facade = new SelfAttemptAbandonSequencingService(
            selfAttemptAbandonTerminalService,
            criticalCommandAuditSupport,
            capabilityAdmissionPolicy,
            capabilityAdmissionRequestFactory,
            foundationStateReadService
        );
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(ACTOR_USER_ID);
        when(foundationStateReadService.findSelfAttemptTerminalAdmissionFoundationState(ACTOR_USER_ID, 7002L))
            .thenReturn(new SelfAttemptTerminalAdmissionFoundationStateReadService.SelfAttemptTerminalAdmissionFoundationState(
                7002L,
                502L
            ));
        when(capabilityAdmissionRequestFactory.createSelfAttemptAbandon(ACTOR_USER_ID, 502L))
            .thenReturn(new CapabilityAdmissionRequest(
                ACTOR_USER_ID,
                "TESTING_SELF_ATTEMPT_ABANDON",
                com.vladislav.training.platform.application.policy.CapabilityTargetEntityType.TEST,
                502L,
                com.vladislav.training.platform.application.policy.CapabilityAdmissionPayload.Empty.INSTANCE,
                FIXED_INSTANT
            ));
        when(selfAttemptAbandonTerminalService.abandonSelfAttempt(ACTOR_USER_ID, 7002L))
            .thenReturn(AttemptTerminalizationOutcome.selfAbandon(ACTOR_USER_ID, abandonedSelfAttempt(9002L)));

        Long abandonedAttemptId = facade.abandonSelfAttempt(7002L);

        assertThat(abandonedAttemptId).isEqualTo(9002L);
        verifyNoInteractions(
            resultRecordingService,
            assignmentCountedResultHandoffService,
            assignedAttemptSubmitTerminalService,
            assignedAttemptSubmitOrchestrationFacade
        );
    }

    @Test
    void selfSubmitAndSelfAbandonDependencyShapesDoNotPullAssignedOrAssignmentOwnerContours() {
        assertThat(fieldTypes(SelfAttemptSubmitSequencingService.class))
            .containsExactlyInAnyOrder(
                SelfAttemptSubmitTerminalService.class,
                ResultRecordingService.class,
                CriticalCommandAuditSupport.class,
                CapabilityAdmissionPolicy.class,
                CapabilityAdmissionRequestFactory.class,
                SelfAttemptTerminalAdmissionFoundationStateReadService.class
            )
            .doesNotContain(
                AssignedAttemptSubmitTerminalService.class,
                AssignedAttemptSubmitSequencingService.class,
                AssignmentCountedResultHandoffService.class,
                com.vladislav.training.platform.assignment.service.AssignmentCommandService.class,
                com.vladislav.training.platform.assignment.repository.AssignmentRepository.class,
                com.vladislav.training.platform.assignment.repository.AssignmentTestRepository.class
            );

        assertThat(fieldTypes(SelfAttemptAbandonSequencingService.class))
            .containsExactlyInAnyOrder(
                SelfAttemptAbandonTerminalService.class,
                CriticalCommandAuditSupport.class,
                CapabilityAdmissionPolicy.class,
                CapabilityAdmissionRequestFactory.class,
                SelfAttemptTerminalAdmissionFoundationStateReadService.class
            )
            .doesNotContain(
                ResultRecordingService.class,
                AssignmentCountedResultHandoffService.class,
                AssignedAttemptSubmitTerminalService.class,
                AssignedAttemptSubmitSequencingService.class,
                com.vladislav.training.platform.assignment.service.AssignmentCommandService.class
            );

        assertThat(fieldTypes(SelfAttemptSubmitTerminalService.class))
            .doesNotContain(
                AssignedAttemptSubmitTerminalService.class,
                AssignmentCountedResultHandoffService.class,
                com.vladislav.training.platform.assignment.service.AssignmentCommandService.class,
                com.vladislav.training.platform.assignment.repository.AssignmentRepository.class,
                com.vladislav.training.platform.assignment.repository.AssignmentTestRepository.class
            );

        assertThat(fieldTypes(SelfAttemptAbandonTerminalService.class))
            .doesNotContain(
                ResultRecordingService.class,
                AssignmentCountedResultHandoffService.class,
                AssignedAttemptSubmitTerminalService.class,
                com.vladislav.training.platform.assignment.service.AssignmentCommandService.class,
                com.vladislav.training.platform.assignment.repository.AssignmentRepository.class,
                com.vladislav.training.platform.assignment.repository.AssignmentTestRepository.class
            );
    }

    @Test
    void resultOwnerDoesNotGiveSelfPathAssignmentOwnedContinuation() throws IOException {
        String resultRecordingSource = read(
            "src/main/java/com/vladislav/training/platform/result/service/ResultRecordingServiceImpl.java"
        );

        assertThat(resultRecordingSource)
            .contains("CountedAssignmentResultValidityGate")
            .contains("countedAssignmentResultValidityGate.allowsAssignmentCountedHandoff(")
            .contains("assignmentCountedResultHandoffService.acceptValidCountedAssignmentResult")
            .doesNotContain(
                "saveAssignment(",
                "saveAssignmentTest(",
                "closeAssignmentTestWithCountedResult(",
                "AssignmentCommandService"
            );
    }

    @Test
    void selfChainsDoNotPullControllerApiLayerOrGenericCompletionDrift() throws IOException {
        String selfSubmitSource = read("src/main/java/com/vladislav/training/platform/testing/service/SelfAttemptSubmitSequencingService.java");
        String selfSubmitTerminalSource = read("src/main/java/com/vladislav/training/platform/testing/service/SelfAttemptSubmitTerminalService.java");
        String selfAbandonSource = read("src/main/java/com/vladislav/training/platform/testing/service/SelfAttemptAbandonSequencingService.java");
        String selfAbandonTerminalSource = read("src/main/java/com/vladislav/training/platform/testing/service/SelfAttemptAbandonTerminalService.java");

        assertThat(selfSubmitSource + selfSubmitTerminalSource + selfAbandonSource + selfAbandonTerminalSource)
            .doesNotContain(
                "SelfAttemptSubmitController",
                "SelfAttemptAbandonController",
                "AssignedAttemptSubmitController",
                "ResponseEntity",
                "completeEverything",
                "finalizeAttemptAndAssignment",
                "executionCompletionCoordinator",
                "attemptLifecycleManager"
            );
        assertThat(selfAbandonSource)
            .contains("terminalizationOutcome.terminalStatus()")
            .contains("terminalizationOutcome.resultRecordable()")
            .doesNotContain("terminalizationOutcome.countedHandoffEligible()")
            .doesNotContain("TestAttempt abandonedAttempt = selfAttemptAbandonTerminalService.abandonSelfAttempt(");
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

    private TestAttempt completedSelfAttempt(Long attemptId) {
        return new TestAttempt(
            attemptId,
            101L,
            501L,
            null,
            AttemptMode.SELF,
            TestAttemptStatus.COMPLETED,
            FIXED_INSTANT.minusSeconds(900),
            FIXED_INSTANT,
            null,
            null,
            FIXED_INSTANT,
            FIXED_INSTANT.minusSeconds(900),
            FIXED_INSTANT
        );
    }

    private TestAttempt abandonedSelfAttempt(Long attemptId) {
        return new TestAttempt(
            attemptId,
            101L,
            501L,
            null,
            AttemptMode.SELF,
            TestAttemptStatus.ABANDONED,
            FIXED_INSTANT.minusSeconds(900),
            null,
            null,
            FIXED_INSTANT,
            FIXED_INSTANT,
            FIXED_INSTANT.minusSeconds(900),
            FIXED_INSTANT
        );
    }
}
