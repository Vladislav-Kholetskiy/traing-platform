package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import com.vladislav.training.platform.assignment.domain.AssignmentTestRole;
import com.vladislav.training.platform.assignment.repository.AssignmentRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentTestRepository;
import com.vladislav.training.platform.assignment.service.AssignmentStatusDefiningCountedResultFactsReader.StatusDefiningCountedResultFacts;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code AssignmentStatusRecalculationServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class AssignmentStatusRecalculationServiceImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-09T12:00:00Z");

    @Mock private AssignmentRepository assignmentRepository;
    @Mock private AssignmentTestRepository assignmentTestRepository;
    @Mock private AssignmentStatusDefiningCountedResultFactsReader countedResultFactsReader;

    private AssignmentStatusRecalculationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AssignmentStatusRecalculationServiceImpl(
            assignmentRepository,
            assignmentTestRepository,
            countedResultFactsReader
        );
    }

    @Test
    void cancelledHasHighestPriorityEvenWhenCompletionProofExists() {
        when(assignmentRepository.findAssignmentById(41L)).thenReturn(assignment(
            41L,
            AssignmentStatus.ASSIGNED,
            FIXED_INSTANT.plusSeconds(3600),
            FIXED_INSTANT.minusSeconds(30)
        ));

        AssignmentStatus status = service.recalculateAssignmentStatus(41L, FIXED_INSTANT);

        assertThat(status).isEqualTo(AssignmentStatus.CANCELLED);
        verify(assignmentTestRepository, never()).findAssignmentTestsByAssignmentId(41L);
        verify(countedResultFactsReader, never()).findStatusDefiningFactsByCountedResultId(any());
    }

    @Test
    void completedWhenAllAssignmentTestsHaveCanonicalCountedResultProof() {
        when(assignmentRepository.findAssignmentById(42L)).thenReturn(assignment(
            42L,
            AssignmentStatus.ASSIGNED,
            FIXED_INSTANT.plusSeconds(3600),
            null
        ));
        when(assignmentTestRepository.findAssignmentTestsByAssignmentId(42L)).thenReturn(List.of(
            closedTest(42L, 7002L),
            closedTest(42L, 7003L)
        ));
        when(countedResultFactsReader.findStatusDefiningFactsByCountedResultId(7002L))
            .thenReturn(provedCompletionFacts());
        when(countedResultFactsReader.findStatusDefiningFactsByCountedResultId(7003L))
            .thenReturn(provedCompletionFacts());

        AssignmentStatus status = service.recalculateAssignmentStatus(42L, FIXED_INSTANT);

        assertThat(status).isEqualTo(AssignmentStatus.COMPLETED);
    }

    @Test
    void missingCountedResultIdPreventsCompleted() {
        when(assignmentRepository.findAssignmentById(43L)).thenReturn(assignment(
            43L,
            AssignmentStatus.ASSIGNED,
            FIXED_INSTANT.plusSeconds(3600),
            null
        ));
        when(assignmentTestRepository.findAssignmentTestsByAssignmentId(43L)).thenReturn(List.of(
            closedTest(43L, null)
        ));

        AssignmentStatus status = service.recalculateAssignmentStatus(43L, FIXED_INSTANT);

        assertThat(status).isEqualTo(AssignmentStatus.ASSIGNED);
        verify(countedResultFactsReader, never()).findStatusDefiningFactsByCountedResultId(any());
    }

    @Test
    void failedCountedResultPreventsCompleted() {
        when(assignmentRepository.findAssignmentById(44L)).thenReturn(assignment(
            44L,
            AssignmentStatus.ASSIGNED,
            FIXED_INSTANT.plusSeconds(3600),
            null
        ));
        when(assignmentTestRepository.findAssignmentTestsByAssignmentId(44L)).thenReturn(List.of(
            closedTest(44L, 7004L)
        ));
        when(countedResultFactsReader.findStatusDefiningFactsByCountedResultId(7004L))
            .thenReturn(new StatusDefiningCountedResultFacts(false, true, true));

        AssignmentStatus status = service.recalculateAssignmentStatus(44L, FIXED_INSTANT);

        assertThat(status).isEqualTo(AssignmentStatus.ASSIGNED);
    }

    @Test
    void countedResultOutsideDeadlinePreventsCompleted() {
        when(assignmentRepository.findAssignmentById(45L)).thenReturn(assignment(
            45L,
            AssignmentStatus.ASSIGNED,
            FIXED_INSTANT.plusSeconds(3600),
            null
        ));
        when(assignmentTestRepository.findAssignmentTestsByAssignmentId(45L)).thenReturn(List.of(
            closedTest(45L, 7005L)
        ));
        when(countedResultFactsReader.findStatusDefiningFactsByCountedResultId(7005L))
            .thenReturn(new StatusDefiningCountedResultFacts(true, false, true));

        AssignmentStatus status = service.recalculateAssignmentStatus(45L, FIXED_INSTANT);

        assertThat(status).isEqualTo(AssignmentStatus.ASSIGNED);
    }

    @Test
    void countedResultNotCountedInAssignmentPreventsCompleted() {
        when(assignmentRepository.findAssignmentById(46L)).thenReturn(assignment(
            46L,
            AssignmentStatus.ASSIGNED,
            FIXED_INSTANT.plusSeconds(3600),
            null
        ));
        when(assignmentTestRepository.findAssignmentTestsByAssignmentId(46L)).thenReturn(List.of(
            closedTest(46L, 7006L)
        ));
        when(countedResultFactsReader.findStatusDefiningFactsByCountedResultId(7006L))
            .thenReturn(new StatusDefiningCountedResultFacts(true, true, false));

        AssignmentStatus status = service.recalculateAssignmentStatus(46L, FIXED_INSTANT);

        assertThat(status).isEqualTo(AssignmentStatus.ASSIGNED);
    }

    @Test
    void overdueWhenDeadlinePassedAndCompletionIsIncomplete() {
        when(assignmentRepository.findAssignmentById(47L)).thenReturn(assignment(
            47L,
            AssignmentStatus.ASSIGNED,
            FIXED_INSTANT.minusSeconds(60),
            null
        ));
        when(assignmentTestRepository.findAssignmentTestsByAssignmentId(47L)).thenReturn(List.of(
            closedTest(47L, 7007L)
        ));
        when(countedResultFactsReader.findStatusDefiningFactsByCountedResultId(7007L))
            .thenReturn(new StatusDefiningCountedResultFacts(true, false, true));

        AssignmentStatus status = service.recalculateAssignmentStatus(47L, FIXED_INSTANT);

        assertThat(status).isEqualTo(AssignmentStatus.OVERDUE);
    }

    @Test
    void assignedWhenDeadlineNotPassedAndCompletionIsIncomplete() {
        when(assignmentRepository.findAssignmentById(48L)).thenReturn(assignment(
            48L,
            AssignmentStatus.ASSIGNED,
            FIXED_INSTANT.plusSeconds(600),
            null
        ));
        when(assignmentTestRepository.findAssignmentTestsByAssignmentId(48L)).thenReturn(List.of(
            openTest(48L)
        ));

        AssignmentStatus status = service.recalculateAssignmentStatus(48L, FIXED_INSTANT);

        assertThat(status).isEqualTo(AssignmentStatus.ASSIGNED);
        verify(countedResultFactsReader, never()).findStatusDefiningFactsByCountedResultId(any());
    }

    @Test
    void refreshCacheIsIdempotentWhenStatusAndCompletedClosureAlreadyMatchCanonicalProof() {
        Assignment assignment = new Assignment(
            49L,
            11L,
            21L,
            31L,
            AssignmentStatus.COMPLETED,
            FIXED_INSTANT.minusSeconds(3600),
            FIXED_INSTANT.plusSeconds(3600),
            null,
            FIXED_INSTANT.minusSeconds(30),
            FIXED_INSTANT.minusSeconds(3600),
            FIXED_INSTANT.minusSeconds(3600)
        );
        when(assignmentRepository.findAssignmentById(49L)).thenReturn(assignment);
        when(assignmentTestRepository.findAssignmentTestsByAssignmentId(49L)).thenReturn(List.of(
            closedTest(49L, 7008L)
        ));
        when(countedResultFactsReader.findStatusDefiningFactsByCountedResultId(7008L))
            .thenReturn(provedCompletionFacts());

        Assignment refreshed = service.refreshAssignmentStatusCache(49L, FIXED_INSTANT);

        assertThat(refreshed).isSameAs(assignment);
        verify(assignmentRepository, never()).saveAssignment(any());
    }

    @Test
    void refreshCachePromotesAssignmentToCompletedWhenCanonicalProofAppears() {
        Assignment assignment = assignment(50L, AssignmentStatus.ASSIGNED, FIXED_INSTANT.plusSeconds(3600), null);
        when(assignmentRepository.findAssignmentById(50L)).thenReturn(assignment);
        when(assignmentTestRepository.findAssignmentTestsByAssignmentId(50L)).thenReturn(List.of(
            closedTest(50L, 7009L)
        ));
        when(countedResultFactsReader.findStatusDefiningFactsByCountedResultId(7009L))
            .thenReturn(provedCompletionFacts());
        when(assignmentRepository.saveAssignment(any())).thenAnswer(invocation -> invocation.getArgument(0, Assignment.class));

        Assignment refreshed = service.refreshAssignmentStatusCache(50L, FIXED_INSTANT);

        assertThat(refreshed.status()).isEqualTo(AssignmentStatus.COMPLETED);
        assertThat(refreshed.closedAt()).isEqualTo(FIXED_INSTANT);
        verify(assignmentRepository).saveAssignment(any());
    }

    @Test
    void recalculateAssignmentStatusReturnsCompletedAfterOwnerLocalClosureMaterializesCanonicalCountedResultProof() {
        when(assignmentRepository.findAssignmentById(55L)).thenReturn(assignment(
            55L,
            AssignmentStatus.ASSIGNED,
            FIXED_INSTANT.plusSeconds(3600),
            null
        ));
        when(assignmentTestRepository.findAssignmentTestsByAssignmentId(55L)).thenReturn(List.of(
            closedTest(55L, 7012L)
        ));
        when(countedResultFactsReader.findStatusDefiningFactsByCountedResultId(7012L))
            .thenReturn(provedCompletionFacts());

        AssignmentStatus status = service.recalculateAssignmentStatus(55L, FIXED_INSTANT);

        assertThat(status).isEqualTo(AssignmentStatus.COMPLETED);
    }

    @Test
    void refreshCacheMaterializesCompletedClosureWhenStatusAlreadyCompletedButClosedAtIsMissing() {
        Assignment assignment = assignment(52L, AssignmentStatus.COMPLETED, FIXED_INSTANT.plusSeconds(3600), null);
        when(assignmentRepository.findAssignmentById(52L)).thenReturn(assignment);
        when(assignmentTestRepository.findAssignmentTestsByAssignmentId(52L)).thenReturn(List.of(
            closedTest(52L, 7011L)
        ));
        when(countedResultFactsReader.findStatusDefiningFactsByCountedResultId(7011L))
            .thenReturn(provedCompletionFacts());
        when(assignmentRepository.saveAssignment(any())).thenAnswer(invocation -> invocation.getArgument(0, Assignment.class));

        Assignment refreshed = service.refreshAssignmentStatusCache(52L, FIXED_INSTANT);

        assertThat(refreshed.status()).isEqualTo(AssignmentStatus.COMPLETED);
        assertThat(refreshed.closedAt()).isEqualTo(FIXED_INSTANT);
        verify(assignmentRepository).saveAssignment(any());
    }

    @Test
    void refreshCacheClearsStaleClosedAtWhenAssignmentReturnsToNonTerminalMaterializedState() {
        Assignment assignment = assignment(53L, AssignmentStatus.COMPLETED, FIXED_INSTANT.plusSeconds(3600), null);
        Assignment staleCompleted = new Assignment(
            assignment.id(),
            assignment.campaignId(),
            assignment.userId(),
            assignment.courseId(),
            AssignmentStatus.COMPLETED,
            assignment.assignedAt(),
            assignment.deadlineAt(),
            assignment.cancelledAt(),
            FIXED_INSTANT.minusSeconds(120),
            assignment.createdAt(),
            assignment.updatedAt()
        );
        when(assignmentRepository.findAssignmentById(53L)).thenReturn(staleCompleted);
        when(assignmentTestRepository.findAssignmentTestsByAssignmentId(53L)).thenReturn(List.of(
            openTest(53L)
        ));
        when(assignmentRepository.saveAssignment(any())).thenAnswer(invocation -> invocation.getArgument(0, Assignment.class));

        Assignment refreshed = service.refreshAssignmentStatusCache(53L, FIXED_INSTANT);

        assertThat(refreshed.status()).isEqualTo(AssignmentStatus.ASSIGNED);
        assertThat(refreshed.closedAt()).isNull();
        verify(assignmentRepository).saveAssignment(any());
    }

    @Test
    void refreshCacheClearsStaleClosedAtForCancelledMaterializationWithoutCreatingManualPatchPath() {
        Assignment staleCancelled = new Assignment(
            54L,
            11L,
            21L,
            31L,
            AssignmentStatus.ASSIGNED,
            FIXED_INSTANT.minusSeconds(3600),
            FIXED_INSTANT.plusSeconds(3600),
            FIXED_INSTANT.minusSeconds(30),
            FIXED_INSTANT.minusSeconds(60),
            FIXED_INSTANT.minusSeconds(3600),
            FIXED_INSTANT.minusSeconds(3600)
        );
        when(assignmentRepository.findAssignmentById(54L)).thenReturn(staleCancelled);
        when(assignmentRepository.saveAssignment(any())).thenAnswer(invocation -> invocation.getArgument(0, Assignment.class));

        Assignment refreshed = service.refreshAssignmentStatusCache(54L, FIXED_INSTANT);

        assertThat(refreshed.status()).isEqualTo(AssignmentStatus.CANCELLED);
        assertThat(refreshed.cancelledAt()).isEqualTo(FIXED_INSTANT.minusSeconds(30));
        assertThat(refreshed.closedAt()).isNull();
        verify(assignmentTestRepository, never()).findAssignmentTestsByAssignmentId(54L);
        verify(assignmentRepository).saveAssignment(any());
    }

    @Test
    void implementationStillDoesNotUseTestAttemptStatusShortcutOrCallSiteIntegration() {
        when(assignmentRepository.findAssignmentById(51L)).thenReturn(assignment(
            51L,
            AssignmentStatus.ASSIGNED,
            FIXED_INSTANT.plusSeconds(3600),
            null
        ));
        when(assignmentTestRepository.findAssignmentTestsByAssignmentId(51L)).thenReturn(List.of(
            closedTest(51L, 7010L)
        ));
        when(countedResultFactsReader.findStatusDefiningFactsByCountedResultId(7010L))
            .thenReturn(provedCompletionFacts());

        AssignmentStatus status = service.recalculateAssignmentStatus(51L, FIXED_INSTANT);

        assertThat(status).isEqualTo(AssignmentStatus.COMPLETED);
        verify(assignmentRepository).findAssignmentById(51L);
        verify(assignmentTestRepository).findAssignmentTestsByAssignmentId(51L);
        verify(countedResultFactsReader).findStatusDefiningFactsByCountedResultId(7010L);
        verify(assignmentRepository, never()).saveAssignment(any());
        verify(assignmentTestRepository, never()).findAssignmentTestByCountedResultId(any());
    }

    private Assignment assignment(Long id, AssignmentStatus status, Instant deadlineAt, Instant cancelledAt) {
        return new Assignment(
            id,
            11L,
            21L,
            31L,
            status,
            FIXED_INSTANT.minusSeconds(3600),
            deadlineAt,
            cancelledAt,
            null,
            FIXED_INSTANT.minusSeconds(3600),
            FIXED_INSTANT.minusSeconds(3600)
        );
    }

    private AssignmentTest openTest(Long assignmentId) {
        return new AssignmentTest(
            52L,
            assignmentId,
            62L,
            AssignmentTestRole.FINAL_TOPIC_CONTROL,
            null,
            null,
            false,
            FIXED_INSTANT.minusSeconds(3600),
            FIXED_INSTANT.minusSeconds(30)
        );
    }

    private AssignmentTest closedTest(Long assignmentId, Long countedResultId) {
        return new AssignmentTest(
            51L,
            assignmentId,
            61L,
            AssignmentTestRole.FINAL_TOPIC_CONTROL,
            countedResultId,
            FIXED_INSTANT.minusSeconds(30),
            true,
            FIXED_INSTANT.minusSeconds(3600),
            FIXED_INSTANT.minusSeconds(30)
        );
    }

    private StatusDefiningCountedResultFacts provedCompletionFacts() {
        return new StatusDefiningCountedResultFacts(true, true, true);
    }
}
