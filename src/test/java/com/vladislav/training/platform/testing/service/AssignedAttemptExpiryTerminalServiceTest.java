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
import com.vladislav.training.platform.audit.service.SystemActorResolver;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.common.time.UtcClock;
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
 * Проверяет поведение сервиса {@code AssignedAttemptExpiryTerminal}.
 * Сценарии сосредоточены на прикладной логике.
 */
@ExtendWith(MockitoExtension.class)
class AssignedAttemptExpiryTerminalServiceTest {

    private static final Instant STARTED_AT = Instant.parse("2026-04-19T18:00:00Z");
    private static final Instant LAST_ACTIVITY_AT = Instant.parse("2026-04-19T18:20:00Z");
    private static final Instant NOW = Instant.parse("2026-04-19T18:45:00Z");

    @Mock
    private CriticalCommandAuditSupport criticalCommandAuditSupport;
    @Mock
    private SystemActorResolver systemActorResolver;
    @Mock
    private TestAttemptRepository testAttemptRepository;
    @Mock
    private UtcClock utcClock;

    private AssignedAttemptExpiryTerminalService service;

    @BeforeEach
    void setUp() {
        service = new AssignedAttemptExpiryTerminalService(
            criticalCommandAuditSupport,
            systemActorResolver,
            testAttemptRepository,
            utcClock
        );
    }

    @Test
    void terminalizesAssignedActiveAttemptIntoExpiredState() {
        TestAttempt activeAttempt = assignedAttempt(9101L, 101L, 501L, 701L, TestAttemptStatus.IN_PROGRESS);
        when(testAttemptRepository.findAndLockTestAttemptById(9101L)).thenReturn(activeAttempt);
        when(utcClock.now()).thenReturn(NOW);
        when(testAttemptRepository.saveTestAttempt(any(TestAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(criticalCommandAuditSupport.resolveSystemActorUserId(systemActorResolver)).thenReturn(900L);
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq("TESTING_ASSIGNED_ATTEMPT_EXPIRE_TERMINAL"),
            any()
        )).thenReturn(new AuditContext("{\"operationCode\":\"TESTING_ASSIGNED_ATTEMPT_EXPIRE_TERMINAL\"}"));

        TestAttempt expiredAttempt = service.expireAssignedAttempt(9101L);

        assertThat(expiredAttempt.id()).isEqualTo(9101L);
        assertThat(expiredAttempt.userId()).isEqualTo(101L);
        assertThat(expiredAttempt.testId()).isEqualTo(501L);
        assertThat(expiredAttempt.assignmentTestId()).isEqualTo(701L);
        assertThat(expiredAttempt.attemptMode()).isEqualTo(AttemptMode.ASSIGNED);
        assertThat(expiredAttempt.status()).isEqualTo(TestAttemptStatus.EXPIRED);
        assertThat(expiredAttempt.startedAt()).isEqualTo(STARTED_AT);
        assertThat(expiredAttempt.completedAt()).isNull();
        assertThat(expiredAttempt.expiredAt()).isEqualTo(NOW);
        assertThat(expiredAttempt.abandonedAt()).isNull();
        assertThat(expiredAttempt.lastActivityAt()).isEqualTo(LAST_ACTIVITY_AT);
        assertThat(expiredAttempt.createdAt()).isEqualTo(STARTED_AT);
        assertThat(expiredAttempt.updatedAt()).isEqualTo(NOW);

        InOrder inOrder = inOrder(testAttemptRepository, utcClock, criticalCommandAuditSupport);
        inOrder.verify(testAttemptRepository).findAndLockTestAttemptById(9101L);
        inOrder.verify(utcClock).now();
        inOrder.verify(criticalCommandAuditSupport).resolveSystemActorUserId(systemActorResolver);
        inOrder.verify(testAttemptRepository).saveTestAttempt(any(TestAttempt.class));
        inOrder.verify(criticalCommandAuditSupport).buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq("TESTING_ASSIGNED_ATTEMPT_EXPIRE_TERMINAL"),
            any()
        );
        inOrder.verify(criticalCommandAuditSupport).recordAudit(
            org.mockito.ArgumentMatchers.eq(900L),
            any(),
            org.mockito.ArgumentMatchers.eq("test_attempt"),
            org.mockito.ArgumentMatchers.eq(9101L),
            any(),
            any(),
            any(AuditContext.class)
        );
        verifyNoMoreInteractions(testAttemptRepository, utcClock, criticalCommandAuditSupport);
    }

