package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPayload;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.application.policy.CapabilityOperationCodes;
import com.vladislav.training.platform.application.policy.CapabilityTargetEntityType;
import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.repository.AssignmentAdministrativeActionRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentTestRepository;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.ValidationException;
import com.vladislav.training.platform.common.time.UtcClock;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code AssignmentAdministrativeActionServiceImplReplaceWithNewValidation}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class AssignmentAdministrativeActionServiceImplReplaceWithNewValidationTest {

    private static final Instant OCCURRED_AT = Instant.parse("2026-04-10T12:00:00Z");
    private static final Instant FUTURE_DEADLINE = Instant.parse("2026-04-12T12:00:00Z");
    private static final Instant REPLACEMENT_DEADLINE = Instant.parse("2026-04-15T12:00:00Z");

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
    private AssignmentReplacementValidationService validationSeam;

    @BeforeEach
    void setUp() {
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
        validationSeam = new AssignmentReplacementValidationService(assignmentRepository);
    }

    @Test
    void replaceWithNewRejectsWhenTargetAssignmentDoesNotExist() {
        CapabilityAdmissionRequest admissionRequest = admissionRequest(77L, 900L);
        when(capabilityAdmissionRequestFactory.createAssignmentReplaceWithNew(77L, 900L)).thenReturn(admissionRequest);
        when(assignmentRepository.findAssignmentById(77L)).thenReturn(null);

        assertThatThrownBy(() -> service.replaceWithNewAssignment(
            replaceCommand(77L, 900L, REPLACEMENT_DEADLINE, OCCURRED_AT, "replace")
        ))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Assignment not found: 77");

        verify(capabilityAdmissionPolicy).check(admissionRequest);
        verify(assignmentRepository).findAssignmentById(77L);
        verifyNoInteractions(
            assignmentAdministrativeActionRepository,
            assignmentStatusRecalculationService,
            criticalCommandAuditSupport
        );
    }

    @Test
    void replacementValidationRejectsTerminalTargetAssignment() {
        Assignment targetAssignment = assignment(
            77L,
            101L,
            301L,
            AssignmentStatus.COMPLETED,
            OCCURRED_AT.minusSeconds(7200),
            FUTURE_DEADLINE,
            null,
            OCCURRED_AT.minusSeconds(300)
        );
        AssignmentReplacementValidationService.ReplacementAssignmentCycleIntent replacementIntent =
            validationSeam.buildReplacementIntent(
                targetAssignment,
                replaceCommand(targetAssignment.id(), 800L, REPLACEMENT_DEADLINE, OCCURRED_AT, null),
                OCCURRED_AT
            );

        assertThatThrownBy(() -> validationSeam.validateTargetedReplacement(targetAssignment, replacementIntent))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("terminal assignment");

        verifyNoInteractions(assignmentRepository);
    }

    @Test
    void replacementValidationRejectsAttemptToChangeAssignee() {
        Assignment targetAssignment = assignmentActive();
        AssignmentReplacementValidationService.ReplacementAssignmentCycleIntent replacementIntent =
            new AssignmentReplacementValidationService.ReplacementAssignmentCycleIntent(
                targetAssignment.id(),
                202L,
                targetAssignment.courseId(),
                900L,
                OCCURRED_AT,
                FUTURE_DEADLINE
            );

        assertThatThrownBy(() -> validationSeam.validateTargetedReplacement(targetAssignment, replacementIntent))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("must not change assignee");

        verifyNoInteractions(assignmentRepository);
    }

    @Test
    void replacementValidationRejectsAttemptToChangeCourse() {
        Assignment targetAssignment = assignmentActive();
        AssignmentReplacementValidationService.ReplacementAssignmentCycleIntent replacementIntent =
            new AssignmentReplacementValidationService.ReplacementAssignmentCycleIntent(
                targetAssignment.id(),
                targetAssignment.userId(),
                999L,
                900L,
                OCCURRED_AT,
                FUTURE_DEADLINE
            );

        assertThatThrownBy(() -> validationSeam.validateTargetedReplacement(targetAssignment, replacementIntent))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("must not change course");

        verifyNoInteractions(assignmentRepository);
    }

    @Test
    void replacementValidationRejectsMissingCampaignId() {
        Assignment targetAssignment = assignmentActive();
        assertThatThrownBy(() -> validationSeam.buildReplacementIntent(
            targetAssignment,
            replaceCommand(targetAssignment.id(), null, REPLACEMENT_DEADLINE, OCCURRED_AT, null),
            OCCURRED_AT
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("campaignId must not be null");

        verifyNoInteractions(assignmentRepository);
    }

    @Test
    void replacementValidationRejectsAdHocCampaignProvenanceChoice() {
        Assignment targetAssignment = assignmentActive();

        assertThatThrownBy(() -> validationSeam.buildReplacementIntent(
            targetAssignment,
            replaceCommand(targetAssignment.id(), 900L, REPLACEMENT_DEADLINE, OCCURRED_AT, null),
            OCCURRED_AT
        ))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("must not choose ad hoc campaignId provenance");

        verifyNoInteractions(assignmentRepository);
    }

    @Test
    void replacementValidationRejectsMissingNewCycleDeadlineSemantics() {
        Assignment targetAssignment = assignmentActive();
        AssignmentReplacementValidationService.ReplacementAssignmentCycleIntent replacementIntent =
            validationSeam.buildReplacementIntent(
                targetAssignment,
                replaceCommand(targetAssignment.id(), 800L, null, OCCURRED_AT, null),
                OCCURRED_AT
            );

        assertThatThrownBy(() -> validationSeam.validateTargetedReplacement(targetAssignment, replacementIntent))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("explicit new-cycle deadlineAt");

        verifyNoInteractions(assignmentRepository);
    }

    @Test
    void replacementValidationRejectsDeadlineEarlierThanAssignedAt() {
        Assignment targetAssignment = assignmentActive();
        AssignmentReplacementValidationService.ReplacementAssignmentCycleIntent replacementIntent =
            new AssignmentReplacementValidationService.ReplacementAssignmentCycleIntent(
                targetAssignment.id(),
                targetAssignment.userId(),
                targetAssignment.courseId(),
                800L,
                OCCURRED_AT,
                OCCURRED_AT.minusSeconds(60)
            );

        assertThatThrownBy(() -> validationSeam.validateTargetedReplacement(targetAssignment, replacementIntent))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("deadlineAt")
            .hasMessageContaining("assignedAt");

        verifyNoInteractions(assignmentRepository);
    }

    @Test
    void replacementValidationRejectsHiddenSecondActiveAssignmentSemantics() {
        Assignment targetAssignment = assignmentActive();
        Assignment anotherActiveAssignment = assignment(
            78L,
            101L,
            301L,
            AssignmentStatus.ASSIGNED,
            OCCURRED_AT.minusSeconds(1800),
            FUTURE_DEADLINE.plusSeconds(1800),
            null,
            null
        );
        AssignmentReplacementValidationService.ReplacementAssignmentCycleIntent replacementIntent =
            validationSeam.buildReplacementIntent(
                targetAssignment,
                replaceCommand(targetAssignment.id(), 800L, REPLACEMENT_DEADLINE, OCCURRED_AT, null),
                OCCURRED_AT
            );
        when(assignmentRepository.findActiveAssignmentByUserIdAndCourseId(101L, 301L)).thenReturn(anotherActiveAssignment);

        assertThatThrownBy(() -> validationSeam.validateTargetedReplacement(targetAssignment, replacementIntent))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("hidden second active assignment semantics");

        verify(assignmentRepository).findActiveAssignmentByUserIdAndCourseId(101L, 301L);
        verifyNoMoreInteractions(assignmentRepository);
    }

    @Test
    void replacementValidationAllowsCurrentTargetAsTheOnlyActiveAssignment() {
        Assignment targetAssignment = assignmentActive();
        AssignmentReplacementValidationService.ReplacementAssignmentCycleIntent replacementIntent =
            validationSeam.buildReplacementIntent(
                targetAssignment,
                replaceCommand(targetAssignment.id(), 800L, REPLACEMENT_DEADLINE, OCCURRED_AT, null),
                OCCURRED_AT
            );
        when(assignmentRepository.findActiveAssignmentByUserIdAndCourseId(101L, 301L)).thenReturn(targetAssignment);

        validationSeam.validateTargetedReplacement(targetAssignment, replacementIntent);

        verify(assignmentRepository).findActiveAssignmentByUserIdAndCourseId(101L, 301L);
        verifyNoMoreInteractions(assignmentRepository);
    }

    @Test
    void replacementValidationAllowsCurrentOverdueTargetAsTheOnlyActiveAssignment() {
        Assignment targetAssignment = assignment(
            77L,
            101L,
            301L,
            AssignmentStatus.OVERDUE,
            OCCURRED_AT.minusSeconds(7200),
            OCCURRED_AT.minusSeconds(60),
            null,
            null
        );
        AssignmentReplacementValidationService.ReplacementAssignmentCycleIntent replacementIntent =
            validationSeam.buildReplacementIntent(
                targetAssignment,
                replaceCommand(targetAssignment.id(), 800L, REPLACEMENT_DEADLINE, OCCURRED_AT, null),
                OCCURRED_AT
            );
        when(assignmentRepository.findActiveAssignmentByUserIdAndCourseId(101L, 301L)).thenReturn(targetAssignment);

        validationSeam.validateTargetedReplacement(targetAssignment, replacementIntent);

        verify(assignmentRepository).findActiveAssignmentByUserIdAndCourseId(101L, 301L);
        verifyNoMoreInteractions(assignmentRepository);
    }

    @Test
    void canonicalReplacementIntentPreservesTypedCycleSemanticsWithoutGenericRewriteDrift() {
        Assignment targetAssignment = assignmentActive();

        AssignmentReplacementValidationService.ReplacementAssignmentCycleIntent replacementIntent =
            validationSeam.buildReplacementIntent(
                targetAssignment,
                replaceCommand(targetAssignment.id(), 800L, REPLACEMENT_DEADLINE, OCCURRED_AT, null),
                OCCURRED_AT
            );

        assertThat(replacementIntent.targetAssignmentId()).isEqualTo(targetAssignment.id());
        assertThat(replacementIntent.assigneeUserId()).isEqualTo(targetAssignment.userId());
        assertThat(replacementIntent.courseId()).isEqualTo(targetAssignment.courseId());
        assertThat(replacementIntent.campaignId()).isEqualTo(800L);
        assertThat(replacementIntent.assignedAt()).isEqualTo(OCCURRED_AT);
        assertThat(replacementIntent.deadlineAt()).isEqualTo(REPLACEMENT_DEADLINE);
        assertThat(replacementIntent.deadlineAt()).isNotEqualTo(targetAssignment.deadlineAt());
        assertThat(replacementIntent.assigneeUserId()).isNotNull();
        assertThat(replacementIntent.courseId()).isNotNull();

        verifyNoInteractions(assignmentRepository);
    }

    private CapabilityAdmissionRequest admissionRequest(Long assignmentId, Long campaignId) {
        return new CapabilityAdmissionRequest(
            101L,
            CapabilityOperationCodes.ASSIGNMENT_REPLACE_WITH_NEW,
            CapabilityTargetEntityType.ASSIGNMENT,
            assignmentId,
            new CapabilityAdmissionPayload.AssignmentReplaceWithNew(campaignId),
            OCCURRED_AT
        );
    }

    private AssignmentAdministrativeActionService.ReplaceWithNewAssignmentCommand replaceCommand(
        Long assignmentId,
        Long campaignId,
        Instant newCycleDeadlineAt,
        Instant occurredAt,
        String note
    ) {
        return new AssignmentAdministrativeActionService.ReplaceWithNewAssignmentCommand(
            assignmentId,
            campaignId,
            newCycleDeadlineAt,
            note
        );
    }

    private Assignment assignmentActive() {
        return assignment(
            77L,
            101L,
            301L,
            AssignmentStatus.ASSIGNED,
            OCCURRED_AT.minusSeconds(7200),
            FUTURE_DEADLINE,
            null,
            null
        );
    }

    private Assignment assignment(
        Long assignmentId,
        Long userId,
        Long courseId,
        AssignmentStatus status,
        Instant assignedAt,
        Instant deadlineAt,
        Instant cancelledAt,
        Instant closedAt
    ) {
        return new Assignment(
            assignmentId,
            800L,
            userId,
            courseId,
            status,
            assignedAt,
            deadlineAt,
            cancelledAt,
            closedAt,
            assignedAt.minusSeconds(60),
            assignedAt.minusSeconds(60)
        );
    }
}


