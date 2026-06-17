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
import com.vladislav.training.platform.result.service.ResultRecordingService;
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
 * Проверяет поведение сервиса {@code SelfAttemptSubmitTerminal}.
 * Сценарии сосредоточены на прикладной логике.
 */
@ExtendWith(MockitoExtension.class)
class SelfAttemptSubmitTerminalServiceTest {

    private static final Instant STARTED_AT = Instant.parse("2026-04-19T19:00:00Z");
    private static final Instant LAST_ACTIVITY_AT = Instant.parse("2026-04-19T19:20:00Z");
    private static final Instant NOW = Instant.parse("2026-04-19T19:45:00Z");
    private static final Long ACTOR_USER_ID = 101L;

    @Mock
    private CriticalCommandAuditSupport criticalCommandAuditSupport;
    @Mock
    private TestAttemptRepository testAttemptRepository;
    @Mock
    private UtcClock utcClock;
    @Mock
    private ResultRecordingService resultRecordingService;

    private SelfAttemptSubmitTerminalService service;

    @BeforeEach
    void setUp() {
        service = new SelfAttemptSubmitTerminalService(criticalCommandAuditSupport, testAttemptRepository, utcClock);
    }

    @Test
    void terminalizesSelfActiveAttemptIntoCompletedState() {
        TestAttempt activeAttempt = selfAttempt(9201L, 101L, 501L, TestAttemptStatus.IN_PROGRESS);
        when(testAttemptRepository.findAndLockTestAttemptByIdAndUserId(9201L, ACTOR_USER_ID)).thenReturn(activeAttempt);
        when(utcClock.now()).thenReturn(NOW);
        when(testAttemptRepository.saveTestAttempt(any(TestAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq("TESTING_SELF_ATTEMPT_SUBMIT_TERMINAL"),
            any()
        )).thenReturn(new AuditContext("{\"operationCode\":\"TESTING_SELF_ATTEMPT_SUBMIT_TERMINAL\"}"));

        AttemptTerminalizationOutcome completedOutcome = service.submitSelfAttempt(ACTOR_USER_ID, 9201L);
        TestAttempt completedAttempt = completedOutcome.terminalizedAttempt();

        assertThat(completedOutcome.attemptId()).isEqualTo(9201L);
        assertThat(completedOutcome.actorUserId()).isEqualTo(ACTOR_USER_ID);
        assertThat(completedOutcome.attemptMode()).isEqualTo(AttemptMode.SELF);
        assertThat(completedOutcome.terminalStatus()).isEqualTo(TestAttemptStatus.COMPLETED);
        assertThat(completedOutcome.terminalizedAt()).isEqualTo(NOW);
        assertThat(completedOutcome.reason()).isEqualTo(AttemptTerminalizationReason.NORMAL_SUBMIT);
        assertThat(completedOutcome.resultRecordable()).isTrue();
        assertThat(completedOutcome.auditEventType())
            .isEqualTo(AttemptTerminalCriticalAuditCatalog.SELF_ATTEMPT_SUBMITTED.auditEventType());
        assertThat(completedAttempt.id()).isEqualTo(9201L);
        assertThat(completedAttempt.userId()).isEqualTo(101L);
        assertThat(completedAttempt.testId()).isEqualTo(501L);
        assertThat(completedAttempt.assignmentTestId()).isNull();
        assertThat(completedAttempt.attemptMode()).isEqualTo(AttemptMode.SELF);
        assertThat(completedAttempt.status()).isEqualTo(TestAttemptStatus.COMPLETED);
        assertThat(completedAttempt.startedAt()).isEqualTo(STARTED_AT);
        assertThat(completedAttempt.completedAt()).isEqualTo(NOW);
        assertThat(completedAttempt.expiredAt()).isNull();
        assertThat(completedAttempt.abandonedAt()).isNull();
        assertThat(completedAttempt.lastActivityAt()).isEqualTo(NOW);
        assertThat(completedAttempt.createdAt()).isEqualTo(STARTED_AT);
        assertThat(completedAttempt.updatedAt()).isEqualTo(NOW);

        InOrder inOrder = inOrder(criticalCommandAuditSupport, testAttemptRepository, utcClock);
        inOrder.verify(testAttemptRepository).findAndLockTestAttemptByIdAndUserId(9201L, ACTOR_USER_ID);
        inOrder.verify(utcClock).now();
        inOrder.verify(testAttemptRepository).saveTestAttempt(any(TestAttempt.class));
        verify(criticalCommandAuditSupport).buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq("TESTING_SELF_ATTEMPT_SUBMIT_TERMINAL"),
            any()
        );
        verify(criticalCommandAuditSupport).recordAudit(
            org.mockito.ArgumentMatchers.eq(ACTOR_USER_ID),
            any(),
            org.mockito.ArgumentMatchers.eq("test_attempt"),
            org.mockito.ArgumentMatchers.eq(9201L),
            any(),
            any(),
            any(AuditContext.class)
        );
        verifyNoMoreInteractions(criticalCommandAuditSupport, testAttemptRepository, utcClock);
        verifyNoInteractions(resultRecordingService);
    }

