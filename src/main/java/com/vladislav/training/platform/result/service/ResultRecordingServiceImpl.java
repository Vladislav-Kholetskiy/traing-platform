package com.vladislav.training.platform.result.service;

import com.vladislav.training.platform.assignment.service.AssignmentCountedResultHandoffService;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.audit.service.SystemActorResolver;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import com.vladislav.training.platform.result.domain.Result;
import com.vladislav.training.platform.result.domain.ResultSnapshotAssembler;
import com.vladislav.training.platform.result.domain.ResultSnapshotFacts;
import com.vladislav.training.platform.result.repository.ResultRepository;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import com.vladislav.training.platform.testing.repository.TestAttemptRepository;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса {@code ResultRecordingServiceImpl}.
 */
@Service
@Transactional
@ConditionalOnBean({
    ResultRepository.class,
    TestAttemptRepository.class,
    ResultRecordingSnapshotFactsProvider.class,
    ResultRecordingSubordinateSnapshotMaterializer.class,
    ResultRecordingIdempotentReplayValidator.class,
    ResultRecordingChildSnapshotCompletenessValidator.class,
    CountedAssignmentResultValidityGate.class,
    AssignmentCountedResultHandoffService.class,
    CriticalCommandAuditSupport.class,
    SystemActorResolver.class
})
class ResultRecordingServiceImpl implements ResultRecordingService {

    private final ResultRepository resultRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final ResultRecordingSnapshotFactsProvider snapshotFactsProvider;
    private final ResultRecordingSubordinateSnapshotMaterializer subordinateSnapshotMaterializer;
    private final CountedAssignmentResultValidityGate countedAssignmentResultValidityGate;
    private final AssignmentCountedResultHandoffService assignmentCountedResultHandoffService;
    private final CriticalCommandAuditSupport criticalCommandAuditSupport;
    private final SystemActorResolver systemActorResolver;
    private final ResultRecordingIdempotentReplayValidator idempotentReplayValidator;
    private final ResultRecordingChildSnapshotCompletenessValidator childSnapshotCompletenessValidator;
    private final ResultSnapshotAssembler resultSnapshotAssembler = new ResultSnapshotAssembler();
    private final ResultRecordingCriticalAuditPayloadFactory auditPayloadFactory =
        new ResultRecordingCriticalAuditPayloadFactory();

    ResultRecordingServiceImpl(
        ResultRepository resultRepository,
        TestAttemptRepository testAttemptRepository,
        ResultRecordingSnapshotFactsProvider snapshotFactsProvider,
        ResultRecordingSubordinateSnapshotMaterializer subordinateSnapshotMaterializer,
        CountedAssignmentResultValidityGate countedAssignmentResultValidityGate,
        AssignmentCountedResultHandoffService assignmentCountedResultHandoffService,
        CriticalCommandAuditSupport criticalCommandAuditSupport,
        SystemActorResolver systemActorResolver,
        ResultRecordingIdempotentReplayValidator idempotentReplayValidator,
        ResultRecordingChildSnapshotCompletenessValidator childSnapshotCompletenessValidator
    ) {
        this.resultRepository = Objects.requireNonNull(resultRepository, "resultRepository must not be null");
        this.testAttemptRepository = Objects.requireNonNull(testAttemptRepository, "testAttemptRepository must not be null");
        this.snapshotFactsProvider = Objects.requireNonNull(snapshotFactsProvider, "snapshotFactsProvider must not be null");
        this.subordinateSnapshotMaterializer = Objects.requireNonNull(
            subordinateSnapshotMaterializer,
            "subordinateSnapshotMaterializer must not be null"
        );
        this.countedAssignmentResultValidityGate = Objects.requireNonNull(
            countedAssignmentResultValidityGate,
            "countedAssignmentResultValidityGate must not be null"
        );
        this.assignmentCountedResultHandoffService = Objects.requireNonNull(
            assignmentCountedResultHandoffService,
            "assignmentCountedResultHandoffService must not be null"
        );
        this.criticalCommandAuditSupport = Objects.requireNonNull(
            criticalCommandAuditSupport,
            "criticalCommandAuditSupport must not be null"
        );
        this.systemActorResolver = Objects.requireNonNull(systemActorResolver, "systemActorResolver must not be null");
        this.idempotentReplayValidator = Objects.requireNonNull(
            idempotentReplayValidator,
            "idempotentReplayValidator must not be null"
        );
        this.childSnapshotCompletenessValidator = Objects.requireNonNull(
            childSnapshotCompletenessValidator,
            "childSnapshotCompletenessValidator must not be null"
        );
    }

