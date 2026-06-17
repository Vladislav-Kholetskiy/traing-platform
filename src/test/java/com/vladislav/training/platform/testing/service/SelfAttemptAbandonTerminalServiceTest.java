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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение сервиса {@code SelfAttemptAbandonTerminal}.
 * Сценарии сосредоточены на прикладной логике.
 */
@ExtendWith(MockitoExtension.class)
class SelfAttemptAbandonTerminalServiceTest {

    private static final Instant STARTED_AT = Instant.parse("2026-04-19T20:00:00Z");
    private static final Instant LAST_ACTIVITY_AT = Instant.parse("2026-04-19T20:20:00Z");
    private static final Instant NOW = Instant.parse("2026-04-19T20:45:00Z");
    private static final Long ACTOR_USER_ID = 101L;

    @Mock
    private CriticalCommandAuditSupport criticalCommandAuditSupport;
    @Mock
    private TestAttemptRepository testAttemptRepository;
    @Mock
    private UtcClock utcClock;
    @Mock
    private ResultRecordingService resultRecordingService;

    private SelfAttemptAbandonTerminalService service;

    @BeforeEach
    void setUp() {
        service = new SelfAttemptAbandonTerminalService(criticalCommandAuditSupport, testAttemptRepository, utcClock);
    }

    @Test
    void terminalizesSelfActiveAttemptIntoAbandonedState() {
        TestAttempt activeAttempt = selfAttempt(9301L, 101L, 501L, TestAttemptStatus.IN_PROGRESS);
        when(testAttemptRepository.findAndLockTestAttemptByIdAndUserId(9301L, ACTOR_USER_ID)).thenReturn(activeAttempt);
        when(utcClock.now()).thenReturn(NOW);
        when(testAttemptRepository.saveTestAttempt(any(TestAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq("TESTING_SELF_ATTEMPT_ABANDON_TERMINAL"),
            any()
        )).thenReturn(new AuditContext("{\"operationCode\":\"TESTING_SELF_ATTEMPT_ABANDON_TERMINAL\"}"));

        AttemptTerminalizationOutcome abandonedOutcome = service.abandonSelfAttempt(ACTOR_USER_ID, 9301L);
        TestAttempt abandonedAttempt = abandonedOutcome.terminalizedAttempt();

        assertThat(abandonedAttempt.id()).isEqualTo(9301L);
        assertThat(abandonedAttempt.userId()).isEqualTo(101L);
        assertThat(abandonedAttempt.testId()).isEqualTo(501L);
        assertThat(abandonedAttempt.assignmentTestId()).isNull();
        assertThat(abandonedAttempt.attemptMode()).isEqualTo(AttemptMode.SELF);
        assertThat(abandonedAttempt.status()).isEqualTo(TestAttemptStatus.ABANDONED);
        assertThat(abandonedAttempt.startedAt()).isEqualTo(STARTED_AT);
        assertThat(abandonedAttempt.completedAt()).isNull();
        assertThat(abandonedAttempt.expiredAt()).isNull();
        assertThat(abandonedAttempt.abandonedAt()).isEqualTo(NOW);
        assertThat(abandonedAttempt.lastActivityAt()).isEqualTo(NOW);
        assertThat(abandonedAttempt.createdAt()).isEqualTo(STARTED_AT);
        assertThat(abandonedAttempt.updatedAt()).isEqualTo(NOW);
        assertThat(abandonedOutcome.attemptId()).isEqualTo(9301L);
        assertThat(abandonedOutcome.actorUserId()).isEqualTo(ACTOR_USER_ID);
        assertThat(abandonedOutcome.attemptMode()).isEqualTo(AttemptMode.SELF);
        assertThat(abandonedOutcome.terminalStatus()).isEqualTo(TestAttemptStatus.ABANDONED);
        assertThat(abandonedOutcome.terminalizedAt()).isEqualTo(NOW);
        assertThat(abandonedOutcome.reason()).isEqualTo(AttemptTerminalizationReason.SELF_ABANDON);
        assertThat(abandonedOutcome.resultRecordable()).isFalse();
        assertThat(abandonedOutcome.auditEventType())
            .isEqualTo(AttemptTerminalCriticalAuditCatalog.SELF_ATTEMPT_ABANDONED.auditEventType());
        assertThat(abandonedOutcome.terminalizedAttempt()).isSameAs(abandonedAttempt);

        InOrder inOrder = inOrder(criticalCommandAuditSupport, testAttemptRepository, utcClock);
        inOrder.verify(testAttemptRepository).findAndLockTestAttemptByIdAndUserId(9301L, ACTOR_USER_ID);
        inOrder.verify(utcClock).now();
        inOrder.verify(testAttemptRepository).saveTestAttempt(any(TestAttempt.class));
        verify(criticalCommandAuditSupport).buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq("TESTING_SELF_ATTEMPT_ABANDON_TERMINAL"),
            any()
        );
        verify(criticalCommandAuditSupport).recordAudit(
            org.mockito.ArgumentMatchers.eq(ACTOR_USER_ID),
            any(),
            org.mockito.ArgumentMatchers.eq("test_attempt"),
            org.mockito.ArgumentMatchers.eq(9301L),
            any(),
            any(),
            any(AuditContext.class)
        );
        verifyNoMoreInteractions(criticalCommandAuditSupport, testAttemptRepository, utcClock);
        verifyNoInteractions(resultRecordingService);
    }

