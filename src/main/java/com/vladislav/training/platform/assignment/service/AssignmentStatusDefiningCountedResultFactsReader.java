package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.common.model.AttemptMode;

public interface AssignmentStatusDefiningCountedResultFactsReader {

    StatusDefiningCountedResultFacts findStatusDefiningFactsByCountedResultId(Long countedResultId);

    CountedAssignmentResultHandoffFacts findCountedAssignmentResultHandoffFactsByResultId(Long resultId);

    record StatusDefiningCountedResultFacts(
        boolean passed,
        boolean withinDeadline,
        boolean countedInAssignment
    ) {
    }

    record CountedAssignmentResultHandoffFacts(
        Long resultId,
        Long assignmentId,
        Long assignmentTestId,
        AttemptMode attemptMode,
        boolean passed,
        boolean withinDeadline,
        boolean countedInAssignment,
        boolean snapshotFinalTopicControlFlag,
        java.time.Instant recordedAt
    ) {
    }
}
