package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.repository.AssignmentRepository;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.ValidationException;
import java.time.Instant;
import java.util.Objects;

/**
 * Контракт сервиса {@code AssignmentReplacementValidationService}.
 */
final class AssignmentReplacementValidationService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentReplacementProvenancePolicy replacementProvenancePolicy;

    AssignmentReplacementValidationService(AssignmentRepository assignmentRepository) {
        this(assignmentRepository, new AssignmentReplacementProvenancePolicy());
    }

    AssignmentReplacementValidationService(
        AssignmentRepository assignmentRepository,
        AssignmentReplacementProvenancePolicy replacementProvenancePolicy
    ) {
        this.assignmentRepository = assignmentRepository;
        this.replacementProvenancePolicy = replacementProvenancePolicy;
    }

    ReplacementAssignmentCycleIntent buildReplacementIntent(
        Assignment targetAssignment,
        AssignmentAdministrativeActionService.ReplaceWithNewAssignmentCommand replacementCommand,
        Instant assignedAt
    ) {
        Objects.requireNonNull(targetAssignment, "targetAssignment must not be null");
        Objects.requireNonNull(replacementCommand, "replacementCommand must not be null");
        Objects.requireNonNull(assignedAt, "assignedAt must not be null");

        return new ReplacementAssignmentCycleIntent(
            targetAssignment.id(),
            targetAssignment.userId(),
            targetAssignment.courseId(),
            replacementProvenancePolicy.resolveReplacementCampaignId(targetAssignment, replacementCommand),
            assignedAt,
            replacementCommand.newCycleDeadlineAt()
        );
    }

    void validateTargetedReplacement(Assignment targetAssignment, ReplacementAssignmentCycleIntent replacementIntent) {
        Objects.requireNonNull(targetAssignment, "targetAssignment must not be null");
        Objects.requireNonNull(replacementIntent, "replacementIntent must not be null");

        requireMatchingTargetAssignment(replacementIntent.targetAssignmentId(), targetAssignment.id());
        ensureNotTerminal(targetAssignment);
        requireSameAssignee(targetAssignment.userId(), replacementIntent.assigneeUserId(), targetAssignment.id());
        requireSameCourse(targetAssignment.courseId(), replacementIntent.courseId(), targetAssignment.id());
        requireExplicitCycleTiming(replacementIntent.assignedAt(), replacementIntent.deadlineAt(), targetAssignment.id());
        ensureNoHiddenSecondActiveAssignment(replacementIntent.assigneeUserId(), replacementIntent.courseId(), targetAssignment.id());
    }

    private void requireMatchingTargetAssignment(Long replacementTargetAssignmentId, Long targetAssignmentId) {
        if (!Objects.equals(replacementTargetAssignmentId, targetAssignmentId)) {
            throw new ValidationException(
                "Для замены назначения идентификатор целевого назначения должен совпадать с текущим"
            );
        }
    }

    private void ensureNotTerminal(Assignment targetAssignment) {
        if (targetAssignment.cancelledAt() != null
            || targetAssignment.closedAt() != null
            || targetAssignment.status() == AssignmentStatus.CANCELLED
            || targetAssignment.status() == AssignmentStatus.COMPLETED) {
            throw new ConflictException(
                "Замена недоступна для уже завершённого или отменённого назначения: "
                    + targetAssignment.id()
                    + ". terminal assignment"
            );
        }
    }

    private void requireSameAssignee(Long expectedUserId, Long replacementUserId, Long assignmentId) {
        if (!Objects.equals(expectedUserId, replacementUserId)) {
            throw new ValidationException(
                "При замене нельзя менять получателя назначения: "
                    + assignmentId
                    + ". replacement must not change assignee"
            );
        }
    }

    private void requireSameCourse(Long expectedCourseId, Long replacementCourseId, Long assignmentId) {
        if (!Objects.equals(expectedCourseId, replacementCourseId)) {
            throw new ValidationException(
                "При замене нельзя менять курс назначения: "
                    + assignmentId
                    + ". replacement must not change course"
            );
        }
    }

    private void requireExplicitCycleTiming(Instant assignedAt, Instant deadlineAt, Long assignmentId) {
        if (assignedAt == null) {
            throw new ValidationException(
                "Для замены нужно явно указать дату выдачи назначения: "
                    + assignmentId
                    + ". explicit assignedAt"
            );
        }
        if (deadlineAt == null) {
            throw new ValidationException(
                "Для замены нужно явно указать новый срок назначения: "
                    + assignmentId
                    + ". explicit new-cycle deadlineAt"
            );
        }
        if (deadlineAt.isBefore(assignedAt)) {
            throw new ValidationException(
                "Срок нового назначения должен быть не раньше даты выдачи: "
                    + assignmentId
                    + ". deadlineAt must not be earlier than assignedAt"
            );
        }
    }

    private void ensureNoHiddenSecondActiveAssignment(Long userId, Long courseId, Long targetAssignmentId) {
        Assignment activeAssignment = assignmentRepository.findActiveAssignmentByUserIdAndCourseId(userId, courseId);
        if (activeAssignment != null && !Objects.equals(activeAssignment.id(), targetAssignmentId)) {
            throw new ConflictException(
                "REPLACE_WITH_NEW is not allowed because it would create hidden second active assignment semantics"
                    + " for assignment: " + targetAssignmentId
            );
        }
    }

    record ReplacementAssignmentCycleIntent(
        Long targetAssignmentId,
        Long assigneeUserId,
        Long courseId,
        Long campaignId,
        Instant assignedAt,
        Instant deadlineAt
    ) {
    }
}
