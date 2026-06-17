package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.assignment.service.AssignmentCountedResultHandoffService;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.testing.admission.AssignedAttemptSubmitAdmissionFoundationStateReadService;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение сервиса {@code AssignedAttemptSubmission}.
 * Сценарии сосредоточены на прикладной логике.
 */
@ExtendWith(MockitoExtension.class)
class AssignedAttemptSubmissionServiceTest {

    private static final Long ACTOR_USER_ID = 101L;

    @Mock
    private AssignedAttemptSubmitSequencingService assignedAttemptSubmitOrchestrationFacade;
    @Mock
    private CriticalCommandAuditSupport criticalCommandAuditSupport;
    @Mock
    private CapabilityAdmissionPolicy capabilityAdmissionPolicy;
    @Mock
    private CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;
    @Mock
    private AssignedAttemptSubmitAdmissionFoundationStateReadService foundationStateReadService;
    @Mock
    private AssignedAttemptSubmitTerminalService assignedAttemptSubmitTerminalService;
    @Mock
    private ResultRecordingService resultRecordingService;
    @Mock
    private AssignmentCountedResultHandoffService assignmentCountedResultHandoffService;

    private CapabilityAdmissionRequest capabilityAdmissionRequest;

    private AssignedAttemptSubmissionService service;

    @BeforeEach
    void setUp() {
        capabilityAdmissionRequest = new CapabilityAdmissionRequest(
            ACTOR_USER_ID,
            "TESTING_ASSIGNED_ATTEMPT_SUBMIT",
            com.vladislav.training.platform.application.policy.CapabilityTargetEntityType.ASSIGNMENT_TEST,
            701L,
            com.vladislav.training.platform.application.policy.CapabilityAdmissionPayload.Empty.INSTANCE,
            java.time.Instant.parse("2026-04-23T10:00:00Z")
        );
        service = new AssignedAttemptSubmissionService(
            assignedAttemptSubmitOrchestrationFacade,
            criticalCommandAuditSupport,
            capabilityAdmissionPolicy,
            capabilityAdmissionRequestFactory,
            foundationStateReadService
        );
    }

    @Test
    void assignedSubmitEntryDelegatesOnlyToAssignedSubmitOrchestrationFacade() {
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(ACTOR_USER_ID);
        when(foundationStateReadService.findAssignedAttemptSubmitAdmissionFoundationState(ACTOR_USER_ID, 9001L))
            .thenReturn(new AssignedAttemptSubmitAdmissionFoundationStateReadService.AssignedAttemptSubmitAdmissionFoundationState(
                9001L,
                77L,
                701L
            ));
        when(capabilityAdmissionRequestFactory.createAssignedAttemptSubmit(ACTOR_USER_ID, 77L, 701L))
            .thenReturn(capabilityAdmissionRequest);
        when(assignedAttemptSubmitOrchestrationFacade.submitAssignedAttempt(ACTOR_USER_ID, 701L, 9001L))
            .thenReturn(AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome.completed(9001L, 7001L));

        assertThat(service.submitAssignedAttempt(9001L))
            .isEqualTo(AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome.completed(9001L, 7001L));

        inOrder(
            criticalCommandAuditSupport,
            foundationStateReadService,
            capabilityAdmissionRequestFactory,
            capabilityAdmissionPolicy,
            assignedAttemptSubmitOrchestrationFacade
        )
            .verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        org.mockito.Mockito.verify(foundationStateReadService).findAssignedAttemptSubmitAdmissionFoundationState(ACTOR_USER_ID, 9001L);
        org.mockito.Mockito.verify(capabilityAdmissionRequestFactory).createAssignedAttemptSubmit(ACTOR_USER_ID, 77L, 701L);
        org.mockito.Mockito.verify(capabilityAdmissionPolicy).check(capabilityAdmissionRequest);
        verify(assignedAttemptSubmitOrchestrationFacade).submitAssignedAttempt(ACTOR_USER_ID, 701L, 9001L);
        verifyNoInteractions(
            assignedAttemptSubmitTerminalService,
            resultRecordingService,
            assignmentCountedResultHandoffService
        );
    }

    @Test
    void assignedSubmitEntryReturnsCanonicalResultIdFromFacade() {
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(ACTOR_USER_ID);
        when(foundationStateReadService.findAssignedAttemptSubmitAdmissionFoundationState(ACTOR_USER_ID, 9002L))
            .thenReturn(new AssignedAttemptSubmitAdmissionFoundationStateReadService.AssignedAttemptSubmitAdmissionFoundationState(
                9002L,
                78L,
                702L
            ));
        when(capabilityAdmissionRequestFactory.createAssignedAttemptSubmit(ACTOR_USER_ID, 78L, 702L))
            .thenReturn(capabilityAdmissionRequest);
        when(assignedAttemptSubmitOrchestrationFacade.submitAssignedAttempt(ACTOR_USER_ID, 702L, 9002L))
            .thenReturn(AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome.completed(9002L, 7002L));

        var outcome = service.submitAssignedAttempt(9002L);
        assertThat(outcome.recordedResult()).isEqualTo(7002L);
        assertThat(outcome.terminalStatus()).isEqualTo(TestAttemptStatus.COMPLETED);
    }