    @Override
    @Transactional
    public Long recordResult(Long testAttemptId) {
        Objects.requireNonNull(testAttemptId, "testAttemptId must not be null");

        Result existingResult = resultRepository.findResultByTestAttemptId(testAttemptId);
        if (existingResult != null) {
            return validatedIdempotentReplayResultId(existingResult, testAttemptId);
        }

        TestAttempt terminalizedAttempt = testAttemptRepository.findTestAttemptById(testAttemptId);
        requireTerminalizedAttempt(terminalizedAttempt);

        ResultSnapshotFacts snapshotFacts = snapshotFactsProvider.provideSnapshotFacts(terminalizedAttempt);
        Result assembledResult = resultSnapshotAssembler.assemble(terminalizedAttempt, snapshotFacts);

        Result savedResult;
        try {
            savedResult = resultRepository.saveResult(assembledResult);
        } catch (PersistenceConstraintViolationException exception) {
            Result canonicalResult = resultRepository.findResultByTestAttemptId(testAttemptId);
            if (canonicalResult != null) {
                return validatedIdempotentReplayResultId(canonicalResult, testAttemptId);
            }
            throw exception;
        }
        subordinateSnapshotMaterializer.materialize(savedResult, terminalizedAttempt, snapshotFacts);
        recordResultAudit(savedResult, terminalizedAttempt.status());
        if (countedAssignmentResultValidityGate.allowsAssignmentCountedHandoff(savedResult)) {
            assignmentCountedResultHandoffService.acceptValidCountedAssignmentResult(savedResult.id());
        }
        return savedResult.id();
    }

    private Long validatedIdempotentReplayResultId(Result existingResult, Long testAttemptId) {
        TestAttempt terminalizedAttempt = testAttemptRepository.findTestAttemptById(testAttemptId);
        requireTerminalizedAttempt(terminalizedAttempt);
        ResultSnapshotFacts replaySnapshotFacts = snapshotFactsProvider.provideSnapshotFacts(terminalizedAttempt);
        Result replayCandidate = resultSnapshotAssembler.assemble(terminalizedAttempt, replaySnapshotFacts);
        if (!idempotentReplayValidator.isIdenticalReplay(existingResult, replayCandidate)) {
            throw new ConflictException(
                "Result replay is not idempotent for attemptId=" + testAttemptId
            );
        }
        childSnapshotCompletenessValidator.requireCompletePersistedChildAggregate(existingResult, replaySnapshotFacts);
        return existingResult.id();
    }

    private void requireTerminalizedAttempt(TestAttempt attempt) {
        if (attempt.status() != TestAttemptStatus.COMPLETED) {
            throw new ConflictException(
                "Result recording is not allowed for non-terminal attempt or non-recordable terminal status; "
                    + "only COMPLETED is recordable: attemptId="
                    + attempt.id()
                    + ", status="
                    + attempt.status()
            );
        }
    }

    private void recordResultAudit(Result savedResult, TestAttemptStatus terminalStatus) {
        String actorSource = terminalStatus == TestAttemptStatus.EXPIRED ? "system" : "interactive";
        Long actorUserId = terminalStatus == TestAttemptStatus.EXPIRED
            ? criticalCommandAuditSupport.resolveSystemActorUserId(systemActorResolver)
            : criticalCommandAuditSupport.resolveInteractiveActorUserId();
        ResultRecordingCriticalAuditCatalog auditCatalog = ResultRecordingCriticalAuditCatalog.RESULT_RECORDED;
        criticalCommandAuditSupport.recordAudit(
            actorUserId,
            auditCatalog.auditEventType(),
            auditCatalog.auditEntityType(),
            savedResult.id(),
            null,
            auditPayloadFactory.payloadAfter(savedResult),
            criticalCommandAuditSupport.buildAuditContext(
                "Result",
                auditCatalog.operationCode(),
                auditPayloadFactory.createRecordingDetails(savedResult, terminalStatus, actorSource)
            )
        );
    }

}
