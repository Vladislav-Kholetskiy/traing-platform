package com.vladislav.training.platform.assignment.repository;

import java.time.Instant;
import java.util.List;

/**
 * Контракт репозитория {@code AssignmentStatusRecalculationCandidateRepository}.
 */
public interface AssignmentStatusRecalculationCandidateRepository {

    List<Long> findCandidateAssignmentIdsForStatusRecalculation(
        Instant deadlineFromInclusive,
        Instant deadlineToInclusive,
        int limit
    );
}
