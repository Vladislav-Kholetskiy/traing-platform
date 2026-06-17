package com.vladislav.training.platform.result.infrastructure.persistence;

import com.vladislav.training.platform.result.query.internal.SelfHistoricalResultReader;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Repository;
/**
 * Читатель {@code JpaSelfHistoricalResultReader}.
 */

@Repository
class JpaSelfHistoricalResultReader implements SelfHistoricalResultReader {

    private final SpringDataResultJpaRepository repository;

    public JpaSelfHistoricalResultReader(SpringDataResultJpaRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public List<SelfHistoricalResultReadRow> findSelfHistoricalResultRows(SelfHistoricalResultReadCriteria criteria) {
        Objects.requireNonNull(criteria, "criteria must not be null");
        return repository.findResultHistorySummaryRowsByUserIdSnapshotOrderByCompletedAtDescIdDesc(criteria.actorUserId()).stream()
            .map(this::toReadRow)
            .toList();
    }

    private SelfHistoricalResultReadRow toReadRow(SpringDataResultJpaRepository.ResultHistorySummaryRowView row) {
        return new SelfHistoricalResultReadRow(
            row.getResultId(),
            row.getRecordedAt(),
            row.getTestAttemptId(),
            row.getTestId(),
            row.getTestName(),
            row.getScorePercent(),
            row.getScore(),
            row.isPassed(),
            row.getAttemptMode(),
            row.getAssignmentId()
        );
    }
}

