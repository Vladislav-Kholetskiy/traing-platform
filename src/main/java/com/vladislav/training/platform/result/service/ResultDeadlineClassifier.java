package com.vladislav.training.platform.result.service;

import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import com.vladislav.training.platform.common.exception.ConflictException;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Component;
/**
 * Класс {@code ResultDeadlineClassifier}.
 */

@Component
class ResultDeadlineClassifier {

    boolean isWithinDeadline(Assignment assignment, AssignmentTest assignmentTest, Instant terminalInstant) {
        Objects.requireNonNull(assignment, "assignment must not be null");
        Objects.requireNonNull(assignmentTest, "assignmentTest must not be null");
        if (terminalInstant == null) {
            throw new ConflictException("Result completedAt/terminalInstant fact is required for deadline classification");
        }

        Instant deadlineAt = assignment.deadlineAt();
        if (deadlineAt == null) {
            throw new ConflictException("Result assignment deadlineAt fact is required for deadline classification");
        }
        return !deadlineAt.isBefore(terminalInstant);
    }
}
