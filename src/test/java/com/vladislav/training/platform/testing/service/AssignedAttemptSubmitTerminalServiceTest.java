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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * Проверяет поведение сервиса {@code AssignedAttemptSubmitTerminal}.
 * Сценарии сосредоточены на прикладной логике.
 */
@ExtendWith(MockitoExtension.class)
class AssignedAttemptSubmitTerminalServiceTest {

    private static final Instant STARTED_AT = Instant.parse("2026-04-19T17:00:00Z");
    private static final Instant LAST_ACTIVITY_AT = Instant.parse("2026-04-19T17:20:00Z");
    private static final Instant NOW = Instant.parse("2026-04-19T17:45:00Z");
    private static final Long ACTOR_USER_ID = 101L;

    @Mock
    private AttemptStatusRecalculationService attemptStatusRecalculationService;
    @Mock
    private CriticalCommandAuditSupport criticalCommandAuditSupport;
    @Mock
    private TestAttemptRepository testAttemptRepository;
    @Mock
    private UtcClock utcClock;
    @Mock
    private ResultRecordingService resultRecordingService;

    private AssignedAttemptSubmitTerminalService service;

    @BeforeEach
    void setUp() {
        service = new AssignedAttemptSubmitTerminalService(
            attemptStatusRecalculationService,
            criticalCommandAuditSupport,
            testAttemptRepository,
            utcClock
        );
    }

    @Test
    void submitPathRefreshesAttemptStatusBeforeTerminalization() {
        TestAttempt activeAttempt = assignedAttempt(9003L, 101L, 501L, 701L, TestAttemptStatus.IN_PROGRESS);
        when(utcClock.now()).thenReturn(NOW);
        when(attemptStatusRecalculationService.refreshAssignedAttemptStatusCacheWithVerdict(ACTOR_USER_ID, 701L, 9003L, NOW))
            .thenReturn(refreshResult(activeAttempt, TestAttemptStatus.IN_PROGRESS, false));
        when(testAttemptRepository.saveTestAttempt(any(TestAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq("TESTING_ASSIGNED_ATTEMPT_SUBMIT_TERMINAL"),
            any()
        )).thenReturn(new AuditContext("{\"operationCode\":\"TESTING_ASSIGNED_ATTEMPT_SUBMIT_TERMINAL\"}"));

        AttemptTerminalizationOutcome completedOutcome = service.submitAssignedAttempt(ACTOR_USER_ID, 701L, 9003L);
        TestAttempt completedAttempt = completedOutcome.terminalizedAttempt();

        assertThat(completedOutcome.attemptId()).isEqualTo(9003L);
        assertThat(completedOutcome.actorUserId()).isEqualTo(ACTOR_USER_ID);
        assertThat(completedOutcome.attemptMode()).isEqualTo(AttemptMode.ASSIGNED);
        assertThat(completedOutcome.terminalStatus()).isEqualTo(TestAttemptStatus.COMPLETED);
        assertThat(completedOutcome.terminalizedAt()).isEqualTo(NOW);
        assertThat(completedOutcome.reason()).isEqualTo(AttemptTerminalizationReason.NORMAL_SUBMIT);
        assertThat(completedOutcome.resultRecordable()).isTrue();
        assertThat(completedOutcome.auditEventType())
            .isEqualTo(AttemptTerminalCriticalAuditCatalog.ASSIGNED_ATTEMPT_SUBMITTED.auditEventType());
        assertThat(completedAttempt.id()).isEqualTo(9003L);
        assertThat(completedAttempt.userId()).isEqualTo(101L);
        assertThat(completedAttempt.testId()).isEqualTo(501L);
        assertThat(completedAttempt.assignmentTestId()).isEqualTo(701L);
        assertThat(completedAttempt.attemptMode()).isEqualTo(AttemptMode.ASSIGNED);
        assertThat(completedAttempt.status()).isEqualTo(TestAttemptStatus.COMPLETED);
        assertThat(completedAttempt.startedAt()).isEqualTo(STARTED_AT);
        assertThat(completedAttempt.completedAt()).isEqualTo(NOW);
        assertThat(completedAttempt.expiredAt()).isNull();
        assertThat(completedAttempt.abandonedAt()).isNull();
        assertThat(completedAttempt.lastActivityAt()).isEqualTo(NOW);
        assertThat(completedAttempt.createdAt()).isEqualTo(STARTED_AT);
        assertThat(completedAttempt.updatedAt()).isEqualTo(NOW);

        InOrder inOrder = inOrder(
            utcClock,
            attemptStatusRecalculationService,
            testAttemptRepository
        );
        inOrder.verify(utcClock).now();
        inOrder.verify(attemptStatusRecalculationService)
            .refreshAssignedAttemptStatusCacheWithVerdict(ACTOR_USER_ID, 701L, 9003L, NOW);
        inOrder.verify(testAttemptRepository).saveTestAttempt(any(TestAttempt.class));
        verify(criticalCommandAuditSupport).buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq("TESTING_ASSIGNED_ATTEMPT_SUBMIT_TERMINAL"),
            any()
        );
        verify(criticalCommandAuditSupport).recordAudit(
            org.mockito.ArgumentMatchers.eq(ACTOR_USER_ID),
            any(),
            org.mockito.ArgumentMatchers.eq("test_attempt"),
            org.mockito.ArgumentMatchers.eq(9003L),
            any(),
            any(),
            any(AuditContext.class)
        );
        verifyNoMoreInteractions(criticalCommandAuditSupport, attemptStatusRecalculationService, testAttemptRepository, utcClock);
        verifyNoInteractions(resultRecordingService);
    }

