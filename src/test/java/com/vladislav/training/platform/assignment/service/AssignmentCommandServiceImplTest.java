package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import com.vladislav.training.platform.assignment.domain.AssignmentTestRole;
import com.vladislav.training.platform.assignment.repository.AssignmentRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentTestRepository;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.NotFoundException;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code AssignmentCommandServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class AssignmentCommandServiceImplTest {

    private static final Instant CREATED_AT = Instant.parse("2026-04-10T08:00:00Z");
    private static final Instant CLOSED_AT = Instant.parse("2026-04-10T12:00:00Z");

    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private AssignmentTestRepository assignmentTestRepository;
    @Mock
    private AssignmentStatusRecalculationService assignmentStatusRecalculationService;

    private AssignmentCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AssignmentCommandServiceImpl(
            assignmentRepository,
            assignmentTestRepository,
            assignmentStatusRecalculationService
        );
    }

    @Test
    void closeAssignmentTestWithCountedResultRefreshesAssignmentStatusOnceAfterSuccessfulMutation() {
        AssignmentTest existing = openAssignmentTest();
        AssignmentTest saved = closedAssignmentTest();
        when(assignmentTestRepository.findAssignmentTestById(77L)).thenReturn(existing);
        when(assignmentTestRepository.saveAssignmentTest(org.mockito.ArgumentMatchers.any(AssignmentTest.class))).thenReturn(saved);

        AssignmentTest result = service.closeAssignmentTestWithCountedResult(77L, 901L, CLOSED_AT);

        assertThat(result).isEqualTo(saved);
        verify(assignmentStatusRecalculationService).refreshAssignmentStatusCache(41L, CLOSED_AT);

        InOrder inOrder = inOrder(assignmentTestRepository, assignmentStatusRecalculationService);
        inOrder.verify(assignmentTestRepository).findAssignmentTestById(77L);
        inOrder.verify(assignmentTestRepository).saveAssignmentTest(org.mockito.ArgumentMatchers.any(AssignmentTest.class));
        inOrder.verify(assignmentStatusRecalculationService).refreshAssignmentStatusCache(41L, CLOSED_AT);
    }

    @Test
    void closeAssignmentTestWithCountedResultDoesNotRefreshWhenAssignmentTestIsNotFound() {
        when(assignmentTestRepository.findAssignmentTestById(77L)).thenReturn(null);

        assertThatThrownBy(() -> service.closeAssignmentTestWithCountedResult(77L, 901L, CLOSED_AT))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("assignmentTestId=77");

        verify(assignmentTestRepository, never()).saveAssignmentTest(org.mockito.ArgumentMatchers.any(AssignmentTest.class));
        verify(assignmentStatusRecalculationService, never()).refreshAssignmentStatusCache(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void closeAssignmentTestWithCountedResultDoesNotRefreshWhenMutationFailsBeforeSave() {
        AssignmentTest preClosedWithoutCountedResult = new AssignmentTest(
            77L,
            41L,
            501L,
            AssignmentTestRole.FINAL_TOPIC_CONTROL,
            null,
            CLOSED_AT,
            true,
            CREATED_AT,
            CLOSED_AT
        );
        when(assignmentTestRepository.findAssignmentTestById(77L)).thenReturn(preClosedWithoutCountedResult);

        assertThatThrownBy(() -> service.closeAssignmentTestWithCountedResult(77L, 901L, CLOSED_AT))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("already closed by counted result");

        verify(assignmentTestRepository, never()).saveAssignmentTest(org.mockito.ArgumentMatchers.any(AssignmentTest.class));
        verify(assignmentStatusRecalculationService, never()).refreshAssignmentStatusCache(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void closeAssignmentTestWithCountedResultDoesNotRefreshWhenSaveFails() {
        when(assignmentTestRepository.findAssignmentTestById(77L)).thenReturn(openAssignmentTest());
        doThrow(new IllegalStateException("save failed"))
            .when(assignmentTestRepository).saveAssignmentTest(org.mockito.ArgumentMatchers.any(AssignmentTest.class));

        assertThatThrownBy(() -> service.closeAssignmentTestWithCountedResult(77L, 901L, CLOSED_AT))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("save failed");

        verify(assignmentStatusRecalculationService, never()).refreshAssignmentStatusCache(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void closeAssignmentTestWithCountedResultRepairsHalfClosedStateAndRefreshesAssignmentStatus() {
        AssignmentTest halfClosed = new AssignmentTest(
            77L,
            41L,
            501L,
            AssignmentTestRole.FINAL_TOPIC_CONTROL,
            901L,
            null,
            false,
            CREATED_AT,
            CREATED_AT.plusSeconds(60)
        );
        when(assignmentTestRepository.findAssignmentTestById(77L)).thenReturn(halfClosed);
        when(assignmentTestRepository.saveAssignmentTest(org.mockito.ArgumentMatchers.any(AssignmentTest.class)))
            .thenReturn(closedAssignmentTest());

        AssignmentTest result = service.closeAssignmentTestWithCountedResult(77L, 901L, CLOSED_AT);

        assertThat(result).isEqualTo(closedAssignmentTest());
        verify(assignmentTestRepository).saveAssignmentTest(new AssignmentTest(
            77L,
            41L,
            501L,
            AssignmentTestRole.FINAL_TOPIC_CONTROL,
            901L,
            CLOSED_AT,
            true,
            CREATED_AT,
            CLOSED_AT
        ));
        verify(assignmentStatusRecalculationService).refreshAssignmentStatusCache(41L, CLOSED_AT);
    }

    @Test
    void closeAssignmentTestWithCountedResultTreatsSameCanonicalClosureAsIdempotentAndRefreshesStatus() {
        when(assignmentTestRepository.findAssignmentTestById(77L)).thenReturn(closedAssignmentTest());

        AssignmentTest result = service.closeAssignmentTestWithCountedResult(77L, 901L, CLOSED_AT);

        assertThat(result).isEqualTo(closedAssignmentTest());
        verify(assignmentTestRepository, never()).saveAssignmentTest(org.mockito.ArgumentMatchers.any(AssignmentTest.class));
        verify(assignmentStatusRecalculationService).refreshAssignmentStatusCache(41L, CLOSED_AT);
    }

    @Test
    void closeAssignmentTestWithCountedResultFailsWhenDifferentCanonicalResultWouldOverwriteOwnerClosure() {
        when(assignmentTestRepository.findAssignmentTestById(77L)).thenReturn(closedAssignmentTest());

        assertThatThrownBy(() -> service.closeAssignmentTestWithCountedResult(77L, 999L, CLOSED_AT))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("already closed by counted result");

        verify(assignmentTestRepository, never()).saveAssignmentTest(org.mockito.ArgumentMatchers.any(AssignmentTest.class));
        verify(assignmentStatusRecalculationService, never()).refreshAssignmentStatusCache(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }

    private AssignmentTest openAssignmentTest() {
        return new AssignmentTest(
            77L,
            41L,
            501L,
            AssignmentTestRole.FINAL_TOPIC_CONTROL,
            null,
            null,
            false,
            CREATED_AT,
            CREATED_AT
        );
    }

    private AssignmentTest closedAssignmentTest() {
        return new AssignmentTest(
            77L,
            41L,
            501L,
            AssignmentTestRole.FINAL_TOPIC_CONTROL,
            901L,
            CLOSED_AT,
            true,
            CREATED_AT,
            CLOSED_AT
        );
    }
}
