package com.vladislav.training.platform.result.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.audit.domain.AuditContext;
import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaignRecipientSnapshot;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import com.vladislav.training.platform.assignment.repository.AssignmentCampaignRecipientSnapshotRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentTestRepository;
import com.vladislav.training.platform.audit.domain.AuditContext;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.audit.service.SystemActorResolver;
import com.vladislav.training.platform.assignment.service.AssignmentCountedResultHandoffService;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.content.domain.AnswerOption;
import com.vladislav.training.platform.content.domain.AnswerOptionRole;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Question;
import com.vladislav.training.platform.content.domain.QuestionType;
import com.vladislav.training.platform.content.domain.TestQuestion;
import com.vladislav.training.platform.content.domain.TestType;
import com.vladislav.training.platform.content.repository.AnswerOptionRepository;
import com.vladislav.training.platform.content.repository.QuestionRepository;
import com.vladislav.training.platform.content.repository.TestQuestionRepository;
import com.vladislav.training.platform.content.repository.TestRepository;
import com.vladislav.training.platform.result.domain.Result;
import com.vladislav.training.platform.result.domain.ResultAnswerOptionSnapshot;
import com.vladislav.training.platform.result.domain.ResultOrgContextSnapshot;
import com.vladislav.training.platform.result.domain.ResultQuestionSnapshot;
import com.vladislav.training.platform.result.domain.ResultQuestionType;
import com.vladislav.training.platform.result.domain.ResultScoringSnapshot;
import com.vladislav.training.platform.result.domain.ResultSnapshotAssembler;
import com.vladislav.training.platform.result.domain.ResultSnapshotFacts;
import com.vladislav.training.platform.result.repository.ResultAnswerOptionSnapshotRepository;
import com.vladislav.training.platform.result.repository.ResultQuestionSnapshotRepository;
import com.vladislav.training.platform.result.repository.ResultRepository;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import com.vladislav.training.platform.testing.domain.UserAnswer;
import com.vladislav.training.platform.testing.domain.UserAnswerItem;
import com.vladislav.training.platform.testing.repository.TestAttemptRepository;
import com.vladislav.training.platform.testing.repository.UserAnswerItemRepository;
import com.vladislav.training.platform.testing.repository.UserAnswerRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;
/**
 * Проверяет поведение {@code ResultRecordingServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class ResultRecordingServiceImplTest {

    private static final Instant TERMINAL_AT = Instant.parse("2026-04-19T10:15:00Z");
    private static final Instant RECORDED_AT = Instant.parse("2026-04-19T10:16:00Z");

    private static final String STEP_7_TARGET_DISABLED_REASON =
        "Step 7 target test: enable after canonical assignment counted result policy is implemented";

    @Mock
    private ResultRepository resultRepository;
    @Mock
    private TestAttemptRepository testAttemptRepository;
    @Mock
    private ResultRecordingSnapshotFactsProvider snapshotFactsProvider;
    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private AssignmentTestRepository assignmentTestRepository;
    @Mock
    private AssignmentCampaignRecipientSnapshotRepository recipientSnapshotRepository;
    @Mock
    private TestRepository testRepository;
    @Mock
    private TestQuestionRepository testQuestionRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private AnswerOptionRepository answerOptionRepository;
    @Mock
    private UserAnswerRepository userAnswerRepository;
    @Mock
    private UserAnswerItemRepository userAnswerItemRepository;
    @Mock
    private SelfCompletionOrgSnapshotFactsReader selfCompletionOrgSnapshotFactsReader;
    @Mock
    private UtcClock utcClock;
    @Mock
    private ResultRecordingSubordinateSnapshotMaterializer subordinateSnapshotMaterializer;
    @Mock
    private ResultQuestionSnapshotRepository resultQuestionSnapshotRepository;
    @Mock
    private ResultAnswerOptionSnapshotRepository resultAnswerOptionSnapshotRepository;
    @Mock
    private AssignmentCountedResultHandoffService assignmentCountedResultHandoffService;
    @Mock
    private CriticalCommandAuditSupport criticalCommandAuditSupport;
    @Mock
    private SystemActorResolver systemActorResolver;
    @Mock
    private ResultRecordingIdempotentReplayValidator idempotentReplayValidator;
    @Mock
    private ResultRecordingChildSnapshotCompletenessValidator childSnapshotCompletenessValidator;

    private ResultRecordingServiceImpl service;
    private CountedAssignmentResultValidityGate countedAssignmentResultValidityGate;

    @BeforeEach
    void setUp() {
        countedAssignmentResultValidityGate = new CountedAssignmentResultValidator();
        service = new ResultRecordingServiceImpl(
            resultRepository,
            testAttemptRepository,
            snapshotFactsProvider,
            subordinateSnapshotMaterializer,
            countedAssignmentResultValidityGate,
            assignmentCountedResultHandoffService,
            criticalCommandAuditSupport,
            systemActorResolver,
            idempotentReplayValidator,
            childSnapshotCompletenessValidator
        );
    }

    @Test
    void idempotentReplayDoesNotInvokeCountedHandoffAgain() {
        Result existing = countedAssignedResult(401L, 9001L);
        TestAttempt completedAttempt = terminalAssignedAttempt(9001L, TestAttemptStatus.COMPLETED);
        ResultSnapshotFacts replayFacts = assignedSnapshotFacts();
        when(resultRepository.findResultByTestAttemptId(9001L)).thenReturn(existing);
        when(testAttemptRepository.findTestAttemptById(9001L)).thenReturn(completedAttempt);
        when(snapshotFactsProvider.provideSnapshotFacts(completedAttempt)).thenReturn(replayFacts);
        when(idempotentReplayValidator.isIdenticalReplay(eq(existing), any(Result.class))).thenReturn(true);

        Long resultId = service.recordResult(9001L);

        assertThat(resultId).isEqualTo(401L);
        verify(resultRepository).findResultByTestAttemptId(9001L);
        verify(testAttemptRepository).findTestAttemptById(9001L);
        verify(snapshotFactsProvider).provideSnapshotFacts(completedAttempt);
        verify(idempotentReplayValidator).isIdenticalReplay(eq(existing), any(Result.class));
        verify(childSnapshotCompletenessValidator).requireCompletePersistedChildAggregate(existing, replayFacts);
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
        verify(subordinateSnapshotMaterializer, never()).materialize(any(), any(), any());
        verify(resultRepository, never()).saveResult(any(Result.class));
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void newlyMaterializedResultIsAuditedSynchronously() {
        TestAttempt completedAttempt = terminalAssignedAttempt(9002L, TestAttemptStatus.COMPLETED);
        ResultSnapshotFacts snapshotFacts = assignedSnapshotFacts();
        Result assembled = new ResultSnapshotAssembler().assemble(completedAttempt, snapshotFacts);
        Result savedResult = new Result(
            402L,
            assembled.testAttemptId(),
            assembled.userIdSnapshot(),
            assembled.attemptMode(),
            assembled.assignmentId(),
            assembled.assignmentTestId(),
            assembled.testIdSnapshot(),
            assembled.testNameSnapshot(),
            assembled.scoringSnapshot(),
            assembled.withinDeadline(),
            assembled.countedInAssignment(),
            assembled.completedAt(),
            assembled.orgContextSnapshot(),
            assembled.snapshotFinalTopicControlFlag(),
            assembled.createdAt()
        );
        AuditContext auditContext = new AuditContext("{\"operationCode\":\"RESULT_RECORDING\"}");

        when(resultRepository.findResultByTestAttemptId(9002L)).thenReturn(null);
        when(testAttemptRepository.findTestAttemptById(9002L)).thenReturn(completedAttempt);
        when(snapshotFactsProvider.provideSnapshotFacts(completedAttempt)).thenReturn(snapshotFacts);
        when(resultRepository.saveResult(any(Result.class))).thenReturn(savedResult);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Result"),
            org.mockito.ArgumentMatchers.eq("RESULT_RECORDING"),
            any()
        )).thenReturn(auditContext);

        Long resultId = service.recordResult(9002L);

        assertThat(resultId).isEqualTo(402L);
        assertThat(assembled.userIdSnapshot()).isEqualTo(completedAttempt.userId());
        InOrder inOrder = inOrder(
            resultRepository,
            testAttemptRepository,
            snapshotFactsProvider,
            subordinateSnapshotMaterializer,
            criticalCommandAuditSupport,
            assignmentCountedResultHandoffService
        );
        inOrder.verify(resultRepository).findResultByTestAttemptId(9002L);
        inOrder.verify(testAttemptRepository).findTestAttemptById(9002L);
        inOrder.verify(snapshotFactsProvider).provideSnapshotFacts(completedAttempt);
        inOrder.verify(resultRepository).saveResult(assembled);
        inOrder.verify(subordinateSnapshotMaterializer).materialize(savedResult, completedAttempt, snapshotFacts);
        inOrder.verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        inOrder.verify(criticalCommandAuditSupport).buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Result"),
            org.mockito.ArgumentMatchers.eq("RESULT_RECORDING"),
            any()
        );
        inOrder.verify(criticalCommandAuditSupport).recordAudit(
            org.mockito.ArgumentMatchers.eq(777L),
            any(),
            org.mockito.ArgumentMatchers.eq("result"),
            org.mockito.ArgumentMatchers.eq(402L),
            org.mockito.ArgumentMatchers.isNull(),
            any(),
            org.mockito.ArgumentMatchers.eq(auditContext)
        );
        inOrder.verify(assignmentCountedResultHandoffService).acceptValidCountedAssignmentResult(402L);
        verifyNoMoreInteractions(
            resultRepository,
            testAttemptRepository,
            snapshotFactsProvider,
            subordinateSnapshotMaterializer,
            assignmentCountedResultHandoffService,
            criticalCommandAuditSupport,
            systemActorResolver
        );
    }

    @Test
    void resultRecordingAuditPayloadUsesRecordedResultFactsWithoutSyntheticFallback() {
        TestAttempt completedAttempt = terminalSelfAttempt(9024L, TestAttemptStatus.COMPLETED);
        ResultSnapshotFacts snapshotFacts = selfSnapshotFacts();
        Result assembled = new ResultSnapshotAssembler().assemble(completedAttempt, snapshotFacts);
        Result savedResult = new Result(
            424L,
            assembled.testAttemptId(),
            assembled.userIdSnapshot(),
            assembled.attemptMode(),
            assembled.assignmentId(),
            assembled.assignmentTestId(),
            assembled.testIdSnapshot(),
            assembled.testNameSnapshot(),
            assembled.scoringSnapshot(),
            assembled.withinDeadline(),
            assembled.countedInAssignment(),
            assembled.completedAt(),
            assembled.orgContextSnapshot(),
            assembled.snapshotFinalTopicControlFlag(),
            assembled.createdAt()
        );
        AuditContext auditContext = new AuditContext("{\"operationCode\":\"RESULT_RECORDING\"}");

        when(resultRepository.findResultByTestAttemptId(9024L)).thenReturn(null);
        when(testAttemptRepository.findTestAttemptById(9024L)).thenReturn(completedAttempt);
        when(snapshotFactsProvider.provideSnapshotFacts(completedAttempt)).thenReturn(snapshotFacts);
        when(resultRepository.saveResult(any(Result.class))).thenReturn(savedResult);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(784L);
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Result"),
            org.mockito.ArgumentMatchers.eq("RESULT_RECORDING"),
            any()
        )).thenReturn(auditContext);

        Long resultId = service.recordResult(9024L);

        assertThat(resultId).isEqualTo(424L);
        ArgumentCaptor<Map> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Object> payloadAfterCaptor = ArgumentCaptor.forClass(Object.class);
        verify(criticalCommandAuditSupport).buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Result"),
            org.mockito.ArgumentMatchers.eq("RESULT_RECORDING"),
            detailsCaptor.capture()
        );
        verify(criticalCommandAuditSupport).recordAudit(
            org.mockito.ArgumentMatchers.eq(784L),
            any(),
            org.mockito.ArgumentMatchers.eq("result"),
            org.mockito.ArgumentMatchers.eq(424L),
            org.mockito.ArgumentMatchers.isNull(),
            payloadAfterCaptor.capture(),
            org.mockito.ArgumentMatchers.eq(auditContext)
        );

        assertThat(detailsCaptor.getValue())
            .containsEntry("commandType", "result_record")
            .containsEntry("recordingKind", "immutable_result_materialization")
            .containsEntry("testAttemptId", 9024L)
            .containsEntry("attemptMode", AttemptMode.SELF)
            .containsEntry("terminalStatus", TestAttemptStatus.COMPLETED)
            .containsEntry("actorSource", "interactive");
        assertThat(detailsCaptor.getValue()).doesNotContainKeys("assignmentId", "assignmentTestId");

        assertThat(payloadAfterCaptor.getValue()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> payloadAfter = (Map<String, Object>) payloadAfterCaptor.getValue();
        assertThat(payloadAfter)
            .containsEntry("id", 424L)
            .containsEntry("testAttemptId", 9024L)
            .containsEntry("attemptMode", AttemptMode.SELF)
            .containsEntry("completedAt", savedResult.completedAt())
            .containsEntry("createdAt", savedResult.createdAt())
            .containsEntry("assignmentId", null)
            .containsEntry("assignmentTestId", null)
            .containsEntry("withinDeadline", null)
            .containsEntry("countedInAssignment", null)
            .containsEntry("snapshotFinalTopicControlFlag", savedResult.snapshotFinalTopicControlFlag())
            .containsEntry("scoringSnapshot", savedResult.scoringSnapshot())
            .containsEntry("orgContextSnapshot", savedResult.orgContextSnapshot());
        assertThat(payloadAfter).doesNotContainKeys("terminalStatus", "actorSource", "userId", "testId");
    }

    @Test
    void auditIsNotUsedAsSourceForResultOrCountedDecision() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/result/service/ResultRecordingServiceImpl.java"
        ));

        int snapshotFactsIndex = source.indexOf(
            "ResultSnapshotFacts snapshotFacts = snapshotFactsProvider.provideSnapshotFacts(terminalizedAttempt);"
        );
        int assembleIndex = source.indexOf(
            "Result assembledResult = resultSnapshotAssembler.assemble(terminalizedAttempt, snapshotFacts);"
        );
        int saveIndex = source.indexOf("savedResult = resultRepository.saveResult(assembledResult);");
        int auditIndex = source.indexOf("recordResultAudit(savedResult, terminalizedAttempt.status());");
        int countedIndex = source.indexOf("countedAssignmentResultValidityGate.allowsAssignmentCountedHandoff(savedResult)");
        int replayCountedIndex = source.indexOf(
            "countedAssignmentResultValidityGate.allowsAssignmentCountedHandoff(existingResult)"
        );

        assertThat(snapshotFactsIndex).isGreaterThanOrEqualTo(0);
        assertThat(assembleIndex).isGreaterThan(snapshotFactsIndex);
        assertThat(saveIndex).isGreaterThan(assembleIndex);
        assertThat(auditIndex).isGreaterThan(saveIndex);
        assertThat(countedIndex).isGreaterThan(auditIndex);
        assertThat(replayCountedIndex).isEqualTo(-1);

        assertThat(source)
            
            .contains("ResultSnapshotFacts snapshotFacts = snapshotFactsProvider.provideSnapshotFacts(terminalizedAttempt);")
            .contains("Result assembledResult = resultSnapshotAssembler.assemble(terminalizedAttempt, snapshotFacts);")
            .contains("countedAssignmentResultValidityGate.allowsAssignmentCountedHandoff(savedResult)")
            .doesNotContain("countedAssignmentResultValidityGate.allowsAssignmentCountedHandoff(existingResult)")
            .doesNotContain("countedAssignmentResultValidityGate.allowsAssignmentCountedHandoff(audit")
            .doesNotContain("countedAssignmentResultValidityGate.allowsAssignmentCountedHandoff(context")
            .doesNotContain("countedAssignmentResultValidityGate.allowsAssignmentCountedHandoff(payload")
            .doesNotContain("resultSnapshotAssembler.assemble(terminalizedAttempt, audit")
            .doesNotContain("resultSnapshotAssembler.assemble(terminalizedAttempt, context")
            .doesNotContain("resultSnapshotAssembler.assemble(terminalizedAttempt, payload")
            .doesNotContain("snapshotFactsProvider.provideSnapshotFacts(savedResult)")
            .doesNotContain("snapshotFactsProvider.provideSnapshotFacts(audit");
    }

    @Test
    void validCountedAssignedResultInvokesHandoffAfterRootAndSnapshots() {
        TestAttempt completedAttempt = terminalAssignedAttempt(9035L, TestAttemptStatus.COMPLETED);
        ResultSnapshotFacts snapshotFacts = assignedSnapshotFacts();
        Result assembled = new ResultSnapshotAssembler().assemble(completedAttempt, snapshotFacts);
        Result savedResult = futureCountedDecisionResult(434L, assembled);
        AuditContext auditContext = new AuditContext("{\"operationCode\":\"RESULT_RECORDING\"}");

        when(resultRepository.findResultByTestAttemptId(9035L)).thenReturn(null);
        when(testAttemptRepository.findTestAttemptById(9035L)).thenReturn(completedAttempt);
        when(snapshotFactsProvider.provideSnapshotFacts(completedAttempt)).thenReturn(snapshotFacts);
        when(resultRepository.saveResult(any(Result.class))).thenReturn(savedResult);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(786L);
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Result"),
            org.mockito.ArgumentMatchers.eq("RESULT_RECORDING"),
            any()
        )).thenReturn(auditContext);

        Long resultId = service.recordResult(9035L);

        assertThat(resultId).isEqualTo(434L);
        assertThat(savedResult.attemptMode()).isEqualTo(AttemptMode.ASSIGNED);
        assertThat(savedResult.snapshotFinalTopicControlFlag()).isTrue();
        assertThat(savedResult.scoringSnapshot().passed()).isTrue();
        assertThat(savedResult.withinDeadline()).isTrue();
        assertThat(savedResult.countedInAssignment()).isTrue();
        InOrder inOrder = inOrder(resultRepository, subordinateSnapshotMaterializer, assignmentCountedResultHandoffService);
        inOrder.verify(resultRepository).saveResult(assembled);
        inOrder.verify(subordinateSnapshotMaterializer).materialize(savedResult, completedAttempt, snapshotFacts);
        inOrder.verify(assignmentCountedResultHandoffService).acceptValidCountedAssignmentResult(434L);
    }

    @Test
    void resultRecordingWritesRootResultAndQuestionSnapshotsInOneTransaction() throws NoSuchMethodException {
        Method recordResultMethod = ResultRecordingServiceImpl.class.getDeclaredMethod("recordResult", Long.class);
        assertThat(recordResultMethod.isAnnotationPresent(Transactional.class)).isTrue();

        TestAttempt completedAttempt = terminalAssignedAttempt(9081L, TestAttemptStatus.COMPLETED);
        ResultSnapshotFacts snapshotFacts = assignedSnapshotFacts();
        Result assembled = new ResultSnapshotAssembler().assemble(completedAttempt, snapshotFacts);
        Result savedResult = futureCountedDecisionResult(481L, assembled);
        AuditContext auditContext = new AuditContext("{\"operationCode\":\"RESULT_RECORDING\"}");

        when(resultRepository.findResultByTestAttemptId(9081L)).thenReturn(null);
        when(testAttemptRepository.findTestAttemptById(9081L)).thenReturn(completedAttempt);
        when(snapshotFactsProvider.provideSnapshotFacts(completedAttempt)).thenReturn(snapshotFacts);
        when(resultRepository.saveResult(any(Result.class))).thenReturn(savedResult);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(881L);
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Result"),
            org.mockito.ArgumentMatchers.eq("RESULT_RECORDING"),
            any()
        )).thenReturn(auditContext);

        Long resultId = service.recordResult(9081L);

        assertThat(resultId).isEqualTo(481L);
        InOrder inOrder = inOrder(
            resultRepository,
            subordinateSnapshotMaterializer,
            criticalCommandAuditSupport,
            assignmentCountedResultHandoffService
        );
        inOrder.verify(resultRepository).saveResult(assembled);
        inOrder.verify(subordinateSnapshotMaterializer).materialize(savedResult, completedAttempt, snapshotFacts);
    }

    @Test
    void resultRecordingWritesAnswerOptionSnapshotsInSameTransaction() throws NoSuchMethodException {
        Method recordResultMethod = ResultRecordingServiceImpl.class.getDeclaredMethod("recordResult", Long.class);
        assertThat(recordResultMethod.isAnnotationPresent(Transactional.class)).isTrue();

        TestAttempt completedAttempt = terminalAssignedAttempt(9082L, TestAttemptStatus.COMPLETED);
        ResultSnapshotFacts snapshotFacts = assignedSnapshotFacts();
        Result assembled = new ResultSnapshotAssembler().assemble(completedAttempt, snapshotFacts);
        Result savedResult = futureCountedDecisionResult(482L, assembled);
        AuditContext auditContext = new AuditContext("{\"operationCode\":\"RESULT_RECORDING\"}");

        when(resultRepository.findResultByTestAttemptId(9082L)).thenReturn(null);
        when(testAttemptRepository.findTestAttemptById(9082L)).thenReturn(completedAttempt);
        when(snapshotFactsProvider.provideSnapshotFacts(completedAttempt)).thenReturn(snapshotFacts);
        when(resultRepository.saveResult(any(Result.class))).thenReturn(savedResult);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(882L);
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Result"),
            org.mockito.ArgumentMatchers.eq("RESULT_RECORDING"),
            any()
        )).thenReturn(auditContext);

        Long resultId = service.recordResult(9082L);

        assertThat(resultId).isEqualTo(482L);
        InOrder inOrder = inOrder(
            resultRepository,
            subordinateSnapshotMaterializer,
            criticalCommandAuditSupport,
            assignmentCountedResultHandoffService
        );
        inOrder.verify(resultRepository).saveResult(assembled);
        inOrder.verify(subordinateSnapshotMaterializer).materialize(savedResult, completedAttempt, snapshotFacts);
    }

    @Test
    void nonTerminalRejectDoesNotWriteAudit() {
        TestAttempt activeAttempt = new TestAttempt(
            9003L,
            101L,
            501L,
            701L,
            AttemptMode.ASSIGNED,
            TestAttemptStatus.IN_PROGRESS,
            TERMINAL_AT.minusSeconds(600),
            null,
            null,
            null,
            TERMINAL_AT.minusSeconds(10),
            TERMINAL_AT.minusSeconds(600),
            TERMINAL_AT.minusSeconds(10)
        );
        when(resultRepository.findResultByTestAttemptId(9003L)).thenReturn(null);
        when(testAttemptRepository.findTestAttemptById(9003L)).thenReturn(activeAttempt);

        assertThatThrownBy(() -> service.recordResult(9003L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("non-terminal attempt")
            .hasMessageContaining("IN_PROGRESS");

        verify(resultRepository).findResultByTestAttemptId(9003L);
        verify(testAttemptRepository).findTestAttemptById(9003L);
        verify(snapshotFactsProvider, never()).provideSnapshotFacts(any());
        verify(resultRepository, never()).saveResult(any());
        verify(subordinateSnapshotMaterializer, never()).materialize(any(), any(), any());
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
        verify(criticalCommandAuditSupport, never()).resolveInteractiveActorUserId();
        verify(criticalCommandAuditSupport, never()).resolveSystemActorUserId(any());
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(String.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void resultSaveFailurePreventsCountedHandoff() {
        TestAttempt completedAttempt = terminalAssignedAttempt(9036L, TestAttemptStatus.COMPLETED);
        ResultSnapshotFacts snapshotFacts = assignedSnapshotFacts();

        when(resultRepository.findResultByTestAttemptId(9036L)).thenReturn(null, null);
        when(testAttemptRepository.findTestAttemptById(9036L)).thenReturn(completedAttempt);
        when(snapshotFactsProvider.provideSnapshotFacts(completedAttempt)).thenReturn(snapshotFacts);
        when(resultRepository.saveResult(any(Result.class)))
            .thenThrow(new PersistenceConstraintViolationException("Failed to persist result"));

        assertThatThrownBy(() -> service.recordResult(9036L))
            .isInstanceOf(PersistenceConstraintViolationException.class)
            .hasMessageContaining("Failed to persist result");

        verify(resultRepository, times(2)).findResultByTestAttemptId(9036L);
        verify(testAttemptRepository).findTestAttemptById(9036L);
        verify(snapshotFactsProvider).provideSnapshotFacts(completedAttempt);
        verify(resultRepository).saveResult(any(Result.class));
        verify(subordinateSnapshotMaterializer, never()).materialize(any(), any(), any());
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
        verify(criticalCommandAuditSupport, never()).resolveInteractiveActorUserId();
        verify(criticalCommandAuditSupport, never()).resolveSystemActorUserId(any());
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(String.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void recordResultRejectsUnknownScoringPolicyCodeBeforeSaveMaterializationAuditAndHandoff() {
        TestAttempt completedAttempt = terminalSelfAttempt(9034L, TestAttemptStatus.COMPLETED);
        ResultRecordingSnapshotFactsProvider realProvider = new ResultRecordingSnapshotFactsProvider(
            assignmentRepository,
            assignmentTestRepository,
            recipientSnapshotRepository,
            testRepository,
            testQuestionRepository,
            questionRepository,
            answerOptionRepository,
            userAnswerRepository,
            userAnswerItemRepository,
            selfCompletionOrgSnapshotFactsReader,
            utcClock,
            new ResultDeadlineClassifier(),
            new AssignmentCountedResultPolicy(),
            new ResultQuestionScoringCalculator()
        );
        ResultRecordingServiceImpl realService = new ResultRecordingServiceImpl(
            resultRepository,
            testAttemptRepository,
            realProvider,
            subordinateSnapshotMaterializer,
            countedAssignmentResultValidityGate,
            assignmentCountedResultHandoffService,
            criticalCommandAuditSupport,
            systemActorResolver,
            new ResultRecordingIdempotentReplayValidator(),
            childSnapshotCompletenessValidator
        );
        TestQuestion testQuestion = testQuestion(5070L, 501L, 6070L, "5.0000");
        Question question = question(6070L, QuestionType.SINGLE_CHOICE);

        when(resultRepository.findResultByTestAttemptId(9034L)).thenReturn(null);
        when(testAttemptRepository.findTestAttemptById(9034L)).thenReturn(completedAttempt);
        when(utcClock.now()).thenReturn(RECORDED_AT);
        when(testRepository.findTestById(501L)).thenReturn(test(501L, "50.00", "EXPERIMENTAL_UNKNOWN_POLICY"));
        when(testQuestionRepository.findTestQuestionsByTestId(501L)).thenReturn(List.of(testQuestion));
        when(questionRepository.findQuestionsByIds(List.of(6070L))).thenReturn(List.of(question));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(6070L)).thenReturn(List.of(
            choiceOption(7140L, 6070L, true),
            choiceOption(7141L, 6070L, false)
        ));
        when(userAnswerRepository.findUserAnswersByTestAttemptId(9034L)).thenReturn(List.of());

        assertThatThrownBy(() -> realService.recordResult(9034L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("canonical scoring policy support")
            .hasMessageContaining("EXPERIMENTAL_UNKNOWN_POLICY");

        verify(resultRepository).findResultByTestAttemptId(9034L);
        verify(testAttemptRepository).findTestAttemptById(9034L);
        verify(resultRepository, never()).saveResult(any(Result.class));
        verify(subordinateSnapshotMaterializer, never()).materialize(any(Result.class), any(TestAttempt.class), any(ResultSnapshotFacts.class));
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
    }

    @Test
    void persistedDuplicateChoiceStateFailsResultRecording() {
        assertPersistedDuplicateStateFailsResultRecording(
            9029L,
            "Result recording rejects duplicate persisted choice state",
            "duplicate persisted choice"
        );
    }

    @Test
    void persistedDuplicateMatchingExactPairFailsResultRecording() {
        assertPersistedDuplicateStateFailsResultRecording(
            9031L,
            "Result recording rejects duplicate persisted matching pair state",
            "duplicate persisted matching pair"
        );
    }

    @Test
    void persistedDuplicateMatchingLeftFailsResultRecording() {
        assertPersistedDuplicateStateFailsResultRecording(
            9030L,
            "Result recording rejects duplicate persisted matching left state",
            "duplicate persisted matching left"
        );
    }

    @Test
    void persistedDuplicateMatchingRightFailsResultRecording() {
        assertPersistedDuplicateStateFailsResultRecording(
            9028L,
            "Result recording rejects duplicate persisted matching right state",
            "duplicate persisted matching right"
        );
    }

    @Test
    void persistedDuplicateOrderingOptionFailsResultRecording() {
        assertPersistedDuplicateStateFailsResultRecording(
            9032L,
            "Result recording rejects duplicate persisted ordering option state",
            "duplicate persisted ordering option"
        );
    }

    @Test
    void persistedDuplicateOrderingPositionFailsResultRecording() {
        assertPersistedDuplicateStateFailsResultRecording(
            9033L,
            "Result recording rejects duplicate persisted ordering position state",
            "duplicate persisted ordering position"
        );
    }

    @Test
    void duplicateRaceReReadPathDoesNotWriteAudit() {
        TestAttempt completedAttempt = terminalAssignedAttempt(9004L, TestAttemptStatus.COMPLETED);
        ResultSnapshotFacts snapshotFacts = assignedSnapshotFacts();
        Result existing = countedAssignedResult(404L, 9004L);

        when(resultRepository.findResultByTestAttemptId(9004L)).thenReturn(null, existing);
        when(testAttemptRepository.findTestAttemptById(9004L)).thenReturn(completedAttempt);
        when(snapshotFactsProvider.provideSnapshotFacts(completedAttempt)).thenReturn(snapshotFacts);
        when(idempotentReplayValidator.isIdenticalReplay(eq(existing), any(Result.class))).thenReturn(true);
        when(resultRepository.saveResult(any(Result.class)))
            .thenThrow(new PersistenceConstraintViolationException("Failed to persist result"));

        Long resultId = service.recordResult(9004L);

        assertThat(resultId).isEqualTo(404L);
        verify(resultRepository).saveResult(any(Result.class));
        verify(resultRepository, times(2)).findResultByTestAttemptId(9004L);
        verify(testAttemptRepository, times(2)).findTestAttemptById(9004L);
        verify(snapshotFactsProvider, times(2)).provideSnapshotFacts(completedAttempt);
        verify(idempotentReplayValidator).isIdenticalReplay(eq(existing), any(Result.class));
        verify(subordinateSnapshotMaterializer, never()).materialize(any(), any(), any());
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
        verify(criticalCommandAuditSupport, never()).resolveInteractiveActorUserId();
        verify(criticalCommandAuditSupport, never()).resolveSystemActorUserId(any());
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(String.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void resultRecordingRejectsExpiredAttempt() {
        TestAttempt expiredAttempt = terminalAssignedAttempt(9005L, TestAttemptStatus.EXPIRED);

        when(resultRepository.findResultByTestAttemptId(9005L)).thenReturn(null);
        when(testAttemptRepository.findTestAttemptById(9005L)).thenReturn(expiredAttempt);
        assertThatThrownBy(() -> service.recordResult(9005L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("EXPIRED");

        verify(resultRepository).findResultByTestAttemptId(9005L);
        verify(testAttemptRepository).findTestAttemptById(9005L);
        verify(snapshotFactsProvider, never()).provideSnapshotFacts(any());
        verify(resultRepository, never()).saveResult(any(Result.class));
        verify(subordinateSnapshotMaterializer, never()).materialize(any(), any(), any());
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
        verify(criticalCommandAuditSupport, never()).resolveInteractiveActorUserId();
        verify(criticalCommandAuditSupport, never()).resolveSystemActorUserId(any());
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(String.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
        verifyNoMoreInteractions(
            resultRepository,
            testAttemptRepository,
            snapshotFactsProvider,
            subordinateSnapshotMaterializer,
            assignmentCountedResultHandoffService,
            criticalCommandAuditSupport,
            systemActorResolver
        );
    }

    @Test
    void resultRecordingRejectsAbandonedAttempt() {
        TestAttempt abandonedAttempt = terminalSelfAttempt(9006L, TestAttemptStatus.ABANDONED);

        when(resultRepository.findResultByTestAttemptId(9006L)).thenReturn(null);
        when(testAttemptRepository.findTestAttemptById(9006L)).thenReturn(abandonedAttempt);

        assertThatThrownBy(() -> service.recordResult(9006L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("ABANDONED");

        verify(resultRepository).findResultByTestAttemptId(9006L);
        verify(testAttemptRepository).findTestAttemptById(9006L);
        verify(snapshotFactsProvider, never()).provideSnapshotFacts(any());
        verify(resultRepository, never()).saveResult(any(Result.class));
        verify(subordinateSnapshotMaterializer, never()).materialize(any(), any(), any());
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
        verify(criticalCommandAuditSupport, never()).resolveInteractiveActorUserId();
        verify(criticalCommandAuditSupport, never()).resolveSystemActorUserId(any());
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(String.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void interactiveTerminalRecordingUsesInteractiveActorPathForAudit() {
        TestAttempt completedAttempt = terminalAssignedAttempt(9007L, TestAttemptStatus.COMPLETED);
        ResultSnapshotFacts snapshotFacts = assignedSnapshotFacts();
        when(resultRepository.findResultByTestAttemptId(9007L)).thenReturn(null);
        when(testAttemptRepository.findTestAttemptById(9007L)).thenReturn(completedAttempt);
        when(snapshotFactsProvider.provideSnapshotFacts(completedAttempt)).thenReturn(snapshotFacts);
        when(resultRepository.saveResult(any(Result.class))).thenReturn(countedAssignedResult(407L, 9007L));
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(criticalCommandAuditSupport.buildAuditContext(any(), any(String.class), any()))
            .thenReturn(new AuditContext("{\"operationCode\":\"RESULT_RECORDING\"}"));

        Long resultId = service.recordResult(9007L);

        assertThat(resultId).isEqualTo(407L);
        verify(subordinateSnapshotMaterializer).materialize(countedAssignedResult(407L, 9007L), completedAttempt, snapshotFacts);
        verify(assignmentCountedResultHandoffService).acceptValidCountedAssignmentResult(407L);
        verify(criticalCommandAuditSupport).resolveInteractiveActorUserId();
        verify(criticalCommandAuditSupport, never()).resolveSystemActorUserId(any());
    }

    @Test
    void auditUsesResultAnchorAndNullBeforePayload() {
        TestAttempt completedAttempt = terminalAssignedAttempt(9008L, TestAttemptStatus.COMPLETED);
        ResultSnapshotFacts snapshotFacts = assignedSnapshotFacts();
        Result savedResult = countedAssignedResult(408L, 9008L);
        AuditContext auditContext = new AuditContext("{\"operationCode\":\"RESULT_RECORDING\"}");

        when(resultRepository.findResultByTestAttemptId(9008L)).thenReturn(null);
        when(testAttemptRepository.findTestAttemptById(9008L)).thenReturn(completedAttempt);
        when(snapshotFactsProvider.provideSnapshotFacts(completedAttempt)).thenReturn(snapshotFacts);
        when(resultRepository.saveResult(any(Result.class))).thenReturn(savedResult);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(778L);
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Result"),
            org.mockito.ArgumentMatchers.eq("RESULT_RECORDING"),
            any()
        )).thenReturn(auditContext);

        service.recordResult(9008L);

        verify(criticalCommandAuditSupport).recordAudit(
            org.mockito.ArgumentMatchers.eq(778L),
            any(),
            org.mockito.ArgumentMatchers.eq("result"),
            org.mockito.ArgumentMatchers.eq(408L),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.argThat(payload -> payload instanceof java.util.Map<?, ?> map
                && map.get("id").equals(408L)
                && map.get("testAttemptId").equals(9008L)
            ),
            org.mockito.ArgumentMatchers.eq(auditContext)
        );
    }

    @Test
    void newlyMaterializedAssignedResultTriggersDownstreamHandoff() {
        TestAttempt completedAttempt = terminalAssignedAttempt(9009L, TestAttemptStatus.COMPLETED);
        ResultSnapshotFacts snapshotFacts = assignedSnapshotFacts();
        when(resultRepository.findResultByTestAttemptId(9009L)).thenReturn(null);
        when(testAttemptRepository.findTestAttemptById(9009L)).thenReturn(completedAttempt);
        when(snapshotFactsProvider.provideSnapshotFacts(completedAttempt)).thenReturn(snapshotFacts);
        when(resultRepository.saveResult(any(Result.class))).thenReturn(countedAssignedResult(409L, 9009L));
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(779L);
        when(criticalCommandAuditSupport.buildAuditContext(any(), any(String.class), any()))
            .thenReturn(new AuditContext("{\"operationCode\":\"RESULT_RECORDING\"}"));

        Long resultId = service.recordResult(9009L);

        assertThat(resultId).isEqualTo(409L);
        verify(subordinateSnapshotMaterializer).materialize(countedAssignedResult(409L, 9009L), completedAttempt, snapshotFacts);
        verify(assignmentCountedResultHandoffService).acceptValidCountedAssignmentResult(409L);
    }

    @Test
    void newlyMaterializedNonCountedAssignedResultDoesNotTriggerDownstreamHandoff() {
        TestAttempt completedAttempt = terminalAssignedAttempt(9019L, TestAttemptStatus.COMPLETED);
        ResultSnapshotFacts snapshotFacts = nonCountedAssignedSnapshotFacts();
        Result assembled = new ResultSnapshotAssembler().assemble(completedAttempt, snapshotFacts);
        Result savedResult = new Result(
            419L,
            assembled.testAttemptId(),
            assembled.userIdSnapshot(),
            assembled.attemptMode(),
            assembled.assignmentId(),
            assembled.assignmentTestId(),
            assembled.testIdSnapshot(),
            assembled.testNameSnapshot(),
            assembled.scoringSnapshot(),
            assembled.withinDeadline(),
            assembled.countedInAssignment(),
            assembled.completedAt(),
            assembled.orgContextSnapshot(),
            assembled.snapshotFinalTopicControlFlag(),
            assembled.createdAt()
        );

        when(resultRepository.findResultByTestAttemptId(9019L)).thenReturn(null);
        when(testAttemptRepository.findTestAttemptById(9019L)).thenReturn(completedAttempt);
        when(snapshotFactsProvider.provideSnapshotFacts(completedAttempt)).thenReturn(snapshotFacts);
        when(resultRepository.saveResult(any(Result.class))).thenReturn(savedResult);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(904L);
        when(criticalCommandAuditSupport.buildAuditContext(any(), any(String.class), any()))
            .thenReturn(new AuditContext("{\"operationCode\":\"RESULT_RECORDING\"}"));

        Long resultId = service.recordResult(9019L);

        assertThat(resultId).isEqualTo(419L);
        assertThat(savedResult.assignmentId()).isEqualTo(801L);
        assertThat(savedResult.assignmentTestId()).isEqualTo(701L);
        assertThat(completedAttempt.status()).isEqualTo(TestAttemptStatus.COMPLETED);
        assertThat(savedResult.scoringSnapshot().passed()).isTrue();
        assertThat(savedResult.withinDeadline()).isTrue();
        assertThat(savedResult.countedInAssignment()).isFalse();
        verify(subordinateSnapshotMaterializer).materialize(savedResult, completedAttempt, snapshotFacts);
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
    }

    @Test
    void terminalizationOutcomeAloneCannotTriggerCountedHandoff() throws Exception {
        String sequencingSource = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/testing/service/AssignedAttemptSubmitSequencingService.java"
        ));
        String outcomeSource = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/testing/service/AttemptTerminalizationOutcome.java"
        ));

        assertThat(outcomeSource)
            .contains("boolean resultRecordable")
            .doesNotContain("countedHandoffEligible")
            .doesNotContain("countedInAssignment");
        assertThat(sequencingSource)
            .contains("terminalizationOutcome.resultRecordable()")
            .doesNotContain("acceptValidCountedAssignmentResult(");
    }

    @Test
    void lateAssignedResultDoesNotInvokeCountedHandoff() {
        TestAttempt expiredAttempt = terminalAssignedAttempt(9016L, TestAttemptStatus.EXPIRED);

        when(resultRepository.findResultByTestAttemptId(9016L)).thenReturn(null);
        when(testAttemptRepository.findTestAttemptById(9016L)).thenReturn(expiredAttempt);

        assertThatThrownBy(() -> service.recordResult(9016L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("only COMPLETED is recordable")
            .hasMessageContaining("EXPIRED");

        verify(snapshotFactsProvider, never()).provideSnapshotFacts(any());
        verify(resultRepository, never()).saveResult(any(Result.class));
        verify(subordinateSnapshotMaterializer, never()).materialize(any(), any(), any());
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void failedAssignedResultDoesNotInvokeCountedHandoff() {
        TestAttempt completedAttempt = terminalAssignedAttempt(9017L, TestAttemptStatus.COMPLETED);
        ResultSnapshotFacts snapshotFacts = failedAssignedNotCountedSnapshotFacts();
        Result assembled = new ResultSnapshotAssembler().assemble(completedAttempt, snapshotFacts);
        Result savedResult = new Result(
            417L,
            assembled.testAttemptId(),
            assembled.userIdSnapshot(),
            assembled.attemptMode(),
            assembled.assignmentId(),
            assembled.assignmentTestId(),
            assembled.testIdSnapshot(),
            assembled.testNameSnapshot(),
            assembled.scoringSnapshot(),
            assembled.withinDeadline(),
            assembled.countedInAssignment(),
            assembled.completedAt(),
            assembled.orgContextSnapshot(),
            assembled.snapshotFinalTopicControlFlag(),
            assembled.createdAt()
        );

        when(resultRepository.findResultByTestAttemptId(9017L)).thenReturn(null);
        when(testAttemptRepository.findTestAttemptById(9017L)).thenReturn(completedAttempt);
        when(snapshotFactsProvider.provideSnapshotFacts(completedAttempt)).thenReturn(snapshotFacts);
        when(resultRepository.saveResult(any(Result.class))).thenReturn(savedResult);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(903L);
        when(criticalCommandAuditSupport.buildAuditContext(any(), any(String.class), any()))
            .thenReturn(new AuditContext("{\"operationCode\":\"RESULT_RECORDING\"}"));

        Long resultId = service.recordResult(9017L);

        assertThat(resultId).isEqualTo(417L);
        assertThat(savedResult.assignmentId()).isEqualTo(801L);
        assertThat(savedResult.assignmentTestId()).isEqualTo(701L);
        assertThat(savedResult.withinDeadline()).isTrue();
        assertThat(savedResult.scoringSnapshot().passed()).isFalse();
        assertThat(savedResult.countedInAssignment()).isFalse();
        verify(subordinateSnapshotMaterializer).materialize(savedResult, completedAttempt, snapshotFacts);
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
    }

    @Test
    void nonFinalControlAssignedResultDoesNotInvokeCountedHandoff() {
        TestAttempt completedAttempt = terminalAssignedAttempt(90171L, TestAttemptStatus.COMPLETED);
        ResultSnapshotFacts snapshotFacts = invalidNonFinalAssignedCountedSnapshotFacts();
        Result assembled = new ResultSnapshotAssembler().assemble(completedAttempt, snapshotFacts);
        Result savedResult = futureCountedDecisionResult(4171L, assembled);

        when(resultRepository.findResultByTestAttemptId(90171L)).thenReturn(null);
        when(testAttemptRepository.findTestAttemptById(90171L)).thenReturn(completedAttempt);
        when(snapshotFactsProvider.provideSnapshotFacts(completedAttempt)).thenReturn(snapshotFacts);
        when(resultRepository.saveResult(any(Result.class))).thenReturn(savedResult);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(9031L);
        when(criticalCommandAuditSupport.buildAuditContext(any(), any(String.class), any()))
            .thenReturn(new AuditContext("{\"operationCode\":\"RESULT_RECORDING\"}"));

        Long resultId = service.recordResult(90171L);

        assertThat(resultId).isEqualTo(4171L);
        assertThat(savedResult.assignmentId()).isEqualTo(801L);
        assertThat(savedResult.assignmentTestId()).isEqualTo(701L);
        assertThat(savedResult.scoringSnapshot().passed()).isTrue();
        assertThat(savedResult.withinDeadline()).isTrue();
        assertThat(savedResult.snapshotFinalTopicControlFlag()).isFalse();
        assertThat(savedResult.countedInAssignment()).isTrue();
        verify(subordinateSnapshotMaterializer).materialize(savedResult, completedAttempt, snapshotFacts);
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
    }

    @Test
    void existingCanonicalCountedResultUsesIdempotentHandoffReplayWithoutNewResultMaterialization() {
        Result existing = countedAssignedResult(410L, 9010L);
        TestAttempt completedAttempt = terminalAssignedAttempt(9010L, TestAttemptStatus.COMPLETED);
        ResultSnapshotFacts replayFacts = assignedSnapshotFacts();
        when(resultRepository.findResultByTestAttemptId(9010L)).thenReturn(existing);
        when(testAttemptRepository.findTestAttemptById(9010L)).thenReturn(completedAttempt);
        when(snapshotFactsProvider.provideSnapshotFacts(completedAttempt)).thenReturn(replayFacts);
        when(idempotentReplayValidator.isIdenticalReplay(eq(existing), any(Result.class))).thenReturn(true);

        Long resultId = service.recordResult(9010L);

        assertThat(resultId).isEqualTo(410L);
        verify(resultRepository).findResultByTestAttemptId(9010L);
        verify(testAttemptRepository).findTestAttemptById(9010L);
        verify(snapshotFactsProvider).provideSnapshotFacts(completedAttempt);
        verify(idempotentReplayValidator).isIdenticalReplay(eq(existing), any(Result.class));
        verify(subordinateSnapshotMaterializer, never()).materialize(any(), any(), any());
        verify(resultRepository, never()).saveResult(any(Result.class));
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void existingCanonicalAssignedResultDoesNotInvokeCountedHandoffOnReplay() {
        idempotentReplayDoesNotInvokeCountedHandoffAgain();
    }

    @Test
    void existingCanonicalAssignedResultDoesNotRematerializeFactsSnapshotsOrAttemptState() {
        Result existing = countedAssignedResult(415L, 9015L);
        TestAttempt completedAttempt = terminalAssignedAttempt(9015L, TestAttemptStatus.COMPLETED);
        ResultSnapshotFacts replayFacts = assignedSnapshotFacts();
        when(resultRepository.findResultByTestAttemptId(9015L)).thenReturn(existing);
        when(testAttemptRepository.findTestAttemptById(9015L)).thenReturn(completedAttempt);
        when(snapshotFactsProvider.provideSnapshotFacts(completedAttempt)).thenReturn(replayFacts);
        when(idempotentReplayValidator.isIdenticalReplay(eq(existing), any(Result.class))).thenReturn(true);

        Long resultId = service.recordResult(9015L);

        assertThat(resultId).isEqualTo(415L);
        verify(resultRepository).findResultByTestAttemptId(9015L);
        verify(testAttemptRepository).findTestAttemptById(9015L);
        verify(snapshotFactsProvider).provideSnapshotFacts(completedAttempt);
        verify(idempotentReplayValidator).isIdenticalReplay(eq(existing), any(Result.class));
        verify(subordinateSnapshotMaterializer, never()).materialize(any(), any(), any());
        verify(resultRepository, never()).saveResult(any(Result.class));
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void secondRecordingForSameAttemptDoesNotOverwriteExistingResult() {
        Result existing = countedAssignedResult(425L, 9025L);
        TestAttempt completedAttempt = terminalAssignedAttempt(9025L, TestAttemptStatus.COMPLETED);
        ResultSnapshotFacts replayFacts = assignedSnapshotFacts();
        when(resultRepository.findResultByTestAttemptId(9025L)).thenReturn(existing);
        when(testAttemptRepository.findTestAttemptById(9025L)).thenReturn(completedAttempt);
        when(snapshotFactsProvider.provideSnapshotFacts(completedAttempt)).thenReturn(replayFacts);
        when(idempotentReplayValidator.isIdenticalReplay(eq(existing), any(Result.class))).thenReturn(true);

        Long resultId = service.recordResult(9025L);

        assertThat(resultId).isEqualTo(425L);
        verify(resultRepository).findResultByTestAttemptId(9025L);
        verify(testAttemptRepository).findTestAttemptById(9025L);
        verify(snapshotFactsProvider).provideSnapshotFacts(completedAttempt);
        verify(idempotentReplayValidator).isIdenticalReplay(eq(existing), any(Result.class));
        verify(resultRepository, never()).saveResult(any(Result.class));
        verify(subordinateSnapshotMaterializer, never()).materialize(any(), any(), any());
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void secondRecordingForSameAttemptReturnsExistingResultOnlyThroughExplicitIdempotentReplay() {
        Result existing = countedAssignedResult(426L, 9026L);
        TestAttempt completedAttempt = terminalAssignedAttempt(9026L, TestAttemptStatus.COMPLETED);
        ResultSnapshotFacts replayFacts = assignedSnapshotFacts();
        when(resultRepository.findResultByTestAttemptId(9026L)).thenReturn(existing);
        when(testAttemptRepository.findTestAttemptById(9026L)).thenReturn(completedAttempt);
        when(snapshotFactsProvider.provideSnapshotFacts(completedAttempt)).thenReturn(replayFacts);
        when(idempotentReplayValidator.isIdenticalReplay(eq(existing), any(Result.class))).thenReturn(true);

        Long resultId = service.recordResult(9026L);

        assertThat(resultId).isEqualTo(426L);
        verify(resultRepository).findResultByTestAttemptId(9026L);
        verify(testAttemptRepository).findTestAttemptById(9026L);
        verify(snapshotFactsProvider).provideSnapshotFacts(completedAttempt);
        verify(idempotentReplayValidator).isIdenticalReplay(eq(existing), any(Result.class));
        verify(resultRepository, never()).saveResult(any(Result.class));
        verify(subordinateSnapshotMaterializer, never()).materialize(any(), any(), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
    }

    @Test
    void incompleteChildSnapshotAggregateDoesNotPassIdempotentReplay() {
        Result existing = new Result(
            428L,
            9048L,
            3901L,
            AttemptMode.ASSIGNED,
            801L,
            701L,
            501L,
            "Assigned Test",
            new ResultScoringSnapshot(
                new BigDecimal("100.00"),
                new BigDecimal("5.0000"),
                new BigDecimal("5.0000"),
                new BigDecimal("100.0000"),
                true,
                "DEFAULT_PARTIAL_CREDIT_V1",
                "{\"policy\":\"v1\"}"
            ),
            true,
            true,
            TERMINAL_AT,
            new ResultOrgContextSnapshot(901L, "/company/ops"),
            true,
            RECORDED_AT
        );
        TestAttempt completedAttempt = terminalAssignedAttempt(9048L, TestAttemptStatus.COMPLETED);
        ResultSnapshotFacts replayFacts = new ResultSnapshotFacts(
            801L,
            701L,
            501L,
            "Assigned Test",
            existing.scoringSnapshot(),
            true,
            true,
            new ResultOrgContextSnapshot(901L, "/company/ops"),
            true,
            RECORDED_AT,
            new ResultSnapshotFacts.ResultSubordinateSnapshotFacts(List.of(
                new ResultSnapshotFacts.ResultQuestionSnapshotFact(
                    6401L,
                    "Frozen question body",
                    QuestionType.SINGLE_CHOICE,
                    0,
                    new BigDecimal("5.0000"),
                    List.of(
                        new ResultSnapshotFacts.ResultAnswerOptionSnapshotFact(
                            7401L,
                            "Frozen correct option",
                            AnswerOptionRole.CHOICE_OPTION,
                            true,
                            0,
                            null,
                            null
                        ),
                        new ResultSnapshotFacts.ResultAnswerOptionSnapshotFact(
                            7402L,
                            "Frozen wrong option",
                            AnswerOptionRole.CHOICE_OPTION,
                            false,
                            1,
                            null,
                            null
                        )
                    ),
                    List.of(
                        new ResultSnapshotFacts.ResultUserAnswerItemSnapshotFact(
                            7401L,
                            null,
                            null,
                            null
                        )
                    )
                )
            ))
        );
        ResultRecordingSubordinateSnapshotMaterializer realMaterializer = new ResultRecordingSubordinateSnapshotMaterializer(
            resultQuestionSnapshotRepository,
            resultAnswerOptionSnapshotRepository,
            testQuestionRepository,
            questionRepository,
            answerOptionRepository,
            userAnswerRepository,
            userAnswerItemRepository,
            new ResultQuestionScoringCalculator()
        );
        ResultRecordingChildSnapshotCompletenessValidator realChildSnapshotValidator =
            new ResultRecordingChildSnapshotCompletenessValidator(
                resultQuestionSnapshotRepository,
                resultAnswerOptionSnapshotRepository,
                realMaterializer
            );
        ResultRecordingSubordinateSnapshotMaterializer.ExpectedSubordinateSnapshotAggregate expectedAggregate =
            realMaterializer.buildExpectedAggregate(existing, replayFacts);
        ResultQuestionSnapshot expectedQuestionSnapshot =
            expectedAggregate.questionSnapshotAggregates().getFirst().questionSnapshot();
        List<ResultAnswerOptionSnapshot> expectedAnswerOptionSnapshots =
            expectedAggregate.questionSnapshotAggregates().getFirst().answerOptionSnapshots();
        ResultRecordingServiceImpl realService = new ResultRecordingServiceImpl(
            resultRepository,
            testAttemptRepository,
            snapshotFactsProvider,
            subordinateSnapshotMaterializer,
            countedAssignmentResultValidityGate,
            assignmentCountedResultHandoffService,
            criticalCommandAuditSupport,
            systemActorResolver,
            idempotentReplayValidator,
            realChildSnapshotValidator
        );

        when(resultRepository.findResultByTestAttemptId(9048L)).thenReturn(existing);
        when(testAttemptRepository.findTestAttemptById(9048L)).thenReturn(completedAttempt);
        when(snapshotFactsProvider.provideSnapshotFacts(completedAttempt)).thenReturn(replayFacts);
        when(idempotentReplayValidator.isIdenticalReplay(eq(existing), any(Result.class))).thenReturn(true);
        when(resultQuestionSnapshotRepository.findResultQuestionSnapshotsByResultId(428L)).thenReturn(List.of());

        assertThatThrownBy(() -> realService.recordResult(9048L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("complete child snapshot aggregate")
            .hasMessageContaining("9048");

        verify(resultRepository).findResultByTestAttemptId(9048L);
        verify(testAttemptRepository).findTestAttemptById(9048L);
        verify(snapshotFactsProvider).provideSnapshotFacts(completedAttempt);
        verify(idempotentReplayValidator).isIdenticalReplay(eq(existing), any(Result.class));
        verify(resultQuestionSnapshotRepository).findResultQuestionSnapshotsByResultId(428L);
        verify(resultAnswerOptionSnapshotRepository, never())
            .findResultAnswerOptionSnapshotsByResultQuestionSnapshotId(any());
        verify(resultRepository, never()).saveResult(any(Result.class));
        verify(subordinateSnapshotMaterializer, never()).materialize(any(), any(), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
    }

    @Test
    void semanticQuestionSnapshotDriftDoesNotPassIdempotentReplay() {
        Result existing = new Result(
            429L,
            9049L,
            3902L,
            AttemptMode.ASSIGNED,
            801L,
            701L,
            501L,
            "Assigned Test",
            new ResultScoringSnapshot(
                new BigDecimal("100.00"),
                new BigDecimal("5.0000"),
                new BigDecimal("5.0000"),
                new BigDecimal("100.0000"),
                true,
                "DEFAULT_PARTIAL_CREDIT_V1",
                "{\"policy\":\"v1\"}"
            ),
            true,
            true,
            TERMINAL_AT,
            new ResultOrgContextSnapshot(901L, "/company/ops"),
            true,
            RECORDED_AT
        );
        TestAttempt completedAttempt = terminalAssignedAttempt(9049L, TestAttemptStatus.COMPLETED);
        ResultSnapshotFacts replayFacts = new ResultSnapshotFacts(
            801L,
            701L,
            501L,
            "Assigned Test",
            existing.scoringSnapshot(),
            true,
            true,
            new ResultOrgContextSnapshot(901L, "/company/ops"),
            true,
            RECORDED_AT,
            new ResultSnapshotFacts.ResultSubordinateSnapshotFacts(List.of(
                new ResultSnapshotFacts.ResultQuestionSnapshotFact(
                    6402L,
                    "Frozen question body",
                    QuestionType.SINGLE_CHOICE,
                    0,
                    new BigDecimal("5.0000"),
                    List.of(
                        new ResultSnapshotFacts.ResultAnswerOptionSnapshotFact(
                            7403L,
                            "Frozen correct option",
                            AnswerOptionRole.CHOICE_OPTION,
                            true,
                            0,
                            null,
                            null
                        ),
                        new ResultSnapshotFacts.ResultAnswerOptionSnapshotFact(
                            7404L,
                            "Frozen wrong option",
                            AnswerOptionRole.CHOICE_OPTION,
                            false,
                            1,
                            null,
                            null
                        )
                    ),
                    List.of(
                        new ResultSnapshotFacts.ResultUserAnswerItemSnapshotFact(
                            7403L,
                            null,
                            null,
                            null
                        )
                    )
                )
            ))
        );
        ResultRecordingSubordinateSnapshotMaterializer realMaterializer = new ResultRecordingSubordinateSnapshotMaterializer(
            resultQuestionSnapshotRepository,
            resultAnswerOptionSnapshotRepository,
            testQuestionRepository,
            questionRepository,
            answerOptionRepository,
            userAnswerRepository,
            userAnswerItemRepository,
            new ResultQuestionScoringCalculator()
        );
        ResultRecordingChildSnapshotCompletenessValidator realChildSnapshotValidator =
            new ResultRecordingChildSnapshotCompletenessValidator(
                resultQuestionSnapshotRepository,
                resultAnswerOptionSnapshotRepository,
                realMaterializer
            );
        ResultRecordingSubordinateSnapshotMaterializer.ExpectedSubordinateSnapshotAggregate expectedAggregate =
            realMaterializer.buildExpectedAggregate(existing, replayFacts);
        ResultQuestionSnapshot expectedQuestionSnapshot =
            expectedAggregate.questionSnapshotAggregates().get(0).questionSnapshot();
        List<ResultAnswerOptionSnapshot> expectedAnswerOptionSnapshots =
            expectedAggregate.questionSnapshotAggregates().get(0).answerOptionSnapshots();
        ResultRecordingServiceImpl realService = new ResultRecordingServiceImpl(
            resultRepository,
            testAttemptRepository,
            snapshotFactsProvider,
            subordinateSnapshotMaterializer,
            countedAssignmentResultValidityGate,
            assignmentCountedResultHandoffService,
            criticalCommandAuditSupport,
            systemActorResolver,
            idempotentReplayValidator,
            realChildSnapshotValidator
        );

        when(resultRepository.findResultByTestAttemptId(9049L)).thenReturn(existing);
        when(testAttemptRepository.findTestAttemptById(9049L)).thenReturn(completedAttempt);
        when(snapshotFactsProvider.provideSnapshotFacts(completedAttempt)).thenReturn(replayFacts);
        when(idempotentReplayValidator.isIdenticalReplay(eq(existing), any(Result.class))).thenReturn(true);
        when(resultQuestionSnapshotRepository.findResultQuestionSnapshotsByResultId(429L)).thenReturn(List.of(
            new ResultQuestionSnapshot(
                9201L,
                429L,
                6402L,
                "Frozen question body",
                ResultQuestionType.SINGLE_CHOICE,
                0,
                new BigDecimal("5.0000"),
                "{\"correctOptionIds\":[7403]}",
                "{\"selectedOptionIds\":[7403]}",
                BigDecimal.ZERO,
                new BigDecimal("5.0000"),
                false,
                "semantic drifted persisted evaluation note",
                RECORDED_AT
            )
        ));
        assertThatThrownBy(() -> realService.recordResult(9049L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("complete child snapshot aggregate")
            .hasMessageContaining("9049");

        verify(resultRepository).findResultByTestAttemptId(9049L);
        verify(testAttemptRepository).findTestAttemptById(9049L);
        verify(snapshotFactsProvider).provideSnapshotFacts(completedAttempt);
        verify(idempotentReplayValidator).isIdenticalReplay(eq(existing), any(Result.class));
        verify(resultQuestionSnapshotRepository).findResultQuestionSnapshotsByResultId(429L);
        verify(resultAnswerOptionSnapshotRepository, never())
            .findResultAnswerOptionSnapshotsByResultQuestionSnapshotId(any());
        verify(resultRepository, never()).saveResult(any(Result.class));
        verify(subordinateSnapshotMaterializer, never()).materialize(any(), any(), any());
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(String.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
    }

    @Test
    void answerOptionQuestionSnapshotBindingDriftDoesNotPassIdempotentReplay() {
        ReplayChildAggregateFixture fixture = assignedReplayChildAggregateFixture(430L, 9050L, 6403L, 7405L, 7406L);

        when(resultRepository.findResultByTestAttemptId(9050L)).thenReturn(fixture.existingResult());
        when(testAttemptRepository.findTestAttemptById(9050L)).thenReturn(fixture.completedAttempt());
        when(snapshotFactsProvider.provideSnapshotFacts(fixture.completedAttempt())).thenReturn(fixture.replayFacts());
        when(idempotentReplayValidator.isIdenticalReplay(eq(fixture.existingResult()), any(Result.class))).thenReturn(true);
        when(resultQuestionSnapshotRepository.findResultQuestionSnapshotsByResultId(430L)).thenReturn(List.of(
            new ResultQuestionSnapshot(
                9202L,
                fixture.expectedQuestionSnapshot().resultId(),
                fixture.expectedQuestionSnapshot().questionOriginalId(),
                fixture.expectedQuestionSnapshot().body(),
                fixture.expectedQuestionSnapshot().questionType(),
                fixture.expectedQuestionSnapshot().displayOrder(),
                fixture.expectedQuestionSnapshot().weight(),
                fixture.expectedQuestionSnapshot().correctAnswerSnapshot(),
                fixture.expectedQuestionSnapshot().userAnswerSnapshot(),
                fixture.expectedQuestionSnapshot().earnedScore(),
                fixture.expectedQuestionSnapshot().maxScore(),
                fixture.expectedQuestionSnapshot().isCorrect(),
                fixture.expectedQuestionSnapshot().evaluationNote(),
                fixture.expectedQuestionSnapshot().createdAt()
            )
        ));
        when(resultAnswerOptionSnapshotRepository.findResultAnswerOptionSnapshotsByResultQuestionSnapshotId(9202L))
            .thenReturn(List.of(
                new ResultAnswerOptionSnapshot(
                    9303L,
                    999999L,
                    fixture.expectedAnswerOptionSnapshots().get(0).answerOptionOriginalId(),
                    fixture.expectedAnswerOptionSnapshots().get(0).body(),
                    fixture.expectedAnswerOptionSnapshots().get(0).displayOrder(),
                    fixture.expectedAnswerOptionSnapshots().get(0).isCorrectAtSnapshot(),
                    fixture.expectedAnswerOptionSnapshots().get(0).isSelectedByUser(),
                    fixture.expectedAnswerOptionSnapshots().get(0).createdAt()
                ),
                new ResultAnswerOptionSnapshot(
                    9304L,
                    999999L,
                    fixture.expectedAnswerOptionSnapshots().get(1).answerOptionOriginalId(),
                    fixture.expectedAnswerOptionSnapshots().get(1).body(),
                    fixture.expectedAnswerOptionSnapshots().get(1).displayOrder(),
                    fixture.expectedAnswerOptionSnapshots().get(1).isCorrectAtSnapshot(),
                    fixture.expectedAnswerOptionSnapshots().get(1).isSelectedByUser(),
                    fixture.expectedAnswerOptionSnapshots().get(1).createdAt()
                )
            ));

        assertThatThrownBy(() -> fixture.realService().recordResult(9050L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("complete child snapshot aggregate")
            .hasMessageContaining("9050");

        verify(resultRepository).findResultByTestAttemptId(9050L);
        verify(testAttemptRepository).findTestAttemptById(9050L);
        verify(snapshotFactsProvider).provideSnapshotFacts(fixture.completedAttempt());
        verify(idempotentReplayValidator).isIdenticalReplay(eq(fixture.existingResult()), any(Result.class));
        verify(resultQuestionSnapshotRepository).findResultQuestionSnapshotsByResultId(430L);
        verify(resultAnswerOptionSnapshotRepository).findResultAnswerOptionSnapshotsByResultQuestionSnapshotId(9202L);
        verify(resultRepository, never()).saveResult(any(Result.class));
        verify(subordinateSnapshotMaterializer, never()).materialize(any(), any(), any());
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(String.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
    }

    @Test
    void questionSnapshotStructuralDriftDoesNotPassIdempotentReplay() {
        ReplayChildAggregateFixture fixture = assignedReplayChildAggregateFixture(431L, 9051L, 6404L, 7407L, 7408L);

        when(resultRepository.findResultByTestAttemptId(9051L)).thenReturn(fixture.existingResult());
        when(testAttemptRepository.findTestAttemptById(9051L)).thenReturn(fixture.completedAttempt());
        when(snapshotFactsProvider.provideSnapshotFacts(fixture.completedAttempt())).thenReturn(fixture.replayFacts());
        when(idempotentReplayValidator.isIdenticalReplay(eq(fixture.existingResult()), any(Result.class))).thenReturn(true);
        when(resultQuestionSnapshotRepository.findResultQuestionSnapshotsByResultId(431L)).thenReturn(List.of(
            new ResultQuestionSnapshot(
                9203L,
                fixture.expectedQuestionSnapshot().resultId(),
                fixture.expectedQuestionSnapshot().questionOriginalId(),
                "Drifted frozen question body",
                fixture.expectedQuestionSnapshot().questionType(),
                fixture.expectedQuestionSnapshot().displayOrder(),
                fixture.expectedQuestionSnapshot().weight(),
                fixture.expectedQuestionSnapshot().correctAnswerSnapshot(),
                fixture.expectedQuestionSnapshot().userAnswerSnapshot(),
                fixture.expectedQuestionSnapshot().earnedScore(),
                fixture.expectedQuestionSnapshot().maxScore(),
                fixture.expectedQuestionSnapshot().isCorrect(),
                fixture.expectedQuestionSnapshot().evaluationNote(),
                fixture.expectedQuestionSnapshot().createdAt()
            )
        ));

        assertThatThrownBy(() -> fixture.realService().recordResult(9051L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("complete child snapshot aggregate")
            .hasMessageContaining("9051");

        verify(resultRepository).findResultByTestAttemptId(9051L);
        verify(testAttemptRepository).findTestAttemptById(9051L);
        verify(snapshotFactsProvider).provideSnapshotFacts(fixture.completedAttempt());
        verify(idempotentReplayValidator).isIdenticalReplay(eq(fixture.existingResult()), any(Result.class));
        verify(resultQuestionSnapshotRepository).findResultQuestionSnapshotsByResultId(431L);
        verify(resultAnswerOptionSnapshotRepository, never()).findResultAnswerOptionSnapshotsByResultQuestionSnapshotId(any());
        verify(resultRepository, never()).saveResult(any(Result.class));
        verify(subordinateSnapshotMaterializer, never()).materialize(any(), any(), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
    }

    @Test
    void extraPersistedQuestionSnapshotDoesNotPassIdempotentReplay() {
        ReplayChildAggregateFixture fixture = assignedReplayChildAggregateFixture(432L, 9052L, 6405L, 7409L, 7410L);

        when(resultRepository.findResultByTestAttemptId(9052L)).thenReturn(fixture.existingResult());
        when(testAttemptRepository.findTestAttemptById(9052L)).thenReturn(fixture.completedAttempt());
        when(snapshotFactsProvider.provideSnapshotFacts(fixture.completedAttempt())).thenReturn(fixture.replayFacts());
        when(idempotentReplayValidator.isIdenticalReplay(eq(fixture.existingResult()), any(Result.class))).thenReturn(true);
        when(resultQuestionSnapshotRepository.findResultQuestionSnapshotsByResultId(432L)).thenReturn(List.of(
            new ResultQuestionSnapshot(
                9204L,
                fixture.expectedQuestionSnapshot().resultId(),
                fixture.expectedQuestionSnapshot().questionOriginalId(),
                fixture.expectedQuestionSnapshot().body(),
                fixture.expectedQuestionSnapshot().questionType(),
                fixture.expectedQuestionSnapshot().displayOrder(),
                fixture.expectedQuestionSnapshot().weight(),
                fixture.expectedQuestionSnapshot().correctAnswerSnapshot(),
                fixture.expectedQuestionSnapshot().userAnswerSnapshot(),
                fixture.expectedQuestionSnapshot().earnedScore(),
                fixture.expectedQuestionSnapshot().maxScore(),
                fixture.expectedQuestionSnapshot().isCorrect(),
                fixture.expectedQuestionSnapshot().evaluationNote(),
                fixture.expectedQuestionSnapshot().createdAt()
            ),
            new ResultQuestionSnapshot(
                9205L,
                432L,
                999001L,
                "Unexpected extra frozen question",
                ResultQuestionType.SINGLE_CHOICE,
                99,
                new BigDecimal("1.0000"),
                "{\"correctOptionIds\":[999101]}",
                "{\"selectedOptionIds\":[]}",
                BigDecimal.ZERO,
                new BigDecimal("1.0000"),
                false,
                "unexpected extra child aggregate row",
                RECORDED_AT
            )
        ));

        assertThatThrownBy(() -> fixture.realService().recordResult(9052L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("complete child snapshot aggregate")
            .hasMessageContaining("9052");

        verify(resultRepository).findResultByTestAttemptId(9052L);
        verify(testAttemptRepository).findTestAttemptById(9052L);
        verify(snapshotFactsProvider).provideSnapshotFacts(fixture.completedAttempt());
        verify(idempotentReplayValidator).isIdenticalReplay(eq(fixture.existingResult()), any(Result.class));
        verify(resultQuestionSnapshotRepository).findResultQuestionSnapshotsByResultId(432L);
        verify(resultAnswerOptionSnapshotRepository, never()).findResultAnswerOptionSnapshotsByResultQuestionSnapshotId(any());
        verify(resultRepository, never()).saveResult(any(Result.class));
        verify(subordinateSnapshotMaterializer, never()).materialize(any(), any(), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
    }

    @Test
    void answerOptionSemanticDriftDoesNotPassIdempotentReplay() {
        ReplayChildAggregateFixture fixture = assignedReplayChildAggregateFixture(433L, 9053L, 6406L, 7411L, 7412L);

        when(resultRepository.findResultByTestAttemptId(9053L)).thenReturn(fixture.existingResult());
        when(testAttemptRepository.findTestAttemptById(9053L)).thenReturn(fixture.completedAttempt());
        when(snapshotFactsProvider.provideSnapshotFacts(fixture.completedAttempt())).thenReturn(fixture.replayFacts());
        when(idempotentReplayValidator.isIdenticalReplay(eq(fixture.existingResult()), any(Result.class))).thenReturn(true);
        when(resultQuestionSnapshotRepository.findResultQuestionSnapshotsByResultId(433L)).thenReturn(List.of(
            persistedQuestionSnapshotCopy(9206L, fixture.expectedQuestionSnapshot())
        ));
        when(resultAnswerOptionSnapshotRepository.findResultAnswerOptionSnapshotsByResultQuestionSnapshotId(9206L))
            .thenReturn(List.of(
                new ResultAnswerOptionSnapshot(
                    9305L,
                    9206L,
                    fixture.expectedAnswerOptionSnapshots().get(0).answerOptionOriginalId(),
                    "Drifted answer option body",
                    fixture.expectedAnswerOptionSnapshots().get(0).displayOrder(),
                    fixture.expectedAnswerOptionSnapshots().get(0).isCorrectAtSnapshot(),
                    fixture.expectedAnswerOptionSnapshots().get(0).isSelectedByUser(),
                    fixture.expectedAnswerOptionSnapshots().get(0).createdAt()
                ),
                persistedAnswerOptionSnapshotCopy(9306L, 9206L, fixture.expectedAnswerOptionSnapshots().get(1))
            ));

        assertThatThrownBy(() -> fixture.realService().recordResult(9053L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("complete child snapshot aggregate")
            .hasMessageContaining("9053");

        verify(resultRepository).findResultByTestAttemptId(9053L);
        verify(testAttemptRepository).findTestAttemptById(9053L);
        verify(snapshotFactsProvider).provideSnapshotFacts(fixture.completedAttempt());
        verify(idempotentReplayValidator).isIdenticalReplay(eq(fixture.existingResult()), any(Result.class));
        verify(resultQuestionSnapshotRepository).findResultQuestionSnapshotsByResultId(433L);
        verify(resultAnswerOptionSnapshotRepository).findResultAnswerOptionSnapshotsByResultQuestionSnapshotId(9206L);
        verify(resultRepository, never()).saveResult(any(Result.class));
        verify(subordinateSnapshotMaterializer, never()).materialize(any(), any(), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
    }

    @Test
    void missingExpectedAnswerOptionSnapshotDoesNotPassIdempotentReplay() {
        ReplayChildAggregateFixture fixture = assignedReplayChildAggregateFixture(434L, 9054L, 6407L, 7413L, 7414L);

        when(resultRepository.findResultByTestAttemptId(9054L)).thenReturn(fixture.existingResult());
        when(testAttemptRepository.findTestAttemptById(9054L)).thenReturn(fixture.completedAttempt());
        when(snapshotFactsProvider.provideSnapshotFacts(fixture.completedAttempt())).thenReturn(fixture.replayFacts());
        when(idempotentReplayValidator.isIdenticalReplay(eq(fixture.existingResult()), any(Result.class))).thenReturn(true);
        when(resultQuestionSnapshotRepository.findResultQuestionSnapshotsByResultId(434L)).thenReturn(List.of(
            persistedQuestionSnapshotCopy(9207L, fixture.expectedQuestionSnapshot())
        ));
        when(resultAnswerOptionSnapshotRepository.findResultAnswerOptionSnapshotsByResultQuestionSnapshotId(9207L))
            .thenReturn(List.of(
                persistedAnswerOptionSnapshotCopy(9307L, 9207L, fixture.expectedAnswerOptionSnapshots().get(0))
            ));

        assertThatThrownBy(() -> fixture.realService().recordResult(9054L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("complete child snapshot aggregate")
            .hasMessageContaining("9054");

        verify(resultRepository).findResultByTestAttemptId(9054L);
        verify(testAttemptRepository).findTestAttemptById(9054L);
        verify(snapshotFactsProvider).provideSnapshotFacts(fixture.completedAttempt());
        verify(idempotentReplayValidator).isIdenticalReplay(eq(fixture.existingResult()), any(Result.class));
        verify(resultQuestionSnapshotRepository).findResultQuestionSnapshotsByResultId(434L);
        verify(resultAnswerOptionSnapshotRepository).findResultAnswerOptionSnapshotsByResultQuestionSnapshotId(9207L);
        verify(resultRepository, never()).saveResult(any(Result.class));
        verify(subordinateSnapshotMaterializer, never()).materialize(any(), any(), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
    }

    @Test
    void extraPersistedAnswerOptionSnapshotDoesNotPassIdempotentReplay() {
        ReplayChildAggregateFixture fixture = assignedReplayChildAggregateFixture(435L, 9055L, 6408L, 7415L, 7416L);

        when(resultRepository.findResultByTestAttemptId(9055L)).thenReturn(fixture.existingResult());
        when(testAttemptRepository.findTestAttemptById(9055L)).thenReturn(fixture.completedAttempt());
        when(snapshotFactsProvider.provideSnapshotFacts(fixture.completedAttempt())).thenReturn(fixture.replayFacts());
        when(idempotentReplayValidator.isIdenticalReplay(eq(fixture.existingResult()), any(Result.class))).thenReturn(true);
        when(resultQuestionSnapshotRepository.findResultQuestionSnapshotsByResultId(435L)).thenReturn(List.of(
            persistedQuestionSnapshotCopy(9208L, fixture.expectedQuestionSnapshot())
        ));
        when(resultAnswerOptionSnapshotRepository.findResultAnswerOptionSnapshotsByResultQuestionSnapshotId(9208L))
            .thenReturn(List.of(
                persistedAnswerOptionSnapshotCopy(9308L, 9208L, fixture.expectedAnswerOptionSnapshots().get(0)),
                persistedAnswerOptionSnapshotCopy(9309L, 9208L, fixture.expectedAnswerOptionSnapshots().get(1)),
                new ResultAnswerOptionSnapshot(
                    9310L,
                    9208L,
                    999002L,
                    "Unexpected extra answer option",
                    99,
                    false,
                    false,
                    RECORDED_AT
                )
            ));

        assertThatThrownBy(() -> fixture.realService().recordResult(9055L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("complete child snapshot aggregate")
            .hasMessageContaining("9055");

        verify(resultRepository).findResultByTestAttemptId(9055L);
        verify(testAttemptRepository).findTestAttemptById(9055L);
        verify(snapshotFactsProvider).provideSnapshotFacts(fixture.completedAttempt());
        verify(idempotentReplayValidator).isIdenticalReplay(eq(fixture.existingResult()), any(Result.class));
        verify(resultQuestionSnapshotRepository).findResultQuestionSnapshotsByResultId(435L);
        verify(resultAnswerOptionSnapshotRepository).findResultAnswerOptionSnapshotsByResultQuestionSnapshotId(9208L);
        verify(resultRepository, never()).saveResult(any(Result.class));
        verify(subordinateSnapshotMaterializer, never()).materialize(any(), any(), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
    }

    @Test
    void secondRecordingForSameAttemptWithDifferentPayloadFailsClosedWithoutOverwrite() {
        Result existing = countedAssignedResult(427L, 9027L);
        TestAttempt completedAttempt = terminalAssignedAttempt(9027L, TestAttemptStatus.COMPLETED);
        ResultSnapshotFacts replayFacts = failedAssignedNotCountedSnapshotFacts();
        when(resultRepository.findResultByTestAttemptId(9027L)).thenReturn(existing);
        when(testAttemptRepository.findTestAttemptById(9027L)).thenReturn(completedAttempt);
        when(snapshotFactsProvider.provideSnapshotFacts(completedAttempt)).thenReturn(replayFacts);
        when(idempotentReplayValidator.isIdenticalReplay(eq(existing), any(Result.class))).thenReturn(false);

        assertThatThrownBy(() -> service.recordResult(9027L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("not idempotent")
            .hasMessageContaining("9027");

        verify(resultRepository).findResultByTestAttemptId(9027L);
        verify(testAttemptRepository).findTestAttemptById(9027L);
        verify(snapshotFactsProvider).provideSnapshotFacts(completedAttempt);
        verify(idempotentReplayValidator).isIdenticalReplay(eq(existing), any(Result.class));
        verify(resultRepository, never()).saveResult(any(Result.class));
        verify(subordinateSnapshotMaterializer, never()).materialize(any(), any(), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
    }

    @Test
    void duplicateRaceReReadCanonicalResultTriggersDownstreamHandoffWithoutNewAudit() {
        TestAttempt completedAttempt = terminalAssignedAttempt(9011L, TestAttemptStatus.COMPLETED);
        ResultSnapshotFacts snapshotFacts = assignedSnapshotFacts();
        Result existing = countedAssignedResult(411L, 9011L);
        when(resultRepository.findResultByTestAttemptId(9011L)).thenReturn(null, existing);
        when(testAttemptRepository.findTestAttemptById(9011L)).thenReturn(completedAttempt);
        when(snapshotFactsProvider.provideSnapshotFacts(completedAttempt)).thenReturn(snapshotFacts);
        when(idempotentReplayValidator.isIdenticalReplay(eq(existing), any(Result.class))).thenReturn(true);
        when(resultRepository.saveResult(any(Result.class)))
            .thenThrow(new PersistenceConstraintViolationException("Failed to persist result"));

        Long resultId = service.recordResult(9011L);

        assertThat(resultId).isEqualTo(411L);
        verify(testAttemptRepository, times(2)).findTestAttemptById(9011L);
        verify(snapshotFactsProvider, times(2)).provideSnapshotFacts(completedAttempt);
        verify(idempotentReplayValidator).isIdenticalReplay(eq(existing), any(Result.class));
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void duplicateRaceRereadWithDifferentPayloadFailsClosedWithoutOverwrite() {
        TestAttempt completedAttempt = terminalAssignedAttempt(9018L, TestAttemptStatus.COMPLETED);
        ResultSnapshotFacts snapshotFacts = failedAssignedNotCountedSnapshotFacts();
        Result existing = countedAssignedResult(418L, 9018L);
        when(resultRepository.findResultByTestAttemptId(9018L)).thenReturn(null, existing);
        when(testAttemptRepository.findTestAttemptById(9018L)).thenReturn(completedAttempt);
        when(snapshotFactsProvider.provideSnapshotFacts(completedAttempt)).thenReturn(snapshotFacts);
        when(idempotentReplayValidator.isIdenticalReplay(eq(existing), any(Result.class))).thenReturn(false);
        when(resultRepository.saveResult(any(Result.class)))
            .thenThrow(new PersistenceConstraintViolationException("Failed to persist result"));

        assertThatThrownBy(() -> service.recordResult(9018L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("not idempotent")
            .hasMessageContaining("9018");

        verify(resultRepository).saveResult(any(Result.class));
        verify(resultRepository, times(2)).findResultByTestAttemptId(9018L);
        verify(testAttemptRepository, times(2)).findTestAttemptById(9018L);
        verify(snapshotFactsProvider, times(2)).provideSnapshotFacts(completedAttempt);
        verify(idempotentReplayValidator).isIdenticalReplay(eq(existing), any(Result.class));
        verify(subordinateSnapshotMaterializer, never()).materialize(any(), any(), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
    }

    @Test
    void existingCanonicalAssignedNonCountedResultDoesNotTriggerDownstreamHandoffWithoutNewAudit() {
        Result existing = nonCountedAssignedResult(420L, 9020L);
        TestAttempt completedAttempt = terminalAssignedAttempt(9020L, TestAttemptStatus.COMPLETED);
        ResultSnapshotFacts replayFacts = nonCountedAssignedSnapshotFacts();
        when(resultRepository.findResultByTestAttemptId(9020L)).thenReturn(existing);
        when(testAttemptRepository.findTestAttemptById(9020L)).thenReturn(completedAttempt);
        when(snapshotFactsProvider.provideSnapshotFacts(completedAttempt)).thenReturn(replayFacts);
        when(idempotentReplayValidator.isIdenticalReplay(eq(existing), any(Result.class))).thenReturn(true);

        Long resultId = service.recordResult(9020L);

        assertThat(resultId).isEqualTo(420L);
        verify(resultRepository).findResultByTestAttemptId(9020L);
        verify(testAttemptRepository).findTestAttemptById(9020L);
        verify(snapshotFactsProvider).provideSnapshotFacts(completedAttempt);
        verify(idempotentReplayValidator).isIdenticalReplay(eq(existing), any(Result.class));
        verify(resultRepository, never()).saveResult(any(Result.class));
        verify(subordinateSnapshotMaterializer, never()).materialize(any(), any(), any());
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void selfResultDoesNotTriggerAssignmentHandoff() {
        Result selfResult = selfRecordedResult(412L, 9012L);
        TestAttempt completedAttempt = terminalSelfAttempt(9012L, TestAttemptStatus.COMPLETED);
        ResultSnapshotFacts replayFacts = selfSnapshotFacts();
        when(resultRepository.findResultByTestAttemptId(9012L)).thenReturn(selfResult);
        when(testAttemptRepository.findTestAttemptById(9012L)).thenReturn(completedAttempt);
        when(snapshotFactsProvider.provideSnapshotFacts(completedAttempt)).thenReturn(replayFacts);
        when(idempotentReplayValidator.isIdenticalReplay(eq(selfResult), any(Result.class))).thenReturn(true);

        Long resultId = service.recordResult(9012L);

        assertThat(resultId).isEqualTo(412L);
        assertThat(selfResult.attemptMode()).isEqualTo(AttemptMode.SELF);
        assertThat(selfResult.countedInAssignment()).isNull();
        verify(testAttemptRepository).findTestAttemptById(9012L);
        verify(snapshotFactsProvider).provideSnapshotFacts(completedAttempt);
        verify(idempotentReplayValidator).isIdenticalReplay(eq(selfResult), any(Result.class));
        verify(resultRepository, never()).saveResult(any(Result.class));
        verify(subordinateSnapshotMaterializer, never()).materialize(any(), any(), any());
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void selfResultDoesNotInvokeCountedHandoff() {
        TestAttempt completedAttempt = terminalSelfAttempt(9014L, TestAttemptStatus.COMPLETED);
        ResultSnapshotFacts snapshotFacts = selfSnapshotFacts();
        Result savedResult = selfRecordedResult(414L, 9014L);
        AuditContext auditContext = new AuditContext("{\"operationCode\":\"RESULT_RECORDING\"}");

        when(resultRepository.findResultByTestAttemptId(9014L)).thenReturn(null);
        when(testAttemptRepository.findTestAttemptById(9014L)).thenReturn(completedAttempt);
        when(snapshotFactsProvider.provideSnapshotFacts(completedAttempt)).thenReturn(snapshotFacts);
        when(resultRepository.saveResult(any(Result.class))).thenReturn(savedResult);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(780L);
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Result"),
            org.mockito.ArgumentMatchers.eq("RESULT_RECORDING"),
            any()
        )).thenReturn(auditContext);

        Long resultId = service.recordResult(9014L);

        assertThat(resultId).isEqualTo(414L);
        assertThat(savedResult.attemptMode()).isEqualTo(AttemptMode.SELF);
        assertThat(savedResult.countedInAssignment()).isNull();
        verify(subordinateSnapshotMaterializer).materialize(savedResult, completedAttempt, snapshotFacts);
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
        verify(criticalCommandAuditSupport).recordAudit(
            org.mockito.ArgumentMatchers.eq(780L),
            any(),
            org.mockito.ArgumentMatchers.eq("result"),
            org.mockito.ArgumentMatchers.eq(414L),
            org.mockito.ArgumentMatchers.isNull(),
            any(),
            org.mockito.ArgumentMatchers.eq(auditContext)
        );
    }

    @Test
    void selfResultWillNeverBeCountedInAssignment() {
        TestAttempt completedAttempt = terminalSelfAttempt(9021L, TestAttemptStatus.COMPLETED);
        ResultSnapshotFacts snapshotFacts = futureCanonicalSelfSnapshotFacts();
        Result assembled = new ResultSnapshotAssembler().assemble(completedAttempt, snapshotFacts);
        Result savedResult = futureCountedDecisionResult(421L, assembled);

        when(resultRepository.findResultByTestAttemptId(9021L)).thenReturn(null);
        when(testAttemptRepository.findTestAttemptById(9021L)).thenReturn(completedAttempt);
        when(snapshotFactsProvider.provideSnapshotFacts(completedAttempt)).thenReturn(snapshotFacts);
        when(resultRepository.saveResult(any(Result.class))).thenReturn(savedResult);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(905L);
        when(criticalCommandAuditSupport.buildAuditContext(any(), any(String.class), any()))
            .thenReturn(new AuditContext("{\"operationCode\":\"RESULT_RECORDING\"}"));

        Long resultId = service.recordResult(9021L);

        assertThat(resultId).isEqualTo(421L);
        assertThat(savedResult.attemptMode()).isEqualTo(AttemptMode.SELF);
        assertThat(savedResult.assignmentId()).isNull();
        assertThat(savedResult.assignmentTestId()).isNull();
        assertThat(savedResult.withinDeadline()).isNull();
        assertThat(savedResult.countedInAssignment()).isNull();
        verify(subordinateSnapshotMaterializer).materialize(savedResult, completedAttempt, snapshotFacts);
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
    }

    @Test
    void passedAssignedResultWithinDeadlineWillBeCountedOnlyAfterSuccessfulResultMaterialization() {
        TestAttempt completedAttempt = terminalAssignedAttempt(9022L, TestAttemptStatus.COMPLETED);
        ResultSnapshotFacts snapshotFacts = assignedSnapshotFacts();
        Result assembled = new ResultSnapshotAssembler().assemble(completedAttempt, snapshotFacts);
        Result savedResult = futureCountedDecisionResult(422L, assembled);

        when(resultRepository.findResultByTestAttemptId(9022L)).thenReturn(null);
        when(testAttemptRepository.findTestAttemptById(9022L)).thenReturn(completedAttempt);
        when(snapshotFactsProvider.provideSnapshotFacts(completedAttempt)).thenReturn(snapshotFacts);
        when(resultRepository.saveResult(any(Result.class))).thenReturn(savedResult);
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(906L);
        when(criticalCommandAuditSupport.buildAuditContext(any(), any(String.class), any()))
            .thenReturn(new AuditContext("{\"operationCode\":\"RESULT_RECORDING\"}"));

        Long resultId = service.recordResult(9022L);

        assertThat(resultId).isEqualTo(422L);
        assertThat(savedResult.attemptMode()).isEqualTo(AttemptMode.ASSIGNED);
        assertThat(savedResult.scoringSnapshot().passed()).isTrue();
        assertThat(savedResult.withinDeadline()).isTrue();
        assertThat(savedResult.countedInAssignment()).isTrue();
        verify(subordinateSnapshotMaterializer).materialize(savedResult, completedAttempt, snapshotFacts);
        verify(assignmentCountedResultHandoffService).acceptValidCountedAssignmentResult(422L);
    }

    @Test
    void countedHandoffMustNotBeDerivedFromTerminalAttemptStatus() {
        TestAttempt expiredAttempt = terminalAssignedAttempt(9023L, TestAttemptStatus.EXPIRED);

        when(resultRepository.findResultByTestAttemptId(9023L)).thenReturn(null);
        when(testAttemptRepository.findTestAttemptById(9023L)).thenReturn(expiredAttempt);
        assertThatThrownBy(() -> service.recordResult(9023L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("EXPIRED");

        verify(resultRepository).findResultByTestAttemptId(9023L);
        verify(testAttemptRepository).findTestAttemptById(9023L);
        verify(snapshotFactsProvider, never()).provideSnapshotFacts(any());
        verify(resultRepository, never()).saveResult(any(Result.class));
        verify(subordinateSnapshotMaterializer, never()).materialize(any(), any(), any());
        verify(criticalCommandAuditSupport, never()).resolveInteractiveActorUserId();
        verify(criticalCommandAuditSupport, never()).resolveSystemActorUserId(any());
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(String.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
    }

    @Test
    void countedDecisionMatrixWillRequireAssignedPassedWithinDeadlineResult() {
        Result selfCandidate = futureCountedDecisionResult(
            424L,
            new ResultSnapshotAssembler().assemble(terminalSelfAttempt(9024L, TestAttemptStatus.COMPLETED), futureCanonicalSelfSnapshotFacts())
        );
        Result failedAssignedCandidate = futureCountedDecisionResult(
            425L,
            new ResultSnapshotAssembler().assemble(terminalAssignedAttempt(9025L, TestAttemptStatus.COMPLETED), failedAssignedNotCountedSnapshotFacts())
        );
        Result overdueAssignedCandidate = futureCountedDecisionResult(
            426L,
            new ResultSnapshotAssembler().assemble(terminalAssignedAttempt(9026L, TestAttemptStatus.EXPIRED), overdueAssignedNotCountedSnapshotFacts())
        );
        Result validAssignedCandidate = futureCountedDecisionResult(
            427L,
            new ResultSnapshotAssembler().assemble(terminalAssignedAttempt(9027L, TestAttemptStatus.COMPLETED), assignedSnapshotFacts())
        );

        assertThat(selfCandidate.attemptMode()).isEqualTo(AttemptMode.SELF);
        assertThat(selfCandidate.withinDeadline()).isNull();
        assertThat(selfCandidate.countedInAssignment()).isNull();
        assertThat(failedAssignedCandidate.scoringSnapshot().passed()).isFalse();
        assertThat(failedAssignedCandidate.withinDeadline()).isTrue();
        assertThat(failedAssignedCandidate.countedInAssignment()).isFalse();
        assertThat(overdueAssignedCandidate.scoringSnapshot().passed()).isTrue();
        assertThat(overdueAssignedCandidate.withinDeadline()).isFalse();
        assertThat(overdueAssignedCandidate.countedInAssignment()).isFalse();
        assertThat(validAssignedCandidate.attemptMode()).isEqualTo(AttemptMode.ASSIGNED);
        assertThat(validAssignedCandidate.scoringSnapshot().passed()).isTrue();
        assertThat(validAssignedCandidate.withinDeadline()).isTrue();
        assertThat(validAssignedCandidate.countedInAssignment()).isTrue();
    }

    @Test
    void selfResultRecordingMaterializesRootSnapshotFieldsFromRealProviderFacts() {
        TestAttempt completedAttempt = terminalSelfAttempt(9018L, TestAttemptStatus.COMPLETED);
        ResultRecordingSnapshotFactsProvider realProvider = new ResultRecordingSnapshotFactsProvider(
            assignmentRepository,
            assignmentTestRepository,
            recipientSnapshotRepository,
            testRepository,
            testQuestionRepository,
            questionRepository,
            answerOptionRepository,
            userAnswerRepository,
            userAnswerItemRepository,
            selfCompletionOrgSnapshotFactsReader,
            utcClock,
            new ResultDeadlineClassifier(),
            new AssignmentCountedResultPolicy(),
            new ResultQuestionScoringCalculator()
        );
        ResultRecordingServiceImpl realService = new ResultRecordingServiceImpl(
            resultRepository,
            testAttemptRepository,
            realProvider,
            subordinateSnapshotMaterializer,
            countedAssignmentResultValidityGate,
            assignmentCountedResultHandoffService,
            criticalCommandAuditSupport,
            systemActorResolver,
            new ResultRecordingIdempotentReplayValidator(),
            childSnapshotCompletenessValidator
        );
        com.vladislav.training.platform.content.domain.Test test = test(501L, "70.00", "DEFAULT_PARTIAL_CREDIT_V1");
        TestQuestion testQuestion = testQuestion(5008L, 501L, 608L, "10.0000");
        Question question = question(608L, QuestionType.SINGLE_CHOICE);
        UserAnswer userAnswer = new UserAnswer(8008L, 9018L, 608L, RECORDED_AT.minusSeconds(30), RECORDED_AT.minusSeconds(20));
        AuditContext auditContext = new AuditContext("{\"operationCode\":\"RESULT_RECORDING\"}");

        when(resultRepository.findResultByTestAttemptId(9018L)).thenReturn(null);
        when(testAttemptRepository.findTestAttemptById(9018L)).thenReturn(completedAttempt);
        when(utcClock.now()).thenReturn(RECORDED_AT);
        when(testRepository.findTestById(501L)).thenReturn(test);
        when(testQuestionRepository.findTestQuestionsByTestId(501L)).thenReturn(List.of(testQuestion));
        when(questionRepository.findQuestionsByIds(List.of(608L))).thenReturn(List.of(question));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(608L)).thenReturn(List.of(
            choiceOption(7011L, 608L, true),
            choiceOption(7012L, 608L, false)
        ));
        when(userAnswerRepository.findUserAnswersByTestAttemptId(9018L)).thenReturn(List.of(userAnswer));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(8008L)).thenReturn(List.of(
            new UserAnswerItem(8108L, 8008L, 7011L, null, null, null, RECORDED_AT.minusSeconds(10), RECORDED_AT.minusSeconds(10))
        ));
        when(selfCompletionOrgSnapshotFactsReader.readSelfCompletionOrgSnapshotFacts(9018L)).thenReturn(
            new SelfCompletionOrgSnapshotFactsReader.SelfCompletionOrgSnapshotFacts(904L, "/company/self")
        );
        when(resultRepository.saveResult(any(Result.class))).thenAnswer(invocation -> {
            Result assembled = invocation.getArgument(0);
            return new Result(
                418L,
                assembled.testAttemptId(),
                assembled.userIdSnapshot(),
                assembled.attemptMode(),
                assembled.assignmentId(),
                assembled.assignmentTestId(),
                assembled.testIdSnapshot(),
                assembled.testNameSnapshot(),
                assembled.scoringSnapshot(),
                assembled.withinDeadline(),
                assembled.countedInAssignment(),
                assembled.completedAt(),
                assembled.orgContextSnapshot(),
                assembled.snapshotFinalTopicControlFlag(),
                assembled.createdAt()
            );
        });
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(781L);
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Result"),
            org.mockito.ArgumentMatchers.eq("RESULT_RECORDING"),
            any()
        )).thenReturn(auditContext);

        Long resultId = realService.recordResult(9018L);

        assertThat(resultId).isEqualTo(418L);
        ArgumentCaptor<Result> resultCaptor = ArgumentCaptor.forClass(Result.class);
        verify(resultRepository).saveResult(resultCaptor.capture());
        Result materializedResult = resultCaptor.getValue();
        assertThat(materializedResult.attemptMode()).isEqualTo(AttemptMode.SELF);
        assertThat(materializedResult.assignmentId()).isNull();
        assertThat(materializedResult.assignmentTestId()).isNull();
        assertThat(materializedResult.withinDeadline()).isNull();
        assertThat(materializedResult.countedInAssignment()).isNull();
        assertThat(materializedResult.scoringSnapshot().earnedScore()).isEqualByComparingTo("10.0000");
        assertThat(materializedResult.scoringSnapshot().maxScore()).isEqualByComparingTo("10.0000");
        assertThat(materializedResult.scoringSnapshot().scorePercent()).isEqualByComparingTo("100.0000");
        assertThat(materializedResult.scoringSnapshot().passed()).isTrue();
        assertThat(materializedResult.orgContextSnapshot().organizationalUnitIdSnapshot()).isEqualTo(904L);
        assertThat(materializedResult.orgContextSnapshot().organizationalPathSnapshot()).isEqualTo("/company/self");
        assertThat(materializedResult.snapshotFinalTopicControlFlag()).isFalse();
        assertThat(materializedResult.createdAt()).isEqualTo(RECORDED_AT);
        verify(subordinateSnapshotMaterializer).materialize(any(Result.class), org.mockito.ArgumentMatchers.eq(completedAttempt), any(ResultSnapshotFacts.class));
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
    }

    @Test
    void recordResultPersistsRootEarnedScoreEqualToSumOfQuestionSnapshotEarnedScores() {
        TestAttempt completedAttempt = terminalSelfAttempt(9030L, TestAttemptStatus.COMPLETED);
        ResultRecordingSnapshotFactsProvider realProvider = new ResultRecordingSnapshotFactsProvider(
            assignmentRepository,
            assignmentTestRepository,
            recipientSnapshotRepository,
            testRepository,
            testQuestionRepository,
            questionRepository,
            answerOptionRepository,
            userAnswerRepository,
            userAnswerItemRepository,
            selfCompletionOrgSnapshotFactsReader,
            utcClock,
            new ResultDeadlineClassifier(),
            new AssignmentCountedResultPolicy(),
            new ResultQuestionScoringCalculator()
        );
        ResultRecordingSubordinateSnapshotMaterializer realMaterializer = new ResultRecordingSubordinateSnapshotMaterializer(
            resultQuestionSnapshotRepository,
            resultAnswerOptionSnapshotRepository,
            testQuestionRepository,
            questionRepository,
            answerOptionRepository,
            userAnswerRepository,
            userAnswerItemRepository,
            new ResultQuestionScoringCalculator()
        );
        ResultRecordingServiceImpl realService = new ResultRecordingServiceImpl(
            resultRepository,
            testAttemptRepository,
            realProvider,
            realMaterializer,
            countedAssignmentResultValidityGate,
            assignmentCountedResultHandoffService,
            criticalCommandAuditSupport,
            systemActorResolver,
            new ResultRecordingIdempotentReplayValidator(),
            childSnapshotCompletenessValidator
        );

        TestQuestion fullQuestion = testQuestion(5030L, 501L, 6030L, "5.0000");
        TestQuestion partialQuestion = testQuestion(5031L, 501L, 6031L, "6.0000");
        TestQuestion zeroQuestion = testQuestion(5032L, 501L, 6032L, "4.0000");
        Question singleChoiceFull = question(6030L, QuestionType.SINGLE_CHOICE);
        Question multipleChoicePartial = question(6031L, QuestionType.MULTIPLE_CHOICE);
        Question singleChoiceZero = question(6032L, QuestionType.SINGLE_CHOICE);
        UserAnswer fullAnswer = new UserAnswer(8030L, 9030L, 6030L, RECORDED_AT.minusSeconds(50), RECORDED_AT.minusSeconds(40));
        UserAnswer partialAnswer = new UserAnswer(8031L, 9030L, 6031L, RECORDED_AT.minusSeconds(35), RECORDED_AT.minusSeconds(25));
        UserAnswer zeroAnswer = new UserAnswer(8032L, 9030L, 6032L, RECORDED_AT.minusSeconds(20), RECORDED_AT.minusSeconds(10));
        AuditContext auditContext = new AuditContext("{\"operationCode\":\"RESULT_RECORDING\"}");

        AtomicReference<Result> storedResult = new AtomicReference<>();
        List<ResultQuestionSnapshot> storedQuestionSnapshots = new ArrayList<>();
        AtomicLong questionSnapshotIdSequence = new AtomicLong(9900L);

        when(resultRepository.findResultByTestAttemptId(9030L)).thenAnswer(invocation -> storedResult.get());
        when(resultRepository.findResultById(430L)).thenAnswer(invocation -> storedResult.get());
        when(resultRepository.saveResult(any(Result.class))).thenAnswer(invocation -> {
            Result assembled = invocation.getArgument(0);
            Result saved = new Result(
                430L,
                assembled.testAttemptId(),
                assembled.userIdSnapshot(),
                assembled.attemptMode(),
                assembled.assignmentId(),
                assembled.assignmentTestId(),
                assembled.testIdSnapshot(),
                assembled.testNameSnapshot(),
                assembled.scoringSnapshot(),
                assembled.withinDeadline(),
                assembled.countedInAssignment(),
                assembled.completedAt(),
                assembled.orgContextSnapshot(),
                assembled.snapshotFinalTopicControlFlag(),
                assembled.createdAt()
            );
            storedResult.set(saved);
            return saved;
        });
        when(resultQuestionSnapshotRepository.saveResultQuestionSnapshot(any(ResultQuestionSnapshot.class))).thenAnswer(invocation -> {
            ResultQuestionSnapshot snapshot = invocation.getArgument(0);
            ResultQuestionSnapshot saved = new ResultQuestionSnapshot(
                questionSnapshotIdSequence.incrementAndGet(),
                snapshot.resultId(),
                snapshot.questionOriginalId(),
                snapshot.body(),
                snapshot.questionType(),
                snapshot.displayOrder(),
                snapshot.weight(),
                snapshot.correctAnswerSnapshot(),
                snapshot.userAnswerSnapshot(),
                snapshot.earnedScore(),
                snapshot.maxScore(),
                snapshot.isCorrect(),
                snapshot.evaluationNote(),
                snapshot.createdAt()
            );
            storedQuestionSnapshots.add(saved);
            return saved;
        });
        when(resultQuestionSnapshotRepository.findResultQuestionSnapshotsByResultId(430L))
            .thenAnswer(invocation -> List.copyOf(storedQuestionSnapshots));
        when(resultAnswerOptionSnapshotRepository.saveResultAnswerOptionSnapshot(any(ResultAnswerOptionSnapshot.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        when(testAttemptRepository.findTestAttemptById(9030L)).thenReturn(completedAttempt);
        when(utcClock.now()).thenReturn(RECORDED_AT);
        when(testRepository.findTestById(501L)).thenReturn(test(501L, "50.00", "DEFAULT_PARTIAL_CREDIT_V1"));
        when(testQuestionRepository.findTestQuestionsByTestId(501L)).thenReturn(List.of(fullQuestion, partialQuestion, zeroQuestion));
        when(questionRepository.findQuestionsByIds(List.of(6030L, 6031L, 6032L)))
            .thenReturn(List.of(singleChoiceFull, multipleChoicePartial, singleChoiceZero));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(6030L)).thenReturn(List.of(
            choiceOption(7030L, 6030L, true),
            choiceOption(7031L, 6030L, false)
        ));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(6031L)).thenReturn(List.of(
            choiceOption(7040L, 6031L, true),
            choiceOption(7041L, 6031L, true),
            choiceOption(7042L, 6031L, true),
            choiceOption(7043L, 6031L, false)
        ));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(6032L)).thenReturn(List.of(
            choiceOption(7050L, 6032L, true),
            choiceOption(7051L, 6032L, false)
        ));
        when(userAnswerRepository.findUserAnswersByTestAttemptId(9030L)).thenReturn(List.of(fullAnswer, partialAnswer, zeroAnswer));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(8030L)).thenReturn(List.of(
            new UserAnswerItem(8130L, 8030L, 7030L, null, null, null, RECORDED_AT.minusSeconds(15), RECORDED_AT.minusSeconds(15))
        ));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(8031L)).thenReturn(List.of(
            new UserAnswerItem(8140L, 8031L, 7040L, null, null, null, RECORDED_AT.minusSeconds(15), RECORDED_AT.minusSeconds(15)),
            new UserAnswerItem(8141L, 8031L, 7041L, null, null, null, RECORDED_AT.minusSeconds(15), RECORDED_AT.minusSeconds(15))
        ));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(8032L)).thenReturn(List.of(
            new UserAnswerItem(8150L, 8032L, 7051L, null, null, null, RECORDED_AT.minusSeconds(15), RECORDED_AT.minusSeconds(15))
        ));
        when(selfCompletionOrgSnapshotFactsReader.readSelfCompletionOrgSnapshotFacts(9030L)).thenReturn(
            new SelfCompletionOrgSnapshotFactsReader.SelfCompletionOrgSnapshotFacts(904L, "/company/self/score-sum")
        );
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(782L);
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Result"),
            org.mockito.ArgumentMatchers.eq("RESULT_RECORDING"),
            any()
        )).thenReturn(auditContext);

        Long resultId = realService.recordResult(9030L);

        Result rereadResult = resultRepository.findResultById(resultId);
        List<ResultQuestionSnapshot> rereadQuestionSnapshots = resultQuestionSnapshotRepository.findResultQuestionSnapshotsByResultId(resultId);
        BigDecimal summedQuestionEarnedScore = rereadQuestionSnapshots.stream()
            .map(ResultQuestionSnapshot::earnedScore)
            .reduce(BigDecimal.ZERO.setScale(4), BigDecimal::add);

        assertThat(resultId).isEqualTo(430L);
        assertThat(rereadQuestionSnapshots).hasSize(3);
        assertThat(rereadQuestionSnapshots)
            .extracting(ResultQuestionSnapshot::earnedScore)
            .containsExactlyInAnyOrder(
                new BigDecimal("5.0000"),
                new BigDecimal("4.0000"),
                new BigDecimal("0.0000")
            );
        assertThat(summedQuestionEarnedScore).isEqualByComparingTo("9.0000");
        assertThat(rereadResult.scoringSnapshot().earnedScore()).isEqualByComparingTo(summedQuestionEarnedScore);
    }

    @Test
    void recordResultPersistsRootMaxScoreEqualToSumOfQuestionSnapshotMaxScores() {
        TestAttempt completedAttempt = terminalSelfAttempt(9031L, TestAttemptStatus.COMPLETED);
        ResultRecordingSnapshotFactsProvider realProvider = new ResultRecordingSnapshotFactsProvider(
            assignmentRepository,
            assignmentTestRepository,
            recipientSnapshotRepository,
            testRepository,
            testQuestionRepository,
            questionRepository,
            answerOptionRepository,
            userAnswerRepository,
            userAnswerItemRepository,
            selfCompletionOrgSnapshotFactsReader,
            utcClock,
            new ResultDeadlineClassifier(),
            new AssignmentCountedResultPolicy(),
            new ResultQuestionScoringCalculator()
        );
        ResultRecordingSubordinateSnapshotMaterializer realMaterializer = new ResultRecordingSubordinateSnapshotMaterializer(
            resultQuestionSnapshotRepository,
            resultAnswerOptionSnapshotRepository,
            testQuestionRepository,
            questionRepository,
            answerOptionRepository,
            userAnswerRepository,
            userAnswerItemRepository,
            new ResultQuestionScoringCalculator()
        );
        ResultRecordingServiceImpl realService = new ResultRecordingServiceImpl(
            resultRepository,
            testAttemptRepository,
            realProvider,
            realMaterializer,
            countedAssignmentResultValidityGate,
            assignmentCountedResultHandoffService,
            criticalCommandAuditSupport,
            systemActorResolver,
            new ResultRecordingIdempotentReplayValidator(),
            childSnapshotCompletenessValidator
        );

        TestQuestion firstQuestion = testQuestion(5040L, 501L, 6040L, "5.0000");
        TestQuestion secondQuestion = testQuestion(5041L, 501L, 6041L, "4.0000");
        TestQuestion thirdQuestion = testQuestion(5042L, 501L, 6042L, "3.0000");
        Question singleChoiceFull = question(6040L, QuestionType.SINGLE_CHOICE);
        Question multipleChoicePartial = question(6041L, QuestionType.MULTIPLE_CHOICE);
        Question singleChoiceZero = question(6042L, QuestionType.SINGLE_CHOICE);
        UserAnswer fullAnswer = new UserAnswer(8040L, 9031L, 6040L, RECORDED_AT.minusSeconds(50), RECORDED_AT.minusSeconds(40));
        UserAnswer partialAnswer = new UserAnswer(8041L, 9031L, 6041L, RECORDED_AT.minusSeconds(35), RECORDED_AT.minusSeconds(25));
        UserAnswer zeroAnswer = new UserAnswer(8042L, 9031L, 6042L, RECORDED_AT.minusSeconds(20), RECORDED_AT.minusSeconds(10));
        AuditContext auditContext = new AuditContext("{\"operationCode\":\"RESULT_RECORDING\"}");

        AtomicReference<Result> storedResult = new AtomicReference<>();
        List<ResultQuestionSnapshot> storedQuestionSnapshots = new ArrayList<>();
        AtomicLong questionSnapshotIdSequence = new AtomicLong(9910L);

        when(resultRepository.findResultByTestAttemptId(9031L)).thenAnswer(invocation -> storedResult.get());
        when(resultRepository.findResultById(431L)).thenAnswer(invocation -> storedResult.get());
        when(resultRepository.saveResult(any(Result.class))).thenAnswer(invocation -> {
            Result assembled = invocation.getArgument(0);
            Result saved = new Result(
                431L,
                assembled.testAttemptId(),
                assembled.userIdSnapshot(),
                assembled.attemptMode(),
                assembled.assignmentId(),
                assembled.assignmentTestId(),
                assembled.testIdSnapshot(),
                assembled.testNameSnapshot(),
                assembled.scoringSnapshot(),
                assembled.withinDeadline(),
                assembled.countedInAssignment(),
                assembled.completedAt(),
                assembled.orgContextSnapshot(),
                assembled.snapshotFinalTopicControlFlag(),
                assembled.createdAt()
            );
            storedResult.set(saved);
            return saved;
        });
        when(resultQuestionSnapshotRepository.saveResultQuestionSnapshot(any(ResultQuestionSnapshot.class))).thenAnswer(invocation -> {
            ResultQuestionSnapshot snapshot = invocation.getArgument(0);
            ResultQuestionSnapshot saved = new ResultQuestionSnapshot(
                questionSnapshotIdSequence.incrementAndGet(),
                snapshot.resultId(),
                snapshot.questionOriginalId(),
                snapshot.body(),
                snapshot.questionType(),
                snapshot.displayOrder(),
                snapshot.weight(),
                snapshot.correctAnswerSnapshot(),
                snapshot.userAnswerSnapshot(),
                snapshot.earnedScore(),
                snapshot.maxScore(),
                snapshot.isCorrect(),
                snapshot.evaluationNote(),
                snapshot.createdAt()
            );
            storedQuestionSnapshots.add(saved);
            return saved;
        });
        when(resultQuestionSnapshotRepository.findResultQuestionSnapshotsByResultId(431L))
            .thenAnswer(invocation -> List.copyOf(storedQuestionSnapshots));
        when(resultAnswerOptionSnapshotRepository.saveResultAnswerOptionSnapshot(any(ResultAnswerOptionSnapshot.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        when(testAttemptRepository.findTestAttemptById(9031L)).thenReturn(completedAttempt);
        when(utcClock.now()).thenReturn(RECORDED_AT);
        when(testRepository.findTestById(501L)).thenReturn(test(501L, "50.00", "DEFAULT_PARTIAL_CREDIT_V1"));
        when(testQuestionRepository.findTestQuestionsByTestId(501L)).thenReturn(List.of(firstQuestion, secondQuestion, thirdQuestion));
        when(questionRepository.findQuestionsByIds(List.of(6040L, 6041L, 6042L)))
            .thenReturn(List.of(singleChoiceFull, multipleChoicePartial, singleChoiceZero));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(6040L)).thenReturn(List.of(
            choiceOption(7060L, 6040L, true),
            choiceOption(7061L, 6040L, false)
        ));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(6041L)).thenReturn(List.of(
            choiceOption(7070L, 6041L, true),
            choiceOption(7071L, 6041L, true),
            choiceOption(7072L, 6041L, true),
            choiceOption(7073L, 6041L, false)
        ));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(6042L)).thenReturn(List.of(
            choiceOption(7080L, 6042L, true),
            choiceOption(7081L, 6042L, false)
        ));
        when(userAnswerRepository.findUserAnswersByTestAttemptId(9031L)).thenReturn(List.of(fullAnswer, partialAnswer, zeroAnswer));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(8040L)).thenReturn(List.of(
            new UserAnswerItem(8160L, 8040L, 7060L, null, null, null, RECORDED_AT.minusSeconds(15), RECORDED_AT.minusSeconds(15))
        ));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(8041L)).thenReturn(List.of(
            new UserAnswerItem(8170L, 8041L, 7070L, null, null, null, RECORDED_AT.minusSeconds(15), RECORDED_AT.minusSeconds(15)),
            new UserAnswerItem(8171L, 8041L, 7071L, null, null, null, RECORDED_AT.minusSeconds(15), RECORDED_AT.minusSeconds(15))
        ));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(8042L)).thenReturn(List.of(
            new UserAnswerItem(8180L, 8042L, 7081L, null, null, null, RECORDED_AT.minusSeconds(15), RECORDED_AT.minusSeconds(15))
        ));
        when(selfCompletionOrgSnapshotFactsReader.readSelfCompletionOrgSnapshotFacts(9031L)).thenReturn(
            new SelfCompletionOrgSnapshotFactsReader.SelfCompletionOrgSnapshotFacts(905L, "/company/self/max-score-sum")
        );
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(783L);
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Result"),
            org.mockito.ArgumentMatchers.eq("RESULT_RECORDING"),
            any()
        )).thenReturn(auditContext);

        Long resultId = realService.recordResult(9031L);

        Result rereadResult = resultRepository.findResultById(resultId);
        List<ResultQuestionSnapshot> rereadQuestionSnapshots = resultQuestionSnapshotRepository.findResultQuestionSnapshotsByResultId(resultId);
        BigDecimal summedQuestionMaxScore = rereadQuestionSnapshots.stream()
            .map(ResultQuestionSnapshot::maxScore)
            .reduce(BigDecimal.ZERO.setScale(4), BigDecimal::add);

        assertThat(resultId).isEqualTo(431L);
        assertThat(rereadQuestionSnapshots).hasSize(3);
        assertThat(rereadQuestionSnapshots)
            .extracting(ResultQuestionSnapshot::maxScore)
            .containsExactlyInAnyOrder(
                new BigDecimal("5.0000"),
                new BigDecimal("4.0000"),
                new BigDecimal("3.0000")
            );
        assertThat(summedQuestionMaxScore).isEqualByComparingTo("12.0000");
        assertThat(rereadResult.scoringSnapshot().maxScore()).isEqualByComparingTo(summedQuestionMaxScore);
    }

    @Test
    void recordResultPersistsScorePercentComputedFromCanonicalEarnedAndMaxScore() {
        TestAttempt completedAttempt = terminalSelfAttempt(9032L, TestAttemptStatus.COMPLETED);
        ResultRecordingSnapshotFactsProvider realProvider = new ResultRecordingSnapshotFactsProvider(
            assignmentRepository,
            assignmentTestRepository,
            recipientSnapshotRepository,
            testRepository,
            testQuestionRepository,
            questionRepository,
            answerOptionRepository,
            userAnswerRepository,
            userAnswerItemRepository,
            selfCompletionOrgSnapshotFactsReader,
            utcClock,
            new ResultDeadlineClassifier(),
            new AssignmentCountedResultPolicy(),
            new ResultQuestionScoringCalculator()
        );
        ResultRecordingSubordinateSnapshotMaterializer realMaterializer = new ResultRecordingSubordinateSnapshotMaterializer(
            resultQuestionSnapshotRepository,
            resultAnswerOptionSnapshotRepository,
            testQuestionRepository,
            questionRepository,
            answerOptionRepository,
            userAnswerRepository,
            userAnswerItemRepository,
            new ResultQuestionScoringCalculator()
        );
        ResultRecordingServiceImpl realService = new ResultRecordingServiceImpl(
            resultRepository,
            testAttemptRepository,
            realProvider,
            realMaterializer,
            countedAssignmentResultValidityGate,
            assignmentCountedResultHandoffService,
            criticalCommandAuditSupport,
            systemActorResolver,
            new ResultRecordingIdempotentReplayValidator(),
            childSnapshotCompletenessValidator
        );

        TestQuestion fullQuestion = testQuestion(5050L, 501L, 6050L, "5.0000");
        TestQuestion partialQuestion = testQuestion(5051L, 501L, 6051L, "6.0000");
        TestQuestion zeroQuestion = testQuestion(5052L, 501L, 6052L, "1.0000");
        Question singleChoiceFull = question(6050L, QuestionType.SINGLE_CHOICE);
        Question multipleChoicePartial = question(6051L, QuestionType.MULTIPLE_CHOICE);
        Question singleChoiceZero = question(6052L, QuestionType.SINGLE_CHOICE);
        UserAnswer fullAnswer = new UserAnswer(8050L, 9032L, 6050L, RECORDED_AT.minusSeconds(50), RECORDED_AT.minusSeconds(40));
        UserAnswer partialAnswer = new UserAnswer(8051L, 9032L, 6051L, RECORDED_AT.minusSeconds(35), RECORDED_AT.minusSeconds(25));
        UserAnswer zeroAnswer = new UserAnswer(8052L, 9032L, 6052L, RECORDED_AT.minusSeconds(20), RECORDED_AT.minusSeconds(10));
        AuditContext auditContext = new AuditContext("{\"operationCode\":\"RESULT_RECORDING\"}");

        AtomicReference<Result> storedResult = new AtomicReference<>();
        List<ResultQuestionSnapshot> storedQuestionSnapshots = new ArrayList<>();
        AtomicLong questionSnapshotIdSequence = new AtomicLong(9920L);

        when(resultRepository.findResultByTestAttemptId(9032L)).thenAnswer(invocation -> storedResult.get());
        when(resultRepository.findResultById(432L)).thenAnswer(invocation -> storedResult.get());
        when(resultRepository.saveResult(any(Result.class))).thenAnswer(invocation -> {
            Result assembled = invocation.getArgument(0);
            Result saved = new Result(
                432L,
                assembled.testAttemptId(),
                assembled.userIdSnapshot(),
                assembled.attemptMode(),
                assembled.assignmentId(),
                assembled.assignmentTestId(),
                assembled.testIdSnapshot(),
                assembled.testNameSnapshot(),
                assembled.scoringSnapshot(),
                assembled.withinDeadline(),
                assembled.countedInAssignment(),
                assembled.completedAt(),
                assembled.orgContextSnapshot(),
                assembled.snapshotFinalTopicControlFlag(),
                assembled.createdAt()
            );
            storedResult.set(saved);
            return saved;
        });
        when(resultQuestionSnapshotRepository.saveResultQuestionSnapshot(any(ResultQuestionSnapshot.class))).thenAnswer(invocation -> {
            ResultQuestionSnapshot snapshot = invocation.getArgument(0);
            ResultQuestionSnapshot saved = new ResultQuestionSnapshot(
                questionSnapshotIdSequence.incrementAndGet(),
                snapshot.resultId(),
                snapshot.questionOriginalId(),
                snapshot.body(),
                snapshot.questionType(),
                snapshot.displayOrder(),
                snapshot.weight(),
                snapshot.correctAnswerSnapshot(),
                snapshot.userAnswerSnapshot(),
                snapshot.earnedScore(),
                snapshot.maxScore(),
                snapshot.isCorrect(),
                snapshot.evaluationNote(),
                snapshot.createdAt()
            );
            storedQuestionSnapshots.add(saved);
            return saved;
        });
        when(resultAnswerOptionSnapshotRepository.saveResultAnswerOptionSnapshot(any(ResultAnswerOptionSnapshot.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        when(testAttemptRepository.findTestAttemptById(9032L)).thenReturn(completedAttempt);
        when(utcClock.now()).thenReturn(RECORDED_AT);
        when(testRepository.findTestById(501L)).thenReturn(test(501L, "90.00", "DEFAULT_PARTIAL_CREDIT_V1"));
        when(testQuestionRepository.findTestQuestionsByTestId(501L)).thenReturn(List.of(fullQuestion, partialQuestion, zeroQuestion));
        when(questionRepository.findQuestionsByIds(List.of(6050L, 6051L, 6052L)))
            .thenReturn(List.of(singleChoiceFull, multipleChoicePartial, singleChoiceZero));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(6050L)).thenReturn(List.of(
            choiceOption(7090L, 6050L, true),
            choiceOption(7091L, 6050L, false)
        ));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(6051L)).thenReturn(List.of(
            choiceOption(7100L, 6051L, true),
            choiceOption(7101L, 6051L, true),
            choiceOption(7102L, 6051L, true),
            choiceOption(7103L, 6051L, false)
        ));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(6052L)).thenReturn(List.of(
            choiceOption(7110L, 6052L, true),
            choiceOption(7111L, 6052L, false)
        ));
        when(userAnswerRepository.findUserAnswersByTestAttemptId(9032L)).thenReturn(List.of(fullAnswer, partialAnswer, zeroAnswer));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(8050L)).thenReturn(List.of(
            new UserAnswerItem(8190L, 8050L, 7090L, null, null, null, RECORDED_AT.minusSeconds(15), RECORDED_AT.minusSeconds(15))
        ));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(8051L)).thenReturn(List.of(
            new UserAnswerItem(8200L, 8051L, 7100L, null, null, null, RECORDED_AT.minusSeconds(15), RECORDED_AT.minusSeconds(15)),
            new UserAnswerItem(8201L, 8051L, 7101L, null, null, null, RECORDED_AT.minusSeconds(15), RECORDED_AT.minusSeconds(15))
        ));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(8052L)).thenReturn(List.of(
            new UserAnswerItem(8210L, 8052L, 7111L, null, null, null, RECORDED_AT.minusSeconds(15), RECORDED_AT.minusSeconds(15))
        ));
        when(selfCompletionOrgSnapshotFactsReader.readSelfCompletionOrgSnapshotFacts(9032L)).thenReturn(
            new SelfCompletionOrgSnapshotFactsReader.SelfCompletionOrgSnapshotFacts(906L, "/company/self/score-percent")
        );
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(784L);
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Result"),
            org.mockito.ArgumentMatchers.eq("RESULT_RECORDING"),
            any()
        )).thenReturn(auditContext);

        Long resultId = realService.recordResult(9032L);

        Result rereadResult = resultRepository.findResultById(resultId);

        assertThat(resultId).isEqualTo(432L);
        assertThat(rereadResult.scoringSnapshot().earnedScore()).isEqualByComparingTo("9.0000");
        assertThat(rereadResult.scoringSnapshot().maxScore()).isEqualByComparingTo("12.0000");
        assertThat(rereadResult.scoringSnapshot().scorePercent()).isEqualByComparingTo("75.0000");
    }

    @Test
    void recordResultPersistsPassedComputedFromScorePercentAndThresholdNotFromCompletedStatus() {
        TestAttempt completedAttempt = terminalSelfAttempt(9033L, TestAttemptStatus.COMPLETED);
        ResultRecordingSnapshotFactsProvider realProvider = new ResultRecordingSnapshotFactsProvider(
            assignmentRepository,
            assignmentTestRepository,
            recipientSnapshotRepository,
            testRepository,
            testQuestionRepository,
            questionRepository,
            answerOptionRepository,
            userAnswerRepository,
            userAnswerItemRepository,
            selfCompletionOrgSnapshotFactsReader,
            utcClock,
            new ResultDeadlineClassifier(),
            new AssignmentCountedResultPolicy(),
            new ResultQuestionScoringCalculator()
        );
        ResultRecordingSubordinateSnapshotMaterializer realMaterializer = new ResultRecordingSubordinateSnapshotMaterializer(
            resultQuestionSnapshotRepository,
            resultAnswerOptionSnapshotRepository,
            testQuestionRepository,
            questionRepository,
            answerOptionRepository,
            userAnswerRepository,
            userAnswerItemRepository,
            new ResultQuestionScoringCalculator()
        );
        ResultRecordingServiceImpl realService = new ResultRecordingServiceImpl(
            resultRepository,
            testAttemptRepository,
            realProvider,
            realMaterializer,
            countedAssignmentResultValidityGate,
            assignmentCountedResultHandoffService,
            criticalCommandAuditSupport,
            systemActorResolver,
            new ResultRecordingIdempotentReplayValidator(),
            childSnapshotCompletenessValidator
        );

        TestQuestion passingQuestion = testQuestion(5060L, 501L, 6060L, "5.0000");
        TestQuestion failingQuestion = testQuestion(5061L, 501L, 6061L, "5.0000");
        Question singleChoiceFull = question(6060L, QuestionType.SINGLE_CHOICE);
        Question singleChoiceZero = question(6061L, QuestionType.SINGLE_CHOICE);
        UserAnswer passingAnswer = new UserAnswer(8060L, 9033L, 6060L, RECORDED_AT.minusSeconds(50), RECORDED_AT.minusSeconds(40));
        UserAnswer failingAnswer = new UserAnswer(8061L, 9033L, 6061L, RECORDED_AT.minusSeconds(20), RECORDED_AT.minusSeconds(10));
        AuditContext auditContext = new AuditContext("{\"operationCode\":\"RESULT_RECORDING\"}");

        AtomicReference<Result> storedResult = new AtomicReference<>();
        AtomicLong questionSnapshotIdSequence = new AtomicLong(9930L);

        when(resultRepository.findResultByTestAttemptId(9033L)).thenAnswer(invocation -> storedResult.get());
        when(resultRepository.findResultById(433L)).thenAnswer(invocation -> storedResult.get());
        when(resultRepository.saveResult(any(Result.class))).thenAnswer(invocation -> {
            Result assembled = invocation.getArgument(0);
            Result saved = new Result(
                433L,
                assembled.testAttemptId(),
                assembled.userIdSnapshot(),
                assembled.attemptMode(),
                assembled.assignmentId(),
                assembled.assignmentTestId(),
                assembled.testIdSnapshot(),
                assembled.testNameSnapshot(),
                assembled.scoringSnapshot(),
                assembled.withinDeadline(),
                assembled.countedInAssignment(),
                assembled.completedAt(),
                assembled.orgContextSnapshot(),
                assembled.snapshotFinalTopicControlFlag(),
                assembled.createdAt()
            );
            storedResult.set(saved);
            return saved;
        });
        when(resultQuestionSnapshotRepository.saveResultQuestionSnapshot(any(ResultQuestionSnapshot.class))).thenAnswer(invocation -> {
            ResultQuestionSnapshot snapshot = invocation.getArgument(0);
            return new ResultQuestionSnapshot(
                questionSnapshotIdSequence.incrementAndGet(),
                snapshot.resultId(),
                snapshot.questionOriginalId(),
                snapshot.body(),
                snapshot.questionType(),
                snapshot.displayOrder(),
                snapshot.weight(),
                snapshot.correctAnswerSnapshot(),
                snapshot.userAnswerSnapshot(),
                snapshot.earnedScore(),
                snapshot.maxScore(),
                snapshot.isCorrect(),
                snapshot.evaluationNote(),
                snapshot.createdAt()
            );
        });
        when(resultAnswerOptionSnapshotRepository.saveResultAnswerOptionSnapshot(any(ResultAnswerOptionSnapshot.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        when(testAttemptRepository.findTestAttemptById(9033L)).thenReturn(completedAttempt);
        when(utcClock.now()).thenReturn(RECORDED_AT);
        when(testRepository.findTestById(501L)).thenReturn(test(501L, "80.00", "DEFAULT_PARTIAL_CREDIT_V1"));
        when(testQuestionRepository.findTestQuestionsByTestId(501L)).thenReturn(List.of(passingQuestion, failingQuestion));
        when(questionRepository.findQuestionsByIds(List.of(6060L, 6061L)))
            .thenReturn(List.of(singleChoiceFull, singleChoiceZero));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(6060L)).thenReturn(List.of(
            choiceOption(7120L, 6060L, true),
            choiceOption(7121L, 6060L, false)
        ));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(6061L)).thenReturn(List.of(
            choiceOption(7130L, 6061L, true),
            choiceOption(7131L, 6061L, false)
        ));
        when(userAnswerRepository.findUserAnswersByTestAttemptId(9033L)).thenReturn(List.of(passingAnswer, failingAnswer));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(8060L)).thenReturn(List.of(
            new UserAnswerItem(8220L, 8060L, 7120L, null, null, null, RECORDED_AT.minusSeconds(15), RECORDED_AT.minusSeconds(15))
        ));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(8061L)).thenReturn(List.of(
            new UserAnswerItem(8230L, 8061L, 7131L, null, null, null, RECORDED_AT.minusSeconds(15), RECORDED_AT.minusSeconds(15))
        ));
        when(selfCompletionOrgSnapshotFactsReader.readSelfCompletionOrgSnapshotFacts(9033L)).thenReturn(
            new SelfCompletionOrgSnapshotFactsReader.SelfCompletionOrgSnapshotFacts(907L, "/company/self/passed-threshold")
        );
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(785L);
        when(criticalCommandAuditSupport.buildAuditContext(
            org.mockito.ArgumentMatchers.eq("Result"),
            org.mockito.ArgumentMatchers.eq("RESULT_RECORDING"),
            any()
        )).thenReturn(auditContext);

        Long resultId = realService.recordResult(9033L);

        Result rereadResult = resultRepository.findResultById(resultId);

        assertThat(resultId).isEqualTo(433L);
        assertThat(completedAttempt.status()).isEqualTo(TestAttemptStatus.COMPLETED);
        assertThat(rereadResult.scoringSnapshot().earnedScore()).isEqualByComparingTo("5.0000");
        assertThat(rereadResult.scoringSnapshot().maxScore()).isEqualByComparingTo("10.0000");
        assertThat(rereadResult.scoringSnapshot().scorePercent()).isEqualByComparingTo("50.0000");
        assertThat(rereadResult.scoringSnapshot().thresholdPercent()).isEqualByComparingTo("80.0000");
        assertThat(rereadResult.scoringSnapshot().passed()).isFalse();
    }

    @Test
    void snapshotMaterializationFailurePreventsCountedHandoff() {
        TestAttempt completedAttempt = terminalAssignedAttempt(9013L, TestAttemptStatus.COMPLETED);
        ResultSnapshotFacts snapshotFacts = assignedSnapshotFacts();
        Result savedResult = countedAssignedResult(413L, 9013L);

        when(resultRepository.findResultByTestAttemptId(9013L)).thenReturn(null);
        when(testAttemptRepository.findTestAttemptById(9013L)).thenReturn(completedAttempt);
        when(snapshotFactsProvider.provideSnapshotFacts(completedAttempt)).thenReturn(snapshotFacts);
        when(resultRepository.saveResult(any(Result.class))).thenReturn(savedResult);
        org.mockito.Mockito.doThrow(new ConflictException("missing subordinate source data"))
            .when(subordinateSnapshotMaterializer)
            .materialize(savedResult, completedAttempt, snapshotFacts);

        assertThatThrownBy(() -> service.recordResult(9013L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("missing subordinate source data");

        verify(resultRepository).saveResult(any(Result.class));
        verify(subordinateSnapshotMaterializer).materialize(savedResult, completedAttempt, snapshotFacts);
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
    }

    @Test
    void subordinateSnapshotFailurePreventsRootOnlySuccessAuditAndHandoff() {
        snapshotMaterializationFailurePreventsCountedHandoff();
    }

    @Test
    void questionSnapshotFailurePreventsRootOnlySuccessAuditAndHandoff() {
        TestAttempt completedAttempt = terminalAssignedAttempt(9014L, TestAttemptStatus.COMPLETED);
        ResultSnapshotFacts snapshotFacts = assignedSnapshotFacts();
        Result savedResult = countedAssignedResult(414L, 9014L);

        when(resultRepository.findResultByTestAttemptId(9014L)).thenReturn(null);
        when(testAttemptRepository.findTestAttemptById(9014L)).thenReturn(completedAttempt);
        when(snapshotFactsProvider.provideSnapshotFacts(completedAttempt)).thenReturn(snapshotFacts);
        when(resultRepository.saveResult(any(Result.class))).thenReturn(savedResult);
        org.mockito.Mockito.doThrow(new PersistenceConstraintViolationException(
            "Failed to persist result_question_snapshot"
        )).when(subordinateSnapshotMaterializer).materialize(savedResult, completedAttempt, snapshotFacts);

        assertThatThrownBy(() -> service.recordResult(9014L))
            .isInstanceOf(PersistenceConstraintViolationException.class)
            .hasMessageContaining("result_question_snapshot");

        verify(resultRepository).findResultByTestAttemptId(9014L);
        verify(testAttemptRepository).findTestAttemptById(9014L);
        verify(snapshotFactsProvider).provideSnapshotFacts(completedAttempt);
        verify(resultRepository).saveResult(any(Result.class));
        verify(subordinateSnapshotMaterializer).materialize(savedResult, completedAttempt, snapshotFacts);
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
    }

    @Test
    void answerOptionSnapshotFailurePreventsPartialResultSuccessAuditAndHandoff() {
        TestAttempt completedAttempt = terminalAssignedAttempt(9016L, TestAttemptStatus.COMPLETED);
        ResultSnapshotFacts snapshotFacts = assignedSnapshotFacts();
        Result savedResult = countedAssignedResult(416L, 9016L);

        when(resultRepository.findResultByTestAttemptId(9016L)).thenReturn(null);
        when(testAttemptRepository.findTestAttemptById(9016L)).thenReturn(completedAttempt);
        when(snapshotFactsProvider.provideSnapshotFacts(completedAttempt)).thenReturn(snapshotFacts);
        when(resultRepository.saveResult(any(Result.class))).thenReturn(savedResult);
        org.mockito.Mockito.doThrow(new PersistenceConstraintViolationException(
            "Failed to persist result_answer_option_snapshot"
        )).when(subordinateSnapshotMaterializer).materialize(savedResult, completedAttempt, snapshotFacts);

        assertThatThrownBy(() -> service.recordResult(9016L))
            .isInstanceOf(PersistenceConstraintViolationException.class)
            .hasMessageContaining("result_answer_option_snapshot");

        verify(resultRepository).findResultByTestAttemptId(9016L);
        verify(testAttemptRepository).findTestAttemptById(9016L);
        verify(snapshotFactsProvider).provideSnapshotFacts(completedAttempt);
        verify(resultRepository).saveResult(any(Result.class));
        verify(subordinateSnapshotMaterializer).materialize(savedResult, completedAttempt, snapshotFacts);
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
    }


    private TestAttempt terminalAssignedAttempt(Long attemptId, TestAttemptStatus status) {
        return new TestAttempt(
            attemptId,
            101L,
            501L,
            701L,
            AttemptMode.ASSIGNED,
            status,
            TERMINAL_AT.minusSeconds(900),
            status == TestAttemptStatus.COMPLETED ? TERMINAL_AT : null,
            status == TestAttemptStatus.EXPIRED ? TERMINAL_AT : null,
            status == TestAttemptStatus.ABANDONED ? TERMINAL_AT : null,
            TERMINAL_AT,
            TERMINAL_AT.minusSeconds(900),
            TERMINAL_AT
        );
    }

    private TestAttempt terminalSelfAttempt(Long attemptId, TestAttemptStatus status) {
        return new TestAttempt(
            attemptId,
            101L,
            501L,
            null,
            AttemptMode.SELF,
            status,
            TERMINAL_AT.minusSeconds(900),
            status == TestAttemptStatus.COMPLETED ? TERMINAL_AT : null,
            status == TestAttemptStatus.EXPIRED ? TERMINAL_AT : null,
            status == TestAttemptStatus.ABANDONED ? TERMINAL_AT : null,
            TERMINAL_AT,
            TERMINAL_AT.minusSeconds(900),
            TERMINAL_AT
        );
    }

    private ResultSnapshotFacts assignedSnapshotFacts() {
        return new ResultSnapshotFacts(
            801L,
            701L,
            501L,
            "Assigned Test",
            new ResultScoringSnapshot(
                new BigDecimal("80.00"),
                new BigDecimal("8.00"),
                new BigDecimal("10.00"),
                new BigDecimal("80.00"),
                true,
                "DEFAULT_POLICY",
                "{\"policy\":\"v1\"}"
            ),
            true,
            true,
            new ResultOrgContextSnapshot(901L, "/company/ops"),
            true,
            RECORDED_AT
        );
    }

    private ResultSnapshotFacts nonCountedAssignedSnapshotFacts() {
        return new ResultSnapshotFacts(
            801L,
            701L,
            501L,
            "Assigned Test",
            new ResultScoringSnapshot(
                new BigDecimal("80.00"),
                new BigDecimal("8.00"),
                new BigDecimal("10.00"),
                new BigDecimal("80.00"),
                true,
                "DEFAULT_POLICY",
                "{\"policy\":\"v1\"}"
            ),
            true,
            false,
            new ResultOrgContextSnapshot(901L, "/company/ops"),
            true,
            RECORDED_AT
        );
    }

    private ResultSnapshotFacts selfSnapshotFacts() {
        return new ResultSnapshotFacts(
            null,
            null,
            501L,
            "Self Test",
            new ResultScoringSnapshot(
                new BigDecimal("70.00"),
                new BigDecimal("7.00"),
                new BigDecimal("10.00"),
                new BigDecimal("70.00"),
                true,
                "SELF_POLICY",
                "{\"policy\":\"self\"}"
            ),
            null,
            null,
            new ResultOrgContextSnapshot(902L, "/company/self"),
            false,
            RECORDED_AT
        );
    }

    private ResultSnapshotFacts overdueAssignedNotCountedSnapshotFacts() {
        return new ResultSnapshotFacts(
            801L,
            701L,
            501L,
            "Assigned Test",
            new ResultScoringSnapshot(
                new BigDecimal("80.00"),
                new BigDecimal("8.00"),
                new BigDecimal("10.00"),
                new BigDecimal("80.00"),
                true,
                "DEFAULT_POLICY",
                "{\"policy\":\"v1\"}"
            ),
            false,
            false,
            new ResultOrgContextSnapshot(901L, "/company/ops"),
            true,
            RECORDED_AT
        );
    }

    private ResultSnapshotFacts failedAssignedNotCountedSnapshotFacts() {
        return new ResultSnapshotFacts(
            801L,
            701L,
            501L,
            "Assigned Test",
            new ResultScoringSnapshot(
                new BigDecimal("60.00"),
                new BigDecimal("6.00"),
                new BigDecimal("10.00"),
                new BigDecimal("60.00"),
                false,
                "DEFAULT_POLICY",
                "{\"policy\":\"v1\"}"
            ),
            true,
            false,
            new ResultOrgContextSnapshot(901L, "/company/ops"),
            true,
            RECORDED_AT
        );
    }

    private ResultSnapshotFacts invalidNonFinalAssignedCountedSnapshotFacts() {
        return new ResultSnapshotFacts(
            801L,
            701L,
            501L,
            "Assigned Test",
            new ResultScoringSnapshot(
                new BigDecimal("80.00"),
                new BigDecimal("8.00"),
                new BigDecimal("10.00"),
                new BigDecimal("80.00"),
                true,
                "DEFAULT_POLICY",
                "{\"policy\":\"v1\"}"
            ),
            true,
            true,
            new ResultOrgContextSnapshot(901L, "/company/ops"),
            false,
            RECORDED_AT
        );
    }

    private ResultSnapshotFacts futureCanonicalSelfSnapshotFacts() {
        return new ResultSnapshotFacts(
            null,
            null,
            501L,
            "Self Test",
            new ResultScoringSnapshot(
                new BigDecimal("70.00"),
                new BigDecimal("7.00"),
                new BigDecimal("10.00"),
                new BigDecimal("70.00"),
                true,
                "SELF_POLICY",
                "{\"policy\":\"self\"}"
            ),
            null,
            null,
            new ResultOrgContextSnapshot(902L, "/company/self"),
            false,
            RECORDED_AT
        );
    }

    private Result futureCountedDecisionResult(Long resultId, Result assembled) {
        return new Result(
            resultId,
            assembled.testAttemptId(),
            assembled.userIdSnapshot(),
            assembled.attemptMode(),
            assembled.assignmentId(),
            assembled.assignmentTestId(),
            assembled.testIdSnapshot(),
            assembled.testNameSnapshot(),
            assembled.scoringSnapshot(),
            assembled.withinDeadline(),
            assembled.countedInAssignment(),
            assembled.completedAt(),
            assembled.orgContextSnapshot(),
            assembled.snapshotFinalTopicControlFlag(),
            assembled.createdAt()
        );
    }

    private ReplayChildAggregateFixture assignedReplayChildAggregateFixture(
        Long resultId,
        Long attemptId,
        Long questionOriginalId,
        Long correctOptionId,
        Long wrongOptionId
    ) {
        Result existing = new Result(
            resultId,
            attemptId,
            3900L + attemptId,
            AttemptMode.ASSIGNED,
            801L,
            701L,
            501L,
            "Assigned Test",
            new ResultScoringSnapshot(
                new BigDecimal("100.00"),
                new BigDecimal("5.0000"),
                new BigDecimal("5.0000"),
                new BigDecimal("100.0000"),
                true,
                "DEFAULT_PARTIAL_CREDIT_V1",
                "{\"policy\":\"v1\"}"
            ),
            true,
            true,
            TERMINAL_AT,
            new ResultOrgContextSnapshot(901L, "/company/ops"),
            true,
            RECORDED_AT
        );
        TestAttempt completedAttempt = terminalAssignedAttempt(attemptId, TestAttemptStatus.COMPLETED);
        ResultSnapshotFacts replayFacts = new ResultSnapshotFacts(
            801L,
            701L,
            501L,
            "Assigned Test",
            existing.scoringSnapshot(),
            true,
            true,
            new ResultOrgContextSnapshot(901L, "/company/ops"),
            true,
            RECORDED_AT,
            new ResultSnapshotFacts.ResultSubordinateSnapshotFacts(List.of(
                new ResultSnapshotFacts.ResultQuestionSnapshotFact(
                    questionOriginalId,
                    "Frozen question body",
                    QuestionType.SINGLE_CHOICE,
                    0,
                    new BigDecimal("5.0000"),
                    List.of(
                        new ResultSnapshotFacts.ResultAnswerOptionSnapshotFact(
                            correctOptionId,
                            "Frozen correct option",
                            AnswerOptionRole.CHOICE_OPTION,
                            true,
                            0,
                            null,
                            null
                        ),
                        new ResultSnapshotFacts.ResultAnswerOptionSnapshotFact(
                            wrongOptionId,
                            "Frozen wrong option",
                            AnswerOptionRole.CHOICE_OPTION,
                            false,
                            1,
                            null,
                            null
                        )
                    ),
                    List.of(
                        new ResultSnapshotFacts.ResultUserAnswerItemSnapshotFact(
                            correctOptionId,
                            null,
                            null,
                            null
                        )
                    )
                )
            ))
        );
        ResultRecordingSubordinateSnapshotMaterializer realMaterializer = new ResultRecordingSubordinateSnapshotMaterializer(
            resultQuestionSnapshotRepository,
            resultAnswerOptionSnapshotRepository,
            testQuestionRepository,
            questionRepository,
            answerOptionRepository,
            userAnswerRepository,
            userAnswerItemRepository,
            new ResultQuestionScoringCalculator()
        );
        ResultRecordingChildSnapshotCompletenessValidator realChildSnapshotValidator =
            new ResultRecordingChildSnapshotCompletenessValidator(
                resultQuestionSnapshotRepository,
                resultAnswerOptionSnapshotRepository,
                realMaterializer
            );
        ResultRecordingSubordinateSnapshotMaterializer.ExpectedSubordinateSnapshotAggregate expectedAggregate =
            realMaterializer.buildExpectedAggregate(existing, replayFacts);
        ResultQuestionSnapshot expectedQuestionSnapshot =
            expectedAggregate.questionSnapshotAggregates().get(0).questionSnapshot();
        List<ResultAnswerOptionSnapshot> expectedAnswerOptionSnapshots =
            expectedAggregate.questionSnapshotAggregates().get(0).answerOptionSnapshots();
        ResultRecordingServiceImpl realService = new ResultRecordingServiceImpl(
            resultRepository,
            testAttemptRepository,
            snapshotFactsProvider,
            subordinateSnapshotMaterializer,
            countedAssignmentResultValidityGate,
            assignmentCountedResultHandoffService,
            criticalCommandAuditSupport,
            systemActorResolver,
            idempotentReplayValidator,
            realChildSnapshotValidator
        );

        return new ReplayChildAggregateFixture(
            existing,
            completedAttempt,
            replayFacts,
            expectedQuestionSnapshot,
            expectedAnswerOptionSnapshots,
            realService
        );
    }

    private ResultQuestionSnapshot persistedQuestionSnapshotCopy(Long persistedId, ResultQuestionSnapshot expectedQuestionSnapshot) {
        return new ResultQuestionSnapshot(
            persistedId,
            expectedQuestionSnapshot.resultId(),
            expectedQuestionSnapshot.questionOriginalId(),
            expectedQuestionSnapshot.body(),
            expectedQuestionSnapshot.questionType(),
            expectedQuestionSnapshot.displayOrder(),
            expectedQuestionSnapshot.weight(),
            expectedQuestionSnapshot.correctAnswerSnapshot(),
            expectedQuestionSnapshot.userAnswerSnapshot(),
            expectedQuestionSnapshot.earnedScore(),
            expectedQuestionSnapshot.maxScore(),
            expectedQuestionSnapshot.isCorrect(),
            expectedQuestionSnapshot.evaluationNote(),
            expectedQuestionSnapshot.createdAt()
        );
    }

    private ResultAnswerOptionSnapshot persistedAnswerOptionSnapshotCopy(
        Long persistedId,
        Long resultQuestionSnapshotId,
        ResultAnswerOptionSnapshot expectedAnswerOptionSnapshot
    ) {
        return new ResultAnswerOptionSnapshot(
            persistedId,
            resultQuestionSnapshotId,
            expectedAnswerOptionSnapshot.answerOptionOriginalId(),
            expectedAnswerOptionSnapshot.body(),
            expectedAnswerOptionSnapshot.displayOrder(),
            expectedAnswerOptionSnapshot.isCorrectAtSnapshot(),
            expectedAnswerOptionSnapshot.isSelectedByUser(),
            expectedAnswerOptionSnapshot.createdAt()
        );
    }

    private void snapshotFactsProviderFailurePreventsResultSaveMaterializationAuditAndHandoff(
        Long attemptId,
        String providerFailureMessage,
        String expectedMessageFragment
    ) {
        TestAttempt completedAttempt = terminalAssignedAttempt(attemptId, TestAttemptStatus.COMPLETED);
        when(resultRepository.findResultByTestAttemptId(attemptId)).thenReturn(null);
        when(testAttemptRepository.findTestAttemptById(attemptId)).thenReturn(completedAttempt);
        when(snapshotFactsProvider.provideSnapshotFacts(completedAttempt))
            .thenThrow(new ConflictException(providerFailureMessage));

        assertThatThrownBy(() -> service.recordResult(attemptId))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining(expectedMessageFragment);

        verify(resultRepository).findResultByTestAttemptId(attemptId);
        verify(testAttemptRepository).findTestAttemptById(attemptId);
        verify(snapshotFactsProvider).provideSnapshotFacts(completedAttempt);
        verify(resultRepository, never()).saveResult(any());
        verify(subordinateSnapshotMaterializer, never()).materialize(any(), any(), any());
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
        verify(criticalCommandAuditSupport, never()).resolveInteractiveActorUserId();
        verify(criticalCommandAuditSupport, never()).resolveSystemActorUserId(any());
        verify(criticalCommandAuditSupport, never()).buildAuditContext(any(), any(String.class), any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    private void assertPersistedDuplicateStateFailsResultRecording(
        Long attemptId,
        String providerFailureMessage,
        String expectedMessageFragment
    ) {
        snapshotFactsProviderFailurePreventsResultSaveMaterializationAuditAndHandoff(
            attemptId,
            providerFailureMessage,
            expectedMessageFragment
        );
    }

    private com.vladislav.training.platform.content.domain.Test test(Long id, String thresholdPercent, String scoringPolicyCode) {
        return new com.vladislav.training.platform.content.domain.Test(
            id,
            201L,
            "Test " + id,
            "Description",
            TestType.CONTROL,
            ContentStatus.PUBLISHED,
            new BigDecimal(thresholdPercent),
            scoringPolicyCode,
            false,
            0,
            RECORDED_AT.minusSeconds(7200),
            RECORDED_AT.minusSeconds(120)
        );
    }

    private TestQuestion testQuestion(Long id, Long testId, Long questionId, String weight) {
        return new TestQuestion(
            id,
            testId,
            questionId,
            0,
            new BigDecimal(weight),
            RECORDED_AT.minusSeconds(7200),
            RECORDED_AT.minusSeconds(120)
        );
    }

    private Question question(Long id, QuestionType type) {
        return new Question(id, 201L, "Question " + id, type, ContentStatus.PUBLISHED, 0, RECORDED_AT, RECORDED_AT);
    }

    private AnswerOption choiceOption(Long id, Long questionId, boolean correct) {
        return new AnswerOption(
            id,
            questionId,
            "Option " + id,
            AnswerOptionRole.CHOICE_OPTION,
            correct,
            0,
            null,
            null,
            RECORDED_AT,
            RECORDED_AT
        );
    }

    private Result countedAssignedResult(Long resultId, Long attemptId) {
        return new Result(
            resultId,
            attemptId,
            4900L + attemptId,
            AttemptMode.ASSIGNED,
            801L,
            701L,
            501L,
            "Assigned Test",
            new ResultScoringSnapshot(
                new BigDecimal("80.00"),
                new BigDecimal("8.00"),
                new BigDecimal("10.00"),
                new BigDecimal("80.00"),
                true,
                "DEFAULT_POLICY",
                "{\"policy\":\"v1\"}"
            ),
            true,
            true,
            TERMINAL_AT,
            new ResultOrgContextSnapshot(901L, "/company/ops"),
            true,
            RECORDED_AT
        );
    }

    private Result nonCountedAssignedResult(Long resultId, Long attemptId) {
        return new Result(
            resultId,
            attemptId,
            5900L + attemptId,
            AttemptMode.ASSIGNED,
            801L,
            701L,
            501L,
            "Assigned Test",
            new ResultScoringSnapshot(
                new BigDecimal("80.00"),
                new BigDecimal("8.00"),
                new BigDecimal("10.00"),
                new BigDecimal("80.00"),
                true,
                "DEFAULT_POLICY",
                "{\"policy\":\"v1\"}"
            ),
            true,
            false,
            TERMINAL_AT,
            new ResultOrgContextSnapshot(901L, "/company/ops"),
            true,
            RECORDED_AT
        );
    }

    private Result selfRecordedResult(Long resultId, Long attemptId) {
        return new Result(
            resultId,
            attemptId,
            6900L + attemptId,
            AttemptMode.SELF,
            null,
            null,
            501L,
            "Self Test",
            new ResultScoringSnapshot(
                new BigDecimal("70.00"),
                new BigDecimal("7.00"),
                new BigDecimal("10.00"),
                new BigDecimal("70.00"),
                true,
                "SELF_POLICY",
                "{\"policy\":\"self\"}"
            ),
            null,
            null,
            TERMINAL_AT,
            new ResultOrgContextSnapshot(902L, "/company/self"),
            false,
            RECORDED_AT
        );
    }

    private record ReplayChildAggregateFixture(
        Result existingResult,
        TestAttempt completedAttempt,
        ResultSnapshotFacts replayFacts,
        ResultQuestionSnapshot expectedQuestionSnapshot,
        List<ResultAnswerOptionSnapshot> expectedAnswerOptionSnapshots,
        ResultRecordingServiceImpl realService
    ) {
    }
}

