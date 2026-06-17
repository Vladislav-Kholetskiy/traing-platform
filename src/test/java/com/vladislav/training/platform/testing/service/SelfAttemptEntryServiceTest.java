package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.audit.domain.AuditContext;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.testing.admission.SelfAttemptEntryFoundationStateReadService;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import com.vladislav.training.platform.testing.repository.TestAttemptRepository;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение сервиса {@code SelfAttemptEntry}.
 * Сценарии сосредоточены на прикладной логике.
 */
@ExtendWith(MockitoExtension.class)
class SelfAttemptEntryServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-19T16:10:00Z");

    @Mock
    private SelfAttemptEntryFoundationStateReadService foundationStateReadService;
    @Mock
    private SelfAttemptAdmissionSupport selfAttemptAdmissionSupport;
    @Mock
    private TestAttemptRepository testAttemptRepository;
    @Mock
    private CriticalCommandAuditSupport criticalCommandAuditSupport;
    @Mock
    private UtcClock utcClock;

    private SelfAttemptEntryService service;

    @BeforeEach
    void setUp() {
        service = new SelfAttemptEntryService(
            foundationStateReadService,
            selfAttemptAdmissionSupport,
            testAttemptRepository,
            criticalCommandAuditSupport,
            utcClock
        );
    }

    @Test
    void createsNewActiveSelfAttemptWhenNoCurrentAttemptExistsAndAdmissionAllowsStart() {
        var foundation = new SelfAttemptEntryFoundationStateReadService.SelfAttemptEntryFoundationState(501L);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(101L);
        when(foundationStateReadService.findSelfAttemptEntryFoundationState(101L, 501L)).thenReturn(foundation);
        when(testAttemptRepository.findAndLockActiveSelfAttempt(101L, 501L)).thenReturn(null);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(testAttemptRepository.saveTestAttempt(any(TestAttempt.class))).thenAnswer(invocation -> {
            TestAttempt saved = invocation.getArgument(0, TestAttempt.class);
            return new TestAttempt(
                9002L,
                saved.userId(),
                saved.testId(),
                saved.assignmentTestId(),
                saved.attemptMode(),
                saved.status(),
                saved.startedAt(),
                saved.completedAt(),
                saved.expiredAt(),
                saved.abandonedAt(),
                saved.lastActivityAt(),
                saved.createdAt(),
                saved.updatedAt()
            );
        });
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq(com.vladislav.training.platform.application.policy.CapabilityOperationCode.TESTING_SELF_ATTEMPT_START),
            any()
        )).thenReturn(new AuditContext("{\"operationCode\":\"TESTING_SELF_ATTEMPT_START\"}"));

        TestAttempt created = service.startOrContinueSelfAttempt(501L);

        assertThat(created.id()).isEqualTo(9002L);
        assertThat(created.userId()).isEqualTo(101L);
        assertThat(created.testId()).isEqualTo(501L);
        assertThat(created.assignmentTestId()).isNull();
        assertThat(created.attemptMode()).isEqualTo(AttemptMode.SELF);
        assertThat(created.status()).isEqualTo(TestAttemptStatus.STARTED);
        assertThat(created.startedAt()).isEqualTo(FIXED_INSTANT);
        assertThat(created.lastActivityAt()).isEqualTo(FIXED_INSTANT);
        assertThat(created.createdAt()).isEqualTo(FIXED_INSTANT);
        assertThat(created.updatedAt()).isEqualTo(FIXED_INSTANT);

        InOrder inOrder = inOrder(
            criticalCommandAuditSupport,
            foundationStateReadService,
            testAttemptRepository,
            selfAttemptAdmissionSupport,
            utcClock
        );
        inOrder.verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        inOrder.verify(foundationStateReadService).findSelfAttemptEntryFoundationState(101L, 501L);
        inOrder.verify(testAttemptRepository).findAndLockActiveSelfAttempt(101L, 501L);
        inOrder.verify(selfAttemptAdmissionSupport).checkSelfAttemptStart(501L);
        inOrder.verify(utcClock).now();
        verify(testAttemptRepository).saveTestAttempt(any(TestAttempt.class));
        verify(criticalCommandAuditSupport).buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq(com.vladislav.training.platform.application.policy.CapabilityOperationCode.TESTING_SELF_ATTEMPT_START),
            any()
        );
        verify(criticalCommandAuditSupport).recordAudit(
            org.mockito.ArgumentMatchers.eq(101L),
            any(),
            org.mockito.ArgumentMatchers.eq("test_attempt"),
            org.mockito.ArgumentMatchers.eq(9002L),
            org.mockito.ArgumentMatchers.isNull(),
            any(),
            any(AuditContext.class)
        );
        verifyNoMoreInteractions(
            criticalCommandAuditSupport,
            foundationStateReadService,
            testAttemptRepository,
            selfAttemptAdmissionSupport,
            utcClock
        );
    }

    @Test
    void selfEntryAuditPayloadUsesCreatedAttemptFactsWithoutSyntheticFallback() {
        var foundation = new SelfAttemptEntryFoundationStateReadService.SelfAttemptEntryFoundationState(501L);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(101L);
        when(foundationStateReadService.findSelfAttemptEntryFoundationState(101L, 501L)).thenReturn(foundation);
        when(testAttemptRepository.findAndLockActiveSelfAttempt(101L, 501L)).thenReturn(null);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(testAttemptRepository.saveTestAttempt(any(TestAttempt.class))).thenAnswer(invocation -> {
            TestAttempt saved = invocation.getArgument(0, TestAttempt.class);
            return new TestAttempt(
                9002L,
                saved.userId(),
                saved.testId(),
                saved.assignmentTestId(),
                saved.attemptMode(),
                saved.status(),
                saved.startedAt(),
                saved.completedAt(),
                saved.expiredAt(),
                saved.abandonedAt(),
                saved.lastActivityAt(),
                saved.createdAt(),
                saved.updatedAt()
            );
        });
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq(com.vladislav.training.platform.application.policy.CapabilityOperationCode.TESTING_SELF_ATTEMPT_START),
            any()
        )).thenReturn(new AuditContext("{\"operationCode\":\"TESTING_SELF_ATTEMPT_START\"}"));

        service.startOrContinueSelfAttempt(501L);

        ArgumentCaptor<Object> payloadAfterCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Map> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(criticalCommandAuditSupport).buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq(com.vladislav.training.platform.application.policy.CapabilityOperationCode.TESTING_SELF_ATTEMPT_START),
            detailsCaptor.capture()
        );
        verify(criticalCommandAuditSupport).recordAudit(
            org.mockito.ArgumentMatchers.eq(101L),
            any(),
            org.mockito.ArgumentMatchers.eq("test_attempt"),
            org.mockito.ArgumentMatchers.eq(9002L),
            org.mockito.ArgumentMatchers.isNull(),
            payloadAfterCaptor.capture(),
            any(AuditContext.class)
        );

        assertThat(detailsCaptor.getValue())
            .containsEntry("commandType", "self_attempt_start")
            .containsEntry("entryMode", "create")
            .containsEntry("testId", 501L);
        assertThat(detailsCaptor.getValue()).doesNotContainKeys("assignmentId", "assignmentTestId", "actorSource");

        assertThat(payloadAfterCaptor.getValue()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> payloadAfter = (Map<String, Object>) payloadAfterCaptor.getValue();
        assertThat(payloadAfter).containsOnlyKeys("attempt");
        assertThat(payloadAfter.get("attempt")).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> attemptPayload = (Map<String, Object>) payloadAfter.get("attempt");
        assertThat(attemptPayload)
            .containsEntry("id", 9002L)
            .containsEntry("userId", 101L)
            .containsEntry("testId", 501L)
            .containsEntry("assignmentTestId", null)
            .containsEntry("attemptMode", AttemptMode.SELF)
            .containsEntry("status", TestAttemptStatus.STARTED)
            .containsEntry("startedAt", FIXED_INSTANT)
            .containsEntry("lastActivityAt", FIXED_INSTANT)
            .containsEntry("createdAt", FIXED_INSTANT)
            .containsEntry("updatedAt", FIXED_INSTANT);
        assertThat(attemptPayload).doesNotContainKeys("assignmentId", "completedAt", "expiredAt", "abandonedAt");
    }

    @Test
    void returnsExistingActiveSelfAttemptAsContinueWithoutCreatingDuplicate() {
        var foundation = new SelfAttemptEntryFoundationStateReadService.SelfAttemptEntryFoundationState(501L);
        TestAttempt existingAttempt = selfAttempt(8003L, 101L, 501L, TestAttemptStatus.IN_PROGRESS);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(101L);
        when(foundationStateReadService.findSelfAttemptEntryFoundationState(101L, 501L)).thenReturn(foundation);
        when(testAttemptRepository.findAndLockActiveSelfAttempt(101L, 501L)).thenReturn(existingAttempt);

        TestAttempt returned = service.startOrContinueSelfAttempt(501L);

        assertThat(returned).isSameAs(existingAttempt);
        verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        verify(selfAttemptAdmissionSupport).checkSelfAttemptContinue(501L);
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(com.vladislav.training.platform.application.policy.CapabilityOperationCode.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
        verify(utcClock, never()).now();
    }

    @Test
    void existingActiveSelfAttemptMustStillRunCommandAdmissionBeforeBeingReturnedAsContinuation() {
        var foundation = new SelfAttemptEntryFoundationStateReadService.SelfAttemptEntryFoundationState(501L);
        TestAttempt existingAttempt = selfAttempt(8003L, 101L, 501L, TestAttemptStatus.IN_PROGRESS);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(101L);
        when(foundationStateReadService.findSelfAttemptEntryFoundationState(101L, 501L)).thenReturn(foundation);
        when(testAttemptRepository.findAndLockActiveSelfAttempt(101L, 501L)).thenReturn(existingAttempt);

        TestAttempt returned = service.startOrContinueSelfAttempt(501L);

        assertThat(returned).isSameAs(existingAttempt);
        InOrder inOrder = inOrder(
            criticalCommandAuditSupport,
            foundationStateReadService,
            testAttemptRepository,
            selfAttemptAdmissionSupport
        );
        inOrder.verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        inOrder.verify(foundationStateReadService).findSelfAttemptEntryFoundationState(101L, 501L);
        inOrder.verify(testAttemptRepository).findAndLockActiveSelfAttempt(101L, 501L);
        inOrder.verify(selfAttemptAdmissionSupport).checkSelfAttemptContinue(501L);
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(com.vladislav.training.platform.application.policy.CapabilityOperationCode.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
        verify(utcClock, never()).now();
    }

    @Test
    void failsClosedWhenSelfFoundationIsUnavailable() {
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(101L);
        when(foundationStateReadService.findSelfAttemptEntryFoundationState(101L, 501L)).thenReturn(null);

        assertThatThrownBy(() -> service.startOrContinueSelfAttempt(501L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("testId=501");

        verify(testAttemptRepository, never()).findAndLockActiveSelfAttempt(any(), any());
        verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        verifyNoInteractions(selfAttemptAdmissionSupport, utcClock);
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(com.vladislav.training.platform.application.policy.CapabilityOperationCode.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void failsClosedWhenLockedActiveSelfAttemptDoesNotMatchSelfAnchor() {
        var foundation = new SelfAttemptEntryFoundationStateReadService.SelfAttemptEntryFoundationState(501L);
        TestAttempt inconsistentAttempt = new TestAttempt(
            8004L,
            101L,
            501L,
            701L,
            AttemptMode.SELF,
            TestAttemptStatus.STARTED,
            FIXED_INSTANT,
            null,
            null,
            null,
            FIXED_INSTANT,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(101L);
        when(foundationStateReadService.findSelfAttemptEntryFoundationState(101L, 501L)).thenReturn(foundation);
        when(testAttemptRepository.findAndLockActiveSelfAttempt(101L, 501L)).thenReturn(inconsistentAttempt);

        assertThatThrownBy(() -> service.startOrContinueSelfAttempt(501L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("testId=501");

        verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        verify(selfAttemptAdmissionSupport, never()).checkSelfAttemptStart(any());
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(com.vladislav.training.platform.application.policy.CapabilityOperationCode.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
        verify(utcClock, never()).now();
    }

    @Test
    void failsClosedWhenLockedActiveSelfAttemptHasWrongUserId() {
        var foundation = new SelfAttemptEntryFoundationStateReadService.SelfAttemptEntryFoundationState(501L);
        TestAttempt inconsistentAttempt = selfAttempt(8005L, 999L, 501L, TestAttemptStatus.STARTED);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(101L);
        when(foundationStateReadService.findSelfAttemptEntryFoundationState(101L, 501L)).thenReturn(foundation);
        when(testAttemptRepository.findAndLockActiveSelfAttempt(101L, 501L)).thenReturn(inconsistentAttempt);

        assertThatThrownBy(() -> service.startOrContinueSelfAttempt(501L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("testId=501");

        verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        verifyNoInteractions(selfAttemptAdmissionSupport, utcClock);
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(com.vladislav.training.platform.application.policy.CapabilityOperationCode.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void failsClosedWhenLockedActiveSelfAttemptHasWrongTestId() {
        var foundation = new SelfAttemptEntryFoundationStateReadService.SelfAttemptEntryFoundationState(501L);
        TestAttempt inconsistentAttempt = selfAttempt(8006L, 101L, 999L, TestAttemptStatus.STARTED);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(101L);
        when(foundationStateReadService.findSelfAttemptEntryFoundationState(101L, 501L)).thenReturn(foundation);
        when(testAttemptRepository.findAndLockActiveSelfAttempt(101L, 501L)).thenReturn(inconsistentAttempt);

        assertThatThrownBy(() -> service.startOrContinueSelfAttempt(501L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("testId=501");

        verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        verifyNoInteractions(selfAttemptAdmissionSupport, utcClock);
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(com.vladislav.training.platform.application.policy.CapabilityOperationCode.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void failsClosedWhenLockedActiveSelfAttemptHasWrongMode() {
        var foundation = new SelfAttemptEntryFoundationStateReadService.SelfAttemptEntryFoundationState(501L);
        TestAttempt inconsistentAttempt = new TestAttempt(
            8007L,
            101L,
            501L,
            null,
            AttemptMode.ASSIGNED,
            TestAttemptStatus.STARTED,
            FIXED_INSTANT,
            null,
            null,
            null,
            FIXED_INSTANT,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(101L);
        when(foundationStateReadService.findSelfAttemptEntryFoundationState(101L, 501L)).thenReturn(foundation);
        when(testAttemptRepository.findAndLockActiveSelfAttempt(101L, 501L)).thenReturn(inconsistentAttempt);

        assertThatThrownBy(() -> service.startOrContinueSelfAttempt(501L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("testId=501");

        verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        verifyNoInteractions(selfAttemptAdmissionSupport, utcClock);
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(com.vladislav.training.platform.application.policy.CapabilityOperationCode.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void failsClosedWhenSelfAdmissionFailsBeforeCreate() {
        var foundation = new SelfAttemptEntryFoundationStateReadService.SelfAttemptEntryFoundationState(501L);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(101L);
        when(foundationStateReadService.findSelfAttemptEntryFoundationState(101L, 501L)).thenReturn(foundation);
        when(testAttemptRepository.findAndLockActiveSelfAttempt(101L, 501L)).thenReturn(null);
        org.mockito.Mockito.doThrow(new com.vladislav.training.platform.common.exception.PolicyViolationException("DENY"))
            .when(selfAttemptAdmissionSupport).checkSelfAttemptStart(501L);

        assertThatThrownBy(() -> service.startOrContinueSelfAttempt(501L))
            .isInstanceOf(com.vladislav.training.platform.common.exception.PolicyViolationException.class)
            .hasMessageContaining("DENY");

        verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verifyNoInteractions(utcClock);
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(com.vladislav.training.platform.application.policy.CapabilityOperationCode.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void doesNotWriteSuccessAuditWhenSelfAttemptCreateOwnerWriteFails() {
        var foundation = new SelfAttemptEntryFoundationStateReadService.SelfAttemptEntryFoundationState(501L);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(101L);
        when(foundationStateReadService.findSelfAttemptEntryFoundationState(101L, 501L)).thenReturn(foundation);
        when(testAttemptRepository.findAndLockActiveSelfAttempt(101L, 501L)).thenReturn(null);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(testAttemptRepository.saveTestAttempt(any(TestAttempt.class)))
            .thenThrow(new com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException(
                "Failed to persist test_attempt"
            ));

        assertThatThrownBy(() -> service.startOrContinueSelfAttempt(501L))
            .isInstanceOf(com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException.class)
            .hasMessageContaining("Failed to persist test_attempt");

        verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        verify(testAttemptRepository).findAndLockActiveSelfAttempt(101L, 501L);
        verify(selfAttemptAdmissionSupport).checkSelfAttemptStart(501L);
        verify(utcClock).now();
        verify(testAttemptRepository).saveTestAttempt(any(TestAttempt.class));
        verify(criticalCommandAuditSupport, never()).buildAuditContext(
            any(),
            any(com.vladislav.training.platform.application.policy.CapabilityOperationCode.class),
            any()
        );
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void deniedSelfContinuationAdmissionDoesNotReturnExistingAttemptOrWriteCreateAudit() {
        var foundation = new SelfAttemptEntryFoundationStateReadService.SelfAttemptEntryFoundationState(501L);
        TestAttempt existingAttempt = selfAttempt(8003L, 101L, 501L, TestAttemptStatus.IN_PROGRESS);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(101L);
        when(foundationStateReadService.findSelfAttemptEntryFoundationState(101L, 501L)).thenReturn(foundation);
        when(testAttemptRepository.findAndLockActiveSelfAttempt(101L, 501L)).thenReturn(existingAttempt);
        org.mockito.Mockito.doThrow(new com.vladislav.training.platform.common.exception.PolicyViolationException("CONTINUE_DENY"))
            .when(selfAttemptAdmissionSupport).checkSelfAttemptContinue(501L);

        assertThatThrownBy(() -> service.startOrContinueSelfAttempt(501L))
            .isInstanceOf(com.vladislav.training.platform.common.exception.PolicyViolationException.class)
            .hasMessageContaining("CONTINUE_DENY");

        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(com.vladislav.training.platform.application.policy.CapabilityOperationCode.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
        verify(utcClock, never()).now();
    }

    private TestAttempt selfAttempt(Long id, Long userId, Long testId, TestAttemptStatus status) {
        return new TestAttempt(
            id,
            userId,
            testId,
            null,
            AttemptMode.SELF,
            status,
            FIXED_INSTANT,
            null,
            null,
            null,
            FIXED_INSTANT,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }
}
