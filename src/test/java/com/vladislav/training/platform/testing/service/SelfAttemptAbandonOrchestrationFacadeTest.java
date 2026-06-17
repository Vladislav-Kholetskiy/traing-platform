package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.assignment.service.AssignmentCountedResultHandoffService;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.testing.admission.SelfAttemptTerminalAdmissionFoundationStateReadService;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code SelfAttemptAbandonSequencingService}.
 * Сценарии сосредоточены на прикладной логике.
 */

@ExtendWith(MockitoExtension.class)
class SelfAttemptAbandonSequencingServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-20T10:15:00Z");
    private static final Long ACTOR_USER_ID = 101L;

    @Mock
    private SelfAttemptAbandonTerminalService selfAttemptAbandonTerminalService;
    @Mock
    private CriticalCommandAuditSupport criticalCommandAuditSupport;
    @Mock
    private CapabilityAdmissionPolicy capabilityAdmissionPolicy;
    @Mock
    private CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;
    @Mock
    private SelfAttemptTerminalAdmissionFoundationStateReadService foundationStateReadService;
    @Mock
    private ResultRecordingService resultRecordingService;
    @Mock
    private AssignmentCountedResultHandoffService assignmentCountedResultHandoffService;
    @Mock
    private SelfAttemptSubmitSequencingService selfAttemptSubmitOrchestrationFacade;
    @Mock
    private AssignedAttemptSubmitSequencingService assignedAttemptSubmitOrchestrationFacade;

    private CapabilityAdmissionRequest capabilityAdmissionRequest;

    private SelfAttemptAbandonSequencingService facade;

    @BeforeEach
    void setUp() {
        capabilityAdmissionRequest = new CapabilityAdmissionRequest(
            ACTOR_USER_ID,
            "TESTING_SELF_ATTEMPT_ABANDON",
            com.vladislav.training.platform.application.policy.CapabilityTargetEntityType.TEST,
            501L,
            com.vladislav.training.platform.application.policy.CapabilityAdmissionPayload.Empty.INSTANCE,
            Instant.parse("2026-04-23T10:00:00Z")
        );
        facade = new SelfAttemptAbandonSequencingService(
            selfAttemptAbandonTerminalService,
            criticalCommandAuditSupport,
            capabilityAdmissionPolicy,
            capabilityAdmissionRequestFactory,
            foundationStateReadService
        );
    }

    @Test
    void abandonSelfAttemptDelegatesToSelfAbandonOwnerOnly() {
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(ACTOR_USER_ID);
        when(foundationStateReadService.findSelfAttemptTerminalAdmissionFoundationState(ACTOR_USER_ID, 9001L))
            .thenReturn(new SelfAttemptTerminalAdmissionFoundationStateReadService.SelfAttemptTerminalAdmissionFoundationState(
                9001L,
                501L
            ));
        when(capabilityAdmissionRequestFactory.createSelfAttemptAbandon(ACTOR_USER_ID, 501L)).thenReturn(capabilityAdmissionRequest);
        when(selfAttemptAbandonTerminalService.abandonSelfAttempt(ACTOR_USER_ID, 9001L))
            .thenReturn(AttemptTerminalizationOutcome.selfAbandon(ACTOR_USER_ID, abandonedSelfAttempt(9001L)));

        assertThat(facade.abandonSelfAttempt(9001L)).isEqualTo(9001L);

        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(
            criticalCommandAuditSupport,
            foundationStateReadService,
            capabilityAdmissionRequestFactory,
            capabilityAdmissionPolicy,
            selfAttemptAbandonTerminalService
        );
        inOrder.verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        inOrder.verify(foundationStateReadService).findSelfAttemptTerminalAdmissionFoundationState(ACTOR_USER_ID, 9001L);
        inOrder.verify(capabilityAdmissionRequestFactory).createSelfAttemptAbandon(ACTOR_USER_ID, 501L);
        inOrder.verify(capabilityAdmissionPolicy).check(capabilityAdmissionRequest);
        org.mockito.Mockito.verify(selfAttemptAbandonTerminalService).abandonSelfAttempt(ACTOR_USER_ID, 9001L);
        verifyNoInteractions(
            resultRecordingService,
            assignmentCountedResultHandoffService,
            selfAttemptSubmitOrchestrationFacade,
            assignedAttemptSubmitOrchestrationFacade
        );
    }

    @Test
    void facadeReturnsCanonicalAbandonedAttemptId() {
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(ACTOR_USER_ID);
        when(foundationStateReadService.findSelfAttemptTerminalAdmissionFoundationState(ACTOR_USER_ID, 9002L))
            .thenReturn(new SelfAttemptTerminalAdmissionFoundationStateReadService.SelfAttemptTerminalAdmissionFoundationState(
                9002L,
                502L
            ));
        when(capabilityAdmissionRequestFactory.createSelfAttemptAbandon(ACTOR_USER_ID, 502L)).thenReturn(capabilityAdmissionRequest);
        when(selfAttemptAbandonTerminalService.abandonSelfAttempt(ACTOR_USER_ID, 9002L))
            .thenReturn(AttemptTerminalizationOutcome.selfAbandon(ACTOR_USER_ID, abandonedSelfAttempt(9002L)));

        assertThat(facade.abandonSelfAttempt(9002L)).isEqualTo(9002L);
    }

    @Test
    void whenSelfAbandonFailsFacadeDoesNotCallForeignOwners() {
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(ACTOR_USER_ID);
        when(foundationStateReadService.findSelfAttemptTerminalAdmissionFoundationState(ACTOR_USER_ID, 9003L))
            .thenReturn(new SelfAttemptTerminalAdmissionFoundationStateReadService.SelfAttemptTerminalAdmissionFoundationState(
                9003L,
                503L
            ));
        when(capabilityAdmissionRequestFactory.createSelfAttemptAbandon(ACTOR_USER_ID, 503L)).thenReturn(capabilityAdmissionRequest);
        when(selfAttemptAbandonTerminalService.abandonSelfAttempt(ACTOR_USER_ID, 9003L))
            .thenThrow(new ConflictException("self abandon failed"));

        assertThatThrownBy(() -> facade.abandonSelfAttempt(9003L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("self abandon failed");

        verifyNoInteractions(
            resultRecordingService,
            assignmentCountedResultHandoffService,
            selfAttemptSubmitOrchestrationFacade,
            assignedAttemptSubmitOrchestrationFacade
        );
    }

    @Test
    void abandonDoesNotRecordResult() {
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(ACTOR_USER_ID);
        when(foundationStateReadService.findSelfAttemptTerminalAdmissionFoundationState(ACTOR_USER_ID, 9004L))
            .thenReturn(new SelfAttemptTerminalAdmissionFoundationStateReadService.SelfAttemptTerminalAdmissionFoundationState(
                9004L,
                504L
            ));
        when(capabilityAdmissionRequestFactory.createSelfAttemptAbandon(ACTOR_USER_ID, 504L)).thenReturn(capabilityAdmissionRequest);
        when(selfAttemptAbandonTerminalService.abandonSelfAttempt(ACTOR_USER_ID, 9004L))
            .thenReturn(AttemptTerminalizationOutcome.selfAbandon(ACTOR_USER_ID, abandonedSelfAttempt(9004L)));

        facade.abandonSelfAttempt(9004L);

        verifyNoInteractions(resultRecordingService, assignmentCountedResultHandoffService);
    }

    @Test
    void abandonSelfAttemptRejectsRecordableTerminalizationOutcomeAsFailClosed() {
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(ACTOR_USER_ID);
        when(foundationStateReadService.findSelfAttemptTerminalAdmissionFoundationState(ACTOR_USER_ID, 9007L))
            .thenReturn(new SelfAttemptTerminalAdmissionFoundationStateReadService.SelfAttemptTerminalAdmissionFoundationState(
                9007L,
                507L
            ));
        when(capabilityAdmissionRequestFactory.createSelfAttemptAbandon(ACTOR_USER_ID, 507L)).thenReturn(capabilityAdmissionRequest);
        when(selfAttemptAbandonTerminalService.abandonSelfAttempt(ACTOR_USER_ID, 9007L))
            .thenReturn(new AttemptTerminalizationOutcome(
                9007L,
                ACTOR_USER_ID,
                AttemptMode.SELF,
                TestAttemptStatus.ABANDONED,
                FIXED_INSTANT,
                AttemptTerminalizationReason.SELF_ABANDON,
                true,
                AttemptTerminalCriticalAuditCatalog.SELF_ATTEMPT_ABANDONED.auditEventType(),
                abandonedSelfAttempt(9007L)
            ));

        assertThatThrownBy(() -> facade.abandonSelfAttempt(9007L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("non-recordable");

        verifyNoInteractions(resultRecordingService, assignmentCountedResultHandoffService);
    }

    @Test
    void abandonSelfAttemptRejectsFailClosedBeforeTerminalCoreWhenAdmissionFails() {
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(ACTOR_USER_ID);
        when(foundationStateReadService.findSelfAttemptTerminalAdmissionFoundationState(ACTOR_USER_ID, 9005L))
            .thenReturn(new SelfAttemptTerminalAdmissionFoundationStateReadService.SelfAttemptTerminalAdmissionFoundationState(
                9005L,
                505L
            ));
        when(capabilityAdmissionRequestFactory.createSelfAttemptAbandon(ACTOR_USER_ID, 505L)).thenReturn(capabilityAdmissionRequest);
        org.mockito.Mockito.doThrow(new ConflictException("self abandon admission denied"))
            .when(capabilityAdmissionPolicy)
            .check(capabilityAdmissionRequest);

        assertThatThrownBy(() -> facade.abandonSelfAttempt(9005L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("self abandon admission denied");

        verifyNoInteractions(
            selfAttemptAbandonTerminalService,
            resultRecordingService,
            assignmentCountedResultHandoffService,
            selfAttemptSubmitOrchestrationFacade,
            assignedAttemptSubmitOrchestrationFacade
        );
    }

    @Test
    void abandonSelfAttemptRejectsWhenFoundationAnchorIsMissing() {
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(ACTOR_USER_ID);
        when(foundationStateReadService.findSelfAttemptTerminalAdmissionFoundationState(ACTOR_USER_ID, 9006L))
            .thenReturn(null);

        assertThatThrownBy(() -> facade.abandonSelfAttempt(9006L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Self abandon foundation not found");

        verifyNoInteractions(
            capabilityAdmissionRequestFactory,
            capabilityAdmissionPolicy,
            selfAttemptAbandonTerminalService,
            resultRecordingService
        );
    }

    private com.vladislav.training.platform.testing.domain.TestAttempt abandonedSelfAttempt(Long attemptId) {
        return new com.vladislav.training.platform.testing.domain.TestAttempt(
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
