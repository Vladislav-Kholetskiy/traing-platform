package com.vladislav.training.platform.result.service;

import com.vladislav.training.platform.result.domain.ResultScoringSnapshot;
import java.util.Objects;
import org.springframework.stereotype.Component;
/**
 * Класс {@code AssignmentCountedResultPolicy}.
 */

@Component
class AssignmentCountedResultPolicy {

    AssignmentCountedResultDecision decide(
        boolean finalControlSnapshot,
        ResultScoringSnapshot scoringSnapshot,
        boolean withinDeadline
    ) {
        Objects.requireNonNull(scoringSnapshot, "scoringSnapshot must not be null");

        if (!finalControlSnapshot) {
            return new AssignmentCountedResultDecision(false, AssignmentCountedResultDecisionReason.NOT_FINAL_CONTROL);
        }
        if (!scoringSnapshot.passed()) {
            return new AssignmentCountedResultDecision(false, AssignmentCountedResultDecisionReason.FAILED);
        }
        if (!withinDeadline) {
            return new AssignmentCountedResultDecision(false, AssignmentCountedResultDecisionReason.OUTSIDE_DEADLINE);
        }
        return new AssignmentCountedResultDecision(true, AssignmentCountedResultDecisionReason.COUNTED);
    }
}
