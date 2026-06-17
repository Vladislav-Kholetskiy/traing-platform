package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.application.policy.CapabilityOperationCode;
import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentAdministrativeAction;
import com.vladislav.training.platform.assignment.domain.AssignmentAdministrativeActionType;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import com.vladislav.training.platform.assignment.repository.AssignmentAdministrativeActionRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentTestRepository;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class AssignmentAdministrativeActionServiceImpl implements AssignmentAdministrativeActionService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentAdministrativeActionRepository assignmentAdministrativeActionRepository;
    private final AssignmentTestRepository assignmentTestRepository;
    private final AssignmentStatusRecalculationService assignmentStatusRecalculationService;
    private final CapabilityAdmissionPolicy capabilityAdmissionPolicy;
    private final CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;
    private final CriticalCommandAuditSupport criticalCommandAuditSupport;
    private final UtcClock utcClock;
    private final AssignmentAdministrativeActionNotificationService assignmentAdministrativeActionNotificationService;
    private final AssignmentReplacementValidationService AssignmentReplacementValidationService;
    private final AssignmentCriticalAuditPlanner AssignmentCriticalAuditPlanner =
        new AssignmentCriticalAuditPlannerImpl();
    private final AssignmentCriticalAuditPayloadFactory assignmentCriticalAuditPayloadFactory =
        new AssignmentCriticalAuditPayloadFactory();

    AssignmentAdministrativeActionServiceImpl(
        AssignmentRepository assignmentRepository,
        AssignmentAdministrativeActionRepository assignmentAdministrativeActionRepository,
        AssignmentTestRepository assignmentTestRepository,
        AssignmentStatusRecalculationService assignmentStatusRecalculationService,
        CapabilityAdmissionPolicy capabilityAdmissionPolicy,
        CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory,
        CriticalCommandAuditSupport criticalCommandAuditSupport,
        UtcClock utcClock,
        AssignmentAdministrativeActionNotificationService assignmentAdministrativeActionNotificationService
    ) {
        this.assignmentRepository = assignmentRepository;
        this.assignmentAdministrativeActionRepository = assignmentAdministrativeActionRepository;
        this.assignmentTestRepository = assignmentTestRepository;
        this.assignmentStatusRecalculationService = assignmentStatusRecalculationService;
        this.capabilityAdmissionPolicy = capabilityAdmissionPolicy;
        this.capabilityAdmissionRequestFactory = capabilityAdmissionRequestFactory;
        this.criticalCommandAuditSupport = criticalCommandAuditSupport;
        this.utcClock = utcClock;
        this.assignmentAdministrativeActionNotificationService = assignmentAdministrativeActionNotificationService;
        this.AssignmentReplacementValidationService = new AssignmentReplacementValidationService(assignmentRepository);
    }

    @Override
    public Assignment cancelAssignment(CancelAssignmentCommand cancelCommand) {
        Objects.requireNonNull(cancelCommand, "cancelCommand must not be null");

        CapabilityAdmissionRequest admissionRequest = capabilityAdmissionRequestFactory.createAssignmentCancel(
            cancelCommand.assignmentId()
        );
        capabilityAdmissionPolicy.check(admissionRequest);

        Instant occurredAt = admissionRequest.requestedAt();
        Assignment existingAssignment = requireExistingAssignment(cancelCommand.assignmentId());
        ensureCancelable(existingAssignment);

        Assignment cancellationFactPersisted = assignmentRepository.saveAssignment(new Assignment(
            existingAssignment.id(),
            existingAssignment.campaignId(),
            existingAssignment.userId(),
            existingAssignment.courseId(),
            existingAssignment.status(),
            existingAssignment.assignedAt(),
            existingAssignment.deadlineAt(),
            occurredAt,
            existingAssignment.closedAt(),
            existingAssignment.createdAt(),
            occurredAt
        ));

        AssignmentAdministrativeAction administrativeAction =
            assignmentAdministrativeActionRepository.saveAssignmentAdministrativeAction(
                new AssignmentAdministrativeAction(
                    null,
                    cancellationFactPersisted.id(),
                    AssignmentAdministrativeActionType.CANCEL_ASSIGNMENT,
                    occurredAt,
                    normalizeOptional(cancelCommand.note()),
                    occurredAt
                )
            );

        Assignment refreshedAssignment = assignmentStatusRecalculationService.refreshAssignmentStatusCache(
            cancellationFactPersisted.id(),
            occurredAt
        );

        recordCancelAudit(
            existingAssignment,
            refreshedAssignment,
            administrativeAction,
            cancelCommand,
            admissionRequest
        );
        assignmentAdministrativeActionNotificationService.createCancellationNotifications(
            existingAssignment,
            refreshedAssignment,
            occurredAt
        );
        return refreshedAssignment;
    }

    @Override
    public Assignment extendAssignmentDeadline(ExtendAssignmentDeadlineCommand extendCommand) {
        Objects.requireNonNull(extendCommand, "extendCommand must not be null");

        CapabilityAdmissionRequest admissionRequest = capabilityAdmissionRequestFactory.createAssignmentDeadlineExtend(
            extendCommand.assignmentId(),
            extendCommand.newDeadlineAt()
        );
        capabilityAdmissionPolicy.check(admissionRequest);

        Instant occurredAt = admissionRequest.requestedAt();
        Assignment existingAssignment = requireExistingAssignment(extendCommand.assignmentId());
        ensureDeadlineExtendable(existingAssignment, extendCommand.newDeadlineAt(), occurredAt);

        Assignment deadlineFactPersisted = assignmentRepository.saveAssignment(new Assignment(
            existingAssignment.id(),
            existingAssignment.campaignId(),
            existingAssignment.userId(),
            existingAssignment.courseId(),
            existingAssignment.status(),
            existingAssignment.assignedAt(),
            extendCommand.newDeadlineAt(),
            existingAssignment.cancelledAt(),
            existingAssignment.closedAt(),
            existingAssignment.createdAt(),
            occurredAt
        ));

        AssignmentAdministrativeAction administrativeAction =
            assignmentAdministrativeActionRepository.saveAssignmentAdministrativeAction(
                new AssignmentAdministrativeAction(
                    null,
                    deadlineFactPersisted.id(),
                    AssignmentAdministrativeActionType.EXTEND_DEADLINE,
                    occurredAt,
                    normalizeOptional(extendCommand.note()),
                    occurredAt
                )
            );

        Assignment refreshedAssignment = assignmentStatusRecalculationService.refreshAssignmentStatusCache(
            deadlineFactPersisted.id(),
            occurredAt
        );

        recordDeadlineExtendAudit(
            existingAssignment,
            refreshedAssignment,
            administrativeAction,
            extendCommand,
            admissionRequest
        );
        assignmentAdministrativeActionNotificationService.createDeadlineExtendedNotifications(
            existingAssignment,
            refreshedAssignment,
            occurredAt
        );
        return refreshedAssignment;
    }

    @Override
    public Assignment replaceWithNewAssignment(ReplaceWithNewAssignmentCommand replacementCommand) {
        Objects.requireNonNull(replacementCommand, "replacementCommand must not be null");

        CapabilityAdmissionRequest admissionRequest = capabilityAdmissionRequestFactory.createAssignmentReplaceWithNew(
            replacementCommand.assignmentId(),
            replacementCommand.campaignId()
        );
        capabilityAdmissionPolicy.check(admissionRequest);

        Instant occurredAt = utcClock.now();
        Assignment existingAssignment = requireExistingAssignment(replacementCommand.assignmentId());
        AssignmentReplacementValidationService.ReplacementAssignmentCycleIntent replacementIntent =
            AssignmentReplacementValidationService.buildReplacementIntent(existingAssignment, replacementCommand, occurredAt);
        AssignmentReplacementValidationService.validateTargetedReplacement(existingAssignment, replacementIntent);

        Assignment cancelledAssignment = assignmentRepository.saveAssignment(cancelReplacementTarget(
            existingAssignment,
            occurredAt
        ));
        Assignment replacementAssignment = createReplacementAssignmentCycle(replacementIntent, occurredAt);
        materializeReplacementAssignmentTests(existingAssignment.id(), replacementAssignment.id(), occurredAt);
        AssignmentAdministrativeAction administrativeAction = assignmentAdministrativeActionRepository.saveAssignmentAdministrativeAction(
            replacementAdministrativeAction(cancelledAssignment.id(), occurredAt, replacementCommand.note())
        );
        Assignment refreshedCancelledAssignment = assignmentStatusRecalculationService.refreshAssignmentStatusCache(
            cancelledAssignment.id(),
            occurredAt
        );
        Assignment refreshedReplacementAssignment = assignmentStatusRecalculationService.refreshAssignmentStatusCache(
            replacementAssignment.id(),
            occurredAt
        );
        recordReplacementAudit(
            existingAssignment,
            refreshedCancelledAssignment,
            refreshedReplacementAssignment,
            administrativeAction,
            replacementCommand.note()
        );
        assignmentAdministrativeActionNotificationService.createReplacementNotifications(
            existingAssignment,
            refreshedCancelledAssignment,
            refreshedReplacementAssignment,
            occurredAt
        );

        return refreshedReplacementAssignment;
    }

    private Assignment requireExistingAssignment(Long assignmentId) {
        Assignment assignment = assignmentRepository.findAssignmentById(assignmentId);
        if (assignment == null) {
            throw new NotFoundException("Assignment not found: " + assignmentId);
        }
        return assignment;
    }

    private void ensureCancelable(Assignment assignment) {
        if (assignment.cancelledAt() != null || assignment.status() == AssignmentStatus.CANCELLED) {
            throw new ConflictException("CANCEL_ASSIGNMENT is not allowed for already CANCELLED assignment: " + assignment.id());
        }
        if (assignment.closedAt() != null || assignment.status() == AssignmentStatus.COMPLETED) {
            throw new ConflictException("CANCEL_ASSIGNMENT is not allowed for COMPLETED assignment: " + assignment.id());
        }
    }

    private void ensureDeadlineExtendable(Assignment assignment, Instant newDeadlineAt, Instant occurredAt) {
        if (assignment.cancelledAt() != null || assignment.status() == AssignmentStatus.CANCELLED) {
            throw new ConflictException(
                "EXTEND_DEADLINE is not allowed for already CANCELLED assignment: " + assignment.id()
            );
        }
        if (assignment.closedAt() != null || assignment.status() == AssignmentStatus.COMPLETED) {
            throw new ConflictException(
                "EXTEND_DEADLINE is not allowed for COMPLETED assignment: " + assignment.id()
            );
        }
        if (!newDeadlineAt.isAfter(assignment.deadlineAt())) {
            throw new ConflictException(
                "EXTEND_DEADLINE requires a strictly later deadline for assignment: " + assignment.id()
            );
        }
        if (assignment.deadlineAt().isBefore(occurredAt) && !newDeadlineAt.isAfter(occurredAt)) {
            throw new ConflictException(
                "EXTEND_DEADLINE must reopen the assignment window for overdue assignment: " + assignment.id()
            );
        }
    }

    private Assignment cancelReplacementTarget(Assignment existingAssignment, Instant occurredAt) {
        return new Assignment(
            existingAssignment.id(),
            existingAssignment.campaignId(),
            existingAssignment.userId(),
            existingAssignment.courseId(),
            AssignmentStatus.CANCELLED,
            existingAssignment.assignedAt(),
            existingAssignment.deadlineAt(),
            occurredAt,
            existingAssignment.closedAt(),
            existingAssignment.createdAt(),
            occurredAt
        );
    }

    private Assignment createReplacementAssignmentCycle(
        AssignmentReplacementValidationService.ReplacementAssignmentCycleIntent replacementIntent,
        Instant occurredAt
    ) {
        try {
            return assignmentRepository.saveAssignment(new Assignment(
                null,
                replacementIntent.campaignId(),
                replacementIntent.assigneeUserId(),
                replacementIntent.courseId(),
                AssignmentStatus.ASSIGNED,
                replacementIntent.assignedAt(),
                replacementIntent.deadlineAt(),
                null,
                null,
                occurredAt,
                occurredAt
            ));
        } catch (PersistenceConstraintViolationException exception) {
            throw new ConflictException(
                "REPLACE_WITH_NEW detected replacement-cycle collision for assignment: "
                    + replacementIntent.targetAssignmentId()
            );
        }
    }

    private AssignmentAdministrativeAction replacementAdministrativeAction(Long assignmentId, Instant occurredAt, String note) {
        return new AssignmentAdministrativeAction(
            null,
            assignmentId,
            AssignmentAdministrativeActionType.REPLACE_WITH_NEW_ASSIGNMENT,
            occurredAt,
            normalizeOptional(note),
            occurredAt
        );
    }

    private void materializeReplacementAssignmentTests(Long oldAssignmentId, Long newAssignmentId, Instant occurredAt) {
        List<AssignmentTest> oldAssignmentTests = assignmentTestRepository.findAssignmentTestsByAssignmentId(oldAssignmentId);
        for (AssignmentTest oldAssignmentTest : oldAssignmentTests) {
            assignmentTestRepository.saveAssignmentTest(new AssignmentTest(
                null,
                newAssignmentId,
                oldAssignmentTest.testId(),
                oldAssignmentTest.assignmentTestRole(),
                null,
                null,
                false,
                occurredAt,
                occurredAt
            ));
        }
    }

    private void recordReplacementAudit(
        Assignment assignmentBefore,
        Assignment cancelledAssignmentAfter,
        Assignment replacementAssignmentAfter,
        AssignmentAdministrativeAction administrativeAction,
        String note
    ) {
        AssignmentCriticalAuditPlanner.AdministrativeAuditPlan auditCompanionPlan =
            AssignmentCriticalAuditPlanner.planAdministrativeAudit(
                CapabilityOperationCode.ASSIGNMENT_REPLACE_WITH_NEW,
                cancelledAssignmentAfter.id()
            );

        Map<String, Object> details = assignmentCriticalAuditPayloadFactory.administrativeDetails(
            "replace_with_new_assignment",
            administrativeAction.occurredAt(),
            administrativeAction.note() != null,
            Map.of(
                "administrativeActionType", administrativeAction.actionType(),
                "administrativeActionId", administrativeAction.id(),
                "relatedAssignmentId", replacementAssignmentAfter.id()
            )
        );
        Map<String, Object> payloadBefore = assignmentCriticalAuditPayloadFactory.assignmentPayload(assignmentBefore);
        Map<String, Object> payloadAfter = assignmentCriticalAuditPayloadFactory.administrativePayloadAfter(
            cancelledAssignmentAfter,
            administrativeAction,
            assignmentCriticalAuditPayloadFactory.replaceCommandPayload(normalizeOptional(note)),
            assignmentCriticalAuditPayloadFactory.replacementAssignmentRelatedPayload(replacementAssignmentAfter)
        );

        criticalCommandAuditSupport.recordAudit(
            criticalCommandAuditSupport.resolveInteractiveActorUserId(),
            auditCompanionPlan.auditCatalog().auditEventType(),
            auditCompanionPlan.auditEntityType(),
            auditCompanionPlan.auditEntityId(),
            payloadBefore,
            payloadAfter,
            criticalCommandAuditSupport.buildAuditContext(
                "Assignment",
                auditCompanionPlan.auditCatalog().operationCode(),
                details
            )
        );
    }

    private void recordCancelAudit(
        Assignment assignmentBefore,
        Assignment assignmentAfter,
        AssignmentAdministrativeAction administrativeAction,
        CancelAssignmentCommand cancelCommand,
        CapabilityAdmissionRequest admissionRequest
    ) {
        AssignmentCriticalAuditPlanner.AdministrativeAuditPlan auditCompanionPlan =
            AssignmentCriticalAuditPlanner.planAdministrativeAudit(
                CapabilityOperationCode.ASSIGNMENT_CANCEL,
                assignmentAfter.id()
            );

        Map<String, Object> details = assignmentCriticalAuditPayloadFactory.administrativeDetails(
            "cancel_assignment",
            administrativeAction.occurredAt(),
            administrativeAction.note() != null,
            Map.of(
                "administrativeActionType", administrativeAction.actionType(),
                "administrativeActionId", administrativeAction.id()
            )
        );
        Map<String, Object> payloadBefore = assignmentCriticalAuditPayloadFactory.assignmentPayload(assignmentBefore);
        Map<String, Object> payloadAfter = assignmentCriticalAuditPayloadFactory.administrativePayloadAfter(
            assignmentAfter,
            administrativeAction,
            assignmentCriticalAuditPayloadFactory.cancelCommandPayload(normalizeOptional(cancelCommand.note())),
            null
        );

        criticalCommandAuditSupport.recordAudit(
            criticalCommandAuditSupport.resolveInteractiveActorUserId(),
            auditCompanionPlan.auditCatalog().auditEventType(),
            auditCompanionPlan.auditEntityType(),
            auditCompanionPlan.auditEntityId(),
            payloadBefore,
            payloadAfter,
            criticalCommandAuditSupport.buildAuditContext(
                "Assignment",
                auditCompanionPlan.auditCatalog().operationCode(),
                details
            )
        );
    }

    private void recordDeadlineExtendAudit(
        Assignment assignmentBefore,
        Assignment assignmentAfter,
        AssignmentAdministrativeAction administrativeAction,
        ExtendAssignmentDeadlineCommand extendCommand,
        CapabilityAdmissionRequest admissionRequest
    ) {
        AssignmentCriticalAuditPlanner.AdministrativeAuditPlan auditCompanionPlan =
            AssignmentCriticalAuditPlanner.planAdministrativeAudit(
                CapabilityOperationCode.ASSIGNMENT_DEADLINE_EXTEND,
                assignmentAfter.id()
            );

        Map<String, Object> details = assignmentCriticalAuditPayloadFactory.administrativeDetails(
            "extend_deadline",
            administrativeAction.occurredAt(),
            administrativeAction.note() != null,
            Map.of(
                "administrativeActionType", administrativeAction.actionType(),
                "administrativeActionId", administrativeAction.id()
            )
        );
        Map<String, Object> payloadBefore = assignmentCriticalAuditPayloadFactory.assignmentPayload(assignmentBefore);
        Map<String, Object> payloadAfter = assignmentCriticalAuditPayloadFactory.administrativePayloadAfter(
            assignmentAfter,
            administrativeAction,
            assignmentCriticalAuditPayloadFactory.extendDeadlineCommandPayload(
                extendCommand.newDeadlineAt(),
                normalizeOptional(extendCommand.note())
            ),
            null
        );

        criticalCommandAuditSupport.recordAudit(
            criticalCommandAuditSupport.resolveInteractiveActorUserId(),
            auditCompanionPlan.auditCatalog().auditEventType(),
            auditCompanionPlan.auditEntityType(),
            auditCompanionPlan.auditEntityId(),
            payloadBefore,
            payloadAfter,
            criticalCommandAuditSupport.buildAuditContext(
                "Assignment",
                auditCompanionPlan.auditCatalog().operationCode(),
                details
            )
        );
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}



