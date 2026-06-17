package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.result.repository.ResultRepository;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.testing.admission.AssignedAttemptSubmitAdmissionFoundationStateReadService;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code AssignedSubmitOrchestrationChain}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class AssignedSubmitOrchestrationChainTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-19T20:10:00Z");
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
    private ResultRepository resultRepository;

    private AssignedAttemptSubmissionService assignedAttemptSubmissionService;

    @BeforeEach
    void setUp() {
        AssignedAttemptSubmitSequencingService facade = new AssignedAttemptSubmitSequencingService(
            assignedAttemptSubmitTerminalService,
            resultRecordingService
        );
        assignedAttemptSubmissionService = new AssignedAttemptSubmissionService(
            facade,
            criticalCommandAuditSupport,
            capabilityAdmissionPolicy,
            capabilityAdmissionRequestFactory,
            foundationStateReadService
        );
    }

    @Test
    void assignedSubmitChainRunsThroughEntryThenFacadeThenTerminalOwnerThenResultOwner() {
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

        AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome outcome =
            assignedAttemptSubmissionService.submitAssignedAttempt(7001L);

        assertThat(outcome.attemptId()).isEqualTo(9001L);
        assertThat(outcome.terminalStatus()).isEqualTo(TestAttemptStatus.COMPLETED);
        assertThat(outcome.recordedResult()).isEqualTo(8001L);
        InOrder inOrder = inOrder(criticalCommandAuditSupport, assignedAttemptSubmitTerminalService, resultRecordingService);
        inOrder.verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        inOrder.verify(assignedAttemptSubmitTerminalService).submitAssignedAttempt(ACTOR_USER_ID, 701L, 7001L);
        inOrder.verify(resultRecordingService).recordResult(9001L);
        verifyNoInteractions(resultRepository);
    }

    @Test
    void whenTerminalOwnerFailsResultOwnerIsNotCalledThroughAssembledChain() {
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(ACTOR_USER_ID);
        when(foundationStateReadService.findAssignedAttemptSubmitAdmissionFoundationState(ACTOR_USER_ID, 7002L))
            .thenReturn(new AssignedAttemptSubmitAdmissionFoundationStateReadService.AssignedAttemptSubmitAdmissionFoundationState(
                7002L,
                78L,
                702L
            ));
        when(capabilityAdmissionRequestFactory.createAssignedAttemptSubmit(ACTOR_USER_ID, 78L, 702L))
            .thenReturn(new CapabilityAdmissionRequest(
                ACTOR_USER_ID,
                "TESTING_ASSIGNED_ATTEMPT_SUBMIT",
                com.vladislav.training.platform.application.policy.CapabilityTargetEntityType.ASSIGNMENT_TEST,
                702L,
                com.vladislav.training.platform.application.policy.CapabilityAdmissionPayload.Empty.INSTANCE,
                FIXED_INSTANT
            ));
        when(assignedAttemptSubmitTerminalService.submitAssignedAttempt(ACTOR_USER_ID, 702L, 7002L))
            .thenThrow(new ConflictException("terminal reject"));

        assertThatThrownBy(() -> assignedAttemptSubmissionService.submitAssignedAttempt(7002L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("terminal reject");

        verifyNoInteractions(resultRecordingService, resultRepository);
    }

    @Test
    void assembledAssignedSubmitChainDoesNotNeedDirectAssignmentSideCollaborators() {
        TestAttempt terminalizedAttempt = completedAssignedAttempt(9003L);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(ACTOR_USER_ID);
        when(foundationStateReadService.findAssignedAttemptSubmitAdmissionFoundationState(ACTOR_USER_ID, 7003L))
            .thenReturn(new AssignedAttemptSubmitAdmissionFoundationStateReadService.AssignedAttemptSubmitAdmissionFoundationState(
                7003L,
                79L,
                703L
            ));
        when(capabilityAdmissionRequestFactory.createAssignedAttemptSubmit(ACTOR_USER_ID, 79L, 703L))
            .thenReturn(new CapabilityAdmissionRequest(
                ACTOR_USER_ID,
                "TESTING_ASSIGNED_ATTEMPT_SUBMIT",
                com.vladislav.training.platform.application.policy.CapabilityTargetEntityType.ASSIGNMENT_TEST,
                703L,
                com.vladislav.training.platform.application.policy.CapabilityAdmissionPayload.Empty.INSTANCE,
                FIXED_INSTANT
            ));
        when(assignedAttemptSubmitTerminalService.submitAssignedAttempt(ACTOR_USER_ID, 703L, 7003L))
            .thenReturn(AttemptTerminalizationOutcome.assignedNormalSubmit(ACTOR_USER_ID, terminalizedAttempt));
        when(resultRecordingService.recordResult(9003L)).thenReturn(8003L);

        assignedAttemptSubmissionService.submitAssignedAttempt(7003L);

        org.mockito.Mockito.verify(resultRecordingService).recordResult(9003L);
    }

    @Test
    void assembledAssignedSubmitChainDoesNotMaterializeResultOutsideResultRecordingService() {
        TestAttempt terminalizedAttempt = completedAssignedAttempt(9004L);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(ACTOR_USER_ID);
        when(foundationStateReadService.findAssignedAttemptSubmitAdmissionFoundationState(ACTOR_USER_ID, 7004L))
            .thenReturn(new AssignedAttemptSubmitAdmissionFoundationStateReadService.AssignedAttemptSubmitAdmissionFoundationState(
                7004L,
                80L,
                704L
            ));
        when(capabilityAdmissionRequestFactory.createAssignedAttemptSubmit(ACTOR_USER_ID, 80L, 704L))
            .thenReturn(new CapabilityAdmissionRequest(
                ACTOR_USER_ID,
                "TESTING_ASSIGNED_ATTEMPT_SUBMIT",
                com.vladislav.training.platform.application.policy.CapabilityTargetEntityType.ASSIGNMENT_TEST,
                704L,
                com.vladislav.training.platform.application.policy.CapabilityAdmissionPayload.Empty.INSTANCE,
                FIXED_INSTANT
            ));
        when(assignedAttemptSubmitTerminalService.submitAssignedAttempt(ACTOR_USER_ID, 704L, 7004L))
            .thenReturn(AttemptTerminalizationOutcome.assignedNormalSubmit(ACTOR_USER_ID, terminalizedAttempt));
        when(resultRecordingService.recordResult(9004L)).thenReturn(8004L);

        AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome outcome =
            assignedAttemptSubmissionService.submitAssignedAttempt(7004L);

        assertThat(outcome.recordedResult()).isEqualTo(8004L);
        org.mockito.Mockito.verify(resultRecordingService).recordResult(9004L);
        verifyNoInteractions(resultRepository);
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
