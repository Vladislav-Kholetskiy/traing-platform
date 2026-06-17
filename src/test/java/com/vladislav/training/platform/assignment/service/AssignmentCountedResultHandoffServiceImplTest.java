package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import com.vladislav.training.platform.assignment.domain.AssignmentTestRole;
import com.vladislav.training.platform.assignment.repository.AssignmentTestRepository;
import com.vladislav.training.platform.assignment.service.AssignmentStatusDefiningCountedResultFactsReader.CountedAssignmentResultHandoffFacts;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.model.AttemptMode;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code AssignmentCountedResultHandoffServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class AssignmentCountedResultHandoffServiceImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-19T12:00:00Z");

    @Mock
    private AssignmentStatusDefiningCountedResultFactsReader countedResultFactsReader;
    @Mock
    private AssignmentTestRepository assignmentTestRepository;
    @Mock
    private AssignmentCommandService assignmentCommandService;

    private AssignmentCountedResultHandoffServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AssignmentCountedResultHandoffServiceImpl(
            countedResultFactsReader,
            assignmentTestRepository,
            assignmentCommandService
        );
    }

    @Test
    void assignedCanonicalResultClosesAssignmentTestOwnerLocallyThroughAssignmentBoundary() {
        CountedAssignmentResultHandoffFacts result = assignedResult(501L, 801L, 701L);
        AssignmentTest assignmentTest = openAssignmentTest(701L, 801L, null);
        when(countedResultFactsReader.findCountedAssignmentResultHandoffFactsByResultId(501L)).thenReturn(result);
        when(assignmentTestRepository.findAssignmentTestById(701L)).thenReturn(assignmentTest);

        service.acceptValidCountedAssignmentResult(501L);

        verify(assignmentCommandService).closeAssignmentTestWithCountedResult(701L, 501L, FIXED_INSTANT);
    }

    @Test
    void assignedCanonicalResultRepairsHalfClosedAssignmentTestWithoutBypassingAssignmentOwner() {
        CountedAssignmentResultHandoffFacts result = assignedResult(507L, 807L, 707L);
        AssignmentTest halfClosedAssignmentTest = new AssignmentTest(
            707L,
            807L,
            601L,
            AssignmentTestRole.FINAL_TOPIC_CONTROL,
            507L,
            null,
            false,
            FIXED_INSTANT.minusSeconds(600),
            FIXED_INSTANT.minusSeconds(300)
        );
        when(countedResultFactsReader.findCountedAssignmentResultHandoffFactsByResultId(507L)).thenReturn(result);
        when(assignmentTestRepository.findAssignmentTestById(707L)).thenReturn(halfClosedAssignmentTest);

        service.acceptValidCountedAssignmentResult(507L);

        verify(assignmentCommandService).closeAssignmentTestWithCountedResult(707L, 507L, FIXED_INSTANT);
    }

    @Test
    void selfResultWithoutAssignmentAnchorIsIgnoredWithoutAssignmentWrites() {
        when(countedResultFactsReader.findCountedAssignmentResultHandoffFactsByResultId(502L)).thenReturn(selfResult(502L));

        service.acceptValidCountedAssignmentResult(502L);

        verify(assignmentTestRepository, never()).findAssignmentTestById(any());
        verify(assignmentCommandService, never()).closeAssignmentTestWithCountedResult(any(), any(), any());
    }

    @Test
    void countedHandoffIsIdempotentForSameResult() {
        CountedAssignmentResultHandoffFacts result = assignedResult(503L, 803L, 703L);
        AssignmentTest assignmentTest = closedAssignmentTest(703L, 803L, 503L);
        when(countedResultFactsReader.findCountedAssignmentResultHandoffFactsByResultId(503L)).thenReturn(result);
        when(assignmentTestRepository.findAssignmentTestById(703L)).thenReturn(assignmentTest);

        service.acceptValidCountedAssignmentResult(503L);

        verify(assignmentCommandService, never()).closeAssignmentTestWithCountedResult(any(), any(), any());
    }

    @Test
    void existingFullyClosedSameCountedResultStateIsStrictlyIdempotentWithoutRewrite() {
        countedHandoffIsIdempotentForSameResult();
    }

    @Test
    void differentExistingCountedResultIdFailsClosedWithoutOverwrite() {
        CountedAssignmentResultHandoffFacts result = assignedResult(504L, 804L, 704L);
        AssignmentTest assignmentTest = openAssignmentTest(704L, 804L, 999L);
        when(countedResultFactsReader.findCountedAssignmentResultHandoffFactsByResultId(504L)).thenReturn(result);
        when(assignmentTestRepository.findAssignmentTestById(704L)).thenReturn(assignmentTest);

        assertThatThrownBy(() -> service.acceptValidCountedAssignmentResult(504L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("cannot overwrite existing counted result");

        verify(assignmentCommandService, never()).closeAssignmentTestWithCountedResult(any(), any(), any());
    }

    @Test
    void conflictingRepeatedHandoffDoesNotOverwriteAlreadyClosedOwnerState() {
        CountedAssignmentResultHandoffFacts result = assignedResult(508L, 808L, 708L);
        AssignmentTest assignmentTest = closedAssignmentTest(708L, 808L, 999L);
        when(countedResultFactsReader.findCountedAssignmentResultHandoffFactsByResultId(508L)).thenReturn(result);
        when(assignmentTestRepository.findAssignmentTestById(708L)).thenReturn(assignmentTest);

        assertThatThrownBy(() -> service.acceptValidCountedAssignmentResult(508L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("cannot overwrite existing counted result");

        verify(assignmentCommandService, never()).closeAssignmentTestWithCountedResult(any(), any(), any());
    }

    @Test
    void assignmentAnchorMismatchFailsClosedWithoutOverwrite() {
        CountedAssignmentResultHandoffFacts result = assignedResult(505L, 805L, 705L);
        AssignmentTest assignmentTest = openAssignmentTest(705L, 999L, null);
        when(countedResultFactsReader.findCountedAssignmentResultHandoffFactsByResultId(505L)).thenReturn(result);
        when(assignmentTestRepository.findAssignmentTestById(705L)).thenReturn(assignmentTest);

        assertThatThrownBy(() -> service.acceptValidCountedAssignmentResult(505L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("anchor mismatch");

        verify(assignmentCommandService, never()).closeAssignmentTestWithCountedResult(any(), any(), any());
    }

    @Test
    void missingAssignmentTestFailsClosed() {
        CountedAssignmentResultHandoffFacts result = assignedResult(506L, 806L, 706L);
        when(countedResultFactsReader.findCountedAssignmentResultHandoffFactsByResultId(506L)).thenReturn(result);
        when(assignmentTestRepository.findAssignmentTestById(706L))
            .thenThrow(new NotFoundException("Assignment test not found: 706"));

        assertThatThrownBy(() -> service.acceptValidCountedAssignmentResult(506L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("706");

        verify(assignmentCommandService, never()).closeAssignmentTestWithCountedResult(any(), any(), any());
    }

    private CountedAssignmentResultHandoffFacts assignedResult(Long resultId, Long assignmentId, Long assignmentTestId) {
        return new CountedAssignmentResultHandoffFacts(
            resultId,
            assignmentId,
            assignmentTestId,
            AttemptMode.ASSIGNED,
            true,
            true,
            true,
            true,
            FIXED_INSTANT
        );
    }

    private CountedAssignmentResultHandoffFacts selfResult(Long resultId) {
        return new CountedAssignmentResultHandoffFacts(
            resultId,
            null,
            null,
            AttemptMode.SELF,
            true,
            false,
            false,
            false,
            FIXED_INSTANT
        );
    }

    private AssignmentTest openAssignmentTest(Long assignmentTestId, Long assignmentId, Long countedResultId) {
        return new AssignmentTest(
            assignmentTestId,
            assignmentId,
            601L,
            AssignmentTestRole.FINAL_TOPIC_CONTROL,
            countedResultId,
            null,
            false,
            FIXED_INSTANT.minusSeconds(600),
            FIXED_INSTANT.minusSeconds(300)
        );
    }

    private AssignmentTest closedAssignmentTest(Long assignmentTestId, Long assignmentId, Long countedResultId) {
        return new AssignmentTest(
            assignmentTestId,
            assignmentId,
            601L,
            AssignmentTestRole.FINAL_TOPIC_CONTROL,
            countedResultId,
            FIXED_INSTANT,
            true,
            FIXED_INSTANT.minusSeconds(600),
            FIXED_INSTANT
        );
    }
}
