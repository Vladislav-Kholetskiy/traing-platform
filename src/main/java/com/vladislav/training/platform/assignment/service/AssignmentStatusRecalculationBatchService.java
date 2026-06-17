package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.assignment.repository.AssignmentStatusRecalculationCandidateRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AssignmentStatusRecalculationBatchService {

    private final AssignmentStatusRecalculationCandidateRepository candidateRepository;
    private final AssignmentStatusRecalculationService assignmentStatusRecalculationService;
    private final int candidateLookbackDays;
    private final int batchSize;

    public AssignmentStatusRecalculationBatchService(
        AssignmentStatusRecalculationCandidateRepository candidateRepository,
        AssignmentStatusRecalculationService assignmentStatusRecalculationService,
        @Value("${assignment.scheduler.status-recalculation-lookback-days:30}") int candidateLookbackDays,
        @Value("${assignment.scheduler.status-recalculation-batch-size:250}") int batchSize
    ) {
        this.candidateRepository = Objects.requireNonNull(
            candidateRepository,
            "candidateRepository must not be null"
        );
        this.assignmentStatusRecalculationService = Objects.requireNonNull(
            assignmentStatusRecalculationService,
            "assignmentStatusRecalculationService must not be null"
        );
        if (candidateLookbackDays <= 0) {
            throw new IllegalArgumentException("candidateLookbackDays must be positive");
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive");
        }
        this.candidateLookbackDays = candidateLookbackDays;
        this.batchSize = batchSize;
    }

    public BatchResult recalculateDueAssignments(Instant effectiveAt) {
        Objects.requireNonNull(effectiveAt, "effectiveAt must not be null");

        Instant windowStartInclusive = effectiveAt.minus(Duration.ofDays(candidateLookbackDays));
        List<Long> assignmentIds = candidateRepository.findCandidateAssignmentIdsForStatusRecalculation(
            windowStartInclusive,
            effectiveAt,
            batchSize
        );

        for (Long assignmentId : assignmentIds) {
            assignmentStatusRecalculationService.refreshAssignmentStatusCache(assignmentId, effectiveAt);
        }

        return new BatchResult(windowStartInclusive, effectiveAt, assignmentIds.size(), assignmentIds.size());
    }

    public record BatchResult(
        Instant windowStartInclusive,
        Instant windowEndInclusive,
        int candidateCount,
        int refreshedCount
    ) {
    }
}
