package com.vladislav.training.platform.assignment.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Контракт репозитория {@code SpringDataAssignmentStatusRecalculationJpaRepository}.
 */
public interface SpringDataAssignmentStatusRecalculationJpaRepository extends JpaRepository<AssignmentEntity, Long> {

    @Query(
        """
        select assignment.id
        from AssignmentEntity assignment
        where assignment.cancelledAt is null
          and assignment.closedAt is null
          and assignment.deadlineAt >= :deadlineFromInclusive
          and assignment.deadlineAt <= :deadlineToInclusive
        order by assignment.deadlineAt asc, assignment.id asc
        """
    )
    List<Long> findCandidateAssignmentIdsForStatusRecalculation(
        @Param("deadlineFromInclusive") Instant deadlineFromInclusive,
        @Param("deadlineToInclusive") Instant deadlineToInclusive,
        Pageable pageable
    );
}
