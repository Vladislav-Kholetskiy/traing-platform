package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import com.vladislav.training.platform.assignment.domain.AssignmentTestRole;
import com.vladislav.training.platform.assignment.repository.AssignmentRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentTestRepository;
import com.vladislav.training.platform.common.exception.NotFoundException;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение сервиса {@code AssignmentAssignedExecutionAdmissionFoundationStateRead}.
 * Сценарии сосредоточены на прикладной логике.
 */
@ExtendWith(MockitoExtension.class)
class AssignmentAssignedExecutionAdmissionFoundationStateReadServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-19T10:00:00Z");

    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private AssignmentTestRepository assignmentTestRepository;

    private AssignmentAssignedExecutionAdmissionFoundationStateReadServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AssignmentAssignedExecutionAdmissionFoundationStateReadServiceImpl(
            assignmentRepository,
            assignmentTestRepository
        );
    }

    @Test
    void returnsOwnedAssignmentAnchoredFoundationStateWhenAssignmentAndAssignmentTestMatch() {
        when(assignmentRepository.findAssignmentById(77L)).thenReturn(assignment(77L, 101L, false, false));
        when(assignmentTestRepository.findAssignmentTestById(701L)).thenReturn(assignmentTest(701L, 77L, 501L, false));

        var foundationState = service.findAssignmentAssignedExecutionAdmissionFoundationState(101L, 77L, 701L);

        assertThat(foundationState.assignmentId()).isEqualTo(77L);
        assertThat(foundationState.assignmentTestId()).isEqualTo(701L);
        assertThat(foundationState.testId()).isEqualTo(501L);
        assertThat(foundationState.assignmentCancelled()).isFalse();
        assertThat(foundationState.assignmentClosed()).isFalse();
        assertThat(foundationState.assignmentTestClosed()).isFalse();
    }

    @Test
    void returnsNullWhenAssignmentBelongsToDifferentActor() {
        when(assignmentRepository.findAssignmentById(77L)).thenReturn(assignment(77L, 202L, false, false));

        assertThat(service.findAssignmentAssignedExecutionAdmissionFoundationState(101L, 77L, 701L)).isNull();
        verifyNoInteractions(assignmentTestRepository);
    }

    @Test
    void returnsNullWhenAssignmentTestBelongsToDifferentAssignment() {
        when(assignmentRepository.findAssignmentById(77L)).thenReturn(assignment(77L, 101L, false, false));
        when(assignmentTestRepository.findAssignmentTestById(701L)).thenReturn(assignmentTest(701L, 88L, 501L, false));

        assertThat(service.findAssignmentAssignedExecutionAdmissionFoundationState(101L, 77L, 701L)).isNull();
    }

    @Test
    void returnsNullWhenAssignmentAnchorIsMissing() {
        when(assignmentRepository.findAssignmentById(77L)).thenThrow(new NotFoundException("Assignment not found: 77"));

        assertThat(service.findAssignmentAssignedExecutionAdmissionFoundationState(101L, 77L, 701L)).isNull();
        verifyNoInteractions(assignmentTestRepository);
    }

    @Test
    void rejectsNullIdsBeforeAnyRepositoryRead() {
        assertThatThrownBy(() -> service.findAssignmentAssignedExecutionAdmissionFoundationState(null, 77L, 701L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("actorUserId");
        assertThatThrownBy(() -> service.findAssignmentAssignedExecutionAdmissionFoundationState(101L, null, 701L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("assignmentId");
        assertThatThrownBy(() -> service.findAssignmentAssignedExecutionAdmissionFoundationState(101L, 77L, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("assignmentTestId");
    }

    private Assignment assignment(Long assignmentId, Long userId, boolean cancelled, boolean closed) {
        return new Assignment(
            assignmentId,
            900L,
            userId,
            301L,
            AssignmentStatus.ASSIGNED,
            FIXED_INSTANT,
            FIXED_INSTANT.plusSeconds(3600),
            cancelled ? FIXED_INSTANT : null,
            closed ? FIXED_INSTANT : null,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }

    private AssignmentTest assignmentTest(Long assignmentTestId, Long assignmentId, Long testId, boolean closed) {
        return new AssignmentTest(
            assignmentTestId,
            assignmentId,
            testId,
            AssignmentTestRole.FINAL_TOPIC_CONTROL,
            null,
            closed ? FIXED_INSTANT : null,
            closed,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }
}
