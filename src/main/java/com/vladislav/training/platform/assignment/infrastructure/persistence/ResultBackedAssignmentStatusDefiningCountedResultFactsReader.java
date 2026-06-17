package com.vladislav.training.platform.assignment.infrastructure.persistence;

import com.vladislav.training.platform.assignment.service.AssignmentStatusDefiningCountedResultFactsReader;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.result.domain.Result;
import com.vladislav.training.platform.result.repository.ResultRepository;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
@ConditionalOnBean(ResultRepository.class)
public class ResultBackedAssignmentStatusDefiningCountedResultFactsReader
    implements AssignmentStatusDefiningCountedResultFactsReader {

    private final ResultRepository resultRepository;

    public ResultBackedAssignmentStatusDefiningCountedResultFactsReader(ResultRepository resultRepository) {
        this.resultRepository = Objects.requireNonNull(resultRepository, "resultRepository must not be null");
    }

    @Override
    public StatusDefiningCountedResultFacts findStatusDefiningFactsByCountedResultId(Long countedResultId) {
        Objects.requireNonNull(countedResultId, "countedResultId must not be null");

        Result result = resultRepository.findResultById(countedResultId);
        if (result == null) {
            throw new NotFoundException("Counted result not found: " + countedResultId);
        }

        return new StatusDefiningCountedResultFacts(
            result.scoringSnapshot().passed(),
            Boolean.TRUE.equals(result.withinDeadline()),
            Boolean.TRUE.equals(result.countedInAssignment())
        );
    }

    @Override
    public CountedAssignmentResultHandoffFacts findCountedAssignmentResultHandoffFactsByResultId(Long resultId) {
        Objects.requireNonNull(resultId, "resultId must not be null");

        Result result = resultRepository.findResultById(resultId);
        if (result == null) {
            throw new NotFoundException("Counted result not found: " + resultId);
        }

        return new CountedAssignmentResultHandoffFacts(
            result.id(),
            result.assignmentId(),
            result.assignmentTestId(),
            result.attemptMode(),
            result.scoringSnapshot().passed(),
            Boolean.TRUE.equals(result.withinDeadline()),
            Boolean.TRUE.equals(result.countedInAssignment()),
            result.snapshotFinalTopicControlFlag(),
            result.createdAt()
        );
    }
}
