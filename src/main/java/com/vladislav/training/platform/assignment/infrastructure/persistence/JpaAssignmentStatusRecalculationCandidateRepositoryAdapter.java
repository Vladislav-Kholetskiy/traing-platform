package com.vladislav.training.platform.assignment.infrastructure.persistence;

import com.vladislav.training.platform.assignment.repository.AssignmentStatusRecalculationCandidateRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Адаптер репозитория {@code JpaAssignmentStatusRecalculationCandidateRepositoryAdapter}.
 */
@Repository
@Transactional(readOnly = true)
public class JpaAssignmentStatusRecalculationCandidateRepositoryAdapter
    implements AssignmentStatusRecalculationCandidateRepository {

    private final SpringDataAssignmentStatusRecalculationJpaRepository repository;

    public JpaAssignmentStatusRecalculationCandidateRepositoryAdapter(
        SpringDataAssignmentStatusRecalculationJpaRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public List<Long> findCandidateAssignmentIdsForStatusRecalculation(
        Instant deadlineFromInclusive,
        Instant deadlineToInclusive,
        int limit
    ) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        return repository.findCandidateAssignmentIdsForStatusRecalculation(
            deadlineFromInclusive,
            deadlineToInclusive,
            PageRequest.of(0, limit)
        );
    }
}