    @Test
    void failsClosedWhenAttemptDoesNotExist() {
        when(testAttemptRepository.findAndLockTestAttemptById(9102L))
            .thenThrow(new NotFoundException("Test attempt not found: 9102"));

        assertThatThrownBy(() -> service.expireAssignedAttempt(9102L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("9102");

        verify(testAttemptRepository).findAndLockTestAttemptById(9102L);
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verifyNoInteractions(utcClock);
        verifyNoInteractions(criticalCommandAuditSupport);
    }

    @Test
    void failsClosedWhenAttemptIsNotAssigned() {
        TestAttempt selfAttempt = new TestAttempt(
            9103L,
            101L,
            501L,
            null,
            AttemptMode.SELF,
            TestAttemptStatus.STARTED,
            STARTED_AT,
            null,
            null,
            null,
            LAST_ACTIVITY_AT,
            STARTED_AT,
            LAST_ACTIVITY_AT
        );
        when(testAttemptRepository.findAndLockTestAttemptById(9103L)).thenReturn(selfAttempt);

        assertThatThrownBy(() -> service.expireAssignedAttempt(9103L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("non-assigned attempt")
            .hasMessageContaining("9103");

        verify(testAttemptRepository).findAndLockTestAttemptById(9103L);
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verifyNoInteractions(utcClock);
        verifyNoInteractions(criticalCommandAuditSupport);
    }

    @Test
    void failsClosedWhenAssignedAttemptIsAlreadyTerminal() {
        TestAttempt completedAttempt = assignedAttempt(9104L, 101L, 501L, 701L, TestAttemptStatus.COMPLETED);
        when(testAttemptRepository.findAndLockTestAttemptById(9104L)).thenReturn(completedAttempt);

        assertThatThrownBy(() -> service.expireAssignedAttempt(9104L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("non-active attempt")
            .hasMessageContaining("status=COMPLETED");

        verify(testAttemptRepository).findAndLockTestAttemptById(9104L);
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verifyNoInteractions(utcClock);
        verifyNoInteractions(criticalCommandAuditSupport);
    }

    @Test
    void failsClosedWhenAssignedAttemptIsAlreadyExpired() {
        TestAttempt expiredAttempt = assignedAttempt(9105L, 101L, 501L, 701L, TestAttemptStatus.EXPIRED);
        when(testAttemptRepository.findAndLockTestAttemptById(9105L)).thenReturn(expiredAttempt);

        assertThatThrownBy(() -> service.expireAssignedAttempt(9105L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("non-active attempt")
            .hasMessageContaining("status=EXPIRED");

        verify(testAttemptRepository).findAndLockTestAttemptById(9105L);
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verifyNoInteractions(utcClock);
        verifyNoInteractions(criticalCommandAuditSupport);
    }

    @Test
    void explicitAssignedExpiryDoesNotAttemptResultRecordingEvenIfResultRecorderWouldFail() {
        TestAttempt activeAttempt = assignedAttempt(9106L, 101L, 501L, 701L, TestAttemptStatus.STARTED);
        when(testAttemptRepository.findAndLockTestAttemptById(9106L)).thenReturn(activeAttempt);
        when(utcClock.now()).thenReturn(NOW);
        when(testAttemptRepository.saveTestAttempt(any(TestAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(criticalCommandAuditSupport.resolveSystemActorUserId(systemActorResolver)).thenReturn(900L);
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq("TESTING_ASSIGNED_ATTEMPT_EXPIRE_TERMINAL"),
            any()
        )).thenReturn(new AuditContext("{\"operationCode\":\"TESTING_ASSIGNED_ATTEMPT_EXPIRE_TERMINAL\"}"));
        TestAttempt expiredAttempt = service.expireAssignedAttempt(9106L);

        assertThat(expiredAttempt.status()).isEqualTo(TestAttemptStatus.EXPIRED);
        verify(testAttemptRepository).saveTestAttempt(any(TestAttempt.class));
    }

    private TestAttempt assignedAttempt(
        Long id,
        Long userId,
        Long testId,
        Long assignmentTestId,
        TestAttemptStatus status
    ) {
        return new TestAttempt(
            id,
            userId,
            testId,
            assignmentTestId,
            AttemptMode.ASSIGNED,
            status,
            STARTED_AT,
            status == TestAttemptStatus.COMPLETED ? LAST_ACTIVITY_AT : null,
            status == TestAttemptStatus.EXPIRED ? LAST_ACTIVITY_AT : null,
            null,
            LAST_ACTIVITY_AT,
            STARTED_AT,
            LAST_ACTIVITY_AT
        );
    }
}
