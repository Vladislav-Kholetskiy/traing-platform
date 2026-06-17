package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPayload;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.application.policy.CapabilityOperationCode;
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
import com.vladislav.training.platform.common.time.UtcClock;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code AssignmentAdministrativeActionServiceImplExtendDeadlineHappyPath}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class AssignmentAdministrativeActionServiceImplExtendDeadlineHappyPathTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-10T08:00:00Z");

    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private AssignmentAdministrativeActionRepository assignmentAdministrativeActionRepository;
    @Mock
    private AssignmentTestRepository assignmentTestRepository;
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

    private AssignmentStatusRecalculationService assignmentStatusRecalculationService;
    private AssignmentAdministrativeActionServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(utcClock.now()).thenReturn(FIXED_INSTANT);
        assignmentStatusRecalculationService = spy(
            new AssignmentStatusRecalculationServiceImpl(assignmentRepository, assignmentTestRepository)
        );
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
    void extendDeadlineReopensOverdueAssignmentCreatesTypedHistoryRefreshesStatusAndAudits() {
        Instant oldDeadlineAt = FIXED_INSTANT.minusSeconds(600);
        Instant newDeadlineAt = FIXED_INSTANT.plusSeconds(7200);
        CapabilityAdmissionRequest admissionRequest = admissionRequest(41L, newDeadlineAt);
        Assignment existingAssignment = assignment(
            41L,
            AssignmentStatus.OVERDUE,
            null,
            null,
            FIXED_INSTANT.minusSeconds(7200),
            oldDeadlineAt,
            FIXED_INSTANT.minusSeconds(7200),
            FIXED_INSTANT.minusSeconds(1200)
        );
        AtomicReference<Assignment> assignmentState = new AtomicReference<>(existingAssignment);

        when(capabilityAdmissionRequestFactory.createAssignmentDeadlineExtend(41L, newDeadlineAt)).thenReturn(admissionRequest);
        when(assignmentRepository.findAssignmentById(41L)).thenAnswer(invocation -> assignmentState.get());
        when(assignmentRepository.saveAssignment(any(Assignment.class))).thenAnswer(invocation -> {
            Assignment saved = invocation.getArgument(0, Assignment.class);
            assignmentState.set(saved);
            return saved;
        });
        when(assignmentTestRepository.findAssignmentTestsByAssignmentId(41L)).thenReturn(java.util.List.of());
        when(assignmentAdministrativeActionRepository.saveAssignmentAdministrativeAction(any(AssignmentAdministrativeAction.class)))
            .thenAnswer(invocation -> {
                AssignmentAdministrativeAction action = invocation.getArgument(0, AssignmentAdministrativeAction.class);
                return new AssignmentAdministrativeAction(
                    601L,
                    action.assignmentId(),
                    action.actionType(),
                    action.occurredAt(),
                    action.note(),
                    action.createdAt()
                );
            });
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(criticalCommandAuditSupport.buildAuditContext(
            eq("Assignment"),
            eq(CapabilityOperationCode.ASSIGNMENT_DEADLINE_EXTEND),
            anyMap()
        )).thenReturn(new AuditContext("{\"operation\":\"deadline-extend\"}"));
        Assignment result = service.extendAssignmentDeadline(
            new AssignmentAdministrativeActionService.ExtendAssignmentDeadlineCommand(41L, newDeadlineAt, "extend deadline")
        );

        assertThat(result.status()).isEqualTo(AssignmentStatus.ASSIGNED);
        assertThat(result.deadlineAt()).isEqualTo(newDeadlineAt);
        assertThat(result.userId()).isEqualTo(existingAssignment.userId());
        assertThat(result.courseId()).isEqualTo(existingAssignment.courseId());

        ArgumentCaptor<Assignment> assignmentCaptor = ArgumentCaptor.forClass(Assignment.class);
        verify(assignmentRepository, org.mockito.Mockito.times(2)).saveAssignment(assignmentCaptor.capture());
        Assignment ownerMutationSave = assignmentCaptor.getAllValues().get(0);
        Assignment refreshedCacheSave = assignmentCaptor.getAllValues().get(1);
        assertThat(ownerMutationSave.status()).isEqualTo(AssignmentStatus.OVERDUE);
        assertThat(ownerMutationSave.deadlineAt()).isEqualTo(newDeadlineAt);
        assertThat(ownerMutationSave.userId()).isEqualTo(existingAssignment.userId());
        assertThat(ownerMutationSave.courseId()).isEqualTo(existingAssignment.courseId());
        assertThat(ownerMutationSave.cancelledAt()).isNull();
        assertThat(refreshedCacheSave.status()).isEqualTo(AssignmentStatus.ASSIGNED);
        assertThat(refreshedCacheSave.deadlineAt()).isEqualTo(newDeadlineAt);
        assertThat(refreshedCacheSave.userId()).isEqualTo(existingAssignment.userId());
        assertThat(refreshedCacheSave.courseId()).isEqualTo(existingAssignment.courseId());
        assertThat(refreshedCacheSave.cancelledAt()).isNull();

        ArgumentCaptor<AssignmentAdministrativeAction> administrativeActionCaptor =
            ArgumentCaptor.forClass(AssignmentAdministrativeAction.class);
        verify(assignmentAdministrativeActionRepository)
            .saveAssignmentAdministrativeAction(administrativeActionCaptor.capture());
        AssignmentAdministrativeAction administrativeAction = administrativeActionCaptor.getValue();
        assertThat(administrativeAction.assignmentId()).isEqualTo(41L);
        assertThat(administrativeAction.actionType()).isEqualTo(AssignmentAdministrativeActionType.EXTEND_DEADLINE);
        assertThat(administrativeAction.occurredAt()).isEqualTo(FIXED_INSTANT);
        assertThat(administrativeAction.note()).isEqualTo("extend deadline");
        assertThat(administrativeAction.createdAt()).isEqualTo(FIXED_INSTANT);

        verify(assignmentStatusRecalculationService).refreshAssignmentStatusCache(41L, FIXED_INSTANT);
        verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        verify(criticalCommandAuditSupport).recordAudit(
            eq(777L),
            any(),
            eq("assignment"),
            eq(41L),
            any(),
            any(),
            any()
        );
        ArgumentCaptor<java.util.Map<String, Object>> detailsCaptor = ArgumentCaptor.forClass(java.util.Map.class);
        verify(criticalCommandAuditSupport).buildAuditContext(
            eq("Assignment"),
            eq(CapabilityOperationCode.ASSIGNMENT_DEADLINE_EXTEND),
            detailsCaptor.capture()
        );
        assertThat(detailsCaptor.getValue())
            .containsEntry("commandType", "extend_deadline")
            .containsEntry("statusRefreshIntegrated", true)
            .containsEntry("notePresent", true)
            .containsEntry("administrativeActionType", AssignmentAdministrativeActionType.EXTEND_DEADLINE)
            .containsEntry("administrativeActionId", 601L);

        ArgumentCaptor<Object> payloadAfterCaptor = ArgumentCaptor.forClass(Object.class);
        verify(criticalCommandAuditSupport).recordAudit(
            eq(777L),
            any(),
            eq("assignment"),
            eq(41L),
            any(),
            payloadAfterCaptor.capture(),
            any()
        );
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> payloadAfter = (java.util.Map<String, Object>) payloadAfterCaptor.getValue();
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> commandPayload =
            (java.util.Map<String, Object>) payloadAfter.get("command");
        assertThat(payloadAfter).containsKeys("administrativeAction", "command");
        assertThat(commandPayload)
            .containsEntry("note", "extend deadline")
            .containsEntry("newDeadlineAt", newDeadlineAt);

        InOrder inOrder = inOrder(
            capabilityAdmissionPolicy,
            assignmentRepository,
            assignmentAdministrativeActionRepository,
            assignmentStatusRecalculationService,
            criticalCommandAuditSupport
        );
        inOrder.verify(capabilityAdmissionPolicy).check(admissionRequest);
        inOrder.verify(assignmentRepository).saveAssignment(any(Assignment.class));
        inOrder.verify(assignmentAdministrativeActionRepository)
            .saveAssignmentAdministrativeAction(any(AssignmentAdministrativeAction.class));
        inOrder.verify(assignmentStatusRecalculationService).refreshAssignmentStatusCache(41L, FIXED_INSTANT);
        inOrder.verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        inOrder.verify(criticalCommandAuditSupport).recordAudit(
            eq(777L),
            any(),
            eq("assignment"),
            eq(41L),
            any(),
            any(),
            any()
        );
    }

    @Test
    void extendDeadlineKeepsAssignedStatusForAlreadyActiveAssignmentWhileChangingDeadlineFact() {
        Instant oldDeadlineAt = FIXED_INSTANT.plusSeconds(1800);
        Instant newDeadlineAt = FIXED_INSTANT.plusSeconds(7200);
        CapabilityAdmissionRequest admissionRequest = admissionRequest(41L, newDeadlineAt);
        Assignment existingAssignment = assignment(
            41L,
            AssignmentStatus.ASSIGNED,
            null,
            null,
            FIXED_INSTANT.minusSeconds(7200),
            oldDeadlineAt,
            FIXED_INSTANT.minusSeconds(7200),
            FIXED_INSTANT.minusSeconds(1200)
        );
        AtomicReference<Assignment> assignmentState = new AtomicReference<>(existingAssignment);

        when(capabilityAdmissionRequestFactory.createAssignmentDeadlineExtend(41L, newDeadlineAt)).thenReturn(admissionRequest);
        when(assignmentRepository.findAssignmentById(41L)).thenAnswer(invocation -> assignmentState.get());
        when(assignmentRepository.saveAssignment(any(Assignment.class))).thenAnswer(invocation -> {
            Assignment saved = invocation.getArgument(0, Assignment.class);
            assignmentState.set(saved);
            return saved;
        });
        when(assignmentTestRepository.findAssignmentTestsByAssignmentId(41L)).thenReturn(java.util.List.of());
        when(assignmentAdministrativeActionRepository.saveAssignmentAdministrativeAction(any(AssignmentAdministrativeAction.class)))
            .thenAnswer(invocation -> {
                AssignmentAdministrativeAction action = invocation.getArgument(0, AssignmentAdministrativeAction.class);
                return new AssignmentAdministrativeAction(
                    602L,
                    action.assignmentId(),
                    action.actionType(),
                    action.occurredAt(),
                    action.note(),
                    action.createdAt()
                );
            });
        when(criticalCommandAuditSupport.buildAuditContext(
            eq("Assignment"),
            eq(CapabilityOperationCode.ASSIGNMENT_DEADLINE_EXTEND),
            anyMap()
        )).thenReturn(new AuditContext("{\"operation\":\"deadline-extend\"}"));

        Assignment result = service.extendAssignmentDeadline(
            new AssignmentAdministrativeActionService.ExtendAssignmentDeadlineCommand(41L, newDeadlineAt, "keep active")
        );

        assertThat(result.status()).isEqualTo(AssignmentStatus.ASSIGNED);
        assertThat(result.deadlineAt()).isEqualTo(newDeadlineAt);
        assertThat(result.userId()).isEqualTo(existingAssignment.userId());
        assertThat(result.courseId()).isEqualTo(existingAssignment.courseId());
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
        Long assignmentId,
        AssignmentStatus status,
        Instant cancelledAt,
        Instant closedAt,
        Instant assignedAt,
        Instant deadlineAt,
        Instant createdAt,
        Instant updatedAt
    ) {
        return new Assignment(
            assignmentId,
            11L,
            21L,
            31L,
            status,
            assignedAt,
            deadlineAt,
            cancelledAt,
            closedAt,
            createdAt,
            updatedAt
        );
    }
}
