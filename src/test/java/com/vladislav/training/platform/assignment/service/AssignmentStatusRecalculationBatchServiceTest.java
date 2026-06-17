package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.assignment.repository.AssignmentStatusRecalculationCandidateRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение сервиса {@code AssignmentStatusRecalculationBatch}.
 * Сценарии сосредоточены на прикладной логике.
 */
@ExtendWith(MockitoExtension.class)
class AssignmentStatusRecalculationBatchServiceTest {

    @Mock private AssignmentStatusRecalculationCandidateRepository candidateRepository;
    @Mock private AssignmentStatusRecalculationService assignmentStatusRecalculationService;

    @Test
    void recalculatesBoundedDueAssignmentsThroughOwnerLocalContract() {
        Instant effectiveAt = Instant.parse("2026-06-07T10:00:00Z");
        AssignmentStatusRecalculationBatchService service = new AssignmentStatusRecalculationBatchService(
            candidateRepository,
            assignmentStatusRecalculationService,
            30,
            200
        );
        when(candidateRepository.findCandidateAssignmentIdsForStatusRecalculation(
            effectiveAt.minusSeconds(30L * 24L * 60L * 60L),
            effectiveAt,
            200
        )).thenReturn(List.of(11L, 12L, 13L));

        AssignmentStatusRecalculationBatchService.BatchResult result = service.recalculateDueAssignments(effectiveAt);

        assertThat(result.candidateCount()).isEqualTo(3);
        assertThat(result.refreshedCount()).isEqualTo(3);
        verify(assignmentStatusRecalculationService).refreshAssignmentStatusCache(11L, effectiveAt);
        verify(assignmentStatusRecalculationService).refreshAssignmentStatusCache(12L, effectiveAt);
        verify(assignmentStatusRecalculationService).refreshAssignmentStatusCache(13L, effectiveAt);
    }

    @Test
    void emptyCandidateBatchDoesNotInvokeOwnerLocalRefresh() {
        Instant effectiveAt = Instant.parse("2026-06-07T10:00:00Z");
        AssignmentStatusRecalculationBatchService service = new AssignmentStatusRecalculationBatchService(
            candidateRepository,
            assignmentStatusRecalculationService,
            30,
            200
        );
        when(candidateRepository.findCandidateAssignmentIdsForStatusRecalculation(
            effectiveAt.minusSeconds(30L * 24L * 60L * 60L),
            effectiveAt,
            200
        )).thenReturn(List.of());

        AssignmentStatusRecalculationBatchService.BatchResult result = service.recalculateDueAssignments(effectiveAt);

        assertThat(result.candidateCount()).isZero();
        assertThat(result.refreshedCount()).isZero();
        verify(assignmentStatusRecalculationService, never()).refreshAssignmentStatusCache(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }
}
