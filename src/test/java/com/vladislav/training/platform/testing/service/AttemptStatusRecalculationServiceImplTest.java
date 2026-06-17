package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import com.vladislav.training.platform.assignment.domain.AssignmentTestRole;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.repository.AssignmentRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentTestRepository;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import com.vladislav.training.platform.testing.repository.TestAttemptRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code AttemptStatusRecalculationServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class AttemptStatusRecalculationServiceImplTest {

    private static final Instant ASSIGNED_AT = Instant.parse("2026-04-19T09:00:00Z");
    private static final Instant DEADLINE_AT = Instant.parse("2026-04-19T10:00:00Z");
    private static final Instant EFFECTIVE_AT = Instant.parse("2026-04-19T11:00:00Z");

    @Mock
    private TestAttemptRepository testAttemptRepository;
    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private AssignmentTestRepository assignmentTestRepository;
    @Mock
    private ResultRecordingService resultRecordingService;

    private AttemptStatusRecalculationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AttemptStatusRecalculationServiceImpl(
            testAttemptRepository,
            assignmentRepository,
            assignmentTestRepository
        );
    }

    @Test
    void recalculatesAssignedActiveAttemptToExpiredWhenDeadlineIsAlreadyLost() {
        TestAttempt activeAttempt = assignedAttempt(9401L, TestAttemptStatus.IN_PROGRESS);
        when(testAttemptRepository.findTestAttemptById(9401L)).thenReturn(activeAttempt);
        when(assignmentTestRepository.findAssignmentTestById(701L)).thenReturn(openAssignmentTest());
        when(assignmentRepository.findAssignmentById(77L)).thenReturn(expiredAssignment(null, null));

        TestAttemptStatus status = service.recalculateAttemptStatus(9401L, EFFECTIVE_AT);

        assertThat(status).isEqualTo(TestAttemptStatus.EXPIRED);
        verify(testAttemptRepository).findTestAttemptById(9401L);
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verifyNoInteractions(resultRecordingService);
    }

    @Test
    void keepsAssignedActiveAttemptStatusWhenServerSideAdmissibilityStillHolds() {
        TestAttempt activeAttempt = assignedAttempt(9402L, TestAttemptStatus.STARTED);
        when(testAttemptRepository.findTestAttemptById(9402L)).thenReturn(activeAttempt);
        when(assignmentTestRepository.findAssignmentTestById(701L)).thenReturn(openAssignmentTest());
        when(assignmentRepository.findAssignmentById(77L)).thenReturn(activeAssignment());

        TestAttemptStatus status = service.recalculateAttemptStatus(9402L, ASSIGNED_AT.plusSeconds(60));

        assertThat(status).isEqualTo(TestAttemptStatus.STARTED);
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verifyNoInteractions(resultRecordingService);
    }

    @Test
    void refreshMaterializesPassiveAssignedExpiryIntoAttemptCache() {
        TestAttempt activeAttempt = assignedAttempt(9403L, TestAttemptStatus.IN_PROGRESS);
        when(testAttemptRepository.findAndLockTestAttemptById(9403L)).thenReturn(activeAttempt);
        when(assignmentTestRepository.findAssignmentTestById(701L)).thenReturn(openAssignmentTest());
        when(assignmentRepository.findAssignmentById(77L)).thenReturn(expiredAssignment(null, null));
        when(testAttemptRepository.saveTestAttempt(any(TestAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TestAttempt refreshed = service.refreshAttemptStatusCache(9403L, EFFECTIVE_AT);

        assertThat(refreshed.status()).isEqualTo(TestAttemptStatus.EXPIRED);
        assertThat(refreshed.expiredAt()).isEqualTo(EFFECTIVE_AT);
        assertThat(refreshed.completedAt()).isNull();
        assertThat(refreshed.abandonedAt()).isNull();
        assertThat(refreshed.updatedAt()).isEqualTo(EFFECTIVE_AT);
        verify(testAttemptRepository).saveTestAttempt(any(TestAttempt.class));
        verifyNoInteractions(resultRecordingService);
    }

    @Test
    void refreshReturnsCurrentAttemptWhenNoStatusChangeIsRequired() {
        TestAttempt selfAttempt = selfAttempt(9404L, TestAttemptStatus.IN_PROGRESS);
        when(testAttemptRepository.findAndLockTestAttemptById(9404L)).thenReturn(selfAttempt);

        TestAttempt refreshed = service.refreshAttemptStatusCache(9404L, EFFECTIVE_AT);

        assertThat(refreshed).isSameAs(selfAttempt);
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verifyNoInteractions(assignmentRepository, assignmentTestRepository, resultRecordingService);
    }

    @Test
    void actorScopedRefreshVerdictMarksExpiryOnlyWhenRefreshMaterializesExpiredStatus() {
        TestAttempt activeAttempt = assignedAttempt(9407L, TestAttemptStatus.STARTED);
        when(testAttemptRepository.findAndLockTestAttemptByIdAndUserId(9407L, 101L)).thenReturn(activeAttempt);
        when(assignmentTestRepository.findAssignmentTestById(701L)).thenReturn(openAssignmentTest());
        when(assignmentRepository.findAssignmentById(77L)).thenReturn(expiredAssignment(null, null));
        when(testAttemptRepository.saveTestAttempt(any(TestAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AttemptStatusRecalculationService.AttemptStatusRefreshResult verdict =
            service.refreshAttemptStatusCacheWithVerdict(101L, 9407L, EFFECTIVE_AT);

        assertThat(verdict.previousStatus()).isEqualTo(TestAttemptStatus.STARTED);
        assertThat(verdict.expiredByThisRefresh()).isTrue();
        assertThat(verdict.refreshedAttempt().status()).isEqualTo(TestAttemptStatus.EXPIRED);
        verify(testAttemptRepository).saveTestAttempt(any(TestAttempt.class));
    }

    @Test
    void assignedScopedRefreshVerdictUsesActorAndAssignmentAnchorScopedLookup() {
        TestAttempt activeAttempt = assignedAttempt(9410L, TestAttemptStatus.STARTED);
        when(testAttemptRepository.findAndLockAssignedAttemptByIdAndUserIdAndAssignmentTestId(9410L, 101L, 701L))
            .thenReturn(activeAttempt);
        when(assignmentTestRepository.findAssignmentTestById(701L)).thenReturn(openAssignmentTest());
        when(assignmentRepository.findAssignmentById(77L)).thenReturn(expiredAssignment(null, null));
        when(testAttemptRepository.saveTestAttempt(any(TestAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AttemptStatusRecalculationService.AttemptStatusRefreshResult verdict =
            service.refreshAssignedAttemptStatusCacheWithVerdict(101L, 701L, 9410L, EFFECTIVE_AT);

        assertThat(verdict.previousStatus()).isEqualTo(TestAttemptStatus.STARTED);
        assertThat(verdict.expiredByThisRefresh()).isTrue();
        assertThat(verdict.refreshedAttempt().status()).isEqualTo(TestAttemptStatus.EXPIRED);
        verify(testAttemptRepository).findAndLockAssignedAttemptByIdAndUserIdAndAssignmentTestId(9410L, 101L, 701L);
        verify(testAttemptRepository, never()).findAndLockTestAttemptByIdAndUserId(9410L, 101L);
        verify(testAttemptRepository, never()).findAndLockTestAttemptById(9410L);
        verify(testAttemptRepository, never()).findAndLockActiveAssignedAttemptForActor(101L, 701L);
        verify(testAttemptRepository, never()).findAndLockActiveSelfAttempt(101L, 501L);
        verify(testAttemptRepository).saveTestAttempt(any(TestAttempt.class));
    }

    @Test
    void assignedScopedRefreshVerdictRejectsNonAssignedAttemptBeforeStatusMutation() {
        TestAttempt selfAttempt = new TestAttempt(
            9411L,
            101L,
            501L,
            701L,
            AttemptMode.SELF,
            TestAttemptStatus.STARTED,
            ASSIGNED_AT,
            null,
            null,
            null,
            ASSIGNED_AT.plusSeconds(300),
            ASSIGNED_AT,
            ASSIGNED_AT.plusSeconds(300)
        );
        when(testAttemptRepository.findAndLockAssignedAttemptByIdAndUserIdAndAssignmentTestId(9411L, 101L, 701L))
            .thenReturn(selfAttempt);

        assertThatThrownBy(() -> service.refreshAssignedAttemptStatusCacheWithVerdict(101L, 701L, 9411L, EFFECTIVE_AT))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("ASSIGNED attempt mode");

        verify(testAttemptRepository).findAndLockAssignedAttemptByIdAndUserIdAndAssignmentTestId(9411L, 101L, 701L);
        verify(testAttemptRepository, never()).saveTestAttempt(any(TestAttempt.class));
        verifyNoInteractions(assignmentRepository, assignmentTestRepository, resultRecordingService);
    }

    @Test
    void actorScopedRefreshVerdictKeepsExpiredByThisRefreshFalseForAlreadyTerminalAttempt() {
        TestAttempt expiredAttempt = assignedAttempt(9408L, TestAttemptStatus.EXPIRED);
        when(testAttemptRepository.findAndLockTestAttemptByIdAndUserId(9408L, 101L)).thenReturn(expiredAttempt);

        AttemptStatusRecalculationService.AttemptStatusRefreshResult verdict =
            service.refreshAttemptStatusCacheWithVerdict(101L, 9408L, EFFECTIVE_AT);

        assertThat(verdict.previousStatus()).isEqualTo(TestAttemptStatus.EXPIRED);
        assertThat(verdict.expiredByThisRefresh()).isFalse();
        assertThat(verdict.refreshedAttempt()).isSameAs(expiredAttempt);
        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verifyNoInteractions(assignmentRepository, assignmentTestRepository, resultRecordingService);
    }

    @Test
    void failsClosedWhenAssignedAnchorDoesNotMatchMaterializedAttemptFacts() {
        TestAttempt inconsistentAttempt = assignedAttempt(9405L, TestAttemptStatus.STARTED);
        when(testAttemptRepository.findTestAttemptById(9405L)).thenReturn(inconsistentAttempt);
        when(assignmentTestRepository.findAssignmentTestById(701L)).thenReturn(openAssignmentTest());
        when(assignmentRepository.findAssignmentById(77L)).thenReturn(new Assignment(
            77L,
            66L,
            999L,
            42L,
            AssignmentStatus.ASSIGNED,
            ASSIGNED_AT,
            DEADLINE_AT,
            null,
            null,
            ASSIGNED_AT,
            ASSIGNED_AT
        ));

        assertThatThrownBy(() -> service.recalculateAttemptStatus(9405L, EFFECTIVE_AT))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("user does not match");

        verify(testAttemptRepository, never()).saveTestAttempt(any());
        verifyNoInteractions(resultRecordingService);
    }

    @Test
    void keepsTerminalStatesAsOwnerFactsWithoutReopeningThem() {
        TestAttempt terminalAttempt = assignedAttempt(9406L, TestAttemptStatus.COMPLETED);
        when(testAttemptRepository.findTestAttemptById(9406L)).thenReturn(terminalAttempt);

        TestAttemptStatus status = service.recalculateAttemptStatus(9406L, EFFECTIVE_AT);

        assertThat(status).isEqualTo(TestAttemptStatus.COMPLETED);
        verifyNoInteractions(assignmentRepository, assignmentTestRepository, resultRecordingService);
        verify(testAttemptRepository, never()).saveTestAttempt(any());
    }

    private TestAttempt assignedAttempt(Long id, TestAttemptStatus status) {
        return new TestAttempt(
            id,
            101L,
            501L,
            701L,
            AttemptMode.ASSIGNED,
            status,
            ASSIGNED_AT,
            status == TestAttemptStatus.COMPLETED ? EFFECTIVE_AT : null,
            status == TestAttemptStatus.EXPIRED ? EFFECTIVE_AT : null,
            null,
            ASSIGNED_AT.plusSeconds(300),
            ASSIGNED_AT,
            ASSIGNED_AT.plusSeconds(300)
        );
    }

    private TestAttempt selfAttempt(Long id, TestAttemptStatus status) {
        return new TestAttempt(
            id,
            101L,
            501L,
            null,
            AttemptMode.SELF,
            status,
            ASSIGNED_AT,
            null,
            null,
            null,
            ASSIGNED_AT.plusSeconds(300),
            ASSIGNED_AT,
            ASSIGNED_AT.plusSeconds(300)
        );
    }

    private Assignment activeAssignment() {
        return expiredAssignment(null, null);
    }

    private Assignment expiredAssignment(Instant cancelledAt, Instant closedAt) {
        return new Assignment(
            77L,
            66L,
            101L,
            42L,
            AssignmentStatus.ASSIGNED,
            ASSIGNED_AT,
            DEADLINE_AT,
            cancelledAt,
            closedAt,
            ASSIGNED_AT,
            ASSIGNED_AT
        );
    }

    private AssignmentTest openAssignmentTest() {
        return new AssignmentTest(
            701L,
            77L,
            501L,
            AssignmentTestRole.FINAL_TOPIC_CONTROL,
            null,
            null,
            false,
            ASSIGNED_AT,
            ASSIGNED_AT
        );
    }
}