    @Test
    void writesSelfSubmitAuditOnlyAfterSuccessfulOwnerTerminalizationWrite() {
        TestAttempt activeAttempt = selfAttempt(9207L, 101L, 501L, TestAttemptStatus.IN_PROGRESS);
        when(testAttemptRepository.findAndLockTestAttemptByIdAndUserId(9207L, ACTOR_USER_ID)).thenReturn(activeAttempt);
        when(utcClock.now()).thenReturn(NOW);
        when(testAttemptRepository.saveTestAttempt(any(TestAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq("TESTING_SELF_ATTEMPT_SUBMIT_TERMINAL"),
            any()
        )).thenReturn(new AuditContext("{\"operationCode\":\"TESTING_SELF_ATTEMPT_SUBMIT_TERMINAL\"}"));

        service.submitSelfAttempt(ACTOR_USER_ID, 9207L);

        InOrder inOrder = inOrder(testAttemptRepository, utcClock, criticalCommandAuditSupport);
        inOrder.verify(testAttemptRepository).findAndLockTestAttemptByIdAndUserId(9207L, ACTOR_USER_ID);
        inOrder.verify(utcClock).now();
        inOrder.verify(testAttemptRepository).saveTestAttempt(any(TestAttempt.class));
        inOrder.verify(criticalCommandAuditSupport).buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq("TESTING_SELF_ATTEMPT_SUBMIT_TERMINAL"),
            any()
        );
        inOrder.verify(criticalCommandAuditSupport).recordAudit(
            org.mockito.ArgumentMatchers.eq(ACTOR_USER_ID),
            org.mockito.ArgumentMatchers.eq(AttemptTerminalCriticalAuditCatalog.SELF_ATTEMPT_SUBMITTED.auditEventType()),
            org.mockito.ArgumentMatchers.eq("test_attempt"),
            org.mockito.ArgumentMatchers.eq(9207L),
            any(),
            any(),
            any(AuditContext.class)
        );
    }

