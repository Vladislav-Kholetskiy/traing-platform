package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.assignment.domain.Assignment;
import java.time.Instant;
import java.util.Objects;

/**
 * Контракт сервиса {@code AssignmentAdministrativeActionService}.
 */
public interface AssignmentAdministrativeActionService {

    /**
     * Команда отмены существующего назначения.
     */
    record CancelAssignmentCommand(Long assignmentId, String note) {
        public CancelAssignmentCommand {
            Objects.requireNonNull(assignmentId, "assignmentId must not be null");
        }
    }

    /**
     * Команда продления срока уже созданного назначения.
     */
    record ExtendAssignmentDeadlineCommand(Long assignmentId, Instant newDeadlineAt, String note) {
        public ExtendAssignmentDeadlineCommand {
            Objects.requireNonNull(assignmentId, "assignmentId must not be null");
            Objects.requireNonNull(newDeadlineAt, "newDeadlineAt must not be null");
        }
    }

    /**
     * Команда замены текущего назначения новым циклом на основе другой кампании.
     */
    record ReplaceWithNewAssignmentCommand(
        Long assignmentId,
        Long campaignId,
        Instant newCycleDeadlineAt,
        String note
    ) {
        public ReplaceWithNewAssignmentCommand {
            Objects.requireNonNull(assignmentId, "assignmentId must not be null");
            Objects.requireNonNull(campaignId, "campaignId must not be null");
        }
    }

    Assignment cancelAssignment(CancelAssignmentCommand cancelCommand);

    Assignment extendAssignmentDeadline(ExtendAssignmentDeadlineCommand extendCommand);

    Assignment replaceWithNewAssignment(ReplaceWithNewAssignmentCommand replacementCommand);
}