    @Test
    void writesAssignedSubmitAuditOnlyAfterSuccessfulOwnerTerminalizationWrite() {
        TestAttempt activeAttempt = assignedAttempt(9016L, 101L, 501L, 701L, TestAttemptStatus.IN_PROGRESS);
        when(utcClock.now()).thenReturn(NOW);
        when(attemptStatusRecalculationService.refreshAssignedAttemptStatusCacheWithVerdict(ACTOR_USER_ID, 701L, 9016L, NOW))
            .thenReturn(refreshResult(activeAttempt, TestAttemptStatus.IN_PROGRESS, false));
        when(testAttemptRepository.saveTestAttempt(any(TestAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq("TESTING_ASSIGNED_ATTEMPT_SUBMIT_TERMINAL"),
            any()
        )).thenReturn(new AuditContext("{\"operationCode\":\"TESTING_ASSIGNED_ATTEMPT_SUBMIT_TERMINAL\"}"));

        service.submitAssignedAttempt(ACTOR_USER_ID, 701L, 9016L);

        InOrder inOrder = inOrder(
            utcClock,
            attemptStatusRecalculationService,
            testAttemptRepository,
            criticalCommandAuditSupport
        );
        inOrder.verify(utcClock).now();
        inOrder.verify(attemptStatusRecalculationService)
            .refreshAssignedAttemptStatusCacheWithVerdict(ACTOR_USER_ID, 701L, 9016L, NOW);
        inOrder.verify(testAttemptRepository).saveTestAttempt(any(TestAttempt.class));
        inOrder.verify(criticalCommandAuditSupport).buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq("TESTING_ASSIGNED_ATTEMPT_SUBMIT_TERMINAL"),
            any()
        );
        inOrder.verify(criticalCommandAuditSupport).recordAudit(
            org.mockito.ArgumentMatchers.eq(ACTOR_USER_ID),
            org.mockito.ArgumentMatchers.eq(AttemptTerminalCriticalAuditCatalog.ASSIGNED_ATTEMPT_SUBMITTED.auditEventType()),
            org.mockito.ArgumentMatchers.eq("test_attempt"),
            org.mockito.ArgumentMatchers.eq(9016L),
            any(),
            any(),
            any(AuditContext.class)
        );
    }

