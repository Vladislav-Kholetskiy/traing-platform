package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import com.vladislav.training.platform.assignment.repository.AssignmentTestRepository;
import com.vladislav.training.platform.common.exception.ConflictException;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@ConditionalOnBean({
    AssignmentStatusDefiningCountedResultFactsReader.class,
    AssignmentTestRepository.class,
    AssignmentCommandService.class
})
class AssignmentCountedResultHandoffServiceImpl implements AssignmentCountedResultHandoffService {

    private final AssignmentStatusDefiningCountedResultFactsReader countedResultFactsReader;
    private final AssignmentTestRepository assignmentTestRepository;
    private final AssignmentCommandService assignmentCommandService;

    AssignmentCountedResultHandoffServiceImpl(
        AssignmentStatusDefiningCountedResultFactsReader countedResultFactsReader,
        AssignmentTestRepository assignmentTestRepository,
        AssignmentCommandService assignmentCommandService
    ) {
        this.countedResultFactsReader = Objects.requireNonNull(
            countedResultFactsReader,
            "countedResultFactsReader must not be null"
        );
        this.assignmentTestRepository = Objects.requireNonNull(
            assignmentTestRepository,
            "assignmentTestRepository must not be null"
        );
        this.assignmentCommandService = Objects.requireNonNull(
            assignmentCommandService,
            "assignmentCommandService must not be null"
        );
    }

    @Override
    public void acceptValidCountedAssignmentResult(Long resultId) {
        Objects.requireNonNull(resultId, "resultId must not be null");

        AssignmentStatusDefiningCountedResultFactsReader.CountedAssignmentResultHandoffFacts countedResultFacts =
            countedResultFactsReader.findCountedAssignmentResultHandoffFactsByResultId(resultId);
        if (countedResultFacts.assignmentId() == null || countedResultFacts.assignmentTestId() == null) {
            return;
        }

        requireAssignedCountedResult(countedResultFacts);
        AssignmentTest assignmentTest = assignmentTestRepository.findAssignmentTestById(countedResultFacts.assignmentTestId());
        requireConsistentAssignmentAnchor(countedResultFacts, assignmentTest);

        if (assignmentTest.countedResultId() != null && !Objects.equals(assignmentTest.countedResultId(), countedResultFacts.resultId())) {
            throw new ConflictException(
                "Assignment counted result handoff cannot overwrite existing counted result: assignmentTestId="
                    + assignmentTest.id()
                    + ", existingCountedResultId="
                    + assignmentTest.countedResultId()
                    + ", incomingResultId="
                    + countedResultFacts.resultId()
            );
        }
        if (Objects.equals(assignmentTest.countedResultId(), countedResultFacts.resultId())
            && assignmentTest.isClosed()
            && assignmentTest.closedAt() != null) {
            return;
        }

        assignmentCommandService.closeAssignmentTestWithCountedResult(
            assignmentTest.id(),
            countedResultFacts.resultId(),
            countedResultFacts.recordedAt()
        );
    }

    private void requireAssignedCountedResult(
        AssignmentStatusDefiningCountedResultFactsReader.CountedAssignmentResultHandoffFacts countedResultFacts
    ) {
        if (countedResultFacts.attemptMode() != com.vladislav.training.platform.common.model.AttemptMode.ASSIGNED) {
            throw new ConflictException(
                "Assignment counted result handoff requires ASSIGNED attempt mode: resultId="
                    + countedResultFacts.resultId()
                    + ", attemptMode="
                    + countedResultFacts.attemptMode()
            );
        }
    }

    private void requireConsistentAssignmentAnchor(
        AssignmentStatusDefiningCountedResultFactsReader.CountedAssignmentResultHandoffFacts countedResultFacts,
        AssignmentTest assignmentTest
    ) {
        if (!Objects.equals(assignmentTest.assignmentId(), countedResultFacts.assignmentId())) {
            throw new ConflictException(
                "Assignment counted result handoff anchor mismatch: resultId="
                    + countedResultFacts.resultId()
                    + ", assignmentId="
                    + countedResultFacts.assignmentId()
                    + ", assignmentTest.assignmentId="
                    + assignmentTest.assignmentId()
            );
        }
    }
}
