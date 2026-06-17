package com.vladislav.training.platform.result.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.content.domain.AnswerOption;
import com.vladislav.training.platform.content.domain.AnswerOptionRole;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Question;
import com.vladislav.training.platform.content.domain.QuestionType;
import com.vladislav.training.platform.content.domain.TestQuestion;
import com.vladislav.training.platform.content.repository.AnswerOptionRepository;
import com.vladislav.training.platform.content.repository.QuestionRepository;
import com.vladislav.training.platform.content.repository.TestQuestionRepository;
import com.vladislav.training.platform.result.domain.Result;
import com.vladislav.training.platform.result.domain.ResultAnswerOptionSnapshot;
import com.vladislav.training.platform.result.domain.ResultOrgContextSnapshot;
import com.vladislav.training.platform.result.domain.ResultQuestionSnapshot;
import com.vladislav.training.platform.result.domain.ResultQuestionType;
import com.vladislav.training.platform.result.domain.ResultScoringSnapshot;
import com.vladislav.training.platform.result.domain.ResultSnapshotFacts;
import com.vladislav.training.platform.result.domain.ResultSnapshotFacts.ResultAnswerOptionSnapshotFact;
import com.vladislav.training.platform.result.domain.ResultSnapshotFacts.ResultQuestionSnapshotFact;
import com.vladislav.training.platform.result.domain.ResultSnapshotFacts.ResultSubordinateSnapshotFacts;
import com.vladislav.training.platform.result.domain.ResultSnapshotFacts.ResultUserAnswerItemSnapshotFact;
import com.vladislav.training.platform.result.repository.ResultAnswerOptionSnapshotRepository;
import com.vladislav.training.platform.result.repository.ResultQuestionSnapshotRepository;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import com.vladislav.training.platform.testing.domain.UserAnswer;
import com.vladislav.training.platform.testing.domain.UserAnswerItem;
import com.vladislav.training.platform.testing.repository.UserAnswerItemRepository;
import com.vladislav.training.platform.testing.repository.UserAnswerRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code ResultRecordingSubordinateSnapshotMaterializer}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class ResultRecordingSubordinateSnapshotMaterializerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-21T08:30:00Z");
    private static final String STEP_6_TARGET_DISABLED_REASON =
        "Step 6 target test: enable after canonical result scoring evaluator is implemented";

    @Mock
    private ResultQuestionSnapshotRepository resultQuestionSnapshotRepository;
    @Mock
    private ResultAnswerOptionSnapshotRepository resultAnswerOptionSnapshotRepository;
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

    private ResultRecordingSubordinateSnapshotMaterializer materializer;

    @BeforeEach
    void setUp() {
        materializer = new ResultRecordingSubordinateSnapshotMaterializer(
            resultQuestionSnapshotRepository,
            resultAnswerOptionSnapshotRepository,
            testQuestionRepository,
            questionRepository,
            answerOptionRepository,
            userAnswerRepository,
            userAnswerItemRepository,
            new ResultQuestionScoringCalculator()
        );
    }

    @Test
    void persistedAnswerFactMaterializationMustNotUseSilentDedupMergeFunctions() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/result/service/ResultRecordingSubordinateSnapshotMaterializer.java"
        ));

        assertThat(source)
            
            .doesNotContain("(left, right) -> left");
        assertThat(source)
            
            .doesNotContain("""
                Collectors.toMap(
                                PairSnapshot::leftAnswerOptionId,
                                PairSnapshot::rightAnswerOptionId,
                                (left, right) -> left,
                """)
            .doesNotContain("""
                Collectors.toMap(
                                OrderSnapshot::answerOptionId,
                                OrderSnapshot::position,
                                (left, right) -> left,
                """);
    }

    @Test
    void happyPathMaterializesQuestionAndAnswerOptionSnapshotsForRecordedResult() {
        Result recordedResult = assignedResult(501L, 1001L);
        TestAttempt completedAttempt = assignedCompletedAttempt(1001L, 301L);
        TestQuestion testQuestion = new TestQuestion(
            401L,
            301L,
            201L,
            0,
            new BigDecimal("2.0000"),
            FIXED_INSTANT.minusSeconds(60),
            FIXED_INSTANT.minusSeconds(60)
        );
        Question question = new Question(
            201L,
            101L,
            "Which option is correct?",
            QuestionType.SINGLE_CHOICE,
            ContentStatus.PUBLISHED,
            0,
            FIXED_INSTANT.minusSeconds(120),
            FIXED_INSTANT.minusSeconds(120)
        );
        AnswerOption correctOption = new AnswerOption(
            301L,
            201L,
            "Correct option",
            AnswerOptionRole.CHOICE_OPTION,
            true,
            0,
            null,
            null,
            FIXED_INSTANT.minusSeconds(120),
            FIXED_INSTANT.minusSeconds(120)
        );
        AnswerOption wrongOption = new AnswerOption(
            302L,
            201L,
            "Wrong option",
            AnswerOptionRole.CHOICE_OPTION,
            false,
            1,
            null,
            null,
            FIXED_INSTANT.minusSeconds(120),
            FIXED_INSTANT.minusSeconds(120)
        );
        UserAnswer userAnswer = new UserAnswer(
            601L,
            1001L,
            201L,
            FIXED_INSTANT.minusSeconds(30),
            FIXED_INSTANT.minusSeconds(30)
        );
        UserAnswerItem selectedItem = new UserAnswerItem(
            701L,
            601L,
            301L,
            null,
            null,
            null,
            FIXED_INSTANT.minusSeconds(20),
            FIXED_INSTANT.minusSeconds(20)
        );

        when(resultQuestionSnapshotRepository.saveResultQuestionSnapshot(any(ResultQuestionSnapshot.class)))
            .thenAnswer(invocation -> {
                ResultQuestionSnapshot snapshot = invocation.getArgument(0);
                return new ResultQuestionSnapshot(
                    801L,
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

        materializer.materialize(
            recordedResult,
            completedAttempt,
            frozenSnapshotFacts(testQuestion, question, List.of(correctOption, wrongOption), List.of(selectedItem))
        );

        ArgumentCaptor<ResultQuestionSnapshot> questionCaptor = ArgumentCaptor.forClass(ResultQuestionSnapshot.class);
        verify(resultQuestionSnapshotRepository).saveResultQuestionSnapshot(questionCaptor.capture());
        ResultQuestionSnapshot savedQuestionSnapshot = questionCaptor.getValue();
        assertThat(savedQuestionSnapshot.resultId()).isEqualTo(501L);
        assertThat(savedQuestionSnapshot.questionOriginalId()).isEqualTo(201L);
        assertThat(savedQuestionSnapshot.body()).isEqualTo("Which option is correct?");
        assertThat(savedQuestionSnapshot.questionType()).isEqualTo(ResultQuestionType.SINGLE_CHOICE);
        assertThat(savedQuestionSnapshot.correctAnswerSnapshot()).contains("\"correctOptionIds\":[301]");
        assertThat(savedQuestionSnapshot.userAnswerSnapshot()).contains("\"selectedOptionIds\":[301]");
        assertThat(savedQuestionSnapshot.earnedScore()).isEqualByComparingTo("2.0000");
        assertThat(savedQuestionSnapshot.maxScore()).isEqualByComparingTo("2.0000");
        assertThat(savedQuestionSnapshot.isCorrect()).isTrue();
        assertThat(savedQuestionSnapshot.createdAt()).isEqualTo(FIXED_INSTANT);

        ArgumentCaptor<ResultAnswerOptionSnapshot> optionCaptor = ArgumentCaptor.forClass(ResultAnswerOptionSnapshot.class);
        verify(resultAnswerOptionSnapshotRepository, org.mockito.Mockito.times(2))
            .saveResultAnswerOptionSnapshot(optionCaptor.capture());
        assertThat(optionCaptor.getAllValues())
            .extracting(ResultAnswerOptionSnapshot::resultQuestionSnapshotId)
            .containsOnly(801L);
        assertThat(optionCaptor.getAllValues())
            .extracting(ResultAnswerOptionSnapshot::answerOptionOriginalId)
            .containsExactly(301L, 302L);
        assertThat(optionCaptor.getAllValues())
            .extracting(ResultAnswerOptionSnapshot::isCorrectAtSnapshot, ResultAnswerOptionSnapshot::isSelectedByUser)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(true, true),
                org.assertj.core.groups.Tuple.tuple(false, false)
            );
    }

    @Test
    void subordinateMaterializerWritesAnswerOptionSnapshotsSynchronously() {
        Result recordedResult = assignedResult(508L, 1008L);
        TestAttempt completedAttempt = assignedCompletedAttempt(1008L, 308L);
        TestQuestion testQuestion = new TestQuestion(
            408L,
            308L,
            208L,
            0,
            new BigDecimal("2.0000"),
            FIXED_INSTANT.minusSeconds(60),
            FIXED_INSTANT.minusSeconds(60)
        );
        Question question = new Question(
            208L,
            101L,
            "Which option is correct?",
            QuestionType.SINGLE_CHOICE,
            ContentStatus.PUBLISHED,
            0,
            FIXED_INSTANT.minusSeconds(120),
            FIXED_INSTANT.minusSeconds(120)
        );
        AnswerOption correctOption = new AnswerOption(
            3081L,
            208L,
            "Correct option",
            AnswerOptionRole.CHOICE_OPTION,
            true,
            0,
            null,
            null,
            FIXED_INSTANT.minusSeconds(120),
            FIXED_INSTANT.minusSeconds(120)
        );
        AnswerOption wrongOption = new AnswerOption(
            3082L,
            208L,
            "Wrong option",
            AnswerOptionRole.CHOICE_OPTION,
            false,
            1,
            null,
            null,
            FIXED_INSTANT.minusSeconds(120),
            FIXED_INSTANT.minusSeconds(120)
        );
        UserAnswer userAnswer = new UserAnswer(
            608L,
            1008L,
            208L,
            FIXED_INSTANT.minusSeconds(30),
            FIXED_INSTANT.minusSeconds(30)
        );
        UserAnswerItem selectedItem = new UserAnswerItem(
            708L,
            608L,
            3081L,
            null,
            null,
            null,
            FIXED_INSTANT.minusSeconds(20),
            FIXED_INSTANT.minusSeconds(20)
        );

        when(resultQuestionSnapshotRepository.saveResultQuestionSnapshot(any(ResultQuestionSnapshot.class)))
            .thenAnswer(invocation -> {
                ResultQuestionSnapshot snapshot = invocation.getArgument(0);
                return new ResultQuestionSnapshot(
                    8808L,
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

        materializer.materialize(
            recordedResult,
            completedAttempt,
            frozenSnapshotFacts(testQuestion, question, List.of(correctOption, wrongOption), List.of(selectedItem))
        );

        InOrder inOrder = inOrder(resultQuestionSnapshotRepository, resultAnswerOptionSnapshotRepository);
        inOrder.verify(resultQuestionSnapshotRepository).saveResultQuestionSnapshot(any(ResultQuestionSnapshot.class));
        inOrder.verify(resultAnswerOptionSnapshotRepository, org.mockito.Mockito.times(2))
            .saveResultAnswerOptionSnapshot(any(ResultAnswerOptionSnapshot.class));
        verify(resultAnswerOptionSnapshotRepository, org.mockito.Mockito.times(2))
            .saveResultAnswerOptionSnapshot(any(ResultAnswerOptionSnapshot.class));
    }

    @Test
    void questionSnapshotWriteFailureStopsAnswerOptionSnapshotWrites() {
        Result recordedResult = assignedResult(509L, 1009L);
        TestAttempt completedAttempt = assignedCompletedAttempt(1009L, 309L);
        TestQuestion testQuestion = new TestQuestion(
            409L,
            309L,
            209L,
            0,
            new BigDecimal("2.0000"),
            FIXED_INSTANT.minusSeconds(60),
            FIXED_INSTANT.minusSeconds(60)
        );
        Question question = new Question(
            209L,
            101L,
            "Which option is correct?",
            QuestionType.SINGLE_CHOICE,
            ContentStatus.PUBLISHED,
            0,
            FIXED_INSTANT.minusSeconds(120),
            FIXED_INSTANT.minusSeconds(120)
        );
        AnswerOption correctOption = new AnswerOption(
            3091L,
            209L,
            "Correct option",
            AnswerOptionRole.CHOICE_OPTION,
            true,
            0,
            null,
            null,
            FIXED_INSTANT.minusSeconds(120),
            FIXED_INSTANT.minusSeconds(120)
        );
        UserAnswer userAnswer = new UserAnswer(
            609L,
            1009L,
            209L,
            FIXED_INSTANT.minusSeconds(30),
            FIXED_INSTANT.minusSeconds(30)
        );
        UserAnswerItem selectedItem = new UserAnswerItem(
            709L,
            609L,
            3091L,
            null,
            null,
            null,
            FIXED_INSTANT.minusSeconds(20),
            FIXED_INSTANT.minusSeconds(20)
        );

        when(resultQuestionSnapshotRepository.saveResultQuestionSnapshot(any(ResultQuestionSnapshot.class)))
            .thenThrow(new PersistenceConstraintViolationException("Failed to persist result_question_snapshot"));

        assertThatThrownBy(() -> materializer.materialize(
            recordedResult,
            completedAttempt,
            frozenSnapshotFacts(testQuestion, question, List.of(correctOption), List.of(selectedItem))
        ))
            .isInstanceOf(PersistenceConstraintViolationException.class)
            .hasMessageContaining("result_question_snapshot");

        verify(resultQuestionSnapshotRepository).saveResultQuestionSnapshot(any(ResultQuestionSnapshot.class));
        verify(resultAnswerOptionSnapshotRepository, never()).saveResultAnswerOptionSnapshot(any());
    }

    @Test
    void answerOptionSnapshotWriteFailurePropagatesWithoutContinuingMaterialization() {
        Result recordedResult = assignedResult(510L, 1010L);
        TestAttempt completedAttempt = assignedCompletedAttempt(1010L, 310L);
        TestQuestion testQuestion = new TestQuestion(
            410L,
            310L,
            210L,
            0,
            new BigDecimal("2.0000"),
            FIXED_INSTANT.minusSeconds(60),
            FIXED_INSTANT.minusSeconds(60)
        );
        Question question = new Question(
            210L,
            101L,
            "Which option is correct?",
            QuestionType.SINGLE_CHOICE,
            ContentStatus.PUBLISHED,
            0,
            FIXED_INSTANT.minusSeconds(120),
            FIXED_INSTANT.minusSeconds(120)
        );
        AnswerOption correctOption = new AnswerOption(
            3101L,
            210L,
            "Correct option",
            AnswerOptionRole.CHOICE_OPTION,
            true,
            0,
            null,
            null,
            FIXED_INSTANT.minusSeconds(120),
            FIXED_INSTANT.minusSeconds(120)
        );
        AnswerOption wrongOption = new AnswerOption(
            3102L,
            210L,
            "Wrong option",
            AnswerOptionRole.CHOICE_OPTION,
            false,
            1,
            null,
            null,
            FIXED_INSTANT.minusSeconds(120),
            FIXED_INSTANT.minusSeconds(120)
        );
        UserAnswer userAnswer = new UserAnswer(
            610L,
            1010L,
            210L,
            FIXED_INSTANT.minusSeconds(30),
            FIXED_INSTANT.minusSeconds(30)
        );
        UserAnswerItem selectedItem = new UserAnswerItem(
            710L,
            610L,
            3101L,
            null,
            null,
            null,
            FIXED_INSTANT.minusSeconds(20),
            FIXED_INSTANT.minusSeconds(20)
        );

        when(resultQuestionSnapshotRepository.saveResultQuestionSnapshot(any(ResultQuestionSnapshot.class)))
            .thenAnswer(invocation -> {
                ResultQuestionSnapshot snapshot = invocation.getArgument(0);
                return new ResultQuestionSnapshot(
                    8810L,
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
            .thenThrow(new PersistenceConstraintViolationException("Failed to persist result_answer_option_snapshot"));

        assertThatThrownBy(() -> materializer.materialize(
            recordedResult,
            completedAttempt,
            frozenSnapshotFacts(testQuestion, question, List.of(correctOption, wrongOption), List.of(selectedItem))
        ))
            .isInstanceOf(PersistenceConstraintViolationException.class)
            .hasMessageContaining("result_answer_option_snapshot");

        verify(resultQuestionSnapshotRepository).saveResultQuestionSnapshot(any(ResultQuestionSnapshot.class));
        verify(resultAnswerOptionSnapshotRepository).saveResultAnswerOptionSnapshot(any(ResultAnswerOptionSnapshot.class));
        verify(resultAnswerOptionSnapshotRepository, org.mockito.Mockito.times(1))
            .saveResultAnswerOptionSnapshot(any(ResultAnswerOptionSnapshot.class));
    }

    @Test
    void missingCanonicalQuestionDataFailsClosedBeforeSnapshotWrites() {
        Result recordedResult = assignedResult(502L, 1002L);
        TestAttempt completedAttempt = assignedCompletedAttempt(1002L, 302L);
        TestQuestion testQuestion = new TestQuestion(
            402L,
            302L,
            202L,
            0,
            new BigDecimal("1.0000"),
            FIXED_INSTANT.minusSeconds(60),
            FIXED_INSTANT.minusSeconds(60)
        );

        assertThatThrownBy(() -> materializer.materialize(
            recordedResult,
            completedAttempt,
            new ResultSnapshotFacts(
                901L,
                902L,
                recordedResult.testIdSnapshot(),
                recordedResult.testNameSnapshot(),
                recordedResult.scoringSnapshot(),
                true,
                true,
                recordedResult.orgContextSnapshot(),
                true,
                recordedResult.createdAt(),
                new ResultSubordinateSnapshotFacts(List.of())
            )
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("frozen subordinate snapshot facts");

        verify(resultQuestionSnapshotRepository, never()).saveResultQuestionSnapshot(any());
        verify(resultAnswerOptionSnapshotRepository, never()).saveResultAnswerOptionSnapshot(any());
    }

    @Test
    void resultSnapshotsDoNotUseLiveContentLookupAfterRootHistoricalContextHasBeenAssembled() {
        Result recordedResult = assignedResult(511L, 1011L);
        TestAttempt completedAttempt = assignedCompletedAttempt(1011L, 311L);
        TestQuestion frozenTestQuestion = new TestQuestion(
            411L,
            311L,
            211L,
            0,
            new BigDecimal("2.0000"),
            FIXED_INSTANT.minusSeconds(60),
            FIXED_INSTANT.minusSeconds(60)
        );
        Question frozenQuestion = new Question(
            211L,
            101L,
            "Frozen question body",
            QuestionType.SINGLE_CHOICE,
            ContentStatus.PUBLISHED,
            0,
            FIXED_INSTANT.minusSeconds(120),
            FIXED_INSTANT.minusSeconds(120)
        );
        AnswerOption frozenCorrectOption = new AnswerOption(
            3111L,
            211L,
            "Frozen correct option",
            AnswerOptionRole.CHOICE_OPTION,
            true,
            0,
            null,
            null,
            FIXED_INSTANT.minusSeconds(120),
            FIXED_INSTANT.minusSeconds(120)
        );
        AnswerOption frozenWrongOption = new AnswerOption(
            3112L,
            211L,
            "Frozen wrong option",
            AnswerOptionRole.CHOICE_OPTION,
            false,
            1,
            null,
            null,
            FIXED_INSTANT.minusSeconds(120),
            FIXED_INSTANT.minusSeconds(120)
        );
        UserAnswerItem frozenSelectedItem = new UserAnswerItem(
            711L,
            611L,
            3111L,
            null,
            null,
            null,
            FIXED_INSTANT.minusSeconds(20),
            FIXED_INSTANT.minusSeconds(20)
        );

        lenient().when(testQuestionRepository.findTestQuestionsByTestId(311L))
            .thenThrow(new AssertionError("live testQuestion lookup must not happen after assembly"));
        lenient().when(questionRepository.findQuestionsByIds(List.of(211L)))
            .thenThrow(new AssertionError("live question lookup must not happen after assembly"));
        lenient().when(answerOptionRepository.findAnswerOptionsByQuestionId(211L))
            .thenThrow(new AssertionError("live answerOption lookup must not happen after assembly"));
        lenient().when(userAnswerRepository.findUserAnswersByTestAttemptId(1011L))
            .thenThrow(new AssertionError("live userAnswer lookup must not happen after assembly"));
        lenient().when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(611L))
            .thenThrow(new AssertionError("live userAnswerItem lookup must not happen after assembly"));
        when(resultQuestionSnapshotRepository.saveResultQuestionSnapshot(any(ResultQuestionSnapshot.class)))
            .thenAnswer(invocation -> {
                ResultQuestionSnapshot snapshot = invocation.getArgument(0);
                return new ResultQuestionSnapshot(
                    8811L,
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

        materializer.materialize(
            recordedResult,
            completedAttempt,
            frozenSnapshotFacts(
                frozenTestQuestion,
                frozenQuestion,
                List.of(frozenCorrectOption, frozenWrongOption),
                List.of(frozenSelectedItem)
            )
        );

        ArgumentCaptor<ResultQuestionSnapshot> questionCaptor = ArgumentCaptor.forClass(ResultQuestionSnapshot.class);
        verify(resultQuestionSnapshotRepository).saveResultQuestionSnapshot(questionCaptor.capture());
        assertThat(questionCaptor.getValue().body()).isEqualTo("Frozen question body");
        assertThat(questionCaptor.getValue().correctAnswerSnapshot()).contains("\"correctOptionIds\":[3111]");
        assertThat(questionCaptor.getValue().userAnswerSnapshot()).contains("\"selectedOptionIds\":[3111]");

        ArgumentCaptor<ResultAnswerOptionSnapshot> optionCaptor = ArgumentCaptor.forClass(ResultAnswerOptionSnapshot.class);
        verify(resultAnswerOptionSnapshotRepository, org.mockito.Mockito.times(2))
            .saveResultAnswerOptionSnapshot(optionCaptor.capture());
        assertThat(optionCaptor.getAllValues())
            .extracting(ResultAnswerOptionSnapshot::body)
            .containsExactly("Frozen correct option", "Frozen wrong option");
        verify(testQuestionRepository, never()).findTestQuestionsByTestId(any());
        verify(questionRepository, never()).findQuestionsByIds(any());
        verify(answerOptionRepository, never()).findAnswerOptionsByQuestionId(any());
        verify(userAnswerRepository, never()).findUserAnswersByTestAttemptId(any());
        verify(userAnswerItemRepository, never()).findUserAnswerItemsByUserAnswerId(any());
    }

    @Test
    void unsupportedScoringPolicyDoesNotMaterializeSnapshotsAndFailsClosed() {
        Result recordedResult = new Result(
            512L,
            1012L,
            1100L,
            AttemptMode.ASSIGNED,
            901L,
            902L,
            312L,
            "Assigned Test",
            new ResultScoringSnapshot(
                new BigDecimal("70.0000"),
                new BigDecimal("2.0000"),
                new BigDecimal("2.0000"),
                new BigDecimal("100.0000"),
                true,
                "UNSUPPORTED_POLICY_FOR_STAGE_6_7",
                "{\"source\":\"content-test\"}"
            ),
            true,
            true,
            FIXED_INSTANT.minusSeconds(10),
            new ResultOrgContextSnapshot(801L, "/company/ops"),
            true,
            FIXED_INSTANT
        );
        TestAttempt completedAttempt = assignedCompletedAttempt(1012L, 312L);
        TestQuestion testQuestion = testQuestion(412L, 312L, 212L, "2.0000", 0);
        Question question = question(212L, "Which option is correct?", QuestionType.SINGLE_CHOICE);
        AnswerOption correctOption = choiceOption(3121L, 212L, true, 0);
        AnswerOption wrongOption = choiceOption(3122L, 212L, false, 1);
        UserAnswerItem selectedItem = new UserAnswerItem(
            712L,
            612L,
            3121L,
            null,
            null,
            null,
            FIXED_INSTANT.minusSeconds(20),
            FIXED_INSTANT.minusSeconds(20)
        );

        assertThatThrownBy(() -> materializer.materialize(
            recordedResult,
            completedAttempt,
            frozenSnapshotFacts(testQuestion, question, List.of(correctOption, wrongOption), List.of(selectedItem))
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("canonical scoring policy support")
            .hasMessageContaining("UNSUPPORTED_POLICY_FOR_STAGE_6_7");

        verify(resultQuestionSnapshotRepository, never()).saveResultQuestionSnapshot(any());
        verify(resultAnswerOptionSnapshotRepository, never()).saveResultAnswerOptionSnapshot(any());
    }

    @Test
    void canonicalEvaluatorWillAssignPartialCreditForMultipleChoiceAnswers() {
        Result recordedResult = assignedResult(503L, 1003L);
        TestAttempt completedAttempt = assignedCompletedAttempt(1003L, 303L);
        TestQuestion testQuestion = testQuestion(403L, 303L, 203L, "6.0000", 0);
        Question question = question(203L, "Select all valid options", QuestionType.MULTIPLE_CHOICE);
        stubQuestionSnapshotSave();
        when(resultAnswerOptionSnapshotRepository.saveResultAnswerOptionSnapshot(any(ResultAnswerOptionSnapshot.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        materializer.materialize(
            recordedResult,
            completedAttempt,
            frozenSnapshotFacts(
                testQuestion,
                question,
                List.of(
                    choiceOption(3031L, 203L, true, 0),
                    choiceOption(3032L, 203L, true, 1),
                    choiceOption(3033L, 203L, false, 2)
                ),
                List.of(
                    new UserAnswerItem(7031L, 603L, 3031L, null, null, null, FIXED_INSTANT.minusSeconds(20), FIXED_INSTANT.minusSeconds(20))
                )
            )
        );

        ResultQuestionSnapshot savedSnapshot = captureSingleSavedQuestionSnapshot();
        assertThat(savedSnapshot.questionType()).isEqualTo(ResultQuestionType.MULTIPLE_CHOICE);
        assertThat(savedSnapshot.earnedScore()).isGreaterThan(BigDecimal.ZERO.setScale(4));
        assertThat(savedSnapshot.earnedScore()).isLessThan(savedSnapshot.maxScore());
        assertThat(savedSnapshot.isCorrect()).isFalse();
    }

    @Test
    void canonicalEvaluatorWillAssignPartialCreditForMatchingAnswers() {
        Result recordedResult = assignedResult(504L, 1004L);
        TestAttempt completedAttempt = assignedCompletedAttempt(1004L, 304L);
        TestQuestion testQuestion = testQuestion(404L, 304L, 204L, "9.0000", 0);
        Question question = question(204L, "Match the pairs", QuestionType.MATCHING);
        stubQuestionSnapshotSave();
        when(resultAnswerOptionSnapshotRepository.saveResultAnswerOptionSnapshot(any(ResultAnswerOptionSnapshot.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        materializer.materialize(
            recordedResult,
            completedAttempt,
            frozenSnapshotFacts(
                testQuestion,
                question,
                List.of(
                    matchingLeftOption(3041L, 204L, "A", 0),
                    matchingLeftOption(3042L, 204L, "B", 1),
                    matchingLeftOption(3043L, 204L, "C", 2),
                    matchingRightOption(3141L, 204L, "A", 3),
                    matchingRightOption(3142L, 204L, "B", 4),
                    matchingRightOption(3143L, 204L, "C", 5)
                ),
                List.of(
                    new UserAnswerItem(7041L, 604L, null, 3041L, 3141L, null, FIXED_INSTANT.minusSeconds(20), FIXED_INSTANT.minusSeconds(20)),
                    new UserAnswerItem(7042L, 604L, null, 3042L, 3143L, null, FIXED_INSTANT.minusSeconds(20), FIXED_INSTANT.minusSeconds(20))
                )
            )
        );

        ResultQuestionSnapshot savedSnapshot = captureSingleSavedQuestionSnapshot();
        assertThat(savedSnapshot.questionType()).isEqualTo(ResultQuestionType.MATCHING);
        assertThat(savedSnapshot.earnedScore()).isGreaterThan(BigDecimal.ZERO.setScale(4));
        assertThat(savedSnapshot.earnedScore()).isLessThan(savedSnapshot.maxScore());
        assertThat(savedSnapshot.isCorrect()).isFalse();
    }

    @Test
    void canonicalEvaluatorWillAssignPartialCreditForOrderingAnswers() {
        Result recordedResult = assignedResult(505L, 1005L);
        TestAttempt completedAttempt = assignedCompletedAttempt(1005L, 305L);
        TestQuestion testQuestion = testQuestion(405L, 305L, 205L, "9.0000", 0);
        Question question = question(205L, "Order the items", QuestionType.ORDERING);
        stubQuestionSnapshotSave();
        when(resultAnswerOptionSnapshotRepository.saveResultAnswerOptionSnapshot(any(ResultAnswerOptionSnapshot.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        materializer.materialize(
            recordedResult,
            completedAttempt,
            frozenSnapshotFacts(
                testQuestion,
                question,
                List.of(
                    orderingOption(3051L, 205L, 1, 0),
                    orderingOption(3052L, 205L, 2, 1),
                    orderingOption(3053L, 205L, 3, 2)
                ),
                List.of(
                    new UserAnswerItem(7051L, 605L, 3051L, null, null, 1, FIXED_INSTANT.minusSeconds(20), FIXED_INSTANT.minusSeconds(20)),
                    new UserAnswerItem(7052L, 605L, 3052L, null, null, 3, FIXED_INSTANT.minusSeconds(20), FIXED_INSTANT.minusSeconds(20)),
                    new UserAnswerItem(7053L, 605L, 3053L, null, null, 2, FIXED_INSTANT.minusSeconds(20), FIXED_INSTANT.minusSeconds(20))
                )
            )
        );

        ResultQuestionSnapshot savedSnapshot = captureSingleSavedQuestionSnapshot();
        assertThat(savedSnapshot.questionType()).isEqualTo(ResultQuestionType.ORDERING);
        assertThat(savedSnapshot.earnedScore()).isGreaterThan(BigDecimal.ZERO.setScale(4));
        assertThat(savedSnapshot.earnedScore()).isLessThan(savedSnapshot.maxScore());
        assertThat(savedSnapshot.isCorrect()).isFalse();
    }

    @Test
    void canonicalEvaluatorWillRejectDuplicatePersistedMatchingPairs() {
        Result recordedResult = assignedResult(506L, 1006L);
        TestAttempt completedAttempt = assignedCompletedAttempt(1006L, 306L);
        TestQuestion testQuestion = testQuestion(406L, 306L, 206L, "9.0000", 0);
        Question question = question(206L, "Match duplicate pairs", QuestionType.MATCHING);
        assertThatThrownBy(() -> materializer.materialize(
            recordedResult,
            completedAttempt,
            frozenSnapshotFacts(
                testQuestion,
                question,
                List.of(
                    matchingLeftOption(3061L, 206L, "A", 0),
                    matchingLeftOption(3062L, 206L, "B", 1),
                    matchingRightOption(3161L, 206L, "A", 2),
                    matchingRightOption(3162L, 206L, "B", 3)
                ),
                List.of(
                    new UserAnswerItem(7061L, 606L, null, 3061L, 3161L, null, FIXED_INSTANT.minusSeconds(20), FIXED_INSTANT.minusSeconds(20)),
                    new UserAnswerItem(7062L, 606L, null, 3061L, 3162L, null, FIXED_INSTANT.minusSeconds(20), FIXED_INSTANT.minusSeconds(20))
                )
            )
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("matching");

        verify(resultQuestionSnapshotRepository, never()).saveResultQuestionSnapshot(any());
        verify(resultAnswerOptionSnapshotRepository, never()).saveResultAnswerOptionSnapshot(any());
    }

    @Test
    void canonicalEvaluatorWillRejectDuplicatePersistedOrderingFacts() {
        Result recordedResult = assignedResult(507L, 1007L);
        TestAttempt completedAttempt = assignedCompletedAttempt(1007L, 307L);
        TestQuestion testQuestion = testQuestion(407L, 307L, 207L, "9.0000", 0);
        Question question = question(207L, "Order duplicate positions", QuestionType.ORDERING);
        assertThatThrownBy(() -> materializer.materialize(
            recordedResult,
            completedAttempt,
            frozenSnapshotFacts(
                testQuestion,
                question,
                List.of(
                    orderingOption(3071L, 207L, 1, 0),
                    orderingOption(3072L, 207L, 2, 1),
                    orderingOption(3073L, 207L, 3, 2)
                ),
                List.of(
                    new UserAnswerItem(7071L, 607L, 3071L, null, null, 1, FIXED_INSTANT.minusSeconds(20), FIXED_INSTANT.minusSeconds(20)),
                    new UserAnswerItem(7072L, 607L, 3072L, null, null, 1, FIXED_INSTANT.minusSeconds(20), FIXED_INSTANT.minusSeconds(20)),
                    new UserAnswerItem(7073L, 607L, 3073L, null, null, 2, FIXED_INSTANT.minusSeconds(20), FIXED_INSTANT.minusSeconds(20))
                )
            )
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("ordering");

        verify(resultQuestionSnapshotRepository, never()).saveResultQuestionSnapshot(any());
        verify(resultAnswerOptionSnapshotRepository, never()).saveResultAnswerOptionSnapshot(any());
    }

    private Result assignedResult(Long resultId, Long attemptId) {
        return new Result(
            resultId,
            attemptId,
            2100L + attemptId,
            AttemptMode.ASSIGNED,
            901L,
            902L,
            301L,
            "Assigned Test",
            new ResultScoringSnapshot(
                new BigDecimal("70.0000"),
                new BigDecimal("2.0000"),
                new BigDecimal("2.0000"),
                new BigDecimal("100.0000"),
                true,
                "DEFAULT_PARTIAL_CREDIT_V1",
                "{\"source\":\"content-test\"}"
            ),
            true,
            true,
            FIXED_INSTANT.minusSeconds(10),
            new ResultOrgContextSnapshot(801L, "/company/ops"),
            true,
            FIXED_INSTANT
        );
    }

    private TestAttempt assignedCompletedAttempt(Long attemptId, Long testId) {
        return new TestAttempt(
            attemptId,
            100L,
            testId,
            902L,
            AttemptMode.ASSIGNED,
            TestAttemptStatus.COMPLETED,
            FIXED_INSTANT.minusSeconds(120),
            FIXED_INSTANT.minusSeconds(10),
            null,
            null,
            FIXED_INSTANT.minusSeconds(10),
            FIXED_INSTANT.minusSeconds(120),
            FIXED_INSTANT.minusSeconds(10)
        );
    }

    private void stubQuestionSnapshotSave() {
        when(resultQuestionSnapshotRepository.saveResultQuestionSnapshot(any(ResultQuestionSnapshot.class)))
            .thenAnswer(invocation -> {
                ResultQuestionSnapshot snapshot = invocation.getArgument(0);
                return new ResultQuestionSnapshot(
                    8801L,
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
    }

    private ResultQuestionSnapshot captureSingleSavedQuestionSnapshot() {
        ArgumentCaptor<ResultQuestionSnapshot> questionCaptor = ArgumentCaptor.forClass(ResultQuestionSnapshot.class);
        verify(resultQuestionSnapshotRepository).saveResultQuestionSnapshot(questionCaptor.capture());
        return questionCaptor.getValue();
    }

    private TestQuestion testQuestion(Long id, Long testId, Long questionId, String weight, int displayOrder) {
        return new TestQuestion(
            id,
            testId,
            questionId,
            displayOrder,
            new BigDecimal(weight),
            FIXED_INSTANT.minusSeconds(60),
            FIXED_INSTANT.minusSeconds(60)
        );
    }

    private Question question(Long id, String body, QuestionType questionType) {
        return new Question(
            id,
            101L,
            body,
            questionType,
            ContentStatus.PUBLISHED,
            0,
            FIXED_INSTANT.minusSeconds(120),
            FIXED_INSTANT.minusSeconds(120)
        );
    }

    private UserAnswer userAnswer(Long id, Long attemptId, Long questionId) {
        return new UserAnswer(
            id,
            attemptId,
            questionId,
            FIXED_INSTANT.minusSeconds(30),
            FIXED_INSTANT.minusSeconds(30)
        );
    }

    private AnswerOption choiceOption(Long id, Long questionId, boolean correct, int displayOrder) {
        return new AnswerOption(
            id,
            questionId,
            "Choice " + id,
            AnswerOptionRole.CHOICE_OPTION,
            correct,
            displayOrder,
            null,
            null,
            FIXED_INSTANT.minusSeconds(120),
            FIXED_INSTANT.minusSeconds(120)
        );
    }

    private AnswerOption matchingLeftOption(Long id, Long questionId, String pairingKey, int displayOrder) {
        return new AnswerOption(
            id,
            questionId,
            "Left " + id,
            AnswerOptionRole.MATCH_LEFT,
            null,
            displayOrder,
            pairingKey,
            null,
            FIXED_INSTANT.minusSeconds(120),
            FIXED_INSTANT.minusSeconds(120)
        );
    }

    private AnswerOption matchingRightOption(Long id, Long questionId, String pairingKey, int displayOrder) {
        return new AnswerOption(
            id,
            questionId,
            "Right " + id,
            AnswerOptionRole.MATCH_RIGHT,
            null,
            displayOrder,
            pairingKey,
            null,
            FIXED_INSTANT.minusSeconds(120),
            FIXED_INSTANT.minusSeconds(120)
        );
    }

    private AnswerOption orderingOption(Long id, Long questionId, int canonicalOrderPosition, int displayOrder) {
        return new AnswerOption(
            id,
            questionId,
            "Order " + id,
            AnswerOptionRole.ORDER_ITEM,
            null,
            displayOrder,
            null,
            canonicalOrderPosition,
            FIXED_INSTANT.minusSeconds(120),
            FIXED_INSTANT.minusSeconds(120)
        );
    }

    private ResultSnapshotFacts frozenSnapshotFacts(
        TestQuestion testQuestion,
        Question question,
        List<AnswerOption> answerOptions,
        List<UserAnswerItem> answerItems
    ) {
        return new ResultSnapshotFacts(
            901L,
            902L,
            question.id(),
            "Frozen Test",
            new ResultScoringSnapshot(
                new BigDecimal("70.0000"),
                new BigDecimal("2.0000"),
                new BigDecimal("2.0000"),
                new BigDecimal("100.0000"),
                true,
                "DEFAULT_PARTIAL_CREDIT_V1",
                "{\"source\":\"content-test\"}"
            ),
            true,
            true,
            new ResultOrgContextSnapshot(801L, "/company/ops"),
            true,
            FIXED_INSTANT,
            new ResultSubordinateSnapshotFacts(List.of(
                new ResultQuestionSnapshotFact(
                    question.id(),
                    question.body(),
                    question.questionType(),
                    testQuestion.displayOrder(),
                    testQuestion.weight().setScale(4),
                    answerOptions.stream()
                        .map(answerOption -> new ResultAnswerOptionSnapshotFact(
                            answerOption.id(),
                            answerOption.body(),
                            answerOption.answerOptionRole(),
                            answerOption.isCorrect(),
                            answerOption.displayOrder(),
                            answerOption.pairingKey(),
                            answerOption.canonicalOrderPosition()
                        ))
                        .toList(),
                    answerItems.stream()
                        .map(answerItem -> new ResultUserAnswerItemSnapshotFact(
                            answerItem.answerOptionId(),
                            answerItem.leftAnswerOptionId(),
                            answerItem.rightAnswerOptionId(),
                            answerItem.userOrderPosition()
                        ))
                        .toList()
                )
            ))
        );
    }
}

