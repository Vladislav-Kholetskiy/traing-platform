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
import com.vladislav.training.platform.application.policy.CapabilityOperationCodes;
import com.vladislav.training.platform.application.policy.CapabilityTargetEntityType;
import com.vladislav.training.platform.audit.domain.AuditContext;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.testing.admission.SelfAnswerMutationAdmissionFoundationStateReadService;
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
 * Проверяет поведение сервиса {@code SelfAttemptAnswerMutationEntry}.
 * Сценарии сосредоточены на прикладной логике.
 */
@ExtendWith(MockitoExtension.class)
class SelfAttemptAnswerMutationEntryServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-21T09:00:00Z");

    @Mock
    private InteractiveActorResolver interactiveActorResolver;
    @Mock
    private CapabilityAdmissionPolicy capabilityAdmissionPolicy;
    @Mock
    private CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;
    @Mock
    private SelfAnswerMutationAdmissionFoundationStateReadService foundationStateReadService;
    @Mock
    private ActiveAttemptAnswerMutationService activeAttemptAnswerMutationService;
    @Mock
    private CriticalCommandAuditSupport criticalCommandAuditSupport;
    @Mock
    private UtcClock utcClock;

    private SelfAttemptAnswerMutationEntryService service;

    @BeforeEach
    void setUp() {
        service = new SelfAttemptAnswerMutationEntryService(
            interactiveActorResolver,
            capabilityAdmissionPolicy,
            capabilityAdmissionRequestFactory,
            foundationStateReadService,
            activeAttemptAnswerMutationService,
            criticalCommandAuditSupport,
            utcClock
        );
    }

    @Test
    void selfSaveAnswerResolvesActorThenChecksAdmissionBeforeMutationAndWritesAuditAfterOwnerMutation() {
        ActiveAttemptAnswerMutationRequest request = request();
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(41L);
        when(foundationStateReadService.findSelfAnswerMutationAdmissionFoundationState(41L, 101L))
            .thenReturn(new SelfAnswerMutationAdmissionFoundationStateReadService.SelfAnswerMutationAdmissionFoundationState(101L, 301L));
        CapabilityAdmissionRequest admissionRequest = selfAnswerMutationRequest();
        when(capabilityAdmissionRequestFactory.createSelfAnswerMutation(41L, 301L)).thenReturn(admissionRequest);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(activeAttemptAnswerMutationService.saveOrReplaceAnswer(
            org.mockito.Mockito.eq(41L),
            org.mockito.Mockito.eq(101L),
            org.mockito.Mockito.eq(501L),
            org.mockito.ArgumentMatchers.anyList(),
            org.mockito.Mockito.eq(FIXED_INSTANT)
        )).thenReturn(updatedAttempt());
        AuditContext auditContext = new AuditContext("{\"operationCode\":\"TESTING_SELF_ANSWER_MUTATION\"}");
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.Mockito.eq("Testing"),
            org.mockito.Mockito.eq(CapabilityOperationCodes.TESTING_SELF_ANSWER_MUTATION),
            org.mockito.ArgumentMatchers.anyMap()
        )).thenReturn(auditContext);

        service.saveOrReplaceSelfAnswer(101L, 501L, request);

        InOrder inOrder = inOrder(
            interactiveActorResolver,
            capabilityAdmissionRequestFactory,
            capabilityAdmissionPolicy,
            foundationStateReadService,
            utcClock,
            activeAttemptAnswerMutationService,
            criticalCommandAuditSupport
        );
        inOrder.verify(interactiveActorResolver).resolveActorUserId();
        inOrder.verify(foundationStateReadService).findSelfAnswerMutationAdmissionFoundationState(41L, 101L);
        inOrder.verify(capabilityAdmissionRequestFactory).createSelfAnswerMutation(41L, 301L);
        inOrder.verify(capabilityAdmissionPolicy).check(admissionRequest);
        inOrder.verify(utcClock).now();
        inOrder.verify(activeAttemptAnswerMutationService).saveOrReplaceAnswer(
            org.mockito.Mockito.eq(41L),
            org.mockito.Mockito.eq(101L),
            org.mockito.Mockito.eq(501L),
            org.mockito.ArgumentMatchers.anyList(),
            org.mockito.Mockito.eq(FIXED_INSTANT)
        );
        inOrder.verify(criticalCommandAuditSupport).buildAuditContext(
            org.mockito.Mockito.eq("Testing"),
            org.mockito.Mockito.eq(CapabilityOperationCodes.TESTING_SELF_ANSWER_MUTATION),
            org.mockito.ArgumentMatchers.argThat(details ->
                "self_answer_mutation".equals(details.get("commandType"))
                    && "save_or_replace".equals(details.get("mutationAction"))
                    && Long.valueOf(301L).equals(details.get("testId"))
                    && Long.valueOf(501L).equals(details.get("questionId"))
                    && Long.valueOf(101L).equals(details.get("attemptId"))
                    && AttemptMode.SELF == details.get("attemptMode")
                    && Integer.valueOf(1).equals(details.get("answerItemCount"))
            )
        );
        inOrder.verify(criticalCommandAuditSupport).recordAudit(
            org.mockito.Mockito.eq(41L),
            org.mockito.ArgumentMatchers.argThat(eventType -> "TESTING_SELF_ANSWER_MUTATED".equals(eventType.value())),
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
                    && payloadMap.get("attempt") instanceof java.util.Map<?, ?> attemptMap
                    && Long.valueOf(101L).equals(attemptMap.get("id"))
                    && AttemptMode.SELF == attemptMap.get("attemptMode")
                    && TestAttemptStatus.IN_PROGRESS == attemptMap.get("status");
            }),
            org.mockito.Mockito.eq(auditContext)
        );
    }

    @Test
    void selfClearAnswerRejectsFailClosedBeforeMutationWhenAdmissionFails() {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(41L);
        when(foundationStateReadService.findSelfAnswerMutationAdmissionFoundationState(41L, 101L))
            .thenReturn(new SelfAnswerMutationAdmissionFoundationStateReadService.SelfAnswerMutationAdmissionFoundationState(101L, 301L));
        CapabilityAdmissionRequest admissionRequest = selfAnswerMutationRequest();
        when(capabilityAdmissionRequestFactory.createSelfAnswerMutation(41L, 301L)).thenReturn(admissionRequest);
        org.mockito.Mockito.doThrow(new PolicyViolationException("DENIED", "Self answer mutation denied"))
            .when(capabilityAdmissionPolicy).check(admissionRequest);

        assertThatThrownBy(() -> service.clearSelfAnswer(101L, 501L))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("denied");

        InOrder inOrder = inOrder(
            interactiveActorResolver,
            foundationStateReadService,
            capabilityAdmissionRequestFactory,
            capabilityAdmissionPolicy
        );
        inOrder.verify(interactiveActorResolver).resolveActorUserId();
        inOrder.verify(foundationStateReadService).findSelfAnswerMutationAdmissionFoundationState(41L, 101L);
        inOrder.verify(capabilityAdmissionRequestFactory).createSelfAnswerMutation(41L, 301L);
        inOrder.verify(capabilityAdmissionPolicy).check(admissionRequest);
        verifyNoInteractions(utcClock, activeAttemptAnswerMutationService, criticalCommandAuditSupport);
    }

    @Test
    void selfClearAnswerSuccessWritesCriticalAuditAfterOwnerMutation() {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(41L);
        when(foundationStateReadService.findSelfAnswerMutationAdmissionFoundationState(41L, 101L))
            .thenReturn(new SelfAnswerMutationAdmissionFoundationStateReadService.SelfAnswerMutationAdmissionFoundationState(101L, 301L));
        CapabilityAdmissionRequest admissionRequest = selfAnswerMutationRequest();
        when(capabilityAdmissionRequestFactory.createSelfAnswerMutation(41L, 301L)).thenReturn(admissionRequest);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(activeAttemptAnswerMutationService.clearAnswer(41L, 101L, 501L, FIXED_INSTANT))
            .thenReturn(updatedAttempt());
        AuditContext auditContext = new AuditContext("{\"operationCode\":\"TESTING_SELF_ANSWER_MUTATION\"}");
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.Mockito.eq("Testing"),
            org.mockito.Mockito.eq(CapabilityOperationCodes.TESTING_SELF_ANSWER_MUTATION),
            org.mockito.ArgumentMatchers.anyMap()
        )).thenReturn(auditContext);

        service.clearSelfAnswer(101L, 501L);

        InOrder inOrder = inOrder(
            interactiveActorResolver,
            foundationStateReadService,
            capabilityAdmissionRequestFactory,
            capabilityAdmissionPolicy,
            utcClock,
            activeAttemptAnswerMutationService,
            criticalCommandAuditSupport
        );
        inOrder.verify(interactiveActorResolver).resolveActorUserId();
        inOrder.verify(foundationStateReadService).findSelfAnswerMutationAdmissionFoundationState(41L, 101L);
        inOrder.verify(capabilityAdmissionRequestFactory).createSelfAnswerMutation(41L, 301L);
        inOrder.verify(capabilityAdmissionPolicy).check(admissionRequest);
        inOrder.verify(utcClock).now();
        inOrder.verify(activeAttemptAnswerMutationService).clearAnswer(41L, 101L, 501L, FIXED_INSTANT);
        inOrder.verify(criticalCommandAuditSupport).recordAudit(
            org.mockito.Mockito.eq(41L),
            org.mockito.ArgumentMatchers.argThat(eventType -> "TESTING_SELF_ANSWER_MUTATED".equals(eventType.value())),
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
    void selfMissingFoundationDoesNotWriteSuccessAudit() {
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(41L);
        when(foundationStateReadService.findSelfAnswerMutationAdmissionFoundationState(41L, 101L)).thenReturn(null);

        assertThatThrownBy(() -> service.clearSelfAnswer(101L, 501L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("foundation not found");

        verifyNoInteractions(capabilityAdmissionPolicy, capabilityAdmissionRequestFactory);
        verifyNoInteractions(activeAttemptAnswerMutationService, criticalCommandAuditSupport, utcClock);
    }

    @Test
    void selfOwnerRejectPathDoesNotWriteSuccessAudit() {
        ActiveAttemptAnswerMutationRequest request = request();
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(41L);
        when(foundationStateReadService.findSelfAnswerMutationAdmissionFoundationState(41L, 101L))
            .thenReturn(new SelfAnswerMutationAdmissionFoundationStateReadService.SelfAnswerMutationAdmissionFoundationState(101L, 301L));
        CapabilityAdmissionRequest admissionRequest = selfAnswerMutationRequest();
        when(capabilityAdmissionRequestFactory.createSelfAnswerMutation(41L, 301L)).thenReturn(admissionRequest);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(activeAttemptAnswerMutationService.saveOrReplaceAnswer(
            org.mockito.Mockito.eq(41L),
            org.mockito.Mockito.eq(101L),
            org.mockito.Mockito.eq(501L),
            org.mockito.ArgumentMatchers.anyList(),
            org.mockito.Mockito.eq(FIXED_INSTANT)
        )).thenThrow(new ConflictException("Answer mutation is allowed only for active attempts"));

        assertThatThrownBy(() -> service.saveOrReplaceSelfAnswer(101L, 501L, request))
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
    void auditFailureAbortsSelfAnswerMutationCommandAfterOwnerMutation() {
        ActiveAttemptAnswerMutationRequest request = request();
        when(interactiveActorResolver.resolveActorUserId()).thenReturn(41L);
        when(foundationStateReadService.findSelfAnswerMutationAdmissionFoundationState(41L, 101L))
            .thenReturn(new SelfAnswerMutationAdmissionFoundationStateReadService.SelfAnswerMutationAdmissionFoundationState(101L, 301L));
        CapabilityAdmissionRequest admissionRequest = selfAnswerMutationRequest();
        when(capabilityAdmissionRequestFactory.createSelfAnswerMutation(41L, 301L)).thenReturn(admissionRequest);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(activeAttemptAnswerMutationService.saveOrReplaceAnswer(
            org.mockito.Mockito.eq(41L),
            org.mockito.Mockito.eq(101L),
            org.mockito.Mockito.eq(501L),
            org.mockito.ArgumentMatchers.anyList(),
            org.mockito.Mockito.eq(FIXED_INSTANT)
        )).thenReturn(updatedAttempt());
        AuditContext auditContext = new AuditContext("{\"operationCode\":\"TESTING_SELF_ANSWER_MUTATION\"}");
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.Mockito.eq("Testing"),
            org.mockito.Mockito.eq(CapabilityOperationCodes.TESTING_SELF_ANSWER_MUTATION),
            org.mockito.ArgumentMatchers.anyMap()
        )).thenReturn(auditContext);
        RuntimeException auditFailure = new RuntimeException("audit persistence failed");
        org.mockito.Mockito.doThrow(auditFailure)
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

        assertThatThrownBy(() -> service.saveOrReplaceSelfAnswer(101L, 501L, request))
            .isSameAs(auditFailure);

        InOrder inOrder = inOrder(activeAttemptAnswerMutationService, criticalCommandAuditSupport);
        inOrder.verify(activeAttemptAnswerMutationService).saveOrReplaceAnswer(
            org.mockito.Mockito.eq(41L),
            org.mockito.Mockito.eq(101L),
            org.mockito.Mockito.eq(501L),
            org.mockito.ArgumentMatchers.anyList(),
            org.mockito.Mockito.eq(FIXED_INSTANT)
        );
        inOrder.verify(criticalCommandAuditSupport).buildAuditContext(
            org.mockito.Mockito.eq("Testing"),
            org.mockito.Mockito.eq(CapabilityOperationCodes.TESTING_SELF_ANSWER_MUTATION),
            org.mockito.ArgumentMatchers.anyMap()
        );
        inOrder.verify(criticalCommandAuditSupport).recordAudit(
            org.mockito.Mockito.eq(41L),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.Mockito.eq(101L),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.Mockito.eq(auditContext)
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

    private CapabilityAdmissionRequest selfAnswerMutationRequest() {
        return new CapabilityAdmissionRequest(
            41L,
            CapabilityOperationCodes.TESTING_SELF_ANSWER_MUTATION,
            CapabilityTargetEntityType.TEST,
            301L,
            CapabilityAdmissionPayload.SelfExecution.INSTANCE,
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
            null,
            AttemptMode.SELF,
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