    @Test
    void assignedSubmitAuditPayloadUsesTerminalizedAttemptFactsAndAssignedSubmitOperation() {
        TestAttempt activeAttempt = assignedAttempt(9018L, 101L, 501L, 701L, TestAttemptStatus.IN_PROGRESS);
        when(utcClock.now()).thenReturn(NOW);
        when(attemptStatusRecalculationService.refreshAssignedAttemptStatusCacheWithVerdict(ACTOR_USER_ID, 701L, 9018L, NOW))
            .thenReturn(refreshResult(activeAttempt, TestAttemptStatus.IN_PROGRESS, false));
        when(testAttemptRepository.saveTestAttempt(any(TestAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq("TESTING_ASSIGNED_ATTEMPT_SUBMIT_TERMINAL"),
            any()
        )).thenReturn(new AuditContext("{\"operationCode\":\"TESTING_ASSIGNED_ATTEMPT_SUBMIT_TERMINAL\"}"));

        AttemptTerminalizationOutcome outcome = service.submitAssignedAttempt(ACTOR_USER_ID, 701L, 9018L);

        ArgumentCaptor<Map> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Object> payloadBeforeCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> payloadAfterCaptor = ArgumentCaptor.forClass(Object.class);
        verify(criticalCommandAuditSupport).buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq("TESTING_ASSIGNED_ATTEMPT_SUBMIT_TERMINAL"),
            detailsCaptor.capture()
        );
        verify(criticalCommandAuditSupport).recordAudit(
            org.mockito.ArgumentMatchers.eq(ACTOR_USER_ID),
            org.mockito.ArgumentMatchers.eq(AttemptTerminalCriticalAuditCatalog.ASSIGNED_ATTEMPT_SUBMITTED.auditEventType()),
            org.mockito.ArgumentMatchers.eq("test_attempt"),
            org.mockito.ArgumentMatchers.eq(9018L),
            payloadBeforeCaptor.capture(),
            payloadAfterCaptor.capture(),
            any(AuditContext.class)
        );

        assertThat(detailsCaptor.getValue())
            .containsEntry("commandType", "assigned_attempt_submit")
            .containsEntry("terminalType", "completed")
            .containsEntry("assignmentTestId", 701L)
            .containsEntry("testId", 501L)
            .containsEntry("actorSource", "interactive");

        assertThat(payloadBeforeCaptor.getValue()).isInstanceOf(Map.class);
        assertThat(payloadAfterCaptor.getValue()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> beforePayload = (Map<String, Object>) payloadBeforeCaptor.getValue();
        @SuppressWarnings("unchecked")
        Map<String, Object> afterPayload = (Map<String, Object>) payloadAfterCaptor.getValue();
        assertThat(beforePayload)
            .containsEntry("id", 9018L)
            .containsEntry("userId", 101L)
            .containsEntry("testId", 501L)
            .containsEntry("assignmentTestId", 701L)
            .containsEntry("attemptMode", AttemptMode.ASSIGNED)
            .containsEntry("status", TestAttemptStatus.IN_PROGRESS)
            .containsEntry("completedAt", null)
            .containsEntry("expiredAt", null)
            .containsEntry("abandonedAt", null);
        assertThat(afterPayload)
            .containsEntry("id", 9018L)
            .containsEntry("userId", 101L)
            .containsEntry("testId", 501L)
            .containsEntry("assignmentTestId", 701L)
            .containsEntry("attemptMode", AttemptMode.ASSIGNED)
            .containsEntry("status", TestAttemptStatus.COMPLETED)
            .containsEntry("completedAt", NOW)
            .containsEntry("expiredAt", null)
            .containsEntry("abandonedAt", null);
        assertThat(outcome.terminalizedAttempt().completedAt()).isEqualTo(NOW);
    }

    @Test
    void doesNotWriteSubmitSuccessAuditWhenAssignedSubmitOwnerWriteFails() {
        TestAttempt activeAttempt = assignedAttempt(9017L, 101L, 501L, 701L, TestAttemptStatus.IN_PROGRESS);
        when(utcClock.now()).thenReturn(NOW);
        when(attemptStatusRecalculationService.refreshAssignedAttemptStatusCacheWithVerdict(ACTOR_USER_ID, 701L, 9017L, NOW))
            .thenReturn(refreshResult(activeAttempt, TestAttemptStatus.IN_PROGRESS, false));
        when(testAttemptRepository.saveTestAttempt(any(TestAttempt.class)))
            .thenThrow(new com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException(
                "Failed to persist test_attempt"
            ));

        assertThatThrownBy(() -> service.submitAssignedAttempt(ACTOR_USER_ID, 701L, 9017L))
            .isInstanceOf(com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException.class)
            .hasMessageContaining("Failed to persist test_attempt");

        verify(testAttemptRepository).saveTestAttempt(any(TestAttempt.class));
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(String.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
        verifyNoInteractions(resultRecordingService);
    }

    @Test
    void assignedSubmitUsesActorAndAssignmentAnchorScopedLookupAndDoesNotFallBackToGenericOrSelfResolution()
        throws IOException {
        String terminalServiceSource = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/testing/service/AssignedAttemptSubmitTerminalService.java"
        ));
        String recalculationSource = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/testing/service/AttemptStatusRecalculationServiceImpl.java"
        ));
        String repositorySource = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/testing/repository/TestAttemptRepository.java"
        ));
        String springDataRepositorySource = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/testing/infrastructure/persistence/SpringDataTestAttemptJpaRepository.java"
        ));

        assertThat(repositorySource)
            
            .contains("findAndLockAssignedAttemptByIdAndUserIdAndAssignmentTestId");
        assertThat(springDataRepositorySource)
            
            .contains("ta.attemptMode = com.vladislav.training.platform.common.model.AttemptMode.ASSIGNED");
        assertThat(terminalServiceSource)
            
            .contains("refreshAssignedAttemptStatusCacheWithVerdict(")
            .doesNotContain("refreshAttemptStatusCacheWithVerdict(actorUserId, testAttemptId, now)");
        int assignedRefreshIndex = recalculationSource.indexOf("refreshAssignedAttemptStatusCacheWithVerdict(");
        int assignedRefreshHelperIndex = recalculationSource.indexOf("refreshVerdictFromLockedAttempt(", assignedRefreshIndex);
        assertThat(assignedRefreshIndex).isGreaterThanOrEqualTo(0);
        assertThat(assignedRefreshHelperIndex).isGreaterThan(assignedRefreshIndex);
        String assignedRefreshBranch = recalculationSource.substring(assignedRefreshIndex, assignedRefreshHelperIndex);
        assertThat(recalculationSource)
            .contains("findAndLockAssignedAttemptByIdAndUserIdAndAssignmentTestId(");
        assertThat(assignedRefreshBranch)
            
            .doesNotContain("findAndLockTestAttemptById(testAttemptId)")
            .doesNotContain("findAndLockTestAttemptByIdAndUserId(testAttemptId, actorUserId)")
            .doesNotContain("findAndLockActiveAssignedAttempt(")
            .doesNotContain("findAndLockActiveSelfAttempt(")
            .contains("requireAssignedRefreshLookupAttempt(")
            .contains("findAndLockAssignedAttemptByIdAndUserIdAndAssignmentTestId(");
        assertThat(recalculationSource)
            .contains("attempt.attemptMode() != AttemptMode.ASSIGNED")
            .contains("Assigned refresh requires ASSIGNED attempt mode");
    }

    @Test
    void failsClosedWhenAttemptDoesNotExist() {
        when(utcClock.now()).thenReturn(NOW);
        when(attemptStatusRecalculationService.refreshAssignedAttemptStatusCacheWithVerdict(ACTOR_USER_ID, 701L, 9003L, NOW))
            .thenThrow(new NotFoundException("Test attempt not found: 9003"));

        assertThatThrownBy(() -> service.submitAssignedAttempt(ACTOR_USER_ID, 701L, 9003L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("9003");

        verify(utcClock).now();
        verify(attemptStatusRecalculationService)
            .refreshAssignedAttemptStatusCacheWithVerdict(ACTOR_USER_ID, 701L, 9003L, NOW);
        verifyNoInteractions(resultRecordingService);
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(String.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void failsClosedWhenAttemptIsNotAssigned() {
        TestAttempt selfAttempt = new TestAttempt(
            9004L,
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
        when(utcClock.now()).thenReturn(NOW);
        when(attemptStatusRecalculationService.refreshAssignedAttemptStatusCacheWithVerdict(ACTOR_USER_ID, 701L, 9004L, NOW))
            .thenReturn(refreshResult(selfAttempt, TestAttemptStatus.STARTED, false));

        assertThatThrownBy(() -> service.submitAssignedAttempt(ACTOR_USER_ID, 701L, 9004L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("non-assigned attempt")
            .hasMessageContaining("9004");

        verify(utcClock).now();
        verify(attemptStatusRecalculationService)
            .refreshAssignedAttemptStatusCacheWithVerdict(ACTOR_USER_ID, 701L, 9004L, NOW);
        verifyNoInteractions(resultRecordingService);
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(String.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void failsClosedWhenAssignedAttemptBelongsToDifferentActor() {
        TestAttempt foreignAttempt = assignedAttempt(9005L, 202L, 501L, 701L, TestAttemptStatus.STARTED);
        when(utcClock.now()).thenReturn(NOW);
        when(attemptStatusRecalculationService.refreshAssignedAttemptStatusCacheWithVerdict(ACTOR_USER_ID, 701L, 9005L, NOW))
            .thenReturn(refreshResult(foreignAttempt, TestAttemptStatus.STARTED, false));

        assertThatThrownBy(() -> service.submitAssignedAttempt(ACTOR_USER_ID, 701L, 9005L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("foreign attempt")
            .hasMessageContaining("9005");

        verify(utcClock).now();
        verify(attemptStatusRecalculationService)
            .refreshAssignedAttemptStatusCacheWithVerdict(ACTOR_USER_ID, 701L, 9005L, NOW);
        verifyNoInteractions(resultRecordingService);
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(String.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void failsClosedWhenAssignedAttemptIsAlreadyTerminal() {
        TestAttempt completedAttempt = assignedAttempt(9006L, 101L, 501L, 701L, TestAttemptStatus.COMPLETED);
        when(utcClock.now()).thenReturn(NOW);
        when(attemptStatusRecalculationService.refreshAssignedAttemptStatusCacheWithVerdict(ACTOR_USER_ID, 701L, 9006L, NOW))
            .thenReturn(refreshResult(completedAttempt, TestAttemptStatus.COMPLETED, false));

        assertThatThrownBy(() -> service.submitAssignedAttempt(ACTOR_USER_ID, 701L, 9006L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("non-active attempt")
            .hasMessageContaining("status=COMPLETED");

        verify(utcClock).now();
        verify(attemptStatusRecalculationService)
            .refreshAssignedAttemptStatusCacheWithVerdict(ACTOR_USER_ID, 701L, 9006L, NOW);
        verifyNoInteractions(resultRecordingService);
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(String.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void staleAssignedAttemptThatBecomesExpiredDuringRefreshReturnsExplicitExpiredTerminalState() {
        TestAttempt expiredAttempt = new TestAttempt(
            9007L,
            101L,
            501L,
            701L,
            AttemptMode.ASSIGNED,
            TestAttemptStatus.EXPIRED,
            STARTED_AT,
            null,
            NOW,
            null,
            LAST_ACTIVITY_AT,
            STARTED_AT,
            NOW
        );
        when(utcClock.now()).thenReturn(NOW);
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq("TESTING_ASSIGNED_ATTEMPT_EXPIRE_TERMINAL"),
            any()
        )).thenReturn(new AuditContext("{\"operationCode\":\"TESTING_ASSIGNED_ATTEMPT_EXPIRE_TERMINAL\"}"));
        when(attemptStatusRecalculationService.refreshAssignedAttemptStatusCacheWithVerdict(ACTOR_USER_ID, 701L, 9007L, NOW))
            .thenReturn(refreshResult(expiredAttempt, TestAttemptStatus.IN_PROGRESS, true));

        AttemptTerminalizationOutcome returned = service.submitAssignedAttempt(ACTOR_USER_ID, 701L, 9007L);

        assertThat(returned.terminalizedAttempt()).isSameAs(expiredAttempt);
        assertThat(returned.attemptId()).isEqualTo(9007L);
        assertThat(returned.actorUserId()).isEqualTo(ACTOR_USER_ID);
        assertThat(returned.attemptMode()).isEqualTo(AttemptMode.ASSIGNED);
        assertThat(returned.terminalStatus()).isEqualTo(TestAttemptStatus.EXPIRED);
        assertThat(returned.terminalizedAt()).isEqualTo(NOW);
        assertThat(returned.reason()).isEqualTo(AttemptTerminalizationReason.EXPIRED_BY_REFRESH);
        assertThat(returned.resultRecordable()).isFalse();
        assertThat(returned.auditEventType())
            .isEqualTo(AttemptTerminalCriticalAuditCatalog.ASSIGNED_ATTEMPT_EXPIRED.auditEventType());
        assertThat(returned.terminalizedAttempt().completedAt()).isNull();
        assertThat(returned.terminalizedAttempt().expiredAt()).isEqualTo(NOW);
        verify(utcClock).now();
        verify(attemptStatusRecalculationService)
            .refreshAssignedAttemptStatusCacheWithVerdict(ACTOR_USER_ID, 701L, 9007L, NOW);
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verify(criticalCommandAuditSupport).buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq("TESTING_ASSIGNED_ATTEMPT_EXPIRE_TERMINAL"),
            any()
        );
        verify(criticalCommandAuditSupport).recordAudit(
            org.mockito.ArgumentMatchers.eq(ACTOR_USER_ID),
            org.mockito.ArgumentMatchers.eq(AttemptTerminalCriticalAuditCatalog.ASSIGNED_ATTEMPT_EXPIRED.auditEventType()),
            org.mockito.ArgumentMatchers.eq("test_attempt"),
            org.mockito.ArgumentMatchers.eq(9007L),
            any(),
            any(),
            any(AuditContext.class)
        );
        verifyNoInteractions(resultRecordingService);
    }

    @Test
    void assignedSubmitAfterDeadlineTerminalizesAttemptAsExpired() {
        TestAttempt expiredAttempt = expiredAssignedAttempt(9015L);
        when(utcClock.now()).thenReturn(NOW);
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq("TESTING_ASSIGNED_ATTEMPT_EXPIRE_TERMINAL"),
            any()
        )).thenReturn(new AuditContext("{\"operationCode\":\"TESTING_ASSIGNED_ATTEMPT_EXPIRE_TERMINAL\"}"));
        when(attemptStatusRecalculationService.refreshAssignedAttemptStatusCacheWithVerdict(ACTOR_USER_ID, 701L, 9015L, NOW))
            .thenReturn(refreshResult(expiredAttempt, TestAttemptStatus.IN_PROGRESS, true));

        AttemptTerminalizationOutcome returned = service.submitAssignedAttempt(ACTOR_USER_ID, 701L, 9015L);

        assertThat(returned.terminalizedAttempt()).isSameAs(expiredAttempt);
        assertThat(returned.terminalStatus()).isEqualTo(TestAttemptStatus.EXPIRED);
        assertThat(returned.reason()).isEqualTo(AttemptTerminalizationReason.EXPIRED_BY_REFRESH);
        assertThat(returned.resultRecordable()).isFalse();
        assertThat(returned.auditEventType())
            .isEqualTo(AttemptTerminalCriticalAuditCatalog.ASSIGNED_ATTEMPT_EXPIRED.auditEventType());
        assertThat(returned.terminalizedAttempt().completedAt()).isNull();
        assertThat(returned.terminalizedAttempt().expiredAt()).isEqualTo(NOW);
        assertThat(returned.terminalizedAttempt().abandonedAt()).isNull();
        verify(utcClock).now();
        verify(attemptStatusRecalculationService)
            .refreshAssignedAttemptStatusCacheWithVerdict(ACTOR_USER_ID, 701L, 9015L, NOW);
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verify(criticalCommandAuditSupport).buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq("TESTING_ASSIGNED_ATTEMPT_EXPIRE_TERMINAL"),
            any()
        );
        verify(criticalCommandAuditSupport).recordAudit(
            org.mockito.ArgumentMatchers.eq(ACTOR_USER_ID),
            org.mockito.ArgumentMatchers.eq(AttemptTerminalCriticalAuditCatalog.ASSIGNED_ATTEMPT_EXPIRED.auditEventType()),
            org.mockito.ArgumentMatchers.eq("test_attempt"),
            org.mockito.ArgumentMatchers.eq(9015L),
            any(),
            any(),
            any(AuditContext.class)
        );
    }

    @Test
    void assignedSubmitExpiredByDeadlineDoesNotRecordResult() {
        TestAttempt expiredAttempt = expiredAssignedAttempt(9016L);
        when(utcClock.now()).thenReturn(NOW);
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq("TESTING_ASSIGNED_ATTEMPT_EXPIRE_TERMINAL"),
            any()
        )).thenReturn(new AuditContext("{\"operationCode\":\"TESTING_ASSIGNED_ATTEMPT_EXPIRE_TERMINAL\"}"));
        when(attemptStatusRecalculationService.refreshAssignedAttemptStatusCacheWithVerdict(ACTOR_USER_ID, 701L, 9016L, NOW))
            .thenReturn(refreshResult(expiredAttempt, TestAttemptStatus.IN_PROGRESS, true));

        AttemptTerminalizationOutcome returned = service.submitAssignedAttempt(ACTOR_USER_ID, 701L, 9016L);

        assertThat(returned.terminalizedAttempt()).isSameAs(expiredAttempt);
        assertThat(returned.terminalStatus()).isEqualTo(TestAttemptStatus.EXPIRED);
        assertThat(returned.resultRecordable()).isFalse();
        verify(utcClock).now();
        verify(attemptStatusRecalculationService)
            .refreshAssignedAttemptStatusCacheWithVerdict(ACTOR_USER_ID, 701L, 9016L, NOW);
        verifyNoInteractions(resultRecordingService);
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verify(criticalCommandAuditSupport).buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq("TESTING_ASSIGNED_ATTEMPT_EXPIRE_TERMINAL"),
            any()
        );
        verify(criticalCommandAuditSupport).recordAudit(
            org.mockito.ArgumentMatchers.eq(ACTOR_USER_ID),
            org.mockito.ArgumentMatchers.eq(AttemptTerminalCriticalAuditCatalog.ASSIGNED_ATTEMPT_EXPIRED.auditEventType()),
            org.mockito.ArgumentMatchers.eq("test_attempt"),
            org.mockito.ArgumentMatchers.eq(9016L),
            any(),
            any(),
            any(AuditContext.class)
        );
    }

    @Test
    void submitAfterDeadlineShouldReturnExpiredTerminalStateWithoutCompletingAttempt() {
        TestAttempt expiredAttempt = expiredAssignedAttempt(9010L);
        when(utcClock.now()).thenReturn(NOW);
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq("TESTING_ASSIGNED_ATTEMPT_EXPIRE_TERMINAL"),
            any()
        )).thenReturn(new AuditContext("{\"operationCode\":\"TESTING_ASSIGNED_ATTEMPT_EXPIRE_TERMINAL\"}"));
        when(attemptStatusRecalculationService.refreshAssignedAttemptStatusCacheWithVerdict(ACTOR_USER_ID, 701L, 9010L, NOW))
            .thenReturn(refreshResult(expiredAttempt, TestAttemptStatus.STARTED, true));

        AttemptTerminalizationOutcome returned = service.submitAssignedAttempt(ACTOR_USER_ID, 701L, 9010L);

        assertThat(returned.terminalizedAttempt()).isSameAs(expiredAttempt);
        assertThat(returned.terminalStatus()).isEqualTo(TestAttemptStatus.EXPIRED);
        assertThat(returned.resultRecordable()).isFalse();
        assertThat(returned.terminalizedAttempt().completedAt()).isNull();
        assertThat(returned.terminalizedAttempt().expiredAt()).isEqualTo(NOW);
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verifyNoInteractions(resultRecordingService);
        verify(criticalCommandAuditSupport).buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq("TESTING_ASSIGNED_ATTEMPT_EXPIRE_TERMINAL"),
            any()
        );
        verify(criticalCommandAuditSupport).recordAudit(
            org.mockito.ArgumentMatchers.eq(ACTOR_USER_ID),
            org.mockito.ArgumentMatchers.eq(AttemptTerminalCriticalAuditCatalog.ASSIGNED_ATTEMPT_EXPIRED.auditEventType()),
            org.mockito.ArgumentMatchers.eq("test_attempt"),
            org.mockito.ArgumentMatchers.eq(9010L),
            any(),
            any(),
            any(AuditContext.class)
        );
    }

    @Test
    void submitAfterDeadlineMustNotBecomeCompletedSuccessOrCountedSuccessPath() {
        TestAttempt expiredAttempt = expiredAssignedAttempt(9011L);
        when(utcClock.now()).thenReturn(NOW);
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq("TESTING_ASSIGNED_ATTEMPT_EXPIRE_TERMINAL"),
            any()
        )).thenReturn(new AuditContext("{\"operationCode\":\"TESTING_ASSIGNED_ATTEMPT_EXPIRE_TERMINAL\"}"));
        when(attemptStatusRecalculationService.refreshAssignedAttemptStatusCacheWithVerdict(ACTOR_USER_ID, 701L, 9011L, NOW))
            .thenReturn(refreshResult(expiredAttempt, TestAttemptStatus.IN_PROGRESS, true));

        AttemptTerminalizationOutcome returned = service.submitAssignedAttempt(ACTOR_USER_ID, 701L, 9011L);

        assertThat(returned.terminalStatus()).isEqualTo(TestAttemptStatus.EXPIRED);
        assertThat(returned.resultRecordable()).isFalse();
        verify(testAttemptRepository, never()).saveTestAttempt(org.mockito.ArgumentMatchers.argThat(attempt ->
            attempt != null && attempt.status() == TestAttemptStatus.COMPLETED
        ));
        verifyNoInteractions(resultRecordingService);
        verify(criticalCommandAuditSupport).recordAudit(
            org.mockito.ArgumentMatchers.eq(ACTOR_USER_ID),
            org.mockito.ArgumentMatchers.eq(AttemptTerminalCriticalAuditCatalog.ASSIGNED_ATTEMPT_EXPIRED.auditEventType()),
            org.mockito.ArgumentMatchers.eq("test_attempt"),
            org.mockito.ArgumentMatchers.eq(9011L),
            any(),
            any(),
            any(AuditContext.class)
        );
    }

    @Test
    void alreadyCompletedAssignedAttemptBeforeSubmitRemainsFailClosedWithoutResultOrSuccessAudit() {
        TestAttempt completedAttempt = assignedAttempt(9012L, ACTOR_USER_ID, 501L, 701L, TestAttemptStatus.COMPLETED);
        when(utcClock.now()).thenReturn(NOW);
        when(attemptStatusRecalculationService.refreshAssignedAttemptStatusCacheWithVerdict(ACTOR_USER_ID, 701L, 9012L, NOW))
            .thenReturn(refreshResult(completedAttempt, TestAttemptStatus.COMPLETED, false));

        assertThatThrownBy(() -> service.submitAssignedAttempt(ACTOR_USER_ID, 701L, 9012L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("non-active attempt")
            .hasMessageContaining("status=COMPLETED");

        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verifyNoInteractions(resultRecordingService);
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(String.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void alreadyAbandonedAssignedAttemptBeforeSubmitRemainsFailClosedWithoutResultOrSuccessAudit() {
        TestAttempt abandonedAttempt = assignedAttempt(9013L, ACTOR_USER_ID, 501L, 701L, TestAttemptStatus.ABANDONED);
        when(utcClock.now()).thenReturn(NOW);
        when(attemptStatusRecalculationService.refreshAssignedAttemptStatusCacheWithVerdict(ACTOR_USER_ID, 701L, 9013L, NOW))
            .thenReturn(refreshResult(abandonedAttempt, TestAttemptStatus.ABANDONED, false));

        assertThatThrownBy(() -> service.submitAssignedAttempt(ACTOR_USER_ID, 701L, 9013L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("non-active attempt")
            .hasMessageContaining("status=ABANDONED");

        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verifyNoInteractions(resultRecordingService);
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(String.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void submitTerminalServiceWillHandleExpiredAfterRefreshAsExplicitBranchInsteadOfGenericConflict() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/testing/service/AssignedAttemptSubmitTerminalService.java"
        ));
        String outcomeSource = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/testing/service/AttemptTerminalizationOutcome.java"
        ));
        int refreshIndex = source.indexOf("refreshAssignedAttemptStatusCacheWithVerdict(");
        int requireIndex = source.indexOf("requireSubmittableAssignedAttempt(");
        int expiredIndex = source.indexOf("expiredByThisRefresh()", refreshIndex);
        int saveIndex = source.indexOf("saveTestAttempt(new TestAttempt(", requireIndex);
        int auditIndex = source.indexOf("recordSubmitAudit(", saveIndex);

        assertThat(refreshIndex).isGreaterThanOrEqualTo(0);
        assertThat(requireIndex).isGreaterThan(refreshIndex);
        assertThat(expiredIndex)
            
            .isGreaterThan(refreshIndex)
            .isLessThan(requireIndex);
        assertThat(saveIndex)
            
            .isGreaterThan(requireIndex);
        assertThat(auditIndex)
            
            .isGreaterThan(saveIndex);
        assertThat(source)
            .contains("AttemptTerminalizationOutcome")
            .doesNotContain("ResultRecordingService")
            .doesNotContain(".recordResult(")
            .doesNotContain("AssignmentCountedResultHandoffService");
        assertThat(outcomeSource)
            .contains("AttemptTerminalizationReason")
            .contains("resultRecordable")
            .doesNotContain("countedHandoffEligible")
            .contains("AuditEventType")
            .contains("auditEventType")
            .contains("Objects.requireNonNull(auditEventType");
    }

    @Test
    void expiredSubmitPathDoesNotRollbackOrDeletePreviouslySavedAnswerFacts() throws IOException {
        String terminalSource = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/testing/service/AssignedAttemptSubmitTerminalService.java"
        ));
        String sequencingSource = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/testing/service/AssignedAttemptSubmitSequencingService.java"
        ));
        String recalculationSource = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/testing/service/AttemptStatusRecalculationServiceImpl.java"
        ));

        assertThat(terminalSource + sequencingSource + recalculationSource)
            
            .doesNotContain(
                "ActiveAttemptAnswerMutationService",
                "UserAnswerRepository",
                "UserAnswerItemRepository",
                "deleteUserAnswerItemsByUserAnswerId(",
                "clearAnswer(",
                "saveOrReplaceAnswer(",
                "removeAnswer(",
                "rollback"
            );
    }

    @Test
    void expiredSubmitAuditCompanionRemainsAssignedActorAndAnchorScoped() throws IOException {
        String expirySource = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/testing/service/AssignedAttemptExpiryTerminalService.java"
        ));
        String outcomeSource = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/testing/service/AttemptTerminalizationOutcome.java"
        ));

        assertThat(outcomeSource)
            .contains("expiredByRefresh(Long actorUserId, TestAttempt terminalizedAttempt)")
            .contains("AttemptTerminalCriticalAuditCatalog.ASSIGNED_ATTEMPT_EXPIRED.auditEventType()")
            .contains("assignedExplicitExpiry(Long actorUserId, TestAttempt terminalizedAttempt)")
            .contains("false,");
        assertThat(expirySource)
            
            .contains("resolveSystemActorUserId(systemActorResolver)")
            .contains("AttemptTerminalCriticalAuditCatalog.ASSIGNED_ATTEMPT_EXPIRED")
            .contains("createAssignedExpiryDetails(after.assignmentTestId(), after.testId())")
            .contains("AttemptTerminalizationOutcome.assignedExplicitExpiry(")
            .doesNotContain("countedHandoffEligible")
            .doesNotContain("terminalizationOutcome.resultRecordable()")
            .doesNotContain("resultRecordingService.recordResult(");
    }

    @Test
    void whenRefreshKeepsAttemptActiveSubmitStillCompletesAndAuditsNormally() {
        TestAttempt activeAttempt = assignedAttempt(9008L, 101L, 501L, 701L, TestAttemptStatus.STARTED);
        when(utcClock.now()).thenReturn(NOW);
        when(attemptStatusRecalculationService.refreshAssignedAttemptStatusCacheWithVerdict(ACTOR_USER_ID, 701L, 9008L, NOW))
            .thenReturn(refreshResult(activeAttempt, TestAttemptStatus.STARTED, false));
        when(testAttemptRepository.saveTestAttempt(any(TestAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq("TESTING_ASSIGNED_ATTEMPT_SUBMIT_TERMINAL"),
            any()
        )).thenReturn(new AuditContext("{\"operationCode\":\"TESTING_ASSIGNED_ATTEMPT_SUBMIT_TERMINAL\"}"));

        AttemptTerminalizationOutcome completedOutcome = service.submitAssignedAttempt(ACTOR_USER_ID, 701L, 9008L);
        TestAttempt completedAttempt = completedOutcome.terminalizedAttempt();

        assertThat(completedOutcome.resultRecordable()).isTrue();
        assertThat(completedOutcome.reason()).isEqualTo(AttemptTerminalizationReason.NORMAL_SUBMIT);
        assertThat(completedOutcome.auditEventType())
            .isEqualTo(AttemptTerminalCriticalAuditCatalog.ASSIGNED_ATTEMPT_SUBMITTED.auditEventType());
        assertThat(completedAttempt.status()).isEqualTo(TestAttemptStatus.COMPLETED);
        assertThat(completedAttempt.completedAt()).isEqualTo(NOW);
        assertThat(completedAttempt.lastActivityAt()).isEqualTo(NOW);

        InOrder inOrder = inOrder(
            utcClock,
            attemptStatusRecalculationService,
            testAttemptRepository
        );
        inOrder.verify(utcClock).now();
        inOrder.verify(attemptStatusRecalculationService)
            .refreshAssignedAttemptStatusCacheWithVerdict(ACTOR_USER_ID, 701L, 9008L, NOW);
        inOrder.verify(testAttemptRepository).saveTestAttempt(any(TestAttempt.class));
        verify(criticalCommandAuditSupport).recordAudit(
            org.mockito.ArgumentMatchers.eq(ACTOR_USER_ID),
            any(),
            org.mockito.ArgumentMatchers.eq("test_attempt"),
            org.mockito.ArgumentMatchers.eq(9008L),
            any(),
            any(),
            any(AuditContext.class)
        );
    }

    @Test
    void expiredBranchAfterRefreshDoesNotSaveCompletedAttemptAndDoesNotRecordSuccessAudit() {
        TestAttempt expiredAttempt = new TestAttempt(
            9009L,
            101L,
            501L,
            701L,
            AttemptMode.ASSIGNED,
            TestAttemptStatus.EXPIRED,
            STARTED_AT,
            null,
            NOW,
            null,
            LAST_ACTIVITY_AT,
            STARTED_AT,
            NOW
        );
        when(utcClock.now()).thenReturn(NOW);
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq("TESTING_ASSIGNED_ATTEMPT_EXPIRE_TERMINAL"),
            any()
        )).thenReturn(new AuditContext("{\"operationCode\":\"TESTING_ASSIGNED_ATTEMPT_EXPIRE_TERMINAL\"}"));
        when(attemptStatusRecalculationService.refreshAssignedAttemptStatusCacheWithVerdict(ACTOR_USER_ID, 701L, 9009L, NOW))
            .thenReturn(refreshResult(expiredAttempt, TestAttemptStatus.STARTED, true));

        AttemptTerminalizationOutcome returned = service.submitAssignedAttempt(ACTOR_USER_ID, 701L, 9009L);

        assertThat(returned.terminalizedAttempt()).isSameAs(expiredAttempt);
        assertThat(returned.terminalStatus()).isEqualTo(TestAttemptStatus.EXPIRED);
        assertThat(returned.resultRecordable()).isFalse();
        assertThat(returned.auditEventType())
            .isEqualTo(AttemptTerminalCriticalAuditCatalog.ASSIGNED_ATTEMPT_EXPIRED.auditEventType());
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verify(criticalCommandAuditSupport).buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Testing"),
            org.mockito.ArgumentMatchers.eq("TESTING_ASSIGNED_ATTEMPT_EXPIRE_TERMINAL"),
            any()
        );
        verify(criticalCommandAuditSupport).recordAudit(
            org.mockito.ArgumentMatchers.eq(ACTOR_USER_ID),
            org.mockito.ArgumentMatchers.eq(AttemptTerminalCriticalAuditCatalog.ASSIGNED_ATTEMPT_EXPIRED.auditEventType()),
            org.mockito.ArgumentMatchers.eq("test_attempt"),
            org.mockito.ArgumentMatchers.eq(9009L),
            any(),
            any(),
            any(AuditContext.class)
        );
        verifyNoInteractions(resultRecordingService);
    }

    @Test
    void alreadyExpiredAssignedAttemptBeforeSubmitFailsClosedWithoutSuccessAuditOrCompletedSave() {
        TestAttempt expiredAttempt = expiredAssignedAttempt(9014L);
        when(utcClock.now()).thenReturn(NOW);
        when(attemptStatusRecalculationService.refreshAssignedAttemptStatusCacheWithVerdict(ACTOR_USER_ID, 701L, 9014L, NOW))
            .thenReturn(refreshResult(expiredAttempt, TestAttemptStatus.EXPIRED, false));

        assertThatThrownBy(() -> service.submitAssignedAttempt(ACTOR_USER_ID, 701L, 9014L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("already expired attempt")
            .hasMessageContaining("status=EXPIRED");

        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(String.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
        verifyNoInteractions(resultRecordingService);
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
            null,
            null,
            LAST_ACTIVITY_AT,
            STARTED_AT,
            LAST_ACTIVITY_AT
        );
    }

    private TestAttempt expiredAssignedAttempt(Long id) {
        return new TestAttempt(
            id,
            ACTOR_USER_ID,
            501L,
            701L,
            AttemptMode.ASSIGNED,
            TestAttemptStatus.EXPIRED,
            STARTED_AT,
            null,
            NOW,
            null,
            LAST_ACTIVITY_AT,
            STARTED_AT,
            NOW
        );
    }

    private AttemptStatusRecalculationService.AttemptStatusRefreshResult refreshResult(
        TestAttempt refreshedAttempt,
        TestAttemptStatus previousStatus,
        boolean expiredByThisRefresh
    ) {
        return new AttemptStatusRecalculationService.AttemptStatusRefreshResult(
            refreshedAttempt,
            previousStatus,
            expiredByThisRefresh
        );
    }
}
