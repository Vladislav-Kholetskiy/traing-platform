package com.vladislav.training.platform.result.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import com.vladislav.training.platform.assignment.domain.AssignmentTestRole;
import com.vladislav.training.platform.common.exception.ConflictException;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code ResultDeadlineClassifier}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class ResultDeadlineClassifierTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-24T09:00:00Z");

    private ResultDeadlineClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new ResultDeadlineClassifier();
    }

    @Test
    void returnsTrueWhenCompletedExactlyAtDeadline() {
        assertThat(classifier.isWithinDeadline(
            assignment(AssignmentStatus.COMPLETED, FIXED_INSTANT, null, null),
            openAssignmentTest(),
            FIXED_INSTANT
        )).isTrue();
    }

    @Test
    void returnsTrueWhenCompletedBeforeDeadline() {
        assertThat(classifier.isWithinDeadline(
            assignment(AssignmentStatus.OVERDUE, FIXED_INSTANT.plusSeconds(5), null, null),
            openAssignmentTest(),
            FIXED_INSTANT
        )).isTrue();
    }

    @Test
    void returnsFalseWhenCompletedAfterDeadline() {
        assertThat(classifier.isWithinDeadline(
            assignment(AssignmentStatus.ASSIGNED, FIXED_INSTANT.minusSeconds(5), null, null),
            openAssignmentTest(),
            FIXED_INSTANT
        )).isFalse();
    }

    @Test
    void rejectsMissingDeadlineForAssignedBackedResultFailClosed() {
        Assignment assignment = org.mockito.Mockito.mock(Assignment.class);
        org.mockito.Mockito.when(assignment.deadlineAt()).thenReturn(null);

        assertThatThrownBy(() -> classifier.isWithinDeadline(assignment, openAssignmentTest(), FIXED_INSTANT))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("assignment deadlineAt fact is required");
    }

    @Test
    void doesNotUseAssignmentStatusForDeadlineDecision() {
        Assignment assignment = assignment(AssignmentStatus.CANCELLED, FIXED_INSTANT.plusSeconds(30), null, null);

        assertThat(classifier.isWithinDeadline(assignment, openAssignmentTest(), FIXED_INSTANT)).isTrue();
    }

    @Test
    void ignoresAssignmentCancelledAtWhenClassifyingHistoricalDeadlineFact() {
        assertThat(classifier.isWithinDeadline(
            assignment(AssignmentStatus.ASSIGNED, FIXED_INSTANT.plusSeconds(30), FIXED_INSTANT.minusSeconds(5), null),
            openAssignmentTest(),
            FIXED_INSTANT
        )).isTrue();
    }

    @Test
    void ignoresAssignmentClosedAtWhenClassifyingHistoricalDeadlineFact() {
        assertThat(classifier.isWithinDeadline(
            assignment(AssignmentStatus.ASSIGNED, FIXED_INSTANT.plusSeconds(30), null, FIXED_INSTANT.minusSeconds(5)),
            openAssignmentTest(),
            FIXED_INSTANT
        )).isTrue();
    }

    @Test
    void ignoresAssignmentTestClosedStateWhenClassifyingHistoricalDeadlineFact() {
        assertThat(classifier.isWithinDeadline(
            assignment(AssignmentStatus.ASSIGNED, FIXED_INSTANT.plusSeconds(30), null, null),
            closedAssignmentTest(),
            FIXED_INSTANT
        )).isTrue();
    }

    private Assignment assignment(
        AssignmentStatus status,
        Instant deadlineAt,
        Instant cancelledAt,
        Instant closedAt
    ) {
        return new Assignment(
            801L,
            901L,
            301L,
            401L,
            status,
            FIXED_INSTANT.minusSeconds(3600),
            deadlineAt,
            cancelledAt,
            closedAt,
            FIXED_INSTANT.minusSeconds(3600),
            FIXED_INSTANT.minusSeconds(120)
        );
    }

    private AssignmentTest openAssignmentTest() {
        return new AssignmentTest(
            701L,
            801L,
            501L,
            AssignmentTestRole.FINAL_TOPIC_CONTROL,
            null,
            null,
            false,
            FIXED_INSTANT.minusSeconds(3600),
            FIXED_INSTANT.minusSeconds(120)
        );
    }

    private AssignmentTest closedAssignmentTest() {
        return new AssignmentTest(
            701L,
            801L,
            501L,
            AssignmentTestRole.FINAL_TOPIC_CONTROL,
            null,
            FIXED_INSTANT.minusSeconds(10),
            true,
            FIXED_INSTANT.minusSeconds(3600),
            FIXED_INSTANT.minusSeconds(120)
        );
    }
}