    @Test
    void failsClosedWhenAttemptDoesNotExist() {
        when(testAttemptRepository.findAndLockTestAttemptByIdAndUserId(9302L, ACTOR_USER_ID))
            .thenThrow(new NotFoundException("Test attempt not found: 9302"));

        assertThatThrownBy(() -> service.abandonSelfAttempt(ACTOR_USER_ID, 9302L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("9302");

        verify(testAttemptRepository).findAndLockTestAttemptByIdAndUserId(9302L, ACTOR_USER_ID);
        verifyNoInteractions(utcClock, resultRecordingService);
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(String.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void selfAbandonUsesActorScopedActiveSelfLookupAndDoesNotFallBackToGenericOrAssignedResolution() {
        when(testAttemptRepository.findAndLockTestAttemptByIdAndUserId(9306L, ACTOR_USER_ID))
            .thenThrow(new NotFoundException("Test attempt not found: 9306"));

        assertThatThrownBy(() -> service.abandonSelfAttempt(ACTOR_USER_ID, 9306L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("9306");

        verify(testAttemptRepository).findAndLockTestAttemptByIdAndUserId(9306L, ACTOR_USER_ID);
        verify(testAttemptRepository, never()).findAndLockTestAttemptById(9306L);
        verify(testAttemptRepository, never()).findAndLockActiveAssignedAttemptForActor(
            org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.anyLong()
        );
        verify(testAttemptRepository, never()).findAndLockAssignedAttemptByIdAndUserIdAndAssignmentTestId(
            org.mockito.ArgumentMatchers.anyLong(),
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
            9303L,
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
        when(testAttemptRepository.findAndLockTestAttemptByIdAndUserId(9303L, ACTOR_USER_ID)).thenReturn(assignedAttempt);

        assertThatThrownBy(() -> service.abandonSelfAttempt(ACTOR_USER_ID, 9303L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("non-self attempt")
            .hasMessageContaining("9303");

        verify(testAttemptRepository).findAndLockTestAttemptByIdAndUserId(9303L, ACTOR_USER_ID);
        verifyNoInteractions(utcClock, resultRecordingService);
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(String.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void failsClosedWhenSelfAttemptBelongsToDifferentActor() {
        TestAttempt foreignAttempt = selfAttempt(9304L, 202L, 501L, TestAttemptStatus.STARTED);
        when(testAttemptRepository.findAndLockTestAttemptByIdAndUserId(9304L, ACTOR_USER_ID)).thenReturn(foreignAttempt);

        assertThatThrownBy(() -> service.abandonSelfAttempt(ACTOR_USER_ID, 9304L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("foreign attempt")
            .hasMessageContaining("9304");

        verify(testAttemptRepository).findAndLockTestAttemptByIdAndUserId(9304L, ACTOR_USER_ID);
        verifyNoInteractions(utcClock, resultRecordingService);
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(String.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void failsClosedWhenSelfAttemptIsAlreadyTerminal() {
        TestAttempt abandonedAttempt = selfAttempt(9305L, 101L, 501L, TestAttemptStatus.ABANDONED);
        when(testAttemptRepository.findAndLockTestAttemptByIdAndUserId(9305L, ACTOR_USER_ID)).thenReturn(abandonedAttempt);

        assertThatThrownBy(() -> service.abandonSelfAttempt(ACTOR_USER_ID, 9305L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("non-active attempt")
            .hasMessageContaining("status=ABANDONED");

        verify(testAttemptRepository).findAndLockTestAttemptByIdAndUserId(9305L, ACTOR_USER_ID);
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
            null,
            null,
            status == TestAttemptStatus.ABANDONED ? LAST_ACTIVITY_AT : null,
            LAST_ACTIVITY_AT,
            STARTED_AT,
            LAST_ACTIVITY_AT
        );
    }
}