    @Test
    void selfSubmitAuditPayloadUsesTerminalizedAttemptFactsAndSelfSubmitOperation() {
        TestAttempt activeAttempt = selfAttempt(9209L, 101L, 501L, TestAttemptStatus.IN_PROGRESS);
        when(testAttemptRepository.findAndLockTestAttemptByIdAndUserId(9209L, ACTOR_USER_ID)).thenReturn(activeAttempt);
        when(utcClock.now()).thenReturn(NOW);
        when(testAttemptRepository.saveTestAttempt(any(TestAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq("TESTING_SELF_ATTEMPT_SUBMIT_TERMINAL"),
            any()
        )).thenReturn(new AuditContext("{\"operationCode\":\"TESTING_SELF_ATTEMPT_SUBMIT_TERMINAL\"}"));

        AttemptTerminalizationOutcome outcome = service.submitSelfAttempt(ACTOR_USER_ID, 9209L);

        ArgumentCaptor<Map> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Object> payloadBeforeCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> payloadAfterCaptor = ArgumentCaptor.forClass(Object.class);
        verify(criticalCommandAuditSupport).buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq("TESTING_SELF_ATTEMPT_SUBMIT_TERMINAL"),
            detailsCaptor.capture()
        );
        verify(criticalCommandAuditSupport).recordAudit(
            org.mockito.ArgumentMatchers.eq(ACTOR_USER_ID),
            org.mockito.ArgumentMatchers.eq(AttemptTerminalCriticalAuditCatalog.SELF_ATTEMPT_SUBMITTED.auditEventType()),
            org.mockito.ArgumentMatchers.eq("test_attempt"),
            org.mockito.ArgumentMatchers.eq(9209L),
            payloadBeforeCaptor.capture(),
            payloadAfterCaptor.capture(),
            any(AuditContext.class)
        );

        assertThat(detailsCaptor.getValue())
            .containsEntry("commandType", "self_attempt_submit")
            .containsEntry("terminalType", "completed")
            .containsEntry("testId", 501L)
            .containsEntry("actorSource", "interactive");
        assertThat(detailsCaptor.getValue()).doesNotContainKeys("assignmentId", "assignmentTestId");

        assertThat(payloadBeforeCaptor.getValue()).isInstanceOf(Map.class);
        assertThat(payloadAfterCaptor.getValue()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> beforePayload = (Map<String, Object>) payloadBeforeCaptor.getValue();
        @SuppressWarnings("unchecked")
        Map<String, Object> afterPayload = (Map<String, Object>) payloadAfterCaptor.getValue();
        assertThat(beforePayload)
            .containsEntry("id", 9209L)
            .containsEntry("userId", 101L)
            .containsEntry("testId", 501L)
            .containsEntry("assignmentTestId", null)
            .containsEntry("attemptMode", AttemptMode.SELF)
            .containsEntry("status", TestAttemptStatus.IN_PROGRESS)
            .containsEntry("completedAt", null)
            .containsEntry("expiredAt", null)
            .containsEntry("abandonedAt", null);
        assertThat(afterPayload)
            .containsEntry("id", 9209L)
            .containsEntry("userId", 101L)
            .containsEntry("testId", 501L)
            .containsEntry("assignmentTestId", null)
            .containsEntry("attemptMode", AttemptMode.SELF)
            .containsEntry("status", TestAttemptStatus.COMPLETED)
            .containsEntry("completedAt", NOW)
            .containsEntry("expiredAt", null)
            .containsEntry("abandonedAt", null);
        assertThat(outcome.terminalizedAttempt().completedAt()).isEqualTo(NOW);
    }

    @Test
    void doesNotWriteSubmitSuccessAuditWhenSelfSubmitOwnerWriteFails() {
        TestAttempt activeAttempt = selfAttempt(9208L, 101L, 501L, TestAttemptStatus.IN_PROGRESS);
        when(testAttemptRepository.findAndLockTestAttemptByIdAndUserId(9208L, ACTOR_USER_ID)).thenReturn(activeAttempt);
        when(utcClock.now()).thenReturn(NOW);
        when(testAttemptRepository.saveTestAttempt(any(TestAttempt.class)))
            .thenThrow(new com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException(
                "Failed to persist test_attempt"
            ));

        assertThatThrownBy(() -> service.submitSelfAttempt(ACTOR_USER_ID, 9208L))
            .isInstanceOf(com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException.class)
            .hasMessageContaining("Failed to persist test_attempt");

        verify(testAttemptRepository).saveTestAttempt(any(TestAttempt.class));
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(String.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
        verifyNoInteractions(resultRecordingService);
    }

    @Test
    void failsClosedWhenAttemptDoesNotExist() {
        when(testAttemptRepository.findAndLockTestAttemptByIdAndUserId(9202L, ACTOR_USER_ID))
            .thenThrow(new NotFoundException("Test attempt not found: 9202"));

        assertThatThrownBy(() -> service.submitSelfAttempt(ACTOR_USER_ID, 9202L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("9202");

        verify(testAttemptRepository).findAndLockTestAttemptByIdAndUserId(9202L, ACTOR_USER_ID);
        verifyNoInteractions(utcClock, resultRecordingService);
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(String.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void selfSubmitUsesActorScopedLookupAndDoesNotFallBackToGenericOrAssignedActiveAttemptResolution() {
        when(testAttemptRepository.findAndLockTestAttemptByIdAndUserId(9206L, ACTOR_USER_ID))
            .thenThrow(new NotFoundException("Test attempt not found: 9206"));

        assertThatThrownBy(() -> service.submitSelfAttempt(ACTOR_USER_ID, 9206L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("9206");

        verify(testAttemptRepository).findAndLockTestAttemptByIdAndUserId(9206L, ACTOR_USER_ID);
        verify(testAttemptRepository, never()).findAndLockTestAttemptById(9206L);
        verify(testAttemptRepository, never()).findAndLockActiveAssignedAttemptForActor(
            org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.anyLong()
        );
        verify(testAttemptRepository, never()).findAndLockActiveSelfAttempt(
            org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.anyLong()
        );
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verifyNoInteractions(utcClock, resultRecordingService);
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(String.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void failsClosedWhenAttemptIsNotSelf() {
        TestAttempt assignedAttempt = new TestAttempt(
            9203L,
            101L,
            501L,
            701L,
            AttemptMode.ASSIGNED,
            TestAttemptStatus.STARTED,
            STARTED_AT,
            null,
            null,
            null,
            LAST_ACTIVITY_AT,
            STARTED_AT,
            LAST_ACTIVITY_AT
        );
        when(testAttemptRepository.findAndLockTestAttemptByIdAndUserId(9203L, ACTOR_USER_ID)).thenReturn(assignedAttempt);

        assertThatThrownBy(() -> service.submitSelfAttempt(ACTOR_USER_ID, 9203L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("non-self attempt")
            .hasMessageContaining("9203");

        verify(testAttemptRepository).findAndLockTestAttemptByIdAndUserId(9203L, ACTOR_USER_ID);
        verifyNoInteractions(utcClock, resultRecordingService);
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(String.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void failsClosedWhenSelfAttemptBelongsToDifferentActor() {
        TestAttempt foreignAttempt = selfAttempt(9204L, 202L, 501L, TestAttemptStatus.STARTED);
        when(testAttemptRepository.findAndLockTestAttemptByIdAndUserId(9204L, ACTOR_USER_ID)).thenReturn(foreignAttempt);

        assertThatThrownBy(() -> service.submitSelfAttempt(ACTOR_USER_ID, 9204L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("foreign attempt")
            .hasMessageContaining("9204");

        verify(testAttemptRepository).findAndLockTestAttemptByIdAndUserId(9204L, ACTOR_USER_ID);
        verifyNoInteractions(utcClock, resultRecordingService);
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(String.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void failsClosedWhenSelfAttemptIsAlreadyTerminal() {
        TestAttempt completedAttempt = selfAttempt(9205L, 101L, 501L, TestAttemptStatus.COMPLETED);
        when(testAttemptRepository.findAndLockTestAttemptByIdAndUserId(9205L, ACTOR_USER_ID)).thenReturn(completedAttempt);

        assertThatThrownBy(() -> service.submitSelfAttempt(ACTOR_USER_ID, 9205L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("non-active attempt")
            .hasMessageContaining("status=COMPLETED");

        verify(testAttemptRepository).findAndLockTestAttemptByIdAndUserId(9205L, ACTOR_USER_ID);
        verifyNoInteractions(utcClock, resultRecordingService);
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(String.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    private TestAttempt selfAttempt(Long id, Long userId, Long testId, TestAttemptStatus status) {
        return new TestAttempt(
            id,
            userId,
            testId,
            null,
            AttemptMode.SELF,
            status,
            STARTED_AT,
            status == TestAttemptStatus.COMPLETED ? LAST_ACTIVITY_AT : null,
            null,
            null,
            LAST_ACTIVITY_AT,
            STARTED_AT,
            LAST_ACTIVITY_AT
        );
    }
}
