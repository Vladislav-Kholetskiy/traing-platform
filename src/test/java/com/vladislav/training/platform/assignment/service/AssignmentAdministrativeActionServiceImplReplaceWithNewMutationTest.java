package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
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
import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import com.vladislav.training.platform.assignment.domain.AssignmentTestRole;
import com.vladislav.training.platform.assignment.repository.AssignmentAdministrativeActionRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentTestRepository;
import com.vladislav.training.platform.audit.domain.AuditContext;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.application.policy.CapabilityOperationCode;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import com.vladislav.training.platform.common.exception.ValidationException;
import com.vladislav.training.platform.common.time.UtcClock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;
/**
 * Проверяет поведение {@code AssignmentAdministrativeActionServiceImplReplaceWithNewMutation}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class AssignmentAdministrativeActionServiceImplReplaceWithNewMutationTest {

    private static final Instant OCCURRED_AT = Instant.parse("2026-04-10T12:00:00Z");
    private static final Instant EXTERNAL_REQUESTED_AT = Instant.parse("2026-04-10T11:30:00Z");
    private static final Instant OLD_ASSIGNED_AT = Instant.parse("2026-04-10T08:00:00Z");
    private static final Instant OLD_DEADLINE_AT = Instant.parse("2026-04-12T12:00:00Z");
    private static final Instant OVERDUE_DEADLINE_AT = Instant.parse("2026-04-10T10:00:00Z");
    private static final Instant NEW_CYCLE_DEADLINE_AT = Instant.parse("2026-04-15T12:00:00Z");

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
        when(utcClock.now()).thenReturn(OCCURRED_AT);
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
    void replaceWithNewCancelsOldCycleCreatesNewCycleRefreshesStatusAndWritesSynchronousAuditCompanion() {
        CapabilityAdmissionRequest admissionRequest = admissionRequest(77L, 800L, EXTERNAL_REQUESTED_AT);
        Assignment existingAssignment = existingAssignment();
        List<AssignmentTest> oldAssignmentTests = List.of(
            assignmentTest(701L, 77L, 501L, AssignmentTestRole.FINAL_TOPIC_CONTROL),
            assignmentTest(702L, 77L, 502L, AssignmentTestRole.FINAL_TOPIC_CONTROL)
        );
        java.util.concurrent.atomic.AtomicReference<Assignment> oldAssignmentState =
            new java.util.concurrent.atomic.AtomicReference<>(existingAssignment);
        java.util.concurrent.atomic.AtomicReference<Assignment> newAssignmentState =
            new java.util.concurrent.atomic.AtomicReference<>();
        when(capabilityAdmissionRequestFactory.createAssignmentReplaceWithNew(77L, 800L)).thenReturn(admissionRequest);
        when(assignmentRepository.findAssignmentById(77L)).thenAnswer(invocation -> oldAssignmentState.get());
        when(assignmentRepository.findAssignmentById(88L)).thenAnswer(invocation -> newAssignmentState.get());
        when(assignmentRepository.findActiveAssignmentByUserIdAndCourseId(101L, 301L))
            .thenAnswer(invocation -> oldAssignmentState.get());
        when(assignmentRepository.saveAssignment(any(Assignment.class))).thenAnswer(invocation -> {
            Assignment assignment = invocation.getArgument(0, Assignment.class);
            if (assignment.id() != null) {
                oldAssignmentState.set(assignment);
                return assignment;
            }
            Assignment saved = new Assignment(
                88L,
                assignment.campaignId(),
                assignment.userId(),
                assignment.courseId(),
                assignment.status(),
                assignment.assignedAt(),
                assignment.deadlineAt(),
                assignment.cancelledAt(),
                assignment.closedAt(),
                assignment.createdAt(),
                assignment.updatedAt()
            );
            newAssignmentState.set(saved);
            return saved;
        });
        when(assignmentTestRepository.findAssignmentTestsByAssignmentId(77L)).thenReturn(oldAssignmentTests);
        when(assignmentTestRepository.findAssignmentTestsByAssignmentId(88L)).thenReturn(java.util.List.of());
        when(assignmentAdministrativeActionRepository.saveAssignmentAdministrativeAction(any(AssignmentAdministrativeAction.class)))
            .thenAnswer(invocation -> {
                AssignmentAdministrativeAction action = invocation.getArgument(0, AssignmentAdministrativeAction.class);
                return new AssignmentAdministrativeAction(
                    501L,
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
            eq(CapabilityOperationCode.ASSIGNMENT_REPLACE_WITH_NEW),
            any(Map.class)
        )).thenReturn(new AuditContext("{\"operation\":\"replace-with-new\"}"));

        Assignment result = service.replaceWithNewAssignment(
            replacementCommand(77L, 800L, NEW_CYCLE_DEADLINE_AT, "replacement note")
        );

        assertThat(result.id()).isEqualTo(88L);
        assertThat(result.status()).isEqualTo(AssignmentStatus.ASSIGNED);
        assertThat(result.campaignId()).isEqualTo(800L);
        assertThat(result.userId()).isEqualTo(existingAssignment.userId());
        assertThat(result.courseId()).isEqualTo(existingAssignment.courseId());
        assertThat(result.assignedAt()).isEqualTo(OCCURRED_AT);
        assertThat(result.deadlineAt()).isEqualTo(NEW_CYCLE_DEADLINE_AT);
        assertThat(result.deadlineAt()).isNotEqualTo(existingAssignment.deadlineAt());
        assertThat(result.cancelledAt()).isNull();
        assertThat(result.id()).isNotEqualTo(existingAssignment.id());

        ArgumentCaptor<Assignment> assignmentCaptor = ArgumentCaptor.forClass(Assignment.class);
        verify(assignmentRepository, times(2)).saveAssignment(assignmentCaptor.capture());
        assertThat(assignmentCaptor.getAllValues()).hasSize(2);

        Assignment cancelledAssignment = assignmentCaptor.getAllValues().get(0);
        assertThat(cancelledAssignment.id()).isEqualTo(77L);
        assertThat(cancelledAssignment.status()).isEqualTo(AssignmentStatus.CANCELLED);
        assertThat(cancelledAssignment.campaignId()).isEqualTo(existingAssignment.campaignId());
        assertThat(cancelledAssignment.userId()).isEqualTo(existingAssignment.userId());
        assertThat(cancelledAssignment.courseId()).isEqualTo(existingAssignment.courseId());
        assertThat(cancelledAssignment.assignedAt()).isEqualTo(existingAssignment.assignedAt());
        assertThat(cancelledAssignment.deadlineAt()).isEqualTo(existingAssignment.deadlineAt());
        assertThat(cancelledAssignment.cancelledAt()).isEqualTo(OCCURRED_AT);
        assertThat(cancelledAssignment.createdAt()).isEqualTo(existingAssignment.createdAt());

        Assignment newAssignment = assignmentCaptor.getAllValues().get(1);
        assertThat(newAssignment.id()).isNull();
        assertThat(newAssignment.status()).isEqualTo(AssignmentStatus.ASSIGNED);
        assertThat(newAssignment.campaignId()).isEqualTo(800L);
        assertThat(newAssignment.userId()).isEqualTo(existingAssignment.userId());
        assertThat(newAssignment.courseId()).isEqualTo(existingAssignment.courseId());
        assertThat(newAssignment.assignedAt()).isEqualTo(OCCURRED_AT);
        assertThat(newAssignment.deadlineAt()).isEqualTo(NEW_CYCLE_DEADLINE_AT);
        assertThat(newAssignment.cancelledAt()).isNull();
        assertThat(newAssignment.closedAt()).isNull();
        assertThat(newAssignment.createdAt()).isEqualTo(OCCURRED_AT);
        assertThat(newAssignment.updatedAt()).isEqualTo(OCCURRED_AT);

        ArgumentCaptor<AssignmentTest> replacementAssignmentTestCaptor = ArgumentCaptor.forClass(AssignmentTest.class);
        verify(assignmentTestRepository, times(2)).saveAssignmentTest(replacementAssignmentTestCaptor.capture());
        assertThat(replacementAssignmentTestCaptor.getAllValues()).hasSize(2);
        assertThat(replacementAssignmentTestCaptor.getAllValues())
            .extracting(AssignmentTest::id)
            .containsOnlyNulls();
        assertThat(replacementAssignmentTestCaptor.getAllValues())
            .extracting(AssignmentTest::assignmentId)
            .containsOnly(88L);
        assertThat(replacementAssignmentTestCaptor.getAllValues())
            .extracting(AssignmentTest::testId)
            .containsExactly(501L, 502L);
        assertThat(replacementAssignmentTestCaptor.getAllValues())
            .extracting(AssignmentTest::assignmentTestRole)
            .containsExactly(
                AssignmentTestRole.FINAL_TOPIC_CONTROL,
                AssignmentTestRole.FINAL_TOPIC_CONTROL
            );
        assertThat(replacementAssignmentTestCaptor.getAllValues())
            .extracting(AssignmentTest::countedResultId)
            .containsOnlyNulls();
        assertThat(replacementAssignmentTestCaptor.getAllValues())
            .extracting(AssignmentTest::closedAt)
            .containsOnlyNulls();
        assertThat(replacementAssignmentTestCaptor.getAllValues())
            .extracting(AssignmentTest::isClosed)
            .containsOnly(false);
        assertThat(replacementAssignmentTestCaptor.getAllValues())
            .extracting(AssignmentTest::createdAt)
            .containsOnly(OCCURRED_AT);
        assertThat(replacementAssignmentTestCaptor.getAllValues())
            .extracting(AssignmentTest::updatedAt)
            .containsOnly(OCCURRED_AT);
        assertThat(oldAssignmentTests)
            .extracting(AssignmentTest::id)
            .containsExactly(701L, 702L);
        assertThat(oldAssignmentTests)
            .extracting(AssignmentTest::assignmentId)
            .containsOnly(77L);

        ArgumentCaptor<AssignmentAdministrativeAction> administrativeActionCaptor =
            ArgumentCaptor.forClass(AssignmentAdministrativeAction.class);
        verify(assignmentAdministrativeActionRepository)
            .saveAssignmentAdministrativeAction(administrativeActionCaptor.capture());
        AssignmentAdministrativeAction administrativeAction = administrativeActionCaptor.getValue();
        assertThat(administrativeAction.assignmentId()).isEqualTo(77L);
        assertThat(administrativeAction.actionType()).isEqualTo(
            AssignmentAdministrativeActionType.REPLACE_WITH_NEW_ASSIGNMENT
        );
        assertThat(administrativeAction.occurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(administrativeAction.note()).isEqualTo("replacement note");
        assertThat(administrativeAction.createdAt()).isEqualTo(OCCURRED_AT);

        verify(assignmentStatusRecalculationService).refreshAssignmentStatusCache(77L, OCCURRED_AT);
        verify(assignmentStatusRecalculationService).refreshAssignmentStatusCache(88L, OCCURRED_AT);
        verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        verify(criticalCommandAuditSupport).recordAudit(
            eq(777L),
            any(),
            eq("assignment"),
            eq(77L),
            any(),
            any(),
            any()
        );

        ArgumentCaptor<Map<String, Object>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(criticalCommandAuditSupport).buildAuditContext(
            eq("Assignment"),
            eq(CapabilityOperationCode.ASSIGNMENT_REPLACE_WITH_NEW),
            detailsCaptor.capture()
        );
        assertThat(detailsCaptor.getValue())
            .containsEntry("commandType", "replace_with_new_assignment")
            .containsEntry("administrativeActionType", AssignmentAdministrativeActionType.REPLACE_WITH_NEW_ASSIGNMENT)
            .containsEntry("administrativeActionId", 501L)
            .containsEntry("statusRefreshIntegrated", true)
            .containsEntry("relatedAssignmentId", 88L);

        ArgumentCaptor<Object> payloadAfterCaptor = ArgumentCaptor.forClass(Object.class);
        verify(criticalCommandAuditSupport).recordAudit(
            eq(777L),
            any(),
            eq("assignment"),
            eq(77L),
            any(),
            payloadAfterCaptor.capture(),
            any()
        );
        @SuppressWarnings("unchecked")
        Map<String, Object> payloadAfter = (Map<String, Object>) payloadAfterCaptor.getValue();
        @SuppressWarnings("unchecked")
        Map<String, Object> commandPayload = (Map<String, Object>) payloadAfter.get("command");
        @SuppressWarnings("unchecked")
        Map<String, Object> relatedAssignmentPayload = (Map<String, Object>) payloadAfter.get("relatedAssignment");
        assertThat(payloadAfter).containsKeys("administrativeAction", "command", "relatedAssignment");
        assertThat(commandPayload).containsEntry("note", "replacement note");
        assertThat(relatedAssignmentPayload)
            .containsEntry("assignmentId", 88L)
            .containsEntry("status", AssignmentStatus.ASSIGNED);

        InOrder inOrder = inOrder(
            capabilityAdmissionPolicy,
            assignmentRepository,
            assignmentTestRepository,
            assignmentAdministrativeActionRepository,
            assignmentStatusRecalculationService,
            criticalCommandAuditSupport
        );
        inOrder.verify(capabilityAdmissionPolicy).check(admissionRequest);
        inOrder.verify(assignmentRepository).findAssignmentById(77L);
        inOrder.verify(assignmentRepository).findActiveAssignmentByUserIdAndCourseId(101L, 301L);
        inOrder.verify(assignmentRepository, times(2)).saveAssignment(any(Assignment.class));
        inOrder.verify(assignmentTestRepository).findAssignmentTestsByAssignmentId(77L);
        inOrder.verify(assignmentTestRepository, times(2)).saveAssignmentTest(any(AssignmentTest.class));
        inOrder.verify(assignmentAdministrativeActionRepository)
            .saveAssignmentAdministrativeAction(any(AssignmentAdministrativeAction.class));
        inOrder.verify(assignmentStatusRecalculationService).refreshAssignmentStatusCache(77L, OCCURRED_AT);
        inOrder.verify(assignmentStatusRecalculationService).refreshAssignmentStatusCache(88L, OCCURRED_AT);
        inOrder.verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        inOrder.verify(criticalCommandAuditSupport).buildAuditContext(
            eq("Assignment"),
            eq(CapabilityOperationCode.ASSIGNMENT_REPLACE_WITH_NEW),
            any(Map.class)
        );
        inOrder.verify(criticalCommandAuditSupport).recordAudit(
            eq(777L),
            any(),
            eq("assignment"),
            eq(77L),
            any(),
            any(),
            any()
        );
    }

    @Test
    void replaceWithNewMustMaterializeFreshAssignmentTestCycleInsteadOfRewritingOldAssignmentTestsInPlace() {
        CapabilityAdmissionRequest admissionRequest = admissionRequest(77L, 800L, EXTERNAL_REQUESTED_AT);
        Assignment existingAssignment = existingAssignment();
        List<AssignmentTest> oldAssignmentTests = List.of(
            assignmentTest(701L, 77L, 501L, AssignmentTestRole.FINAL_TOPIC_CONTROL),
            assignmentTest(702L, 77L, 502L, AssignmentTestRole.FINAL_TOPIC_CONTROL)
        );
        java.util.concurrent.atomic.AtomicReference<Assignment> oldAssignmentState =
            new java.util.concurrent.atomic.AtomicReference<>(existingAssignment);
        java.util.concurrent.atomic.AtomicReference<Assignment> newAssignmentState =
            new java.util.concurrent.atomic.AtomicReference<>();
        when(capabilityAdmissionRequestFactory.createAssignmentReplaceWithNew(77L, 800L)).thenReturn(admissionRequest);
        when(assignmentRepository.findAssignmentById(77L)).thenAnswer(invocation -> oldAssignmentState.get());
        when(assignmentRepository.findAssignmentById(88L)).thenAnswer(invocation -> newAssignmentState.get());
        when(assignmentRepository.findActiveAssignmentByUserIdAndCourseId(101L, 301L))
            .thenAnswer(invocation -> oldAssignmentState.get());
        when(assignmentRepository.saveAssignment(any(Assignment.class))).thenAnswer(invocation -> {
            Assignment assignment = invocation.getArgument(0, Assignment.class);
            if (assignment.id() != null) {
                oldAssignmentState.set(assignment);
                return assignment;
            }
            Assignment saved = new Assignment(
                88L,
                assignment.campaignId(),
                assignment.userId(),
                assignment.courseId(),
                assignment.status(),
                assignment.assignedAt(),
                assignment.deadlineAt(),
                assignment.cancelledAt(),
                assignment.closedAt(),
                assignment.createdAt(),
                assignment.updatedAt()
            );
            newAssignmentState.set(saved);
            return saved;
        });
        when(assignmentTestRepository.findAssignmentTestsByAssignmentId(77L)).thenReturn(oldAssignmentTests);
        when(assignmentTestRepository.findAssignmentTestsByAssignmentId(88L)).thenReturn(java.util.List.of());
        when(assignmentAdministrativeActionRepository.saveAssignmentAdministrativeAction(any(AssignmentAdministrativeAction.class)))
            .thenReturn(new AssignmentAdministrativeAction(
                501L,
                77L,
                AssignmentAdministrativeActionType.REPLACE_WITH_NEW_ASSIGNMENT,
                OCCURRED_AT,
                "replacement note",
                OCCURRED_AT
            ));
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(criticalCommandAuditSupport.buildAuditContext(
            eq("Assignment"),
            eq(CapabilityOperationCode.ASSIGNMENT_REPLACE_WITH_NEW),
            any(Map.class)
        )).thenReturn(new AuditContext("{\"operation\":\"replace-with-new\"}"));

        service.replaceWithNewAssignment(
            replacementCommand(77L, 800L, NEW_CYCLE_DEADLINE_AT, "replacement note")
        );

        ArgumentCaptor<AssignmentTest> replacementAssignmentTestCaptor = ArgumentCaptor.forClass(AssignmentTest.class);
        verify(assignmentTestRepository, times(2)).saveAssignmentTest(replacementAssignmentTestCaptor.capture());
        assertThat(replacementAssignmentTestCaptor.getAllValues())
            .extracting(AssignmentTest::assignmentId)
            .containsOnly(88L);
        assertThat(replacementAssignmentTestCaptor.getAllValues())
            .extracting(AssignmentTest::id)
            .containsOnlyNulls();
        assertThat(replacementAssignmentTestCaptor.getAllValues())
            .extracting(AssignmentTest::testId)
            .containsExactlyElementsOf(oldAssignmentTests.stream().map(AssignmentTest::testId).toList());
        assertThat(replacementAssignmentTestCaptor.getAllValues())
            .extracting(AssignmentTest::assignmentTestRole)
            .containsExactlyElementsOf(oldAssignmentTests.stream().map(AssignmentTest::assignmentTestRole).toList());
        assertThat(replacementAssignmentTestCaptor.getAllValues())
            .extracting(AssignmentTest::countedResultId)
            .containsOnlyNulls();
        assertThat(replacementAssignmentTestCaptor.getAllValues())
            .extracting(AssignmentTest::closedAt)
            .containsOnlyNulls();
        assertThat(replacementAssignmentTestCaptor.getAllValues())
            .extracting(AssignmentTest::isClosed)
            .containsOnly(false);
        assertThat(oldAssignmentTests)
            .extracting(AssignmentTest::assignmentId)
            .containsOnly(77L);
        assertThat(oldAssignmentTests)
            .extracting(AssignmentTest::id)
            .containsExactly(701L, 702L);
    }

    @Test
    void replaceWithNewAllowsOverdueTargetThroughCanonicalTypedPath() {
        CapabilityAdmissionRequest admissionRequest = admissionRequest(77L, 800L, EXTERNAL_REQUESTED_AT);
        Assignment existingAssignment = overdueAssignment();
        List<AssignmentTest> oldAssignmentTests = List.of(
            assignmentTest(701L, 77L, 501L, AssignmentTestRole.FINAL_TOPIC_CONTROL)
        );
        java.util.concurrent.atomic.AtomicReference<Assignment> oldAssignmentState =
            new java.util.concurrent.atomic.AtomicReference<>(existingAssignment);
        java.util.concurrent.atomic.AtomicReference<Assignment> newAssignmentState =
            new java.util.concurrent.atomic.AtomicReference<>();
        when(capabilityAdmissionRequestFactory.createAssignmentReplaceWithNew(77L, 800L)).thenReturn(admissionRequest);
        when(assignmentRepository.findAssignmentById(77L)).thenAnswer(invocation -> oldAssignmentState.get());
        when(assignmentRepository.findAssignmentById(88L)).thenAnswer(invocation -> newAssignmentState.get());
        when(assignmentRepository.findActiveAssignmentByUserIdAndCourseId(101L, 301L))
            .thenAnswer(invocation -> oldAssignmentState.get());
        when(assignmentRepository.saveAssignment(any(Assignment.class))).thenAnswer(invocation -> {
            Assignment assignment = invocation.getArgument(0, Assignment.class);
            if (assignment.id() != null) {
                oldAssignmentState.set(assignment);
                return assignment;
            }
            Assignment saved = new Assignment(
                88L,
                assignment.campaignId(),
                assignment.userId(),
                assignment.courseId(),
                assignment.status(),
                assignment.assignedAt(),
                assignment.deadlineAt(),
                assignment.cancelledAt(),
                assignment.closedAt(),
                assignment.createdAt(),
                assignment.updatedAt()
            );
            newAssignmentState.set(saved);
            return saved;
        });
        when(assignmentTestRepository.findAssignmentTestsByAssignmentId(77L)).thenReturn(oldAssignmentTests);
        when(assignmentTestRepository.findAssignmentTestsByAssignmentId(88L)).thenReturn(java.util.List.of());
        when(assignmentAdministrativeActionRepository.saveAssignmentAdministrativeAction(any(AssignmentAdministrativeAction.class)))
            .thenReturn(new AssignmentAdministrativeAction(
                501L,
                77L,
                AssignmentAdministrativeActionType.REPLACE_WITH_NEW_ASSIGNMENT,
                OCCURRED_AT,
                "replacement note",
                OCCURRED_AT
            ));
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(criticalCommandAuditSupport.buildAuditContext(
            eq("Assignment"),
            eq(CapabilityOperationCode.ASSIGNMENT_REPLACE_WITH_NEW),
            any(Map.class)
        )).thenReturn(new AuditContext("{\"operation\":\"replace-with-new\"}"));

        Assignment result = service.replaceWithNewAssignment(
            replacementCommand(77L, 800L, NEW_CYCLE_DEADLINE_AT, "replacement note")
        );

        assertThat(result.id()).isEqualTo(88L);
        assertThat(result.status()).isEqualTo(AssignmentStatus.ASSIGNED);
        assertThat(result.deadlineAt()).isEqualTo(NEW_CYCLE_DEADLINE_AT);
        assertThat(result.deadlineAt()).isNotEqualTo(existingAssignment.deadlineAt());

        ArgumentCaptor<Assignment> assignmentCaptor = ArgumentCaptor.forClass(Assignment.class);
        verify(assignmentRepository, times(2)).saveAssignment(assignmentCaptor.capture());

        Assignment cancelledAssignment = assignmentCaptor.getAllValues().get(0);
        assertThat(cancelledAssignment.id()).isEqualTo(77L);
        assertThat(cancelledAssignment.status()).isEqualTo(AssignmentStatus.CANCELLED);
        assertThat(cancelledAssignment.deadlineAt()).isEqualTo(OVERDUE_DEADLINE_AT);
        assertThat(cancelledAssignment.cancelledAt()).isEqualTo(OCCURRED_AT);

        Assignment newAssignment = assignmentCaptor.getAllValues().get(1);
        assertThat(newAssignment.id()).isNull();
        assertThat(newAssignment.status()).isEqualTo(AssignmentStatus.ASSIGNED);
        assertThat(newAssignment.deadlineAt()).isEqualTo(NEW_CYCLE_DEADLINE_AT);
        assertThat(newAssignment.assignedAt()).isEqualTo(OCCURRED_AT);

        verify(assignmentRepository).findActiveAssignmentByUserIdAndCourseId(101L, 301L);
        verify(assignmentStatusRecalculationService).refreshAssignmentStatusCache(77L, OCCURRED_AT);
        verify(assignmentStatusRecalculationService).refreshAssignmentStatusCache(88L, OCCURRED_AT);
    }

    @Test
    void replaceWithNewFailsClosedWhenNewCycleDeadlineSemanticsIsMissing() {
        CapabilityAdmissionRequest admissionRequest = admissionRequest(77L, 800L, EXTERNAL_REQUESTED_AT);
        Assignment existingAssignment = existingAssignment();
        when(capabilityAdmissionRequestFactory.createAssignmentReplaceWithNew(77L, 800L)).thenReturn(admissionRequest);
        when(assignmentRepository.findAssignmentById(77L)).thenReturn(existingAssignment);

        assertThatThrownBy(() -> service.replaceWithNewAssignment(
            replacementCommand(77L, 800L, null, "replacement note")
        ))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("explicit new-cycle deadlineAt");

        verify(assignmentRepository).findAssignmentById(77L);
        verify(assignmentRepository, never()).saveAssignment(any(Assignment.class));
        verifyNoInteractions(
            assignmentTestRepository,
            assignmentAdministrativeActionRepository,
            assignmentStatusRecalculationService,
            criticalCommandAuditSupport
        );
    }

    @Test
    void replaceWithNewFailsClosedWhenNewCyclePersistenceCollides() {
        CapabilityAdmissionRequest admissionRequest = admissionRequest(77L, 800L, EXTERNAL_REQUESTED_AT);
        Assignment existingAssignment = existingAssignment();
        when(capabilityAdmissionRequestFactory.createAssignmentReplaceWithNew(77L, 800L)).thenReturn(admissionRequest);
        when(assignmentRepository.findAssignmentById(77L)).thenReturn(existingAssignment);
        when(assignmentRepository.findActiveAssignmentByUserIdAndCourseId(101L, 301L)).thenReturn(existingAssignment);
        when(assignmentRepository.saveAssignment(any(Assignment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0, Assignment.class))
            .thenThrow(new PersistenceConstraintViolationException("unique conflict"));

        assertThatThrownBy(() -> service.replaceWithNewAssignment(
            replacementCommand(77L, 800L, NEW_CYCLE_DEADLINE_AT, "replacement note")
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("replacement-cycle collision");

        verify(assignmentRepository).findAssignmentById(77L);
        verify(assignmentRepository).findActiveAssignmentByUserIdAndCourseId(101L, 301L);
        verify(assignmentRepository, times(2)).saveAssignment(any(Assignment.class));
        verify(assignmentAdministrativeActionRepository, never())
            .saveAssignmentAdministrativeAction(any(AssignmentAdministrativeAction.class));
        verifyNoInteractions(assignmentStatusRecalculationService, criticalCommandAuditSupport);
    }

    @Test
    void replacementAdministrativeActionPersistenceFailurePropagatesInsideTransactionalBoundary() {
        CapabilityAdmissionRequest admissionRequest = admissionRequest(77L, 800L, EXTERNAL_REQUESTED_AT);
        Assignment existingAssignment = existingAssignment();
        List<AssignmentTest> oldAssignmentTests = List.of(
            assignmentTest(701L, 77L, 501L, AssignmentTestRole.FINAL_TOPIC_CONTROL)
        );
        when(capabilityAdmissionRequestFactory.createAssignmentReplaceWithNew(77L, 800L)).thenReturn(admissionRequest);
        when(assignmentRepository.findAssignmentById(77L)).thenReturn(existingAssignment);
        when(assignmentRepository.findActiveAssignmentByUserIdAndCourseId(101L, 301L)).thenReturn(existingAssignment);
        when(assignmentRepository.saveAssignment(any(Assignment.class))).thenAnswer(invocation -> {
            Assignment assignment = invocation.getArgument(0, Assignment.class);
            if (assignment.id() != null) {
                return assignment;
            }
            return new Assignment(
                88L,
                assignment.campaignId(),
                assignment.userId(),
                assignment.courseId(),
                assignment.status(),
                assignment.assignedAt(),
                assignment.deadlineAt(),
                assignment.cancelledAt(),
                assignment.closedAt(),
                assignment.createdAt(),
                assignment.updatedAt()
            );
        });
        when(assignmentTestRepository.findAssignmentTestsByAssignmentId(77L)).thenReturn(oldAssignmentTests);
        when(assignmentAdministrativeActionRepository.saveAssignmentAdministrativeAction(any(AssignmentAdministrativeAction.class)))
            .thenThrow(new PersistenceConstraintViolationException("administrative action failure"));

        assertThat(AssignmentAdministrativeActionServiceImpl.class.isAnnotationPresent(Transactional.class)).isTrue();
        assertThatThrownBy(() -> service.replaceWithNewAssignment(
            replacementCommand(77L, 800L, NEW_CYCLE_DEADLINE_AT, "replacement note")
        ))
            .isInstanceOf(PersistenceConstraintViolationException.class)
            .hasMessageContaining("administrative action failure");

        verify(assignmentRepository).findAssignmentById(77L);
        verify(assignmentRepository).findActiveAssignmentByUserIdAndCourseId(101L, 301L);
        verify(assignmentRepository, times(2)).saveAssignment(any(Assignment.class));
        verify(assignmentTestRepository).findAssignmentTestsByAssignmentId(77L);
        verify(assignmentTestRepository).saveAssignmentTest(any(AssignmentTest.class));
        verify(assignmentAdministrativeActionRepository)
            .saveAssignmentAdministrativeAction(any(AssignmentAdministrativeAction.class));
        verifyNoInteractions(assignmentStatusRecalculationService, criticalCommandAuditSupport);
    }

    @Test
    void replacementAuditFailurePropagatesAndDoesNotLeaveSuccessfulOutwardCompletion() {
        CapabilityAdmissionRequest admissionRequest = admissionRequest(77L, 800L, EXTERNAL_REQUESTED_AT);
        Assignment existingAssignment = existingAssignment();
        List<AssignmentTest> oldAssignmentTests = List.of(
            assignmentTest(701L, 77L, 501L, AssignmentTestRole.FINAL_TOPIC_CONTROL)
        );
        java.util.concurrent.atomic.AtomicReference<Assignment> oldAssignmentState =
            new java.util.concurrent.atomic.AtomicReference<>(existingAssignment);
        java.util.concurrent.atomic.AtomicReference<Assignment> newAssignmentState =
            new java.util.concurrent.atomic.AtomicReference<>();
        when(capabilityAdmissionRequestFactory.createAssignmentReplaceWithNew(77L, 800L)).thenReturn(admissionRequest);
        when(assignmentRepository.findAssignmentById(77L)).thenAnswer(invocation -> oldAssignmentState.get());
        when(assignmentRepository.findAssignmentById(88L)).thenAnswer(invocation -> newAssignmentState.get());
        when(assignmentRepository.findActiveAssignmentByUserIdAndCourseId(101L, 301L))
            .thenAnswer(invocation -> oldAssignmentState.get());
        when(assignmentRepository.saveAssignment(any(Assignment.class))).thenAnswer(invocation -> {
            Assignment assignment = invocation.getArgument(0, Assignment.class);
            if (assignment.id() != null) {
                oldAssignmentState.set(assignment);
                return assignment;
            }
            Assignment saved = new Assignment(
                88L,
                assignment.campaignId(),
                assignment.userId(),
                assignment.courseId(),
                assignment.status(),
                assignment.assignedAt(),
                assignment.deadlineAt(),
                assignment.cancelledAt(),
                assignment.closedAt(),
                assignment.createdAt(),
                assignment.updatedAt()
            );
            newAssignmentState.set(saved);
            return saved;
        });
        when(assignmentTestRepository.findAssignmentTestsByAssignmentId(77L)).thenReturn(oldAssignmentTests);
        when(assignmentTestRepository.findAssignmentTestsByAssignmentId(88L)).thenReturn(java.util.List.of());
        when(assignmentAdministrativeActionRepository.saveAssignmentAdministrativeAction(any(AssignmentAdministrativeAction.class)))
            .thenReturn(new AssignmentAdministrativeAction(
                501L,
                77L,
                AssignmentAdministrativeActionType.REPLACE_WITH_NEW_ASSIGNMENT,
                OCCURRED_AT,
                "replacement note",
                OCCURRED_AT
            ));
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(criticalCommandAuditSupport.buildAuditContext(
            eq("Assignment"),
            eq(CapabilityOperationCode.ASSIGNMENT_REPLACE_WITH_NEW),
            any(Map.class)
        )).thenReturn(new AuditContext("{\"operation\":\"replace-with-new\"}"));
        org.mockito.Mockito.doThrow(new IllegalStateException("audit required"))
            .when(criticalCommandAuditSupport)
            .recordAudit(any(), any(), any(), any(), any(), any(), any());

        assertThatThrownBy(() -> service.replaceWithNewAssignment(
            replacementCommand(77L, 800L, NEW_CYCLE_DEADLINE_AT, "replacement note")
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("audit required");

        verify(assignmentAdministrativeActionRepository)
            .saveAssignmentAdministrativeAction(any(AssignmentAdministrativeAction.class));
        verify(assignmentTestRepository).findAssignmentTestsByAssignmentId(77L);
        verify(assignmentTestRepository).saveAssignmentTest(any(AssignmentTest.class));
        verify(assignmentStatusRecalculationService).refreshAssignmentStatusCache(77L, OCCURRED_AT);
        verify(assignmentStatusRecalculationService).refreshAssignmentStatusCache(88L, OCCURRED_AT);
        verify(criticalCommandAuditSupport).recordAudit(
            eq(777L),
            any(),
            eq("assignment"),
            eq(77L),
            any(),
            any(),
            any()
        );
    }

    @Test
    void replaceWithNewUsesServiceOwnedClockChronologyInsteadOfExternalRequestedTimestamp() {
        CapabilityAdmissionRequest admissionRequest = admissionRequest(77L, 800L, EXTERNAL_REQUESTED_AT);
        Assignment existingAssignment = existingAssignment();
        java.util.concurrent.atomic.AtomicReference<Assignment> oldAssignmentState =
            new java.util.concurrent.atomic.AtomicReference<>(existingAssignment);
        java.util.concurrent.atomic.AtomicReference<Assignment> newAssignmentState =
            new java.util.concurrent.atomic.AtomicReference<>();
        when(capabilityAdmissionRequestFactory.createAssignmentReplaceWithNew(77L, 800L)).thenReturn(admissionRequest);
        when(assignmentRepository.findAssignmentById(77L)).thenAnswer(invocation -> oldAssignmentState.get());
        when(assignmentRepository.findAssignmentById(88L)).thenAnswer(invocation -> newAssignmentState.get());
        when(assignmentRepository.findActiveAssignmentByUserIdAndCourseId(101L, 301L))
            .thenAnswer(invocation -> oldAssignmentState.get());
        when(assignmentRepository.saveAssignment(any(Assignment.class))).thenAnswer(invocation -> {
            Assignment assignment = invocation.getArgument(0, Assignment.class);
            if (assignment.id() != null) {
                oldAssignmentState.set(assignment);
                return assignment;
            }
            Assignment saved = new Assignment(
                88L,
                assignment.campaignId(),
                assignment.userId(),
                assignment.courseId(),
                assignment.status(),
                assignment.assignedAt(),
                assignment.deadlineAt(),
                assignment.cancelledAt(),
                assignment.closedAt(),
                assignment.createdAt(),
                assignment.updatedAt()
            );
            newAssignmentState.set(saved);
            return saved;
        });
        when(assignmentTestRepository.findAssignmentTestsByAssignmentId(77L)).thenReturn(java.util.List.of());
        when(assignmentTestRepository.findAssignmentTestsByAssignmentId(88L)).thenReturn(java.util.List.of());
        when(assignmentAdministrativeActionRepository.saveAssignmentAdministrativeAction(any(AssignmentAdministrativeAction.class)))
            .thenAnswer(invocation -> {
                AssignmentAdministrativeAction action = invocation.getArgument(0, AssignmentAdministrativeAction.class);
                return new AssignmentAdministrativeAction(
                    501L,
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
            eq(CapabilityOperationCode.ASSIGNMENT_REPLACE_WITH_NEW),
            any(Map.class)
        )).thenReturn(new AuditContext("{\"operation\":\"replace-with-new\"}"));

        Assignment result = service.replaceWithNewAssignment(
            replacementCommand(77L, 800L, NEW_CYCLE_DEADLINE_AT, "replacement note")
        );

        assertThat(result.assignedAt()).isEqualTo(OCCURRED_AT);
        assertThat(result.assignedAt()).isNotEqualTo(EXTERNAL_REQUESTED_AT);

        ArgumentCaptor<AssignmentAdministrativeAction> administrativeActionCaptor =
            ArgumentCaptor.forClass(AssignmentAdministrativeAction.class);
        verify(assignmentAdministrativeActionRepository)
            .saveAssignmentAdministrativeAction(administrativeActionCaptor.capture());
        assertThat(administrativeActionCaptor.getValue().occurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(administrativeActionCaptor.getValue().occurredAt()).isNotEqualTo(EXTERNAL_REQUESTED_AT);
    }

    private CapabilityAdmissionRequest admissionRequest(Long assignmentId, Long campaignId, Instant requestedAt) {
        return new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.ASSIGNMENT_REPLACE_WITH_NEW,
            CapabilityTargetEntityType.ASSIGNMENT,
            assignmentId,
            new CapabilityAdmissionPayload.AssignmentReplaceWithNew(campaignId),
            requestedAt
        );
    }

    private AssignmentAdministrativeActionService.ReplaceWithNewAssignmentCommand replacementCommand(
        Long assignmentId,
        Long campaignId,
        Instant newCycleDeadlineAt,
        String note
    ) {
        return new AssignmentAdministrativeActionService.ReplaceWithNewAssignmentCommand(
            assignmentId,
            campaignId,
            newCycleDeadlineAt,
            note
        );
    }

    private Assignment existingAssignment() {
        return new Assignment(
            77L,
            800L,
            101L,
            301L,
            AssignmentStatus.ASSIGNED,
            OLD_ASSIGNED_AT,
            OLD_DEADLINE_AT,
            null,
            null,
            OLD_ASSIGNED_AT.minusSeconds(60),
            OLD_ASSIGNED_AT.minusSeconds(60)
        );
    }

    private Assignment overdueAssignment() {
        return new Assignment(
            77L,
            800L,
            101L,
            301L,
            AssignmentStatus.OVERDUE,
            OLD_ASSIGNED_AT,
            OVERDUE_DEADLINE_AT,
            null,
            null,
            OLD_ASSIGNED_AT.minusSeconds(60),
            OLD_ASSIGNED_AT.minusSeconds(60)
        );
    }

    private Assignment cancelledAssignment() {
        return new Assignment(
            77L,
            800L,
            101L,
            301L,
            AssignmentStatus.CANCELLED,
            OLD_ASSIGNED_AT,
            OLD_DEADLINE_AT,
            OCCURRED_AT,
            null,
            OLD_ASSIGNED_AT.minusSeconds(60),
            OCCURRED_AT
        );
    }

    private Assignment replacementAssignment() {
        return new Assignment(
            88L,
            800L,
            101L,
            301L,
            AssignmentStatus.ASSIGNED,
            OCCURRED_AT,
            OLD_DEADLINE_AT,
            null,
            null,
            OCCURRED_AT,
            OCCURRED_AT
        );
    }

    private AssignmentTest assignmentTest(
        Long assignmentTestId,
        Long assignmentId,
        Long testId,
        AssignmentTestRole assignmentTestRole
    ) {
        return new AssignmentTest(
            assignmentTestId,
            assignmentId,
            testId,
            assignmentTestRole,
            null,
            null,
            false,
            OLD_ASSIGNED_AT.minusSeconds(30),
            OLD_ASSIGNED_AT.minusSeconds(30)
        );
    }
}
