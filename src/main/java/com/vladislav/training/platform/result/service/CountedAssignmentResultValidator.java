package com.vladislav.training.platform.result.service;

import com.vladislav.training.platform.result.domain.Result;
import java.util.Objects;
import org.springframework.stereotype.Service;
/**
 * Проверка {@code CountedAssignmentResultValidator}.
 */

@Service
class CountedAssignmentResultValidator implements CountedAssignmentResultValidityGate {

    @Override
    public boolean allowsAssignmentCountedHandoff(Result materializedResult) {
        Objects.requireNonNull(materializedResult, "materializedResult must not be null");

        if (materializedResult.assignmentId() == null || materializedResult.assignmentTestId() == null) {
            return false;
        }
        if (!Boolean.TRUE.equals(materializedResult.countedInAssignment())) {
            return false;
        }
        if (!Boolean.TRUE.equals(materializedResult.withinDeadline())) {
            return false;
        }
        if (!materializedResult.snapshotFinalTopicControlFlag()) {
            return false;
        }
        return materializedResult.scoringSnapshot() != null && materializedResult.scoringSnapshot().passed();
    }
}

