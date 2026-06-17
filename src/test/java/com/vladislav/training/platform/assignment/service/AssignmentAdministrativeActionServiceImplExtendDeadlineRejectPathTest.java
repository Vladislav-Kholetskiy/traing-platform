package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPayload;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.application.policy.CapabilityOperationCodes;
import com.vladislav.training.platform.application.policy.CapabilityTargetEntityType;
import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentAdministrativeAction;
import com.vladislav.training.platform.assignment.domain.AssignmentAdministrativeActionType;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.repository.AssignmentAdministrativeActionRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentTestRepository;
import com.vladislav.training.platform.audit.domain.AuditContext;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.common.time.UtcClock;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;
/**
 * Проверяет поведение {@code AssignmentAdministrativeActionServiceImplExtendDeadlineRejectPath}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class AssignmentAdministrativeActionServiceImplExtendDeadlineRejectPathTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-10T08:00:00Z");

    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private AssignmentAdministrativeActionRepository assignmentAdministrativeActionRepository;
    @Mock
    private AssignmentTestRepository assignmentTestRepository;
    @Mock
    private AssignmentStatusRecalculationService assignmentStatusRecalculationService;
    @Mock
    private CapabilityAdmissionPolicy capabilityAdmissionPolicy;
    @Mock
    private CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;
    @Mock
    private CriticalCommandAuditSupport criticalCommandAuditSupport;
    @Mock
    private UtcClock utcClock;
    @Mock
    private AssignmentAdministrativeActionNotificationService assignmentAdministrativeActionNotificationService;

    private AssignmentAdministrativeActionServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(utcClock.now()).thenReturn(FIXED_INSTANT);
        service = new AssignmentAdministrativeActionServiceImpl(
            assignmentRepository,
            assignmentAdministrativeActionRepository,
            assignmentTestRepository,
            assignmentStatusRecalculationService,
            capabilityAdmissionPolicy,
            capabilityAdmissionRequestFactory,
            criticalCommandAuditSupport,
            utcClock,
            assignmentAdministrativeActionNotificationService
        );
    }

    @Test
    void admissionDeniedStopsBeforeMutation() {
        Instant newDeadlineAt = FIXED_INSTANT.plusSeconds(3600);
        CapabilityAdmissionRequest admissionRequest = admissionRequest(41L, newDeadlineAt);
        when(capabilityAdmissionRequestFactory.createAssignmentDeadlineExtend(41L, newDeadlineAt)).thenReturn(admissionRequest);
        doThrow(new PolicyViolationException("ACTOR_NOT_AUTHORIZED", "denied"))
            .when(capabilityAdmissionPolicy).check(admissionRequest);

        assertThatThrownBy(() -> service.extendAssignmentDeadline(
            new AssignmentAdministrativeActionService.ExtendAssignmentDeadlineCommand(41L, newDeadlineAt, "extend")
        ))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("denied");

        verifyNoInteractions(
            assignmentRepository,
            assignmentAdministrativeActionRepository,
            assignmentStatusRecalculationService,
            criticalCommandAuditSupport
        );
    }

    @Test
    void assignmentNotFoundRejectsBeforeTypedHistoryOrAudit() {
        Instant newDeadlineAt = FIXED_INSTANT.plusSeconds(3600);
        when(capabilityAdmissionRequestFactory.createAssignmentDeadlineExtend(41L, newDeadlineAt))
            .thenReturn(admissionRequest(41L, newDeadlineAt));
        when(assignmentRepository.findAssignmentById(41L)).thenThrow(new NotFoundException("Assignment not found: 41"));

        assertThatThrownBy(() -> service.extendAssignmentDeadline(
            new AssignmentAdministrativeActionService.ExtendAssignmentDeadlineCommand(41L, newDeadlineAt, "extend")
        ))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("41");

        verify(assignmentRepository, never()).saveAssignment(any(Assignment.class));
        verifyNoInteractions(assignmentAdministrativeActionRepository, assignmentStatusRecalculationService, criticalCommandAuditSupport);
    }

    @Test
    void extendOnAlreadyCancelledAssignmentFailsClosed() {
        Instant newDeadlineAt = FIXED_INSTANT.plusSeconds(3600);
        when(capabilityAdmissionRequestFactory.createAssignmentDeadlineExtend(41L, newDeadlineAt))
            .thenReturn(admissionRequest(41L, newDeadlineAt));
        when(assignmentRepository.findAssignmentById(41L)).thenReturn(assignment(
            AssignmentStatus.CANCELLED,
            FIXED_INSTANT.minusSeconds(60),
            null,
            FIXED_INSTANT.minusSeconds(3600),
            FIXED_INSTANT.minusSeconds(600)
        ));

        assertThatThrownBy(() -> service.extendAssignmentDeadline(
            new AssignmentAdministrativeActionService.ExtendAssignmentDeadlineCommand(41L, newDeadlineAt, "extend")
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("already CANCELLED");

        verify(assignmentRepository, never()).saveAssignment(any(Assignment.class));
        verifyNoInteractions(assignmentAdministrativeActionRepository, assignmentStatusRecalculationService, criticalCommandAuditSupport);
    }

    @Test
    void extendOnCompletedAssignmentFailsClosed() {
        Instant newDeadlineAt = FIXED_INSTANT.plusSeconds(3600);
        when(capabilityAdmissionRequestFactory.createAssignmentDeadlineExtend(41L, newDeadlineAt))
            .thenReturn(admissionRequest(41L, newDeadlineAt));
        when(assignmentRepository.findAssignmentById(41L)).thenReturn(assignment(
            AssignmentStatus.COMPLETED,
            null,
            FIXED_INSTANT.minusSeconds(30),
            FIXED_INSTANT.minusSeconds(3600),
            FIXED_INSTANT.minusSeconds(600)
        ));

        assertThatThrownBy(() -> service.extendAssignmentDeadline(
            new AssignmentAdministrativeActionService.ExtendAssignmentDeadlineCommand(41L, newDeadlineAt, "extend")
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("COMPLETED");

        verify(assignmentRepository, never()).saveAssignment(any(Assignment.class));
        verifyNoInteractions(assignmentAdministrativeActionRepository, assignmentStatusRecalculationService, criticalCommandAuditSupport);
    }

    @Test
    void equalDeadlineIsRejectedAsNoOpAdministrativeCommand() {
        Instant currentDeadlineAt = FIXED_INSTANT.plusSeconds(3600);
        when(capabilityAdmissionRequestFactory.createAssignmentDeadlineExtend(41L, currentDeadlineAt))
            .thenReturn(admissionRequest(41L, currentDeadlineAt));
        when(assignmentRepository.findAssignmentById(41L)).thenReturn(assignment(
            AssignmentStatus.ASSIGNED,
            null,
            null,
            FIXED_INSTANT.minusSeconds(3600),
            currentDeadlineAt
        ));

        assertThatThrownBy(() -> service.extendAssignmentDeadline(
            new AssignmentAdministrativeActionService.ExtendAssignmentDeadlineCommand(41L, currentDeadlineAt, "extend")
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("strictly later deadline");

        verify(assignmentRepository, never()).saveAssignment(any(Assignment.class));
        verifyNoInteractions(assignmentAdministrativeActionRepository, assignmentStatusRecalculationService, criticalCommandAuditSupport);
    }

    @Test
    void overdueAssignmentDoesNotReopenIfNewDeadlineDoesNotRestoreLiveWindow() {
        Instant currentDeadlineAt = FIXED_INSTANT.minusSeconds(3600);
        Instant stillPastDeadlineAt = FIXED_INSTANT.minusSeconds(1);
        when(capabilityAdmissionRequestFactory.createAssignmentDeadlineExtend(41L, stillPastDeadlineAt))
            .thenReturn(admissionRequest(41L, stillPastDeadlineAt));
        when(assignmentRepository.findAssignmentById(41L)).thenReturn(assignment(
            AssignmentStatus.OVERDUE,
            null,
            null,
            FIXED_INSTANT.minusSeconds(7200),
            currentDeadlineAt
        ));

        assertThatThrownBy(() -> service.extendAssignmentDeadline(
            new AssignmentAdministrativeActionService.ExtendAssignmentDeadlineCommand(41L, stillPastDeadlineAt, "extend")
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("must reopen the assignment window");

        verify(assignmentRepository, never()).saveAssignment(any(Assignment.class));
        verifyNoInteractions(assignmentAdministrativeActionRepository, assignmentStatusRecalculationService, criticalCommandAuditSupport);
    }

    @Test
    void ownerMutationFailureLeavesNoAdministrativeHistory() {
        Instant newDeadlineAt = FIXED_INSTANT.plusSeconds(7200);
        when(capabilityAdmissionRequestFactory.createAssignmentDeadlineExtend(41L, newDeadlineAt))
            .thenReturn(admissionRequest(41L, newDeadlineAt));
        when(assignmentRepository.findAssignmentById(41L)).thenReturn(assignment(
            AssignmentStatus.OVERDUE,
            null,
            null,
            FIXED_INSTANT.minusSeconds(7200),
            FIXED_INSTANT.minusSeconds(600)
        ));
        doThrow(new IllegalStateException("persistence failure"))
            .when(assignmentRepository).saveAssignment(any(Assignment.class));

        assertThatThrownBy(() -> service.extendAssignmentDeadline(
            new AssignmentAdministrativeActionService.ExtendAssignmentDeadlineCommand(41L, newDeadlineAt, "extend")
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("persistence failure");

        verify(assignmentAdministrativeActionRepository, never())
            .saveAssignmentAdministrativeAction(any(AssignmentAdministrativeAction.class));
        verifyNoInteractions(assignmentStatusRecalculationService, criticalCommandAuditSupport);
    }

    @Test
    void auditFailurePropagatesInsideTransactionalExtendBoundary() {
        Instant oldDeadlineAt = FIXED_INSTANT.minusSeconds(600);
        Instant newDeadlineAt = FIXED_INSTANT.plusSeconds(7200);
        CapabilityAdmissionRequest admissionRequest = admissionRequest(41L, newDeadlineAt);
        Assignment existingAssignment = assignment(
            AssignmentStatus.OVERDUE,
            null,
            null,
            FIXED_INSTANT.minusSeconds(7200),
            oldDeadlineAt
        );
        Assignment deadlineFactPersisted = assignment(
            AssignmentStatus.OVERDUE,
            null,
            null,
            existingAssignment.assignedAt(),
            newDeadlineAt
        );
        Assignment refreshedAssignment = assignment(
            AssignmentStatus.ASSIGNED,
            null,
            null,
            existingAssignment.assignedAt(),
            newDeadlineAt
        );

        when(capabilityAdmissionRequestFactory.createAssignmentDeadlineExtend(41L, newDeadlineAt)).thenReturn(admissionRequest);
        when(assignmentRepository.findAssignmentById(41L)).thenReturn(existingAssignment);
        when(assignmentRepository.saveAssignment(any(Assignment.class))).thenReturn(deadlineFactPersisted);
        when(assignmentAdministrativeActionRepository.saveAssignmentAdministrativeAction(any(AssignmentAdministrativeAction.class)))
            .thenReturn(new AssignmentAdministrativeAction(
                601L,
                41L,
                AssignmentAdministrativeActionType.EXTEND_DEADLINE,
                FIXED_INSTANT,
                "extend",
                FIXED_INSTANT
            ));
        when(assignmentStatusRecalculationService.refreshAssignmentStatusCache(41L, FIXED_INSTANT))
            .thenReturn(refreshedAssignment);
        when(criticalCommandAuditSupport.buildAuditContext(any(), any(com.vladislav.training.platform.application.policy.CapabilityOperationCode.class), any()))
            .thenReturn(new AuditContext("{\"operation\":\"deadline-extend\"}"));
        doThrow(new IllegalStateException("audit required"))
            .when(criticalCommandAuditSupport)
            .recordAudit(any(), any(), any(), any(), any(), any(), any());

        assertThat(AssignmentAdministrativeActionServiceImpl.class.isAnnotationPresent(Transactional.class)).isTrue();
        assertThatThrownBy(() -> service.extendAssignmentDeadline(
            new AssignmentAdministrativeActionService.ExtendAssignmentDeadlineCommand(41L, newDeadlineAt, "extend")
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("audit required");

        InOrder inOrder = org.mockito.Mockito.inOrder(
            assignmentRepository,
            assignmentAdministrativeActionRepository,
            assignmentStatusRecalculationService,
            criticalCommandAuditSupport
        );
        inOrder.verify(assignmentRepository).saveAssignment(any(Assignment.class));
        inOrder.verify(assignmentAdministrativeActionRepository)
            .saveAssignmentAdministrativeAction(any(AssignmentAdministrativeAction.class));
        inOrder.verify(assignmentStatusRecalculationService).refreshAssignmentStatusCache(41L, FIXED_INSTANT);
        inOrder.verify(criticalCommandAuditSupport)
            .recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    private CapabilityAdmissionRequest admissionRequest(Long assignmentId, Instant newDeadlineAt) {
        return new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.ASSIGNMENT_DEADLINE_EXTEND,
            CapabilityTargetEntityType.ASSIGNMENT,
            assignmentId,
            new CapabilityAdmissionPayload.AssignmentDeadlineExtend(newDeadlineAt),
            FIXED_INSTANT
        );
    }

    private Assignment assignment(
        AssignmentStatus status,
        Instant cancelledAt,
        Instant closedAt,
        Instant assignedAt,
        Instant deadlineAt
    ) {
        return new Assignment(
            41L,
            11L,
            21L,
            31L,
            status,
            assignedAt,
            deadlineAt,
            cancelledAt,
            closedAt,
            FIXED_INSTANT.minusSeconds(7200),
            FIXED_INSTANT.minusSeconds(7200)
        );
    }
}
