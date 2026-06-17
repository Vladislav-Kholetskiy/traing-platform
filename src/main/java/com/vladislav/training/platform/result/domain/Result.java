package com.vladislav.training.platform.result.domain;

import com.vladislav.training.platform.common.model.AttemptMode;
import java.time.Instant;
import java.util.Objects;
/**
 * Запись данных {@code Result}.
 */
public record Result(
    Long id,
    Long testAttemptId,
    Long userIdSnapshot,
    AttemptMode attemptMode,
    Long assignmentId,
    Long assignmentTestId,
    Long testIdSnapshot,
    String testNameSnapshot,
    ResultScoringSnapshot scoringSnapshot,
    Boolean withinDeadline,
    Boolean countedInAssignment,
    Instant completedAt,
    ResultOrgContextSnapshot orgContextSnapshot,
    boolean snapshotFinalTopicControlFlag,
    Instant createdAt
) {
    public Result {
        Objects.requireNonNull(testAttemptId, "testAttemptId must not be null");
        Objects.requireNonNull(userIdSnapshot, "userIdSnapshot must not be null");
        Objects.requireNonNull(attemptMode, "attemptMode must not be null");
        Objects.requireNonNull(testIdSnapshot, "testIdSnapshot must not be null");
        Objects.requireNonNull(testNameSnapshot, "testNameSnapshot must not be null");
        Objects.requireNonNull(scoringSnapshot, "scoringSnapshot must not be null");
        Objects.requireNonNull(completedAt, "completedAt must not be null");
        Objects.requireNonNull(orgContextSnapshot, "orgContextSnapshot must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (testNameSnapshot.isBlank()) {
            throw new IllegalArgumentException("testNameSnapshot must not be blank");
        }
        if (attemptMode == AttemptMode.ASSIGNED) {
            Objects.requireNonNull(assignmentId, "assignmentId must not be null for ASSIGNED mode");
            Objects.requireNonNull(assignmentTestId, "assignmentTestId must not be null for ASSIGNED mode");
            Objects.requireNonNull(withinDeadline, "withinDeadline must not be null for ASSIGNED mode");
            Objects.requireNonNull(countedInAssignment, "countedInAssignment must not be null for ASSIGNED mode");
        }
        if (attemptMode == AttemptMode.SELF) {
            if (assignmentId != null) {
                throw new IllegalArgumentException("assignmentId must be null for SELF mode");
            }
            if (assignmentTestId != null) {
                throw new IllegalArgumentException("assignmentTestId must be null for SELF mode");
            }
            if (withinDeadline != null) {
                throw new IllegalArgumentException("withinDeadline must be null for SELF mode");
            }
            if (countedInAssignment != null) {
                throw new IllegalArgumentException("countedInAssignment must be null for SELF mode");
            }
        }
    }
}
