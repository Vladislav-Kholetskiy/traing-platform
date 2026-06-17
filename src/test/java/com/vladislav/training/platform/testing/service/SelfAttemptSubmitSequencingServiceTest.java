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
import com.vladislav.training.platform.assignment.service.AssignmentCountedResultHandoffService;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.testing.admission.SelfAttemptTerminalAdmissionFoundationStateReadService;
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
 * Проверяет поведение сервиса {@code SelfAttemptSubmitSequencing}.
 * Сценарии сосредоточены на прикладной логике.
 */
@ExtendWith(MockitoExtension.class)
class SelfAttemptSubmitSequencingServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-20T09:30:00Z");
    private static final Long ACTOR_USER_ID = 101L;

    @Mock
    private SelfAttemptSubmitTerminalService selfAttemptSubmitTerminalService;
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

    private CapabilityAdmissionRequest capabilityAdmissionRequest;

    private SelfAttemptSubmitSequencingService facade;

    @BeforeEach
    void setUp() {
        capabilityAdmissionRequest = new CapabilityAdmissionRequest(
            ACTOR_USER_ID,
            "TESTING_SELF_ATTEMPT_SUBMIT",
            com.vladislav.training.platform.application.policy.CapabilityTargetEntityType.TEST,
            501L,
            com.vladislav.training.platform.application.policy.CapabilityAdmissionPayload.Empty.INSTANCE,
            Instant.parse("2026-04-23T10:00:00Z")
        );
        facade = new SelfAttemptSubmitSequencingService(
            selfAttemptSubmitTerminalService,
            resultRecordingService,
            criticalCommandAuditSupport,
            capabilityAdmissionPolicy,
            capabilityAdmissionRequestFactory,
            foundationStateReadService
        );
    }

    @Test
    void submitSelfAttemptTerminalizesFirstThenRecordsCanonicalResult() {
        TestAttempt terminalizedAttempt = completedSelfAttempt(9001L);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(ACTOR_USER_ID);
        when(foundationStateReadService.findSelfAttemptTerminalAdmissionFoundationState(ACTOR_USER_ID, 9001L))
            .thenReturn(new SelfAttemptTerminalAdmissionFoundationStateReadService.SelfAttemptTerminalAdmissionFoundationState(
                9001L,
                501L
            ));
        when(capabilityAdmissionRequestFactory.createSelfAttemptSubmit(ACTOR_USER_ID, 501L)).thenReturn(capabilityAdmissionRequest);
        when(selfAttemptSubmitTerminalService.submitSelfAttempt(ACTOR_USER_ID, 9001L))
            .thenReturn(AttemptTerminalizationOutcome.selfNormalSubmit(ACTOR_USER_ID, terminalizedAttempt));
        when(resultRecordingService.recordResult(9001L)).thenReturn(7001L);

        Long resultId = facade.submitSelfAttempt(9001L);

        assertThat(resultId).isEqualTo(7001L);
        InOrder inOrder = inOrder(
            criticalCommandAuditSupport,
            foundationStateReadService,
            capabilityAdmissionRequestFactory,
            capabilityAdmissionPolicy,
            selfAttemptSubmitTerminalService,
            resultRecordingService
        );
        inOrder.verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        inOrder.verify(foundationStateReadService).findSelfAttemptTerminalAdmissionFoundationState(ACTOR_USER_ID, 9001L);
        inOrder.verify(capabilityAdmissionRequestFactory).createSelfAttemptSubmit(ACTOR_USER_ID, 501L);
        inOrder.verify(capabilityAdmissionPolicy).check(capabilityAdmissionRequest);
        inOrder.verify(selfAttemptSubmitTerminalService).submitSelfAttempt(ACTOR_USER_ID, 9001L);
        inOrder.verify(resultRecordingService).recordResult(9001L);
    }

    @Test
    void resultRecordingUsesTerminalizedAttemptIdReturnedBySelfSubmitOwnerService() {
        TestAttempt terminalizedAttempt = completedSelfAttempt(9002L);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(ACTOR_USER_ID);
        when(foundationStateReadService.findSelfAttemptTerminalAdmissionFoundationState(ACTOR_USER_ID, 1234L))
            .thenReturn(new SelfAttemptTerminalAdmissionFoundationStateReadService.SelfAttemptTerminalAdmissionFoundationState(
                1234L,
                502L
            ));
        when(capabilityAdmissionRequestFactory.createSelfAttemptSubmit(ACTOR_USER_ID, 502L)).thenReturn(capabilityAdmissionRequest);
        when(selfAttemptSubmitTerminalService.submitSelfAttempt(ACTOR_USER_ID, 1234L))
            .thenReturn(AttemptTerminalizationOutcome.selfNormalSubmit(ACTOR_USER_ID, terminalizedAttempt));
        when(resultRecordingService.recordResult(9002L)).thenReturn(7002L);

        Long resultId = facade.submitSelfAttempt(1234L);

        assertThat(resultId).isEqualTo(7002L);
        org.mockito.Mockito.verify(resultRecordingService).recordResult(9002L);
    }

    @Test
    void whenSelfSubmitFailsResultRecordingIsNotCalled() {
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(ACTOR_USER_ID);
        when(foundationStateReadService.findSelfAttemptTerminalAdmissionFoundationState(ACTOR_USER_ID, 9003L))
            .thenReturn(new SelfAttemptTerminalAdmissionFoundationStateReadService.SelfAttemptTerminalAdmissionFoundationState(
                9003L,
                503L
            ));
        when(capabilityAdmissionRequestFactory.createSelfAttemptSubmit(ACTOR_USER_ID, 503L)).thenReturn(capabilityAdmissionRequest);
        when(selfAttemptSubmitTerminalService.submitSelfAttempt(ACTOR_USER_ID, 9003L))
            .thenThrow(new ConflictException("self submit failed"));

        assertThatThrownBy(() -> facade.submitSelfAttempt(9003L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("self submit failed");

        verifyNoInteractions(resultRecordingService, assignmentCountedResultHandoffService);
    }

    @Test
    void whenResultRecordingBoundaryFailsSequencingDoesNotPretendSelfResultWasMaterialized() {
        TestAttempt terminalizedAttempt = completedSelfAttempt(9004L);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(ACTOR_USER_ID);
        when(foundationStateReadService.findSelfAttemptTerminalAdmissionFoundationState(ACTOR_USER_ID, 9004L))
            .thenReturn(new SelfAttemptTerminalAdmissionFoundationStateReadService.SelfAttemptTerminalAdmissionFoundationState(
                9004L,
                504L
            ));
        when(capabilityAdmissionRequestFactory.createSelfAttemptSubmit(ACTOR_USER_ID, 504L)).thenReturn(capabilityAdmissionRequest);
        when(selfAttemptSubmitTerminalService.submitSelfAttempt(ACTOR_USER_ID, 9004L))
            .thenReturn(AttemptTerminalizationOutcome.selfNormalSubmit(ACTOR_USER_ID, terminalizedAttempt));
        when(resultRecordingService.recordResult(9004L))
            .thenThrow(new ConflictException("self result recording failed"));

        assertThatThrownBy(() -> facade.submitSelfAttempt(9004L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("self result recording failed");
    }

    @Test
    void rejectsNonRecordableSelfTerminalizationOutcomeWithoutRecordingResult() {
        TestAttempt terminalizedAttempt = completedSelfAttempt(9008L);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(ACTOR_USER_ID);
        when(foundationStateReadService.findSelfAttemptTerminalAdmissionFoundationState(ACTOR_USER_ID, 9008L))
            .thenReturn(new SelfAttemptTerminalAdmissionFoundationStateReadService.SelfAttemptTerminalAdmissionFoundationState(
                9008L,
                508L
            ));
        when(capabilityAdmissionRequestFactory.createSelfAttemptSubmit(ACTOR_USER_ID, 508L)).thenReturn(capabilityAdmissionRequest);
        when(selfAttemptSubmitTerminalService.submitSelfAttempt(ACTOR_USER_ID, 9008L))
            .thenReturn(new AttemptTerminalizationOutcome(
                9008L,
                ACTOR_USER_ID,
                AttemptMode.SELF,
                TestAttemptStatus.COMPLETED,
                FIXED_INSTANT,
                AttemptTerminalizationReason.NORMAL_SUBMIT,
                false,
                AttemptTerminalCriticalAuditCatalog.SELF_ATTEMPT_SUBMITTED.auditEventType(),
                terminalizedAttempt
            ));

        assertThatThrownBy(() -> facade.submitSelfAttempt(9008L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("requires recordable terminalization outcome");

        verifyNoInteractions(resultRecordingService, assignmentCountedResultHandoffService);
    }

    @Test
    void facadeDoesNotInteractWithAssignmentSideDirectly() {
        TestAttempt terminalizedAttempt = completedSelfAttempt(9005L);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(ACTOR_USER_ID);
        when(foundationStateReadService.findSelfAttemptTerminalAdmissionFoundationState(ACTOR_USER_ID, 9005L))
            .thenReturn(new SelfAttemptTerminalAdmissionFoundationStateReadService.SelfAttemptTerminalAdmissionFoundationState(
                9005L,
                505L
            ));
        when(capabilityAdmissionRequestFactory.createSelfAttemptSubmit(ACTOR_USER_ID, 505L)).thenReturn(capabilityAdmissionRequest);
        when(selfAttemptSubmitTerminalService.submitSelfAttempt(ACTOR_USER_ID, 9005L))
            .thenReturn(AttemptTerminalizationOutcome.selfNormalSubmit(ACTOR_USER_ID, terminalizedAttempt));
        when(resultRecordingService.recordResult(9005L)).thenReturn(7005L);

        facade.submitSelfAttempt(9005L);

        verifyNoInteractions(assignmentCountedResultHandoffService);
    }

    @Test
    void submitSelfAttemptRejectsFailClosedBeforeTerminalCoreWhenAdmissionFails() {
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(ACTOR_USER_ID);
        when(foundationStateReadService.findSelfAttemptTerminalAdmissionFoundationState(ACTOR_USER_ID, 9006L))
            .thenReturn(new SelfAttemptTerminalAdmissionFoundationStateReadService.SelfAttemptTerminalAdmissionFoundationState(
                9006L,
                506L
            ));
        when(capabilityAdmissionRequestFactory.createSelfAttemptSubmit(ACTOR_USER_ID, 506L)).thenReturn(capabilityAdmissionRequest);
        org.mockito.Mockito.doThrow(new ConflictException("self admission denied"))
            .when(capabilityAdmissionPolicy)
            .check(capabilityAdmissionRequest);

        assertThatThrownBy(() -> facade.submitSelfAttempt(9006L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("self admission denied");

        verifyNoInteractions(selfAttemptSubmitTerminalService, resultRecordingService, assignmentCountedResultHandoffService);
    }

    @Test
    void submitSelfAttemptRejectsWhenFoundationAnchorIsMissing() {
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(ACTOR_USER_ID);
        when(foundationStateReadService.findSelfAttemptTerminalAdmissionFoundationState(ACTOR_USER_ID, 9007L)).thenReturn(null);

        assertThatThrownBy(() -> facade.submitSelfAttempt(9007L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Self submit foundation not found");

        verifyNoInteractions(
            capabilityAdmissionRequestFactory,
            capabilityAdmissionPolicy,
            selfAttemptSubmitTerminalService,
            resultRecordingService
        );
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
}
