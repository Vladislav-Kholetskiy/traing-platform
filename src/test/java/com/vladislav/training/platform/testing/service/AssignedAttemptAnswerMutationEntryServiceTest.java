package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPayload;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.audit.domain.AuditContext;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.application.policy.CapabilityOperationCodes;
import com.vladislav.training.platform.application.policy.CapabilityTargetEntityType;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.testing.admission.AssignedAnswerMutationAdmissionFoundationStateReadService;
import com.vladislav.training.platform.testing.controller.dto.ActiveAttemptAnswerMutationRequest;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение сервиса {@code AssignedAttemptAnswerMutationEntry}.
 * Сценарии сосредоточены на прикладной логике.
 */
@ExtendWith(MockitoExtension.class)
class AssignedAttemptAnswerMutationEntryServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-21T09:00:00Z");

    @Mock
    private InteractiveActorResolver interactiveActorResolver;
    @Mock
    private CapabilityAdmissionPolicy capabilityAdmissionPolicy;
    @Mock
    private CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;
    @Mock
    private AssignedAnswerMutationAdmissionFoundationStateReadService foundationStateReadService;
    @Mock
    private AttemptStatusRecalculationService attemptStatusRecalculationService;
    @Mock
    private ActiveAttemptAnswerMutationService activeAttemptAnswerMutationService;
    @Mock
    private CriticalCommandAuditSupport criticalCommandAuditSupport;
    @Mock
    private UtcClock utcClock;

    private AssignedAttemptAnswerMutationEntryService service;

    @BeforeEach
    void setUp() {
        service = new AssignedAttemptAnswerMutationEntryService(
            interactiveActorResolver,
            capabilityAdmissionPolicy,
            capabilityAdmissionRequestFactory,
            foundationStateReadService,
            attemptStatusRecalculationService,
            activeAttemptAnswerMutationService,
            criticalCommandAuditSupport,
            utcClock
        );
    }

    @Test
    void assignedSaveAnswerResolvesActorThenChecksAdmissionBeforeMutationAndWritesAuditAfterOwnerMutation() {
        ActiveAttemptAnswerMutationRequest request = request();
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(41L);
        when(foundationStateReadService.findAssignedAnswerMutationAdmissionFoundationState(41L, 101L))
            .thenReturn(new AssignedAnswerMutationAdmissionFoundationStateReadService.AssignedAnswerMutationAdmissionFoundationState(
                101L,
                71L,
                901L
            ));
        CapabilityAdmissionRequest admissionRequest = assignedAnswerMutationRequest();
        when(capabilityAdmissionRequestFactory.createAssignedAnswerMutation(41L, 71L, 901L)).thenReturn(admissionRequest);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(attemptStatusRecalculationService.recalculateAttemptStatus(101L, FIXED_INSTANT))
            .thenReturn(TestAttemptStatus.IN_PROGRESS);
        when(activeAttemptAnswerMutationService.saveOrReplaceAnswer(
            org.mockito.Mockito.eq(41L),
            org.mockito.Mockito.eq(101L),
            org.mockito.Mockito.eq(501L),
            org.mockito.ArgumentMatchers.anyList(),
            org.mockito.Mockito.eq(FIXED_INSTANT)
        )).thenReturn(updatedAttempt());
        AuditContext auditContext = new AuditContext("{\"operationCode\":\"TESTING_ASSIGNED_ANSWER_MUTATION\"}");
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.Mockito.eq("Testing"),
            org.mockito.Mockito.eq(CapabilityOperationCodes.TESTING_ASSIGNED_ANSWER_MUTATION),
            org.mockito.ArgumentMatchers.anyMap()
        )).thenReturn(auditContext);

        service.saveOrReplaceAssignedAnswer(101L, 501L, request);

        InOrder inOrder = inOrder(
            interactiveActorResolver,
            capabilityAdmissionRequestFactory,
            capabilityAdmissionPolicy,
            foundationStateReadService,
            attemptStatusRecalculationService,
            utcClock,
            activeAttemptAnswerMutationService,
            criticalCommandAuditSupport
        );
        inOrder.verify(interactiveActorResolver).resolveActorUserId();
        inOrder.verify(foundationStateReadService).findAssignedAnswerMutationAdmissionFoundationState(41L, 101L);
        inOrder.verify(capabilityAdmissionRequestFactory).createAssignedAnswerMutation(41L, 71L, 901L);
        inOrder.verify(capabilityAdmissionPolicy).check(admissionRequest);
        inOrder.verify(utcClock).now();
        inOrder.verify(attemptStatusRecalculationService).recalculateAttemptStatus(101L, FIXED_INSTANT);
        inOrder.verify(activeAttemptAnswerMutationService).saveOrReplaceAnswer(
            org.mockito.Mockito.eq(41L),
            org.mockito.Mockito.eq(101L),
            org.mockito.Mockito.eq(501L),
            org.mockito.ArgumentMatchers.anyList(),
            org.mockito.Mockito.eq(FIXED_INSTANT)
        );
        inOrder.verify(criticalCommandAuditSupport).buildAuditContext(
            org.mockito.Mockito.eq("Testing"),
            org.mockito.Mockito.eq(CapabilityOperationCodes.TESTING_ASSIGNED_ANSWER_MUTATION),
            org.mockito.ArgumentMatchers.argThat(details ->
                "assigned_answer_mutation".equals(details.get("commandType"))
                    && "save_or_replace".equals(details.get("mutationAction"))
                    && Long.valueOf(71L).equals(details.get("assignmentId"))
                    && Long.valueOf(901L).equals(details.get("assignmentTestId"))
                    && Long.valueOf(301L).equals(details.get("testId"))
                    && Long.valueOf(501L).equals(details.get("questionId"))
                    && Long.valueOf(101L).equals(details.get("attemptId"))
                    && AttemptMode.ASSIGNED == details.get("attemptMode")
                    && Integer.valueOf(1).equals(details.get("answerItemCount"))
            )
        );
        inOrder.verify(criticalCommandAuditSupport).recordAudit(
            org.mockito.Mockito.eq(41L),
            org.mockito.ArgumentMatchers.argThat(eventType -> "TESTING_ASSIGNED_ANSWER_MUTATED".equals(eventType.value())),
            org.mockito.Mockito.eq("test_attempt"),
            org.mockito.Mockito.eq(101L),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.argThat(payload -> {
                if (!(payload instanceof java.util.Map<?, ?> payloadMap)) {
                    return false;
                }
                return "save_or_replace".equals(payloadMap.get("mutationAction"))
                    && Long.valueOf(501L).equals(payloadMap.get("questionId"))
                    && Integer.valueOf(1).equals(payloadMap.get("answerItemCount"))
                    && Long.valueOf(71L).equals(payloadMap.get("assignmentId"))
                    && payloadMap.get("attempt") instanceof java.util.Map<?, ?> attemptMap
                    && Long.valueOf(101L).equals(attemptMap.get("id"))
                    && AttemptMode.ASSIGNED == attemptMap.get("attemptMode")
                    && TestAttemptStatus.IN_PROGRESS == attemptMap.get("status");
            }),
            org.mockito.Mockito.eq(auditContext)
        );
    }

    @Test
    void assignedClearAnswerRejectsFailClosedBeforeMutationWhenAdmissionFails() {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(41L);
        when(foundationStateReadService.findAssignedAnswerMutationAdmissionFoundationState(41L, 101L))
            .thenReturn(new AssignedAnswerMutationAdmissionFoundationStateReadService.AssignedAnswerMutationAdmissionFoundationState(
                101L,
                71L,
                901L
            ));
        CapabilityAdmissionRequest admissionRequest = assignedAnswerMutationRequest();
        when(capabilityAdmissionRequestFactory.createAssignedAnswerMutation(41L, 71L, 901L)).thenReturn(admissionRequest);
        org.mockito.Mockito.doThrow(new PolicyViolationException("DENIED", "Assigned answer mutation denied"))
            .when(capabilityAdmissionPolicy).check(admissionRequest);

        assertThatThrownBy(() -> service.clearAssignedAnswer(101L, 501L))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("denied");

        InOrder inOrder = inOrder(
            interactiveActorResolver,
            capabilityAdmissionRequestFactory,
            capabilityAdmissionPolicy,
            foundationStateReadService,
            utcClock
        );
        inOrder.verify(interactiveActorResolver).resolveActorUserId();
        inOrder.verify(foundationStateReadService).findAssignedAnswerMutationAdmissionFoundationState(41L, 101L);
        inOrder.verify(capabilityAdmissionRequestFactory).createAssignedAnswerMutation(41L, 71L, 901L);
        inOrder.verify(capabilityAdmissionPolicy).check(admissionRequest);
        verifyNoInteractions(utcClock, attemptStatusRecalculationService, activeAttemptAnswerMutationService, criticalCommandAuditSupport);
    }

    @Test
    void assignedSaveAnswerRejectsAfterDeadlineBeforeMutationEvenIfAttemptCouldLookActive() {
        ActiveAttemptAnswerMutationRequest request = request();
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(41L);
        when(foundationStateReadService.findAssignedAnswerMutationAdmissionFoundationState(41L, 101L))
            .thenReturn(new AssignedAnswerMutationAdmissionFoundationStateReadService.AssignedAnswerMutationAdmissionFoundationState(
                101L,
                71L,
                901L
            ));
        CapabilityAdmissionRequest admissionRequest = assignedAnswerMutationRequest();
        when(capabilityAdmissionRequestFactory.createAssignedAnswerMutation(41L, 71L, 901L)).thenReturn(admissionRequest);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(attemptStatusRecalculationService.recalculateAttemptStatus(101L, FIXED_INSTANT))
            .thenReturn(TestAttemptStatus.EXPIRED);

        assertThatThrownBy(() -> service.saveOrReplaceAssignedAnswer(101L, 501L, request))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("after deadline");

        InOrder inOrder = inOrder(
            interactiveActorResolver,
            capabilityAdmissionRequestFactory,
            capabilityAdmissionPolicy,
            foundationStateReadService,
            utcClock,
            attemptStatusRecalculationService
        );
        inOrder.verify(interactiveActorResolver).resolveActorUserId();
        inOrder.verify(foundationStateReadService).findAssignedAnswerMutationAdmissionFoundationState(41L, 101L);
        inOrder.verify(capabilityAdmissionRequestFactory).createAssignedAnswerMutation(41L, 71L, 901L);
        inOrder.verify(capabilityAdmissionPolicy).check(admissionRequest);
        inOrder.verify(utcClock).now();
        inOrder.verify(attemptStatusRecalculationService).recalculateAttemptStatus(101L, FIXED_INSTANT);
        verifyNoInteractions(activeAttemptAnswerMutationService, criticalCommandAuditSupport);
    }

    @Test
    void assignedClearAnswerBeforeDeadlineStillReachesMutationCore() {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(41L);
        when(foundationStateReadService.findAssignedAnswerMutationAdmissionFoundationState(41L, 101L))
            .thenReturn(new AssignedAnswerMutationAdmissionFoundationStateReadService.AssignedAnswerMutationAdmissionFoundationState(
                101L,
                71L,
                901L
            ));
        CapabilityAdmissionRequest admissionRequest = assignedAnswerMutationRequest();
        when(capabilityAdmissionRequestFactory.createAssignedAnswerMutation(41L, 71L, 901L)).thenReturn(admissionRequest);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(attemptStatusRecalculationService.recalculateAttemptStatus(101L, FIXED_INSTANT))
            .thenReturn(TestAttemptStatus.STARTED);
        when(activeAttemptAnswerMutationService.clearAnswer(41L, 101L, 501L, FIXED_INSTANT))
            .thenReturn(updatedAttempt());
        AuditContext auditContext = new AuditContext("{\"operationCode\":\"TESTING_ASSIGNED_ANSWER_MUTATION\"}");
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.Mockito.eq("Testing"),
            org.mockito.Mockito.eq(CapabilityOperationCodes.TESTING_ASSIGNED_ANSWER_MUTATION),
            org.mockito.ArgumentMatchers.anyMap()
        )).thenReturn(auditContext);

        service.clearAssignedAnswer(101L, 501L);

        InOrder inOrder = inOrder(
            interactiveActorResolver,
            capabilityAdmissionRequestFactory,
            capabilityAdmissionPolicy,
            foundationStateReadService,
            utcClock,
            attemptStatusRecalculationService,
            activeAttemptAnswerMutationService,
            criticalCommandAuditSupport
        );
        inOrder.verify(interactiveActorResolver).resolveActorUserId();
        inOrder.verify(foundationStateReadService).findAssignedAnswerMutationAdmissionFoundationState(41L, 101L);
        inOrder.verify(capabilityAdmissionRequestFactory).createAssignedAnswerMutation(41L, 71L, 901L);
        inOrder.verify(capabilityAdmissionPolicy).check(admissionRequest);
        inOrder.verify(utcClock).now();
        inOrder.verify(attemptStatusRecalculationService).recalculateAttemptStatus(101L, FIXED_INSTANT);
        inOrder.verify(activeAttemptAnswerMutationService).clearAnswer(41L, 101L, 501L, FIXED_INSTANT);
        inOrder.verify(criticalCommandAuditSupport).recordAudit(
            org.mockito.Mockito.eq(41L),
            org.mockito.ArgumentMatchers.argThat(eventType -> "TESTING_ASSIGNED_ANSWER_MUTATED".equals(eventType.value())),
            org.mockito.Mockito.eq("test_attempt"),
            org.mockito.Mockito.eq(101L),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.argThat(payload -> payload instanceof java.util.Map<?, ?> payloadMap
                && "clear".equals(payloadMap.get("mutationAction"))
                && Integer.valueOf(0).equals(payloadMap.get("answerItemCount"))),
            org.mockito.Mockito.eq(auditContext)
        );
    }

    @Test
    void assignedClearAnswerRejectsAfterDeadlineBeforeMutationCore() {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(41L);
        when(foundationStateReadService.findAssignedAnswerMutationAdmissionFoundationState(41L, 101L))
            .thenReturn(new AssignedAnswerMutationAdmissionFoundationStateReadService.AssignedAnswerMutationAdmissionFoundationState(
                101L,
                71L,
                901L
            ));
        CapabilityAdmissionRequest admissionRequest = assignedAnswerMutationRequest();
        when(capabilityAdmissionRequestFactory.createAssignedAnswerMutation(41L, 71L, 901L)).thenReturn(admissionRequest);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(attemptStatusRecalculationService.recalculateAttemptStatus(101L, FIXED_INSTANT))
            .thenReturn(TestAttemptStatus.EXPIRED);

        assertThatThrownBy(() -> service.clearAssignedAnswer(101L, 501L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("after deadline");

        verifyNoInteractions(activeAttemptAnswerMutationService, criticalCommandAuditSupport);
    }

    @Test
    void assignedMissingFoundationDoesNotWriteSuccessAudit() {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(41L);
        when(foundationStateReadService.findAssignedAnswerMutationAdmissionFoundationState(41L, 101L))
            .thenReturn(null);

        assertThatThrownBy(() -> service.clearAssignedAnswer(101L, 501L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("foundation not found");

        verifyNoInteractions(capabilityAdmissionPolicy, capabilityAdmissionRequestFactory, attemptStatusRecalculationService);
        verifyNoInteractions(activeAttemptAnswerMutationService, criticalCommandAuditSupport);
    }

    @Test
    void assignedOwnerRejectPathDoesNotWriteSuccessAudit() {
        ActiveAttemptAnswerMutationRequest request = request();
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(41L);
        when(foundationStateReadService.findAssignedAnswerMutationAdmissionFoundationState(41L, 101L))
            .thenReturn(new AssignedAnswerMutationAdmissionFoundationStateReadService.AssignedAnswerMutationAdmissionFoundationState(
                101L,
                71L,
                901L
            ));
        CapabilityAdmissionRequest admissionRequest = assignedAnswerMutationRequest();
        when(capabilityAdmissionRequestFactory.createAssignedAnswerMutation(41L, 71L, 901L)).thenReturn(admissionRequest);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(attemptStatusRecalculationService.recalculateAttemptStatus(101L, FIXED_INSTANT))
            .thenReturn(TestAttemptStatus.IN_PROGRESS);
        when(activeAttemptAnswerMutationService.saveOrReplaceAnswer(
            org.mockito.Mockito.eq(41L),
            org.mockito.Mockito.eq(101L),
            org.mockito.Mockito.eq(501L),
            org.mockito.ArgumentMatchers.anyList(),
            org.mockito.Mockito.eq(FIXED_INSTANT)
        )).thenThrow(new ConflictException("Answer mutation is allowed only for active attempts"));

        assertThatThrownBy(() -> service.saveOrReplaceAssignedAnswer(101L, 501L, request))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("active attempts");

        verify(criticalCommandAuditSupport, never()).buildAuditContext(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyMap()
        );
        verify(criticalCommandAuditSupport, never()).recordAudit(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void assignedAuditFailureAbortsCommandAfterOwnerMutation() {
        ActiveAttemptAnswerMutationRequest request = request();
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(41L);
        when(foundationStateReadService.findAssignedAnswerMutationAdmissionFoundationState(41L, 101L))
            .thenReturn(new AssignedAnswerMutationAdmissionFoundationStateReadService.AssignedAnswerMutationAdmissionFoundationState(
                101L,
                71L,
                901L
            ));
        CapabilityAdmissionRequest admissionRequest = assignedAnswerMutationRequest();
        when(capabilityAdmissionRequestFactory.createAssignedAnswerMutation(41L, 71L, 901L)).thenReturn(admissionRequest);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(attemptStatusRecalculationService.recalculateAttemptStatus(101L, FIXED_INSTANT))
            .thenReturn(TestAttemptStatus.IN_PROGRESS);
        when(activeAttemptAnswerMutationService.saveOrReplaceAnswer(
            org.mockito.Mockito.eq(41L),
            org.mockito.Mockito.eq(101L),
            org.mockito.Mockito.eq(501L),
            org.mockito.ArgumentMatchers.anyList(),
            org.mockito.Mockito.eq(FIXED_INSTANT)
        )).thenReturn(updatedAttempt());
        AuditContext auditContext = new AuditContext("{\"operationCode\":\"TESTING_ASSIGNED_ANSWER_MUTATION\"}");
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.Mockito.eq("Testing"),
            org.mockito.Mockito.eq(CapabilityOperationCodes.TESTING_ASSIGNED_ANSWER_MUTATION),
            org.mockito.ArgumentMatchers.anyMap()
        )).thenReturn(auditContext);
        org.mockito.Mockito.doThrow(new ConflictException("audit persistence failed"))
            .when(criticalCommandAuditSupport)
            .recordAudit(
                org.mockito.Mockito.eq(41L),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.Mockito.eq(101L),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.Mockito.eq(auditContext)
            );

        assertThatThrownBy(() -> service.saveOrReplaceAssignedAnswer(101L, 501L, request))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("audit persistence failed");

        verify(activeAttemptAnswerMutationService).saveOrReplaceAnswer(
            org.mockito.Mockito.eq(41L),
            org.mockito.Mockito.eq(101L),
            org.mockito.Mockito.eq(501L),
            org.mockito.ArgumentMatchers.anyList(),
            org.mockito.Mockito.eq(FIXED_INSTANT)
        );
        verify(criticalCommandAuditSupport).recordAudit(
            org.mockito.Mockito.eq(41L),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.Mockito.eq(101L),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.Mockito.eq(auditContext)
        );
    }

    private CapabilityAdmissionRequest assignedAnswerMutationRequest() {
        return new CapabilityAdmissionRequest(
            41L,
            CapabilityOperationCodes.TESTING_ASSIGNED_ANSWER_MUTATION,
            CapabilityTargetEntityType.ASSIGNMENT_TEST,
            901L,
            new CapabilityAdmissionPayload.AssignedExecution(71L),
            FIXED_INSTANT
        );
    }

    private ActiveAttemptAnswerMutationRequest request() {
        return new ActiveAttemptAnswerMutationRequest(
            List.of(new ActiveAttemptAnswerMutationRequest.ActiveAttemptAnswerItemRequest(7001L, null, null, null))
        );
    }

    private TestAttempt updatedAttempt() {
        return new TestAttempt(
            101L,
            41L,
            301L,
            901L,
            AttemptMode.ASSIGNED,
            TestAttemptStatus.IN_PROGRESS,
            FIXED_INSTANT.minusSeconds(300),
            null,
            null,
            null,
            FIXED_INSTANT,
            FIXED_INSTANT.minusSeconds(300),
            FIXED_INSTANT
        );
    }
}