    @Test
    void whenFacadeFailsEntryDoesNotCallTerminalOrResultOwnersDirectly() {
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(ACTOR_USER_ID);
        when(foundationStateReadService.findAssignedAttemptSubmitAdmissionFoundationState(ACTOR_USER_ID, 9003L))
            .thenReturn(new AssignedAttemptSubmitAdmissionFoundationStateReadService.AssignedAttemptSubmitAdmissionFoundationState(
                9003L,
                79L,
                703L
            ));
        when(capabilityAdmissionRequestFactory.createAssignedAttemptSubmit(ACTOR_USER_ID, 79L, 703L))
            .thenReturn(capabilityAdmissionRequest);
        when(assignedAttemptSubmitOrchestrationFacade.submitAssignedAttempt(ACTOR_USER_ID, 703L, 9003L))
            .thenThrow(new ConflictException("orchestration failed"));

        assertThatThrownBy(() -> service.submitAssignedAttempt(9003L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("orchestration failed");

        verify(assignedAttemptSubmitOrchestrationFacade).submitAssignedAttempt(ACTOR_USER_ID, 703L, 9003L);
        verifyNoInteractions(
            assignedAttemptSubmitTerminalService,
            resultRecordingService,
            assignmentCountedResultHandoffService
        );
    }

    @Test
    void assignedSubmitEntryDoesNotInteractWithAssignmentSideDirectly() {
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(ACTOR_USER_ID);
        when(foundationStateReadService.findAssignedAttemptSubmitAdmissionFoundationState(ACTOR_USER_ID, 9004L))
            .thenReturn(new AssignedAttemptSubmitAdmissionFoundationStateReadService.AssignedAttemptSubmitAdmissionFoundationState(
                9004L,
                80L,
                704L
            ));
        when(capabilityAdmissionRequestFactory.createAssignedAttemptSubmit(ACTOR_USER_ID, 80L, 704L))
            .thenReturn(capabilityAdmissionRequest);
        when(assignedAttemptSubmitOrchestrationFacade.submitAssignedAttempt(ACTOR_USER_ID, 704L, 9004L))
            .thenReturn(AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome.completed(9004L, 7004L));

        assertThat(service.submitAssignedAttempt(9004L).attemptId()).isEqualTo(9004L);

        verifyNoInteractions(assignmentCountedResultHandoffService);
    }

    @Test
    void assignedSubmitEntryRejectsFailClosedBeforeSequencingWhenAdmissionFails() {
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(ACTOR_USER_ID);
        when(foundationStateReadService.findAssignedAttemptSubmitAdmissionFoundationState(ACTOR_USER_ID, 9005L))
            .thenReturn(new AssignedAttemptSubmitAdmissionFoundationStateReadService.AssignedAttemptSubmitAdmissionFoundationState(
                9005L,
                81L,
                705L
            ));
        when(capabilityAdmissionRequestFactory.createAssignedAttemptSubmit(ACTOR_USER_ID, 81L, 705L))
            .thenReturn(capabilityAdmissionRequest);
        org.mockito.Mockito.doThrow(new ConflictException("admission denied"))
            .when(capabilityAdmissionPolicy)
            .check(capabilityAdmissionRequest);

        assertThatThrownBy(() -> service.submitAssignedAttempt(9005L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("admission denied");

        verifyNoInteractions(
            assignedAttemptSubmitOrchestrationFacade,
            assignedAttemptSubmitTerminalService,
            resultRecordingService,
            assignmentCountedResultHandoffService
        );
    }

    @Test
    void assignedSubmitEntryRejectsWhenFoundationAnchorIsMissing() {
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(ACTOR_USER_ID);
        when(foundationStateReadService.findAssignedAttemptSubmitAdmissionFoundationState(ACTOR_USER_ID, 9006L))
            .thenReturn(null);

        assertThatThrownBy(() -> service.submitAssignedAttempt(9006L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Assigned submit foundation not found");

        verifyNoInteractions(
            capabilityAdmissionRequestFactory,
            capabilityAdmissionPolicy,
            assignedAttemptSubmitOrchestrationFacade,
            assignedAttemptSubmitTerminalService,
            resultRecordingService
        );
    }

    @Test
    void assignedSubmitEntryCanReturnExplicitExpiredOutcomeWithoutAmbiguousNull() {
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(ACTOR_USER_ID);
        when(foundationStateReadService.findAssignedAttemptSubmitAdmissionFoundationState(ACTOR_USER_ID, 9007L))
            .thenReturn(new AssignedAttemptSubmitAdmissionFoundationStateReadService.AssignedAttemptSubmitAdmissionFoundationState(
                9007L,
                82L,
                706L
            ));
        when(capabilityAdmissionRequestFactory.createAssignedAttemptSubmit(ACTOR_USER_ID, 82L, 706L))
            .thenReturn(capabilityAdmissionRequest);
        when(assignedAttemptSubmitOrchestrationFacade.submitAssignedAttempt(ACTOR_USER_ID, 706L, 9007L))
            .thenReturn(AssignedAttemptSubmitSequencingService.AssignedAttemptSubmitOutcome.expired(9007L));

        var outcome = service.submitAssignedAttempt(9007L);

        assertThat(outcome.attemptId()).isEqualTo(9007L);
        assertThat(outcome.terminalStatus()).isEqualTo(TestAttemptStatus.EXPIRED);
        assertThat(outcome.recordedResult()).isNull();
    }
}
