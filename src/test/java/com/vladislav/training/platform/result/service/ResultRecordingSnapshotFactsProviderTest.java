package com.vladislav.training.platform.result.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaignRecipientSnapshot;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import com.vladislav.training.platform.assignment.domain.AssignmentTestRole;
import com.vladislav.training.platform.assignment.repository.AssignmentCampaignRecipientSnapshotRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentTestRepository;
import com.vladislav.training.platform.common.exception.ConflictException;
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
import com.vladislav.training.platform.result.domain.ResultScoringSnapshot;
import com.vladislav.training.platform.result.domain.ResultSnapshotFacts;
import com.vladislav.training.platform.result.repository.ResultAnswerOptionSnapshotRepository;
import com.vladislav.training.platform.result.repository.ResultQuestionSnapshotRepository;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import com.vladislav.training.platform.testing.domain.UserAnswer;
import com.vladislav.training.platform.testing.domain.UserAnswerItem;
import com.vladislav.training.platform.testing.repository.UserAnswerItemRepository;
import com.vladislav.training.platform.testing.repository.UserAnswerRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code ResultRecordingSnapshotFactsProvider}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class ResultRecordingSnapshotFactsProviderTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-21T12:00:00Z");
    private static final String STEP_6_TARGET_DISABLED_REASON =
        "Step 6 target test: enable after canonical result scoring evaluator is implemented";

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
    private ResultDeadlineClassifier resultDeadlineClassifier;
    @Mock
    private AssignmentCountedResultPolicy assignmentCountedResultPolicy;
    @Mock
    private ResultQuestionSnapshotRepository resultQuestionSnapshotRepository;
    @Mock
    private ResultAnswerOptionSnapshotRepository resultAnswerOptionSnapshotRepository;
    @Mock
    private ResultQuestionScoringEvaluator resultQuestionScoringEvaluator;

    private ResultRecordingSnapshotFactsProvider provider;

    @BeforeEach
    void setUp() {
        provider = new ResultRecordingSnapshotFactsProvider(
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
    }

    @Test
    void assignedRootFactsComeFromRealCanonicalSourcesWithoutSyntheticDefaults() {
        TestAttempt attempt = assignedCompletedAttempt(9001L, 501L, 701L);
        AssignmentTest assignmentTest = assignmentTest(701L, 801L, 501L);
        Assignment assignment = assignment(801L, 501L, 301L, null, null);
        com.vladislav.training.platform.content.domain.Test test = test(501L, "80.00", "DEFAULT_PARTIAL_CREDIT_V1");
        TestQuestion testQuestion = testQuestion(5001L, 501L, 601L, "10.0000");
        Question question = question(601L, QuestionType.SINGLE_CHOICE);
        AnswerOption correctOption = choiceOption(7001L, 601L, true);
        AnswerOption incorrectOption = choiceOption(7002L, 601L, false);
        UserAnswer userAnswer = new UserAnswer(8001L, 9001L, 601L, FIXED_INSTANT.minusSeconds(30), FIXED_INSTANT.minusSeconds(20));

        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(assignmentTestRepository.findAssignmentTestById(701L)).thenReturn(assignmentTest);
        when(assignmentRepository.findAssignmentById(801L)).thenReturn(assignment);
        when(testRepository.findTestById(501L)).thenReturn(test);
        when(testQuestionRepository.findTestQuestionsByTestId(501L)).thenReturn(List.of(testQuestion));
        when(questionRepository.findQuestionsByIds(List.of(601L))).thenReturn(List.of(question));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(601L)).thenReturn(List.of(correctOption, incorrectOption));
        when(userAnswerRepository.findUserAnswersByTestAttemptId(9001L)).thenReturn(List.of(userAnswer));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(8001L)).thenReturn(List.of(
            new UserAnswerItem(8101L, 8001L, 7001L, null, null, null, FIXED_INSTANT.minusSeconds(10), FIXED_INSTANT.minusSeconds(10))
        ));
        when(recipientSnapshotRepository.findAssignmentCampaignRecipientSnapshotsByCampaignId(501L)).thenReturn(List.of(
            recipientSnapshot(9101L, 501L, 301L, 901L, "/company/ops")
        ));

        ResultSnapshotFacts facts = provider.provideSnapshotFacts(attempt);

        assertThat(facts.assignmentId()).isEqualTo(801L);
        assertThat(facts.assignmentTestId()).isEqualTo(701L);
        assertThat(facts.testIdSnapshot()).isEqualTo(501L);
        assertThat(facts.testNameSnapshot()).isEqualTo("Test 501");
        assertThat(facts.scoringSnapshot().earnedScore()).isEqualByComparingTo("10.0000");
        assertThat(facts.scoringSnapshot().maxScore()).isEqualByComparingTo("10.0000");
        assertThat(facts.scoringSnapshot().scorePercent()).isEqualByComparingTo("100.0000");
        assertThat(facts.scoringSnapshot().passed()).isTrue();
        assertThat(facts.scoringSnapshot().scoringPolicyCode()).isEqualTo("DEFAULT_PARTIAL_CREDIT_V1");
        assertThat(facts.scoringSnapshot().scoringPolicySnapshot()).contains("\"testId\":501");
        assertThat(facts.withinDeadline()).isTrue();
        assertThat(facts.countedInAssignment()).isTrue();
        assertThat(facts.orgContextSnapshot().organizationalUnitIdSnapshot()).isEqualTo(901L);
        assertThat(facts.orgContextSnapshot().organizationalPathSnapshot()).isEqualTo("/company/ops");
    }

    @Test
    void singleChoiceScoringRemainsAllOrNothingForCorrectWrongExtraAndMissingAnswer() {
        assertSingleChoiceScoringCase(
            1L,
            List.of(new UserAnswerItem(19001L, 18001L, 17001L, null, null, null, FIXED_INSTANT, FIXED_INSTANT)),
            "10.0000",
            true
        );
        assertSingleChoiceScoringCase(
            2L,
            List.of(new UserAnswerItem(19002L, 18002L, 17004L, null, null, null, FIXED_INSTANT, FIXED_INSTANT)),
            "0.0000",
            false
        );
        assertSingleChoiceScoringCase(
            3L,
            List.of(
                new UserAnswerItem(19003L, 18003L, 17005L, null, null, null, FIXED_INSTANT, FIXED_INSTANT),
                new UserAnswerItem(19004L, 18003L, 17006L, null, null, null, FIXED_INSTANT, FIXED_INSTANT)
            ),
            "0.0000",
            false
        );
        assertSingleChoiceScoringCase(4L, null, "0.0000", false);
    }

    @Test
    void multipleChoiceScoringGivesPartialCreditByCanonicalV4Rule() {
        TestAttempt attempt = assignedCompletedAttempt(9120L, 620L, 720L);
        AssignmentTest assignmentTest = assignmentTest(720L, 820L, 620L);
        Assignment assignment = assignment(820L, 920L, 301L, null, null);
        com.vladislav.training.platform.content.domain.Test test = test(620L, "50.00", "DEFAULT_PARTIAL_CREDIT_V1");
        TestQuestion testQuestion = testQuestion(1620L, 620L, 6620L, "12.0000");
        Question question = question(6620L, QuestionType.MULTIPLE_CHOICE);
        UserAnswer userAnswer = new UserAnswer(
            18200L,
            9120L,
            6620L,
            FIXED_INSTANT.minusSeconds(30),
            FIXED_INSTANT.minusSeconds(20)
        );

        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(assignmentTestRepository.findAssignmentTestById(720L)).thenReturn(assignmentTest);
        when(assignmentRepository.findAssignmentById(820L)).thenReturn(assignment);
        when(testRepository.findTestById(620L)).thenReturn(test);
        when(testQuestionRepository.findTestQuestionsByTestId(620L)).thenReturn(List.of(testQuestion));
        when(questionRepository.findQuestionsByIds(List.of(6620L))).thenReturn(List.of(question));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(6620L)).thenReturn(List.of(
            choiceOption(17201L, 6620L, true),
            choiceOption(17202L, 6620L, true),
            choiceOption(17203L, 6620L, true),
            choiceOption(17204L, 6620L, false)
        ));
        when(userAnswerRepository.findUserAnswersByTestAttemptId(9120L)).thenReturn(List.of(userAnswer));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(18200L)).thenReturn(List.of(
            new UserAnswerItem(19201L, 18200L, 17201L, null, null, null, FIXED_INSTANT, FIXED_INSTANT),
            new UserAnswerItem(19202L, 18200L, 17202L, null, null, null, FIXED_INSTANT, FIXED_INSTANT)
        ));
        when(recipientSnapshotRepository.findAssignmentCampaignRecipientSnapshotsByCampaignId(920L))
            .thenReturn(List.of(recipientSnapshot(29200L, 920L, 301L, 39200L, "/company/scoring")));

        ResultSnapshotFacts facts = provider.provideSnapshotFacts(attempt);

        assertThat(facts.scoringSnapshot().earnedScore()).isEqualByComparingTo("8.0000");
        assertThat(facts.scoringSnapshot().earnedScore()).isGreaterThan(BigDecimal.ZERO.setScale(4));
        assertThat(facts.scoringSnapshot().earnedScore()).isLessThan(facts.scoringSnapshot().maxScore());
        assertThat(facts.scoringSnapshot().maxScore()).isEqualByComparingTo("12.0000");
    }

    @Test
    void multipleChoicePenaltyIsClampedAtZeroByCanonicalV4Rule() {
        TestAttempt attempt = assignedCompletedAttempt(9121L, 621L, 721L);
        AssignmentTest assignmentTest = assignmentTest(721L, 821L, 621L);
        Assignment assignment = assignment(821L, 921L, 301L, null, null);
        com.vladislav.training.platform.content.domain.Test test = test(621L, "50.00", "DEFAULT_PARTIAL_CREDIT_V1");
        TestQuestion testQuestion = testQuestion(1621L, 621L, 6621L, "12.0000");
        Question question = question(6621L, QuestionType.MULTIPLE_CHOICE);
        UserAnswer userAnswer = new UserAnswer(
            18201L,
            9121L,
            6621L,
            FIXED_INSTANT.minusSeconds(30),
            FIXED_INSTANT.minusSeconds(20)
        );

        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(assignmentTestRepository.findAssignmentTestById(721L)).thenReturn(assignmentTest);
        when(assignmentRepository.findAssignmentById(821L)).thenReturn(assignment);
        when(testRepository.findTestById(621L)).thenReturn(test);
        when(testQuestionRepository.findTestQuestionsByTestId(621L)).thenReturn(List.of(testQuestion));
        when(questionRepository.findQuestionsByIds(List.of(6621L))).thenReturn(List.of(question));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(6621L)).thenReturn(List.of(
            choiceOption(17211L, 6621L, true),
            choiceOption(17212L, 6621L, true),
            choiceOption(17213L, 6621L, true),
            choiceOption(17214L, 6621L, false),
            choiceOption(17215L, 6621L, false)
        ));
        when(userAnswerRepository.findUserAnswersByTestAttemptId(9121L)).thenReturn(List.of(userAnswer));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(18201L)).thenReturn(List.of(
            new UserAnswerItem(19211L, 18201L, 17211L, null, null, null, FIXED_INSTANT, FIXED_INSTANT),
            new UserAnswerItem(19212L, 18201L, 17214L, null, null, null, FIXED_INSTANT, FIXED_INSTANT),
            new UserAnswerItem(19213L, 18201L, 17215L, null, null, null, FIXED_INSTANT, FIXED_INSTANT)
        ));
        when(recipientSnapshotRepository.findAssignmentCampaignRecipientSnapshotsByCampaignId(921L))
            .thenReturn(List.of(recipientSnapshot(29201L, 921L, 301L, 39201L, "/company/scoring")));

        ResultSnapshotFacts facts = provider.provideSnapshotFacts(attempt);

        assertThat(facts.scoringSnapshot().earnedScore()).isEqualByComparingTo("0.0000");
        assertThat(facts.scoringSnapshot().earnedScore()).isGreaterThanOrEqualTo(BigDecimal.ZERO.setScale(4));
        assertThat(facts.scoringSnapshot().maxScore()).isEqualByComparingTo("12.0000");
    }

    @Test
    void matchingScoringGivesPartialCreditByCorrectPairs() {
        TestAttempt attempt = assignedCompletedAttempt(9122L, 622L, 722L);
        AssignmentTest assignmentTest = assignmentTest(722L, 822L, 622L);
        Assignment assignment = assignment(822L, 922L, 301L, null, null);
        com.vladislav.training.platform.content.domain.Test test = test(622L, "50.00", "DEFAULT_PARTIAL_CREDIT_V1");
        TestQuestion testQuestion = testQuestion(1622L, 622L, 6622L, "12.0000");
        Question question = question(6622L, QuestionType.MATCHING);
        UserAnswer userAnswer = new UserAnswer(
            18202L,
            9122L,
            6622L,
            FIXED_INSTANT.minusSeconds(30),
            FIXED_INSTANT.minusSeconds(20)
        );

        stubAssignedQuestionContext(
            922L,
            attempt,
            assignmentTest,
            assignment,
            test,
            testQuestion,
            question,
            List.of(
                matchingLeftOption(17321L, 6622L, "A", 0),
                matchingLeftOption(17322L, 6622L, "B", 1),
                matchingLeftOption(17323L, 6622L, "C", 2),
                matchingLeftOption(17324L, 6622L, "D", 3),
                matchingRightOption(17421L, 6622L, "A", 4),
                matchingRightOption(17422L, 6622L, "B", 5),
                matchingRightOption(17423L, 6622L, "C", 6),
                matchingRightOption(17424L, 6622L, "D", 7)
            ),
            userAnswer,
            List.of(
                new UserAnswerItem(19221L, 18202L, null, 17321L, 17421L, null, FIXED_INSTANT, FIXED_INSTANT),
                new UserAnswerItem(19222L, 18202L, null, 17322L, 17422L, null, FIXED_INSTANT, FIXED_INSTANT),
                new UserAnswerItem(19223L, 18202L, null, 17323L, 17424L, null, FIXED_INSTANT, FIXED_INSTANT),
                new UserAnswerItem(19224L, 18202L, null, 17324L, 17423L, null, FIXED_INSTANT, FIXED_INSTANT)
            ),
            "/company/scoring/matching"
        );

        ResultSnapshotFacts facts = provider.provideSnapshotFacts(attempt);

        assertThat(facts.scoringSnapshot().earnedScore()).isEqualByComparingTo("6.0000");
        assertThat(facts.scoringSnapshot().earnedScore()).isGreaterThan(BigDecimal.ZERO.setScale(4));
        assertThat(facts.scoringSnapshot().earnedScore()).isLessThan(facts.scoringSnapshot().maxScore());
        assertThat(facts.scoringSnapshot().maxScore()).isEqualByComparingTo("12.0000");
    }

    @Test
    void orderingScoringGivesPartialCreditByCorrectPositions() {
        TestAttempt attempt = assignedCompletedAttempt(9123L, 623L, 723L);
        AssignmentTest assignmentTest = assignmentTest(723L, 823L, 623L);
        Assignment assignment = assignment(823L, 923L, 301L, null, null);
        com.vladislav.training.platform.content.domain.Test test = test(623L, "50.00", "DEFAULT_PARTIAL_CREDIT_V1");
        TestQuestion testQuestion = testQuestion(1623L, 623L, 6623L, "12.0000");
        Question question = question(6623L, QuestionType.ORDERING);
        UserAnswer userAnswer = new UserAnswer(
            18203L,
            9123L,
            6623L,
            FIXED_INSTANT.minusSeconds(30),
            FIXED_INSTANT.minusSeconds(20)
        );

        stubAssignedQuestionContext(
            923L,
            attempt,
            assignmentTest,
            assignment,
            test,
            testQuestion,
            question,
            List.of(
                orderingOption(17521L, 6623L, 1, 0),
                orderingOption(17522L, 6623L, 2, 1),
                orderingOption(17523L, 6623L, 3, 2),
                orderingOption(17524L, 6623L, 4, 3)
            ),
            userAnswer,
            List.of(
                new UserAnswerItem(19231L, 18203L, 17521L, null, null, 1, FIXED_INSTANT, FIXED_INSTANT),
                new UserAnswerItem(19232L, 18203L, 17522L, null, null, 4, FIXED_INSTANT, FIXED_INSTANT),
                new UserAnswerItem(19233L, 18203L, 17523L, null, null, 3, FIXED_INSTANT, FIXED_INSTANT),
                new UserAnswerItem(19234L, 18203L, 17524L, null, null, 2, FIXED_INSTANT, FIXED_INSTANT)
            ),
            "/company/scoring/ordering"
        );

        ResultSnapshotFacts facts = provider.provideSnapshotFacts(attempt);

        assertThat(facts.scoringSnapshot().earnedScore()).isEqualByComparingTo("6.0000");
        assertThat(facts.scoringSnapshot().earnedScore()).isGreaterThan(BigDecimal.ZERO.setScale(4));
        assertThat(facts.scoringSnapshot().earnedScore()).isLessThan(facts.scoringSnapshot().maxScore());
        assertThat(facts.scoringSnapshot().maxScore()).isEqualByComparingTo("12.0000");
    }

    @Test
    void rootAndQuestionSnapshotsUseTheSameCanonicalQuestionScoringEvaluator() {
        TestAttempt selfAttempt = new TestAttempt(
            9901L,
            101L,
            501L,
            null,
            AttemptMode.SELF,
            TestAttemptStatus.COMPLETED,
            FIXED_INSTANT.minusSeconds(180),
            FIXED_INSTANT.minusSeconds(5),
            null,
            null,
            FIXED_INSTANT.minusSeconds(5),
            FIXED_INSTANT.minusSeconds(180),
            FIXED_INSTANT.minusSeconds(5)
        );
        com.vladislav.training.platform.content.domain.Test test = test(501L, "80.00", "DEFAULT_PARTIAL_CREDIT_V1");
        TestQuestion testQuestion = testQuestion(5901L, 501L, 6901L, "10.0000");
        Question question = question(6901L, QuestionType.MULTIPLE_CHOICE);
        UserAnswer userAnswer = new UserAnswer(
            8901L,
            9901L,
            6901L,
            FIXED_INSTANT.minusSeconds(60),
            FIXED_INSTANT.minusSeconds(40)
        );
        AnswerOption firstCorrectOption = choiceOption(7901L, 6901L, true);
        AnswerOption secondCorrectOption = choiceOption(7902L, 6901L, true);
        AnswerOption incorrectOption = choiceOption(7903L, 6901L, false);
        List<AnswerOption> answerOptions = List.of(firstCorrectOption, secondCorrectOption, incorrectOption);
        List<UserAnswerItem> answerItems = List.of(
            new UserAnswerItem(8801L, 8901L, 7901L, null, null, null, FIXED_INSTANT.minusSeconds(10), FIXED_INSTANT.minusSeconds(10))
        );
        Result recordedResult = new Result(
            99001L,
            9901L,
            301L,
            AttemptMode.SELF,
            null,
            null,
            501L,
            "Test 501",
            new ResultScoringSnapshot(
                new BigDecimal("80.0000"),
                new BigDecimal("5.0000"),
                new BigDecimal("10.0000"),
                new BigDecimal("50.0000"),
                false,
                "DEFAULT_PARTIAL_CREDIT_V1",
                "{\"source\":\"test\"}"
            ),
            null,
            null,
            FIXED_INSTANT.minusSeconds(5),
            new ResultOrgContextSnapshot(901L, "/company/self"),
            false,
            FIXED_INSTANT
        );

        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(testRepository.findTestById(501L)).thenReturn(test);
        when(testQuestionRepository.findTestQuestionsByTestId(501L)).thenReturn(List.of(testQuestion));
        when(questionRepository.findQuestionsByIds(List.of(6901L))).thenReturn(List.of(question));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(6901L)).thenReturn(answerOptions);
        when(userAnswerRepository.findUserAnswersByTestAttemptId(9901L)).thenReturn(List.of(userAnswer));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(8901L)).thenReturn(answerItems);
        when(selfCompletionOrgSnapshotFactsReader.readSelfCompletionOrgSnapshotFacts(9901L)).thenReturn(
            new SelfCompletionOrgSnapshotFactsReader.SelfCompletionOrgSnapshotFacts(901L, "/company/self")
        );
        when(resultQuestionScoringEvaluator.evaluateQuestion(
            org.mockito.ArgumentMatchers.any(Question.class),
            org.mockito.ArgumentMatchers.anyList(),
            org.mockito.ArgumentMatchers.anyList(),
            eq(new BigDecimal("10.0000")),
            eq("DEFAULT_PARTIAL_CREDIT_V1")
        )).thenReturn(new ResultQuestionScoringEvaluator.QuestionScore(new BigDecimal("5.0000"), false));
        when(resultQuestionSnapshotRepository.saveResultQuestionSnapshot(org.mockito.ArgumentMatchers.any(ResultQuestionSnapshot.class)))
            .thenAnswer(invocation -> {
                ResultQuestionSnapshot snapshot = invocation.getArgument(0);
                return new ResultQuestionSnapshot(
                    99101L,
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
        when(resultAnswerOptionSnapshotRepository.saveResultAnswerOptionSnapshot(org.mockito.ArgumentMatchers.any(ResultAnswerOptionSnapshot.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ResultRecordingSnapshotFactsProvider evaluatorBackedProvider = new ResultRecordingSnapshotFactsProvider(
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
            resultQuestionScoringEvaluator
        );
        ResultRecordingSubordinateSnapshotMaterializer evaluatorBackedMaterializer =
            new ResultRecordingSubordinateSnapshotMaterializer(
                resultQuestionSnapshotRepository,
                resultAnswerOptionSnapshotRepository,
                testQuestionRepository,
                questionRepository,
                answerOptionRepository,
                userAnswerRepository,
                userAnswerItemRepository,
                resultQuestionScoringEvaluator
            );

        ResultSnapshotFacts facts = evaluatorBackedProvider.provideSnapshotFacts(selfAttempt);
        evaluatorBackedMaterializer.materialize(recordedResult, selfAttempt, facts);

        assertThat(facts.scoringSnapshot().earnedScore()).isEqualByComparingTo("5.0000");
        ArgumentCaptor<ResultQuestionSnapshot> questionSnapshotCaptor = ArgumentCaptor.forClass(ResultQuestionSnapshot.class);
        verify(resultQuestionSnapshotRepository).saveResultQuestionSnapshot(questionSnapshotCaptor.capture());
        assertThat(questionSnapshotCaptor.getValue().earnedScore()).isEqualByComparingTo("5.0000");
        assertThat(questionSnapshotCaptor.getValue().isCorrect()).isFalse();
        verify(resultQuestionScoringEvaluator, times(2)).evaluateQuestion(
            org.mockito.ArgumentMatchers.any(Question.class),
            org.mockito.ArgumentMatchers.anyList(),
            org.mockito.ArgumentMatchers.anyList(),
            eq(new BigDecimal("10.0000")),
            eq("DEFAULT_PARTIAL_CREDIT_V1")
        );
    }

    @Test
    void providerFailsClosedWhenCanonicalScoringFactsAreMissingInsteadOfUsingSyntheticDefaults() {
        TestAttempt attempt = assignedCompletedAttempt(9002L, 502L, 702L);
        AssignmentTest assignmentTest = assignmentTest(702L, 802L, 502L);
        Assignment assignment = assignment(802L, 502L, 301L, null, null);
        com.vladislav.training.platform.content.domain.Test test = test(502L, "80.00", "DEFAULT_PARTIAL_CREDIT_V1");

        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(assignmentTestRepository.findAssignmentTestById(702L)).thenReturn(assignmentTest);
        when(assignmentRepository.findAssignmentById(802L)).thenReturn(assignment);
        when(testRepository.findTestById(502L)).thenReturn(test);
        when(testQuestionRepository.findTestQuestionsByTestId(502L)).thenReturn(List.of());

        assertThatThrownBy(() -> provider.provideSnapshotFacts(attempt))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("non-empty test composition");
    }

    @Test
    void providerFailsClosedWhenAssignedOrgContextFactIsMissing() {
        TestAttempt attempt = assignedCompletedAttempt(9003L, 503L, 703L);
        AssignmentTest assignmentTest = assignmentTest(703L, 803L, 503L);
        Assignment assignment = assignment(803L, 503L, 301L, null, null);
        com.vladislav.training.platform.content.domain.Test test = test(503L, "50.00", "DEFAULT_PARTIAL_CREDIT_V1");
        TestQuestion testQuestion = testQuestion(5003L, 503L, 603L, "10.0000");
        Question question = question(603L, QuestionType.SINGLE_CHOICE);
        UserAnswer userAnswer = new UserAnswer(8003L, 9003L, 603L, FIXED_INSTANT.minusSeconds(30), FIXED_INSTANT.minusSeconds(20));

        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(assignmentTestRepository.findAssignmentTestById(703L)).thenReturn(assignmentTest);
        when(assignmentRepository.findAssignmentById(803L)).thenReturn(assignment);
        when(testRepository.findTestById(503L)).thenReturn(test);
        when(testQuestionRepository.findTestQuestionsByTestId(503L)).thenReturn(List.of(testQuestion));
        when(questionRepository.findQuestionsByIds(List.of(603L))).thenReturn(List.of(question));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(603L)).thenReturn(List.of(
            choiceOption(7003L, 603L, true),
            choiceOption(7004L, 603L, false)
        ));
        when(userAnswerRepository.findUserAnswersByTestAttemptId(9003L)).thenReturn(List.of(userAnswer));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(8003L)).thenReturn(List.of(
            new UserAnswerItem(8103L, 8003L, 7003L, null, null, null, FIXED_INSTANT.minusSeconds(10), FIXED_INSTANT.minusSeconds(10))
        ));
        when(recipientSnapshotRepository.findAssignmentCampaignRecipientSnapshotsByCampaignId(503L)).thenReturn(List.of());

        assertThatThrownBy(() -> provider.provideSnapshotFacts(attempt))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("recipient org snapshot");
    }

    @Test
    void providerRejectsDuplicatePersistedMatchingRightStateInsteadOfSilentDeduplication() {
        TestAttempt attempt = assignedCompletedAttempt(9028L, 528L, 728L);
        AssignmentTest assignmentTest = assignmentTest(728L, 828L, 528L);
        Assignment assignment = assignment(828L, 528L, 301L, null, null);
        com.vladislav.training.platform.content.domain.Test test = test(528L, "70.00", "DEFAULT_PARTIAL_CREDIT_V1");
        TestQuestion testQuestion = testQuestion(5028L, 528L, 628L, "10.0000");
        Question question = question(628L, QuestionType.MATCHING);
        UserAnswer userAnswer = new UserAnswer(8028L, 9028L, 628L, FIXED_INSTANT.minusSeconds(30), FIXED_INSTANT.minusSeconds(20));

        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(assignmentTestRepository.findAssignmentTestById(728L)).thenReturn(assignmentTest);
        when(assignmentRepository.findAssignmentById(828L)).thenReturn(assignment);
        when(testRepository.findTestById(528L)).thenReturn(test);
        when(testQuestionRepository.findTestQuestionsByTestId(528L)).thenReturn(List.of(testQuestion));
        when(questionRepository.findQuestionsByIds(List.of(628L))).thenReturn(List.of(question));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(628L)).thenReturn(List.of(
            matchingLeftOption(7128L, 628L, "A", 0),
            matchingLeftOption(7129L, 628L, "B", 1),
            matchingRightOption(7228L, 628L, "A", 2),
            matchingRightOption(7229L, 628L, "B", 3)
        ));
        when(userAnswerRepository.findUserAnswersByTestAttemptId(9028L)).thenReturn(List.of(userAnswer));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(8028L)).thenReturn(List.of(
            new UserAnswerItem(8128L, 8028L, null, 7128L, 7228L, null, FIXED_INSTANT.minusSeconds(10), FIXED_INSTANT.minusSeconds(10)),
            new UserAnswerItem(8129L, 8028L, null, 7129L, 7228L, null, FIXED_INSTANT.minusSeconds(10), FIXED_INSTANT.minusSeconds(10))
        ));
        lenient().when(recipientSnapshotRepository.findAssignmentCampaignRecipientSnapshotsByCampaignId(528L)).thenReturn(List.of(
            recipientSnapshot(9128L, 528L, 301L, 928L, "/company/ops/dup-right")
        ));

        assertThatThrownBy(() -> provider.provideSnapshotFacts(attempt))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("right");
    }

    @Test
    void providerRejectsDuplicatePersistedChoiceRowsInsteadOfSilentSetDeduplication() {
        TestAttempt attempt = assignedCompletedAttempt(9029L, 529L, 729L);
        AssignmentTest assignmentTest = assignmentTest(729L, 829L, 529L);
        Assignment assignment = assignment(829L, 529L, 301L, null, null);
        com.vladislav.training.platform.content.domain.Test test = test(529L, "70.00", "DEFAULT_PARTIAL_CREDIT_V1");
        TestQuestion testQuestion = testQuestion(5029L, 529L, 629L, "10.0000");
        Question question = question(629L, QuestionType.SINGLE_CHOICE);
        UserAnswer userAnswer = new UserAnswer(8029L, 9029L, 629L, FIXED_INSTANT.minusSeconds(30), FIXED_INSTANT.minusSeconds(20));

        stubAssignedQuestionContext(
            529L,
            attempt,
            assignmentTest,
            assignment,
            test,
            testQuestion,
            question,
            List.of(choiceOption(7130L, 629L, true), choiceOption(7131L, 629L, false)),
            userAnswer,
            List.of(
                new UserAnswerItem(8130L, 8029L, 7130L, null, null, null, FIXED_INSTANT.minusSeconds(10), FIXED_INSTANT.minusSeconds(10)),
                new UserAnswerItem(8131L, 8029L, 7130L, null, null, null, FIXED_INSTANT.minusSeconds(10), FIXED_INSTANT.minusSeconds(10))
            ),
            "/company/ops/dup-choice"
        );

        assertThatThrownBy(() -> provider.provideSnapshotFacts(attempt))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("choice option");
    }

    @Test
    void providerRejectsDuplicatePersistedMatchingLeftStateInsteadOfSilentDeduplication() {
        TestAttempt attempt = assignedCompletedAttempt(9030L, 530L, 730L);
        AssignmentTest assignmentTest = assignmentTest(730L, 830L, 530L);
        Assignment assignment = assignment(830L, 530L, 301L, null, null);
        com.vladislav.training.platform.content.domain.Test test = test(530L, "70.00", "DEFAULT_PARTIAL_CREDIT_V1");
        TestQuestion testQuestion = testQuestion(5030L, 530L, 630L, "10.0000");
        Question question = question(630L, QuestionType.MATCHING);
        UserAnswer userAnswer = new UserAnswer(8030L, 9030L, 630L, FIXED_INSTANT.minusSeconds(30), FIXED_INSTANT.minusSeconds(20));

        stubAssignedQuestionContext(
            530L,
            attempt,
            assignmentTest,
            assignment,
            test,
            testQuestion,
            question,
            List.of(
                matchingLeftOption(7132L, 630L, "A", 0),
                matchingLeftOption(7133L, 630L, "B", 1),
                matchingRightOption(7230L, 630L, "A", 2),
                matchingRightOption(7231L, 630L, "B", 3),
                matchingRightOption(7232L, 630L, "C", 4)
            ),
            userAnswer,
            List.of(
                new UserAnswerItem(8132L, 8030L, null, 7132L, 7230L, null, FIXED_INSTANT.minusSeconds(10), FIXED_INSTANT.minusSeconds(10)),
                new UserAnswerItem(8133L, 8030L, null, 7132L, 7231L, null, FIXED_INSTANT.minusSeconds(10), FIXED_INSTANT.minusSeconds(10))
            ),
            "/company/ops/dup-left"
        );

        assertThatThrownBy(() -> provider.provideSnapshotFacts(attempt))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("matching left");
    }

    @Test
    void providerRejectsDuplicatePersistedMatchingExactPairStateInsteadOfSilentDeduplication() {
        TestAttempt attempt = assignedCompletedAttempt(9031L, 531L, 731L);
        AssignmentTest assignmentTest = assignmentTest(731L, 831L, 531L);
        Assignment assignment = assignment(831L, 531L, 301L, null, null);
        com.vladislav.training.platform.content.domain.Test test = test(531L, "70.00", "DEFAULT_PARTIAL_CREDIT_V1");
        TestQuestion testQuestion = testQuestion(5031L, 531L, 631L, "10.0000");
        Question question = question(631L, QuestionType.MATCHING);
        UserAnswer userAnswer = new UserAnswer(8031L, 9031L, 631L, FIXED_INSTANT.minusSeconds(30), FIXED_INSTANT.minusSeconds(20));

        stubAssignedQuestionContext(
            531L,
            attempt,
            assignmentTest,
            assignment,
            test,
            testQuestion,
            question,
            List.of(
                matchingLeftOption(7134L, 631L, "A", 0),
                matchingRightOption(7233L, 631L, "A", 1)
            ),
            userAnswer,
            List.of(
                new UserAnswerItem(8134L, 8031L, null, 7134L, 7233L, null, FIXED_INSTANT.minusSeconds(10), FIXED_INSTANT.minusSeconds(10)),
                new UserAnswerItem(8135L, 8031L, null, 7134L, 7233L, null, FIXED_INSTANT.minusSeconds(10), FIXED_INSTANT.minusSeconds(10))
            ),
            "/company/ops/dup-pair"
        );

        assertThatThrownBy(() -> provider.provideSnapshotFacts(attempt))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("matching pair");
    }

    @Test
    void providerRejectsDuplicatePersistedOrderingAnswerOptionStateInsteadOfSilentDeduplication() {
        TestAttempt attempt = assignedCompletedAttempt(9032L, 532L, 732L);
        AssignmentTest assignmentTest = assignmentTest(732L, 832L, 532L);
        Assignment assignment = assignment(832L, 532L, 301L, null, null);
        com.vladislav.training.platform.content.domain.Test test = test(532L, "70.00", "DEFAULT_PARTIAL_CREDIT_V1");
        TestQuestion testQuestion = testQuestion(5032L, 532L, 632L, "10.0000");
        Question question = question(632L, QuestionType.ORDERING);
        UserAnswer userAnswer = new UserAnswer(8032L, 9032L, 632L, FIXED_INSTANT.minusSeconds(30), FIXED_INSTANT.minusSeconds(20));

        stubAssignedQuestionContext(
            532L,
            attempt,
            assignmentTest,
            assignment,
            test,
            testQuestion,
            question,
            List.of(
                orderingOption(7234L, 632L, 1, 0),
                orderingOption(7235L, 632L, 2, 1),
                orderingOption(7236L, 632L, 3, 2)
            ),
            userAnswer,
            List.of(
                new UserAnswerItem(8136L, 8032L, 7234L, null, null, 1, FIXED_INSTANT.minusSeconds(10), FIXED_INSTANT.minusSeconds(10)),
                new UserAnswerItem(8137L, 8032L, 7234L, null, null, 2, FIXED_INSTANT.minusSeconds(10), FIXED_INSTANT.minusSeconds(10))
            ),
            "/company/ops/dup-order-option"
        );

        assertThatThrownBy(() -> provider.provideSnapshotFacts(attempt))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("ordering option");
    }

    @Test
    void providerRejectsDuplicatePersistedOrderingUserPositionStateInsteadOfSilentDeduplication() {
        TestAttempt attempt = assignedCompletedAttempt(9033L, 533L, 733L);
        AssignmentTest assignmentTest = assignmentTest(733L, 833L, 533L);
        Assignment assignment = assignment(833L, 533L, 301L, null, null);
        com.vladislav.training.platform.content.domain.Test test = test(533L, "70.00", "DEFAULT_PARTIAL_CREDIT_V1");
        TestQuestion testQuestion = testQuestion(5033L, 533L, 633L, "10.0000");
        Question question = question(633L, QuestionType.ORDERING);
        UserAnswer userAnswer = new UserAnswer(8033L, 9033L, 633L, FIXED_INSTANT.minusSeconds(30), FIXED_INSTANT.minusSeconds(20));

        stubAssignedQuestionContext(
            533L,
            attempt,
            assignmentTest,
            assignment,
            test,
            testQuestion,
            question,
            List.of(
                orderingOption(7237L, 633L, 1, 0),
                orderingOption(7238L, 633L, 2, 1),
                orderingOption(7239L, 633L, 3, 2)
            ),
            userAnswer,
            List.of(
                new UserAnswerItem(8138L, 8033L, 7237L, null, null, 1, FIXED_INSTANT.minusSeconds(10), FIXED_INSTANT.minusSeconds(10)),
                new UserAnswerItem(8139L, 8033L, 7238L, null, null, 1, FIXED_INSTANT.minusSeconds(10), FIXED_INSTANT.minusSeconds(10))
            ),
            "/company/ops/dup-order-position"
        );

        assertThatThrownBy(() -> provider.provideSnapshotFacts(attempt))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("ordering position");
    }

    @Test
    void selfResultDoesNotHaveDeadlineOrCountedAssignmentFacts() {
        TestAttempt selfAttempt = new TestAttempt(
            9004L,
            301L,
            504L,
            null,
            AttemptMode.SELF,
            TestAttemptStatus.COMPLETED,
            FIXED_INSTANT.minusSeconds(600),
            FIXED_INSTANT.minusSeconds(10),
            null,
            null,
            FIXED_INSTANT.minusSeconds(10),
            FIXED_INSTANT.minusSeconds(600),
            FIXED_INSTANT.minusSeconds(10)
        );
        com.vladislav.training.platform.content.domain.Test test = test(504L, "70.00", "DEFAULT_PARTIAL_CREDIT_V1");
        TestQuestion testQuestion = testQuestion(5004L, 504L, 604L, "10.0000");
        Question question = question(604L, QuestionType.SINGLE_CHOICE);
        UserAnswer userAnswer = new UserAnswer(8004L, 9004L, 604L, FIXED_INSTANT.minusSeconds(30), FIXED_INSTANT.minusSeconds(20));

        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(testRepository.findTestById(504L)).thenReturn(test);
        when(testQuestionRepository.findTestQuestionsByTestId(504L)).thenReturn(List.of(testQuestion));
        when(questionRepository.findQuestionsByIds(List.of(604L))).thenReturn(List.of(question));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(604L)).thenReturn(List.of(
            choiceOption(7007L, 604L, true),
            choiceOption(7008L, 604L, false)
        ));
        when(userAnswerRepository.findUserAnswersByTestAttemptId(9004L)).thenReturn(List.of(userAnswer));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(8004L)).thenReturn(List.of(
            new UserAnswerItem(8104L, 8004L, 7007L, null, null, null, FIXED_INSTANT.minusSeconds(10), FIXED_INSTANT.minusSeconds(10))
        ));
        when(selfCompletionOrgSnapshotFactsReader.readSelfCompletionOrgSnapshotFacts(9004L)).thenReturn(
            new SelfCompletionOrgSnapshotFactsReader.SelfCompletionOrgSnapshotFacts(904L, "/company/self")
        );

        ResultSnapshotFacts facts = provider.provideSnapshotFacts(selfAttempt);

        assertThat(facts.assignmentId()).isNull();
        assertThat(facts.assignmentTestId()).isNull();
        assertThat(facts.scoringSnapshot().earnedScore()).isEqualByComparingTo("10.0000");
        assertThat(facts.scoringSnapshot().passed()).isTrue();
        assertThat(facts.withinDeadline()).isNull();
        assertThat(facts.countedInAssignment()).isNull();
        assertThat(facts.snapshotFinalTopicControlFlag()).isFalse();
        assertThat(facts.orgContextSnapshot().organizationalUnitIdSnapshot()).isEqualTo(904L);
        assertThat(facts.orgContextSnapshot().organizationalPathSnapshot()).isEqualTo("/company/self");
        org.mockito.Mockito.verifyNoInteractions(assignmentRepository, assignmentTestRepository);
    }

    @Test
    void selfRootFactsComeFromCanonicalSelfOrgSnapshotSource() {
        selfResultDoesNotHaveDeadlineOrCountedAssignmentFacts();
    }

    @Test
    void assignedFinalControlPassedWithinDeadlineIsCountedInAssignment() {
        TestAttempt attempt = assignedCompletedAttempt(9015L, 515L, 715L);
        AssignmentTest assignmentTest = assignmentTest(715L, 815L, 515L);
        Assignment assignment = assignment(815L, 915L, 301L, FIXED_INSTANT.plusSeconds(30), null);
        com.vladislav.training.platform.content.domain.Test test = test(515L, "80.00", "DEFAULT_PARTIAL_CREDIT_V1");
        TestQuestion testQuestion = testQuestion(5515L, 515L, 6515L, "10.0000");
        Question question = question(6515L, QuestionType.SINGLE_CHOICE);
        UserAnswer userAnswer = new UserAnswer(
            8515L,
            9015L,
            6515L,
            FIXED_INSTANT.minusSeconds(30),
            FIXED_INSTANT.minusSeconds(20)
        );
        ResultRecordingSnapshotFactsProvider classifierBackedProvider = new ResultRecordingSnapshotFactsProvider(
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
            resultDeadlineClassifier,
            new AssignmentCountedResultPolicy(),
            new ResultQuestionScoringCalculator()
        );

        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(assignmentTestRepository.findAssignmentTestById(715L)).thenReturn(assignmentTest);
        when(assignmentRepository.findAssignmentById(815L)).thenReturn(assignment);
        when(testRepository.findTestById(515L)).thenReturn(test);
        when(testQuestionRepository.findTestQuestionsByTestId(515L)).thenReturn(List.of(testQuestion));
        when(questionRepository.findQuestionsByIds(List.of(6515L))).thenReturn(List.of(question));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(6515L)).thenReturn(List.of(
            choiceOption(7515L, 6515L, true),
            choiceOption(7516L, 6515L, false)
        ));
        when(userAnswerRepository.findUserAnswersByTestAttemptId(9015L)).thenReturn(List.of(userAnswer));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(8515L)).thenReturn(List.of(
            new UserAnswerItem(8615L, 8515L, 7515L, null, null, null, FIXED_INSTANT, FIXED_INSTANT)
        ));
        when(recipientSnapshotRepository.findAssignmentCampaignRecipientSnapshotsByCampaignId(915L)).thenReturn(List.of(
            recipientSnapshot(9615L, 915L, 301L, 9715L, "/company/ops/deadline")
        ));
        when(resultDeadlineClassifier.isWithinDeadline(eq(assignment), eq(assignmentTest), eq(FIXED_INSTANT.minusSeconds(10))))
            .thenReturn(true);

        ResultSnapshotFacts facts = classifierBackedProvider.provideSnapshotFacts(attempt);

        assertThat(facts.withinDeadline()).isTrue();
        assertThat(facts.countedInAssignment()).isTrue();
        verify(resultDeadlineClassifier).isWithinDeadline(assignment, assignmentTest, FIXED_INSTANT.minusSeconds(10));
    }

    @Test
    void assignedCountedInAssignmentIsDecidedThroughExplicitCountedResultPolicy() {
        TestAttempt attempt = assignedCompletedAttempt(9016L, 516L, 716L);
        AssignmentTest assignmentTest = assignmentTest(716L, 816L, 516L);
        Assignment assignment = assignment(816L, 916L, 301L, FIXED_INSTANT.plusSeconds(30), null);
        com.vladislav.training.platform.content.domain.Test test = test(516L, "80.00", "DEFAULT_PARTIAL_CREDIT_V1");
        TestQuestion testQuestion = testQuestion(5516L, 516L, 6516L, "10.0000");
        Question question = question(6516L, QuestionType.SINGLE_CHOICE);
        UserAnswer userAnswer = new UserAnswer(
            8516L,
            9016L,
            6516L,
            FIXED_INSTANT.minusSeconds(30),
            FIXED_INSTANT.minusSeconds(20)
        );
        ResultRecordingSnapshotFactsProvider policyBackedProvider = new ResultRecordingSnapshotFactsProvider(
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
            assignmentCountedResultPolicy,
            resultQuestionScoringEvaluator
        );

        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(assignmentTestRepository.findAssignmentTestById(716L)).thenReturn(assignmentTest);
        when(assignmentRepository.findAssignmentById(816L)).thenReturn(assignment);
        when(testRepository.findTestById(516L)).thenReturn(test);
        when(testQuestionRepository.findTestQuestionsByTestId(516L)).thenReturn(List.of(testQuestion));
        when(questionRepository.findQuestionsByIds(List.of(6516L))).thenReturn(List.of(question));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(6516L)).thenReturn(List.of(
            choiceOption(7517L, 6516L, true),
            choiceOption(7518L, 6516L, false)
        ));
        when(userAnswerRepository.findUserAnswersByTestAttemptId(9016L)).thenReturn(List.of(userAnswer));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(8516L)).thenReturn(List.of(
            new UserAnswerItem(8616L, 8516L, 7517L, null, null, null, FIXED_INSTANT, FIXED_INSTANT)
        ));
        when(recipientSnapshotRepository.findAssignmentCampaignRecipientSnapshotsByCampaignId(916L)).thenReturn(List.of(
            recipientSnapshot(9616L, 916L, 301L, 9716L, "/company/ops/counted")
        ));
        when(resultQuestionScoringEvaluator.evaluateQuestion(
            eq(question),
            org.mockito.ArgumentMatchers.anyList(),
            org.mockito.ArgumentMatchers.anyList(),
            eq(new BigDecimal("10.0000")),
            eq("DEFAULT_PARTIAL_CREDIT_V1")
        )).thenReturn(new ResultQuestionScoringEvaluator.QuestionScore(
            new BigDecimal("10.0000"),
            true
        ));

        ResultScoringSnapshot expectedScoringSnapshot = new ResultScoringSnapshot(
            new BigDecimal("80.0000"),
            new BigDecimal("10.0000"),
            new BigDecimal("10.0000"),
            new BigDecimal("100.0000"),
            true,
            "DEFAULT_PARTIAL_CREDIT_V1",
            "{\"source\":\"content-test\",\"testId\":516,\"thresholdPercent\":\"80.0000\",\"scoringPolicyCode\":\"DEFAULT_PARTIAL_CREDIT_V1\"}"
        );
        when(assignmentCountedResultPolicy.decide(eq(true), eq(expectedScoringSnapshot), eq(true)))
            .thenReturn(new AssignmentCountedResultDecision(true, AssignmentCountedResultDecisionReason.COUNTED));

        ResultSnapshotFacts facts = policyBackedProvider.provideSnapshotFacts(attempt);

        assertThat(facts.snapshotFinalTopicControlFlag()).isTrue();
        assertThat(facts.withinDeadline()).isTrue();
        assertThat(facts.countedInAssignment()).isTrue();
        verify(assignmentCountedResultPolicy).decide(true, expectedScoringSnapshot, true);
    }

    @Test
    void selfRootFactsFailClosedWhenCanonicalOrgContextSourceDoesNotExist() {
        TestAttempt selfAttempt = new TestAttempt(
            9006L,
            301L,
            506L,
            null,
            AttemptMode.SELF,
            TestAttemptStatus.COMPLETED,
            FIXED_INSTANT.minusSeconds(600),
            FIXED_INSTANT.minusSeconds(10),
            null,
            null,
            FIXED_INSTANT.minusSeconds(10),
            FIXED_INSTANT.minusSeconds(600),
            FIXED_INSTANT.minusSeconds(10)
        );
        com.vladislav.training.platform.content.domain.Test test = test(506L, "70.00", "DEFAULT_PARTIAL_CREDIT_V1");
        TestQuestion testQuestion = testQuestion(5006L, 506L, 606L, "10.0000");
        Question question = question(606L, QuestionType.SINGLE_CHOICE);
        UserAnswer userAnswer = new UserAnswer(8006L, 9006L, 606L, FIXED_INSTANT.minusSeconds(30), FIXED_INSTANT.minusSeconds(20));

        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(testRepository.findTestById(506L)).thenReturn(test);
        when(testQuestionRepository.findTestQuestionsByTestId(506L)).thenReturn(List.of(testQuestion));
        when(questionRepository.findQuestionsByIds(List.of(606L))).thenReturn(List.of(question));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(606L)).thenReturn(List.of(
            choiceOption(7009L, 606L, true),
            choiceOption(7010L, 606L, false)
        ));
        when(userAnswerRepository.findUserAnswersByTestAttemptId(9006L)).thenReturn(List.of(userAnswer));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(8006L)).thenReturn(List.of(
            new UserAnswerItem(8106L, 8006L, 7009L, null, null, null, FIXED_INSTANT.minusSeconds(10), FIXED_INSTANT.minusSeconds(10))
        ));
        when(selfCompletionOrgSnapshotFactsReader.readSelfCompletionOrgSnapshotFacts(9006L)).thenReturn(null);

        assertThatThrownBy(() -> provider.provideSnapshotFacts(selfAttempt))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    void assignedFinalControlPassedLateIsNotCountedInAssignment() {
        TestAttempt attempt = assignedCompletedAttempt(9005L, 505L, 705L);
        AssignmentTest assignmentTest = assignmentTest(705L, 805L, 505L);
        Assignment overdueAssignment = assignment(805L, 505L, 301L, FIXED_INSTANT.minusSeconds(20), null);
        com.vladislav.training.platform.content.domain.Test test = test(505L, "50.00", "DEFAULT_PARTIAL_CREDIT_V1");
        TestQuestion testQuestion = testQuestion(5005L, 505L, 605L, "10.0000");
        Question question = question(605L, QuestionType.SINGLE_CHOICE);
        UserAnswer userAnswer = new UserAnswer(8005L, 9005L, 605L, FIXED_INSTANT.minusSeconds(30), FIXED_INSTANT.minusSeconds(20));

        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(assignmentTestRepository.findAssignmentTestById(705L)).thenReturn(assignmentTest);
        when(assignmentRepository.findAssignmentById(805L)).thenReturn(overdueAssignment);
        when(testRepository.findTestById(505L)).thenReturn(test);
        when(testQuestionRepository.findTestQuestionsByTestId(505L)).thenReturn(List.of(testQuestion));
        when(questionRepository.findQuestionsByIds(List.of(605L))).thenReturn(List.of(question));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(605L)).thenReturn(List.of(
            choiceOption(7005L, 605L, true),
            choiceOption(7006L, 605L, false)
        ));
        when(userAnswerRepository.findUserAnswersByTestAttemptId(9005L)).thenReturn(List.of(userAnswer));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(8005L)).thenReturn(List.of(
            new UserAnswerItem(8105L, 8005L, 7005L, null, null, null, FIXED_INSTANT.minusSeconds(10), FIXED_INSTANT.minusSeconds(10))
        ));
        when(recipientSnapshotRepository.findAssignmentCampaignRecipientSnapshotsByCampaignId(505L)).thenReturn(List.of(
            recipientSnapshot(9105L, 505L, 301L, 905L, "/company/ops/late")
        ));

        ResultSnapshotFacts facts = provider.provideSnapshotFacts(attempt);

        assertThat(facts.scoringSnapshot().passed()).isTrue();
        assertThat(facts.withinDeadline()).isFalse();
        assertThat(facts.countedInAssignment()).isFalse();
        assertThat(facts.snapshotFinalTopicControlFlag()).isTrue();
    }

    @Test
    void missingAssignmentDeadlineIsRejectedAsV100IncompatibleState() {
        assertThatThrownBy(() -> new Assignment(
            810L,
            510L,
            301L,
            401L,
            AssignmentStatus.ASSIGNED,
            FIXED_INSTANT.minusSeconds(3600),
            null,
            null,
            null,
            FIXED_INSTANT.minusSeconds(3600),
            FIXED_INSTANT.minusSeconds(120)
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("deadlineAt must not be null");
    }

    @Test
    void withinDeadlineTreatsCompletionAfterDeadlineAsFalse() {
        TestAttempt attempt = assignedCompletedAttempt(9009L, 509L, 709L);
        AssignmentTest assignmentTest = assignmentTest(709L, 809L, 509L);
        Assignment assignment = assignment(809L, 509L, 301L, FIXED_INSTANT.minusSeconds(20), null);
        com.vladislav.training.platform.content.domain.Test test = test(509L, "50.00", "DEFAULT_PARTIAL_CREDIT_V1");
        TestQuestion testQuestion = testQuestion(5009L, 509L, 609L, "10.0000");
        Question question = question(609L, QuestionType.SINGLE_CHOICE);
        UserAnswer userAnswer = new UserAnswer(8009L, 9009L, 609L, FIXED_INSTANT.minusSeconds(30), FIXED_INSTANT.minusSeconds(20));

        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(assignmentTestRepository.findAssignmentTestById(709L)).thenReturn(assignmentTest);
        when(assignmentRepository.findAssignmentById(809L)).thenReturn(assignment);
        when(testRepository.findTestById(509L)).thenReturn(test);
        when(testQuestionRepository.findTestQuestionsByTestId(509L)).thenReturn(List.of(testQuestion));
        when(questionRepository.findQuestionsByIds(List.of(609L))).thenReturn(List.of(question));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(609L)).thenReturn(List.of(
            choiceOption(7013L, 609L, true),
            choiceOption(7014L, 609L, false)
        ));
        when(userAnswerRepository.findUserAnswersByTestAttemptId(9009L)).thenReturn(List.of(userAnswer));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(8009L)).thenReturn(List.of(
            new UserAnswerItem(8109L, 8009L, 7013L, null, null, null, FIXED_INSTANT.minusSeconds(10), FIXED_INSTANT.minusSeconds(10))
        ));
        when(recipientSnapshotRepository.findAssignmentCampaignRecipientSnapshotsByCampaignId(509L)).thenReturn(List.of(
            recipientSnapshot(9109L, 509L, 301L, 909L, "/company/ops/late-boundary")
        ));

        ResultSnapshotFacts facts = provider.provideSnapshotFacts(attempt);

        assertThat(attempt.completedAt()).isEqualTo(FIXED_INSTANT.minusSeconds(10));
        assertThat(assignment.deadlineAt()).isEqualTo(FIXED_INSTANT.minusSeconds(20));
        assertThat(facts.withinDeadline()).isFalse();
    }

    @Test
    void withinDeadlineTreatsCompletionEqualToDeadlineAsTrue() {
        TestAttempt attempt = assignedCompletedAttempt(9008L, 508L, 708L);
        AssignmentTest assignmentTest = assignmentTest(708L, 808L, 508L);
        Assignment assignment = assignment(808L, 508L, 301L, FIXED_INSTANT.minusSeconds(10), null);
        com.vladislav.training.platform.content.domain.Test test = test(508L, "50.00", "DEFAULT_PARTIAL_CREDIT_V1");
        TestQuestion testQuestion = testQuestion(5008L, 508L, 608L, "10.0000");
        Question question = question(608L, QuestionType.SINGLE_CHOICE);
        UserAnswer userAnswer = new UserAnswer(8008L, 9008L, 608L, FIXED_INSTANT.minusSeconds(30), FIXED_INSTANT.minusSeconds(20));

        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(assignmentTestRepository.findAssignmentTestById(708L)).thenReturn(assignmentTest);
        when(assignmentRepository.findAssignmentById(808L)).thenReturn(assignment);
        when(testRepository.findTestById(508L)).thenReturn(test);
        when(testQuestionRepository.findTestQuestionsByTestId(508L)).thenReturn(List.of(testQuestion));
        when(questionRepository.findQuestionsByIds(List.of(608L))).thenReturn(List.of(question));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(608L)).thenReturn(List.of(
            choiceOption(7008L, 608L, true),
            choiceOption(7009L, 608L, false)
        ));
        when(userAnswerRepository.findUserAnswersByTestAttemptId(9008L)).thenReturn(List.of(userAnswer));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(8008L)).thenReturn(List.of(
            new UserAnswerItem(8108L, 8008L, 7008L, null, null, null, FIXED_INSTANT.minusSeconds(10), FIXED_INSTANT.minusSeconds(10))
        ));
        when(recipientSnapshotRepository.findAssignmentCampaignRecipientSnapshotsByCampaignId(508L)).thenReturn(List.of(
            recipientSnapshot(9108L, 508L, 301L, 908L, "/company/ops/boundary")
        ));

        ResultSnapshotFacts facts = provider.provideSnapshotFacts(attempt);

        assertThat(attempt.completedAt()).isEqualTo(FIXED_INSTANT.minusSeconds(10));
        assertThat(assignment.deadlineAt()).isEqualTo(FIXED_INSTANT.minusSeconds(10));
        assertThat(facts.withinDeadline()).isTrue();
    }

    @Test
    void assignedNonFinalControlPassedWithinDeadlineIsNotCountedInAssignment() {
        TestAttempt attempt = assignedCompletedAttempt(9006L, 506L, 706L);
        AssignmentTest assignmentTest = assignmentTest(706L, 806L, 506L, AssignmentTestRole.NON_FINAL);
        Assignment assignment = assignment(806L, 506L, 301L, null, null);
        com.vladislav.training.platform.content.domain.Test test = test(506L, "50.00", "DEFAULT_PARTIAL_CREDIT_V1");
        TestQuestion testQuestion = testQuestion(5006L, 506L, 606L, "10.0000");
        Question question = question(606L, QuestionType.SINGLE_CHOICE);
        UserAnswer userAnswer = new UserAnswer(8006L, 9006L, 606L, FIXED_INSTANT.minusSeconds(30), FIXED_INSTANT.minusSeconds(20));

        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(assignmentTestRepository.findAssignmentTestById(706L)).thenReturn(assignmentTest);
        when(assignmentRepository.findAssignmentById(806L)).thenReturn(assignment);
        when(testRepository.findTestById(506L)).thenReturn(test);
        when(testQuestionRepository.findTestQuestionsByTestId(506L)).thenReturn(List.of(testQuestion));
        when(questionRepository.findQuestionsByIds(List.of(606L))).thenReturn(List.of(question));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(606L)).thenReturn(List.of(
            choiceOption(7006L, 606L, true),
            choiceOption(7007L, 606L, false)
        ));
        when(userAnswerRepository.findUserAnswersByTestAttemptId(9006L)).thenReturn(List.of(userAnswer));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(8006L)).thenReturn(List.of(
            new UserAnswerItem(8106L, 8006L, 7006L, null, null, null, FIXED_INSTANT.minusSeconds(10), FIXED_INSTANT.minusSeconds(10))
        ));
        when(recipientSnapshotRepository.findAssignmentCampaignRecipientSnapshotsByCampaignId(506L)).thenReturn(List.of(
            recipientSnapshot(9106L, 506L, 301L, 906L, "/company/ops/non-final")
        ));

        ResultSnapshotFacts facts = provider.provideSnapshotFacts(attempt);

        assertThat(facts.scoringSnapshot().passed()).isTrue();
        assertThat(facts.withinDeadline()).isTrue();
        assertThat(facts.snapshotFinalTopicControlFlag()).isFalse();
        assertThat(facts.countedInAssignment()).isFalse();
    }

    @Test
    void countedDecisionUsesAssignmentTestRoleInsteadOfLiveActiveFinalTestFlag() {
        TestAttempt attempt = assignedCompletedAttempt(9014L, 514L, 714L);
        AssignmentTest assignmentTest = assignmentTest(714L, 814L, 514L, AssignmentTestRole.FINAL_TOPIC_CONTROL);
        Assignment assignment = assignment(814L, 914L, 301L, null, null);
        com.vladislav.training.platform.content.domain.Test test = new com.vladislav.training.platform.content.domain.Test(
            514L,
            201L,
            "Test 514",
            "Description",
            TestType.CONTROL,
            ContentStatus.PUBLISHED,
            new BigDecimal("50.00"),
            "DEFAULT_PARTIAL_CREDIT_V1",
            false,
            0,
            FIXED_INSTANT.minusSeconds(7200),
            FIXED_INSTANT.minusSeconds(120)
        );
        TestQuestion testQuestion = testQuestion(5014L, 514L, 614L, "10.0000");
        Question question = question(614L, QuestionType.SINGLE_CHOICE);
        UserAnswer userAnswer = new UserAnswer(8014L, 9014L, 614L, FIXED_INSTANT.minusSeconds(30), FIXED_INSTANT.minusSeconds(20));

        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(assignmentTestRepository.findAssignmentTestById(714L)).thenReturn(assignmentTest);
        when(assignmentRepository.findAssignmentById(814L)).thenReturn(assignment);
        when(testRepository.findTestById(514L)).thenReturn(test);
        when(testQuestionRepository.findTestQuestionsByTestId(514L)).thenReturn(List.of(testQuestion));
        when(questionRepository.findQuestionsByIds(List.of(614L))).thenReturn(List.of(question));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(614L)).thenReturn(List.of(
            choiceOption(7014L, 614L, true),
            choiceOption(7015L, 614L, false)
        ));
        when(userAnswerRepository.findUserAnswersByTestAttemptId(9014L)).thenReturn(List.of(userAnswer));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(8014L)).thenReturn(List.of(
            new UserAnswerItem(8114L, 8014L, 7014L, null, null, null, FIXED_INSTANT.minusSeconds(10), FIXED_INSTANT.minusSeconds(10))
        ));
        when(recipientSnapshotRepository.findAssignmentCampaignRecipientSnapshotsByCampaignId(914L)).thenReturn(List.of(
            recipientSnapshot(9114L, 914L, 301L, 9214L, "/company/ops/final-role")
        ));

        ResultSnapshotFacts facts = provider.provideSnapshotFacts(attempt);

        assertThat(test.isActiveFinalForTopic()).isFalse();
        assertThat(facts.scoringSnapshot().passed()).isTrue();
        assertThat(facts.withinDeadline()).isTrue();
        assertThat(facts.snapshotFinalTopicControlFlag()).isTrue();
        assertThat(facts.countedInAssignment()).isTrue();
        verify(testRepository).findTestById(514L);
    }

    @Test
    void assignedFinalControlFailedWithinDeadlineIsNotCountedInAssignment() {
        TestAttempt attempt = assignedCompletedAttempt(9007L, 507L, 707L);
        AssignmentTest assignmentTest = assignmentTest(707L, 807L, 507L);
        Assignment assignment = assignment(807L, 507L, 301L, null, null);
        com.vladislav.training.platform.content.domain.Test test = test(507L, "90.00", "DEFAULT_PARTIAL_CREDIT_V1");
        TestQuestion testQuestion = testQuestion(5007L, 507L, 607L, "10.0000");
        Question question = question(607L, QuestionType.SINGLE_CHOICE);
        UserAnswer userAnswer = new UserAnswer(8007L, 9007L, 607L, FIXED_INSTANT.minusSeconds(30), FIXED_INSTANT.minusSeconds(20));

        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(assignmentTestRepository.findAssignmentTestById(707L)).thenReturn(assignmentTest);
        when(assignmentRepository.findAssignmentById(807L)).thenReturn(assignment);
        when(testRepository.findTestById(507L)).thenReturn(test);
        when(testQuestionRepository.findTestQuestionsByTestId(507L)).thenReturn(List.of(testQuestion));
        when(questionRepository.findQuestionsByIds(List.of(607L))).thenReturn(List.of(question));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(607L)).thenReturn(List.of(
            choiceOption(7011L, 607L, true),
            choiceOption(7012L, 607L, false)
        ));
        when(userAnswerRepository.findUserAnswersByTestAttemptId(9007L)).thenReturn(List.of(userAnswer));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(8007L)).thenReturn(List.of(
            new UserAnswerItem(8107L, 8007L, 7012L, null, null, null, FIXED_INSTANT.minusSeconds(10), FIXED_INSTANT.minusSeconds(10))
        ));
        when(recipientSnapshotRepository.findAssignmentCampaignRecipientSnapshotsByCampaignId(507L)).thenReturn(List.of(
            recipientSnapshot(9107L, 507L, 301L, 907L, "/company/ops/final")
        ));

        ResultSnapshotFacts facts = provider.provideSnapshotFacts(attempt);

        assertThat(facts.scoringSnapshot().passed()).isFalse();
        assertThat(facts.withinDeadline()).isTrue();
        assertThat(facts.countedInAssignment()).isFalse();
        assertThat(facts.snapshotFinalTopicControlFlag()).isTrue();
    }

    @Test
    void rootScoringFactsUseCanonicalQuestionScoresAndDefaultPartialCreditPolicy() {
        ScoringTargetObservation observation = materializePartialRootAndQuestionScoringObservation();

        BigDecimal summedQuestionEarnedScore = observation.questionSnapshots().stream()
            .map(ResultQuestionSnapshot::earnedScore)
            .reduce(BigDecimal.ZERO.setScale(4), BigDecimal::add);

        assertThat(observation.rootFacts().scoringSnapshot().earnedScore()).isEqualByComparingTo("4.3333");
        assertThat(observation.rootFacts().scoringSnapshot().maxScore()).isEqualByComparingTo("10.0000");
        assertThat(observation.rootFacts().scoringSnapshot().scorePercent()).isEqualByComparingTo("43.3330");
        assertThat(observation.rootFacts().scoringSnapshot().passed()).isFalse();
        assertThat(observation.rootFacts().scoringSnapshot().scoringPolicyCode()).isEqualTo("DEFAULT_PARTIAL_CREDIT_V1");
        assertThat(observation.rootFacts().scoringSnapshot().earnedScore()).isEqualByComparingTo(summedQuestionEarnedScore);
    }

    @Test
    void questionSnapshotsPersistCanonicalQuestionScores() {
        ScoringTargetObservation observation = materializePartialRootAndQuestionScoringObservation();

        assertThat(observation.questionSnapshots()).hasSize(2);
        assertThat(observation.questionSnapshots())
            .extracting(ResultQuestionSnapshot::questionType, ResultQuestionSnapshot::earnedScore, ResultQuestionSnapshot::maxScore, ResultQuestionSnapshot::isCorrect)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple(
                    com.vladislav.training.platform.result.domain.ResultQuestionType.MULTIPLE_CHOICE,
                    new BigDecimal("3.0000"),
                    new BigDecimal("6.0000"),
                    false
                ),
                org.assertj.core.groups.Tuple.tuple(
                    com.vladislav.training.platform.result.domain.ResultQuestionType.ORDERING,
                    new BigDecimal("1.3333"),
                    new BigDecimal("4.0000"),
                    false
                )
            );
    }

    @Test
    void rootResultEarnedScoreWillEqualSumOfCanonicalQuestionEarnedScores() {
        ScoringTargetObservation observation = materializePartialRootAndQuestionScoringObservation();

        BigDecimal summedQuestionEarnedScore = observation.questionSnapshots().stream()
            .map(ResultQuestionSnapshot::earnedScore)
            .reduce(BigDecimal.ZERO.setScale(4), BigDecimal::add);

        assertThat(observation.rootFacts().scoringSnapshot().earnedScore()).isEqualByComparingTo(summedQuestionEarnedScore);
        assertThat(observation.rootFacts().scoringSnapshot().earnedScore()).isGreaterThan(BigDecimal.ZERO.setScale(4));
        assertThat(observation.questionSnapshots())
            .extracting(ResultQuestionSnapshot::earnedScore)
            .allSatisfy(score -> {
                assertThat((BigDecimal) score).isGreaterThan(BigDecimal.ZERO.setScale(4));
            });
    }

    @Test
    void rootAndQuestionSnapshotsWillUseSameCanonicalScoringDecision() {
        ScoringTargetObservation observation = materializePartialRootAndQuestionScoringObservation();

        assertThat(observation.questionSnapshots()).hasSize(2);
        assertThat(observation.questionSnapshots())
            .extracting(ResultQuestionSnapshot::questionType)
            .containsExactlyInAnyOrder(
                com.vladislav.training.platform.result.domain.ResultQuestionType.MULTIPLE_CHOICE,
                com.vladislav.training.platform.result.domain.ResultQuestionType.ORDERING
            );
        assertThat(observation.questionSnapshots())
            .allSatisfy(snapshot -> {
                assertThat(snapshot.earnedScore()).isGreaterThan(BigDecimal.ZERO.setScale(4));
                assertThat(snapshot.earnedScore()).isLessThan(snapshot.maxScore());
                assertThat(snapshot.isCorrect()).isFalse();
            });
        assertThat(observation.rootFacts().scoringSnapshot().earnedScore()).isGreaterThan(BigDecimal.ZERO.setScale(4));
        assertThat(observation.rootFacts().scoringSnapshot().earnedScore())
            .isLessThan(observation.rootFacts().scoringSnapshot().maxScore());
    }

    @Test
    void scoringTargetsDoNotDeriveAssignmentCountedSemantics() {
        TestAttempt attempt = assignedCompletedAttempt(9011L, 511L, 711L);
        AssignmentTest assignmentTest = assignmentTest(711L, 811L, 511L);
        Assignment assignment = assignment(811L, 511L, 301L, null, null);
        com.vladislav.training.platform.content.domain.Test test = test(511L, "80.00", "DEFAULT_PARTIAL_CREDIT_V1");
        TestQuestion testQuestion = testQuestion(5111L, 511L, 611L, "10.0000");
        Question question = question(611L, QuestionType.MULTIPLE_CHOICE);

        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(assignmentTestRepository.findAssignmentTestById(711L)).thenReturn(assignmentTest);
        when(assignmentRepository.findAssignmentById(811L)).thenReturn(assignment);
        when(testRepository.findTestById(511L)).thenReturn(test);
        when(testQuestionRepository.findTestQuestionsByTestId(511L)).thenReturn(List.of(testQuestion));
        when(questionRepository.findQuestionsByIds(List.of(611L))).thenReturn(List.of(question));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(611L)).thenReturn(List.of(
            choiceOption(7111L, 611L, true),
            choiceOption(7112L, 611L, true),
            choiceOption(7113L, 611L, false)
        ));
        when(userAnswerRepository.findUserAnswersByTestAttemptId(9011L)).thenReturn(List.of(
            new UserAnswer(8111L, 9011L, 611L, FIXED_INSTANT.minusSeconds(30), FIXED_INSTANT.minusSeconds(20))
        ));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(8111L)).thenReturn(List.of(
            new UserAnswerItem(8211L, 8111L, 7111L, null, null, null, FIXED_INSTANT.minusSeconds(10), FIXED_INSTANT.minusSeconds(10))
        ));
        when(recipientSnapshotRepository.findAssignmentCampaignRecipientSnapshotsByCampaignId(511L)).thenReturn(List.of(
            recipientSnapshot(9111L, 511L, 301L, 911L, "/company/ops/step6")
        ));

        ResultSnapshotFacts facts = provider.provideSnapshotFacts(attempt);

        assertThat(facts.scoringSnapshot().earnedScore()).isGreaterThan(BigDecimal.ZERO.setScale(4));
        assertThat(facts.scoringSnapshot().earnedScore()).isLessThan(facts.scoringSnapshot().maxScore());
        assertThat(facts.countedInAssignment()).isEqualTo(facts.scoringSnapshot().passed() && Boolean.TRUE.equals(facts.withinDeadline()));
        assertThat(facts.snapshotFinalTopicControlFlag()).isTrue();
    }

    private ScoringTargetObservation materializePartialRootAndQuestionScoringObservation() {
        TestAttempt selfAttempt = new TestAttempt(
            9010L,
            301L,
            510L,
            null,
            AttemptMode.SELF,
            TestAttemptStatus.COMPLETED,
            FIXED_INSTANT.minusSeconds(600),
            FIXED_INSTANT.minusSeconds(10),
            null,
            null,
            FIXED_INSTANT.minusSeconds(10),
            FIXED_INSTANT.minusSeconds(600),
            FIXED_INSTANT.minusSeconds(10)
        );
        com.vladislav.training.platform.content.domain.Test test = test(510L, "70.00", "DEFAULT_PARTIAL_CREDIT_V1");
        TestQuestion multipleChoiceQuestion = testQuestion(5110L, 510L, 610L, "6.0000");
        TestQuestion orderingQuestion = testQuestion(5111L, 510L, 611L, "4.0000");
        Question multipleChoice = question(610L, QuestionType.MULTIPLE_CHOICE);
        Question ordering = question(611L, QuestionType.ORDERING);
        UserAnswer multipleChoiceAnswer = new UserAnswer(
            8110L,
            9010L,
            610L,
            FIXED_INSTANT.minusSeconds(30),
            FIXED_INSTANT.minusSeconds(20)
        );
        UserAnswer orderingAnswer = new UserAnswer(
            8111L,
            9010L,
            611L,
            FIXED_INSTANT.minusSeconds(30),
            FIXED_INSTANT.minusSeconds(20)
        );

        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(testRepository.findTestById(510L)).thenReturn(test);
        when(testQuestionRepository.findTestQuestionsByTestId(510L)).thenReturn(List.of(multipleChoiceQuestion, orderingQuestion));
        when(questionRepository.findQuestionsByIds(List.of(610L, 611L))).thenReturn(List.of(multipleChoice, ordering));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(610L)).thenReturn(List.of(
            choiceOption(7110L, 610L, true),
            choiceOption(7111L, 610L, true),
            choiceOption(7112L, 610L, false)
        ));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(611L)).thenReturn(List.of(
            orderingOption(7210L, 611L, 1, 0),
            orderingOption(7211L, 611L, 2, 1),
            orderingOption(7212L, 611L, 3, 2)
        ));
        when(userAnswerRepository.findUserAnswersByTestAttemptId(9010L)).thenReturn(List.of(
            multipleChoiceAnswer,
            orderingAnswer
        ));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(8110L)).thenReturn(List.of(
            new UserAnswerItem(8210L, 8110L, 7110L, null, null, null, FIXED_INSTANT.minusSeconds(10), FIXED_INSTANT.minusSeconds(10))
        ));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(8111L)).thenReturn(List.of(
            new UserAnswerItem(8211L, 8111L, 7210L, null, null, 1, FIXED_INSTANT.minusSeconds(10), FIXED_INSTANT.minusSeconds(10)),
            new UserAnswerItem(8212L, 8111L, 7211L, null, null, 3, FIXED_INSTANT.minusSeconds(10), FIXED_INSTANT.minusSeconds(10)),
            new UserAnswerItem(8213L, 8111L, 7212L, null, null, 2, FIXED_INSTANT.minusSeconds(10), FIXED_INSTANT.minusSeconds(10))
        ));
        when(selfCompletionOrgSnapshotFactsReader.readSelfCompletionOrgSnapshotFacts(9010L)).thenReturn(
            new SelfCompletionOrgSnapshotFactsReader.SelfCompletionOrgSnapshotFacts(910L, "/company/self/step6")
        );

        AtomicLong questionSnapshotIdSequence = new AtomicLong(9900L);
        when(resultQuestionSnapshotRepository.saveResultQuestionSnapshot(org.mockito.ArgumentMatchers.any(ResultQuestionSnapshot.class)))
            .thenAnswer(invocation -> {
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
        when(resultAnswerOptionSnapshotRepository.saveResultAnswerOptionSnapshot(org.mockito.ArgumentMatchers.any(ResultAnswerOptionSnapshot.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ResultSnapshotFacts rootFacts = provider.provideSnapshotFacts(selfAttempt);
        Result recordedResult = new Result(
            9901L,
            selfAttempt.id(),
            selfAttempt.userId(),
            AttemptMode.SELF,
            null,
            null,
            rootFacts.testIdSnapshot(),
            rootFacts.testNameSnapshot(),
            rootFacts.scoringSnapshot(),
            rootFacts.withinDeadline(),
            rootFacts.countedInAssignment(),
            selfAttempt.completedAt(),
            new ResultOrgContextSnapshot(
                rootFacts.orgContextSnapshot().organizationalUnitIdSnapshot(),
                rootFacts.orgContextSnapshot().organizationalPathSnapshot()
            ),
            rootFacts.snapshotFinalTopicControlFlag(),
            rootFacts.recordedAt()
        );
        ResultRecordingSubordinateSnapshotMaterializer subordinateMaterializer =
            new ResultRecordingSubordinateSnapshotMaterializer(
                resultQuestionSnapshotRepository,
                resultAnswerOptionSnapshotRepository,
                testQuestionRepository,
                questionRepository,
                answerOptionRepository,
                userAnswerRepository,
                userAnswerItemRepository,
                new ResultQuestionScoringCalculator()
            );

        subordinateMaterializer.materialize(recordedResult, selfAttempt, rootFacts);

        ArgumentCaptor<ResultQuestionSnapshot> questionSnapshotCaptor = ArgumentCaptor.forClass(ResultQuestionSnapshot.class);
        org.mockito.Mockito.verify(resultQuestionSnapshotRepository, org.mockito.Mockito.times(2))
            .saveResultQuestionSnapshot(questionSnapshotCaptor.capture());
        return new ScoringTargetObservation(rootFacts, questionSnapshotCaptor.getAllValues());
    }

    private TestAttempt assignedCompletedAttempt(Long attemptId, Long testId, Long assignmentTestId) {
        return new TestAttempt(
            attemptId,
            301L,
            testId,
            assignmentTestId,
            AttemptMode.ASSIGNED,
            TestAttemptStatus.COMPLETED,
            FIXED_INSTANT.minusSeconds(600),
            FIXED_INSTANT.minusSeconds(10),
            null,
            null,
            FIXED_INSTANT.minusSeconds(10),
            FIXED_INSTANT.minusSeconds(600),
            FIXED_INSTANT.minusSeconds(10)
        );
    }

    private void stubAssignedQuestionContext(
        Long campaignId,
        TestAttempt attempt,
        AssignmentTest assignmentTest,
        Assignment assignment,
        com.vladislav.training.platform.content.domain.Test test,
        TestQuestion testQuestion,
        Question question,
        List<AnswerOption> answerOptions,
        UserAnswer userAnswer,
        List<UserAnswerItem> userAnswerItems,
        String orgPath
    ) {
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(assignmentTestRepository.findAssignmentTestById(attempt.assignmentTestId())).thenReturn(assignmentTest);
        when(assignmentRepository.findAssignmentById(assignment.id())).thenReturn(assignment);
        when(testRepository.findTestById(attempt.testId())).thenReturn(test);
        when(testQuestionRepository.findTestQuestionsByTestId(attempt.testId())).thenReturn(List.of(testQuestion));
        when(questionRepository.findQuestionsByIds(List.of(question.id()))).thenReturn(List.of(question));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(question.id())).thenReturn(answerOptions);
        when(userAnswerRepository.findUserAnswersByTestAttemptId(attempt.id())).thenReturn(List.of(userAnswer));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(userAnswer.id())).thenReturn(userAnswerItems);
        lenient().when(recipientSnapshotRepository.findAssignmentCampaignRecipientSnapshotsByCampaignId(campaignId)).thenReturn(List.of(
            recipientSnapshot(9900L + campaignId, campaignId, 301L, 930L + campaignId, orgPath)
        ));
    }

    private void assertSingleChoiceScoringCase(
        Long caseId,
        List<UserAnswerItem> userAnswerItems,
        String expectedEarnedScore,
        boolean expectedPassed
    ) {
        Long attemptId = 9100L + caseId;
        Long testId = 520L + caseId;
        Long assignmentTestId = 720L + caseId;
        Long assignmentId = 820L + caseId;
        Long questionId = 620L + caseId;
        Long userAnswerId = 18000L + caseId;
        Long correctOptionId = 17000L + ((caseId - 1L) * 2L) + 1L;
        Long wrongOptionId = correctOptionId + 1L;
        TestAttempt attempt = assignedCompletedAttempt(attemptId, testId, assignmentTestId);
        AssignmentTest assignmentTest = assignmentTest(assignmentTestId, assignmentId, testId);
        Assignment assignment = assignment(assignmentId, 920L + caseId, 301L, null, null);
        com.vladislav.training.platform.content.domain.Test test = test(testId, "50.00", "DEFAULT_PARTIAL_CREDIT_V1");
        TestQuestion testQuestion = testQuestion(1520L + caseId, testId, questionId, "10.0000");
        Question question = question(questionId, QuestionType.SINGLE_CHOICE);
        UserAnswer userAnswer = new UserAnswer(
            userAnswerId,
            attemptId,
            questionId,
            FIXED_INSTANT.minusSeconds(30),
            FIXED_INSTANT.minusSeconds(20)
        );

        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(assignmentTestRepository.findAssignmentTestById(assignmentTestId)).thenReturn(assignmentTest);
        when(assignmentRepository.findAssignmentById(assignmentId)).thenReturn(assignment);
        when(testRepository.findTestById(testId)).thenReturn(test);
        when(testQuestionRepository.findTestQuestionsByTestId(testId)).thenReturn(List.of(testQuestion));
        when(questionRepository.findQuestionsByIds(List.of(questionId))).thenReturn(List.of(question));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(questionId)).thenReturn(List.of(
            choiceOption(correctOptionId, questionId, true),
            choiceOption(wrongOptionId, questionId, false)
        ));
        if (userAnswerItems == null) {
            when(userAnswerRepository.findUserAnswersByTestAttemptId(attemptId)).thenReturn(List.of());
        } else {
            when(userAnswerRepository.findUserAnswersByTestAttemptId(attemptId)).thenReturn(List.of(userAnswer));
            when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(userAnswerId)).thenReturn(userAnswerItems);
        }
        when(recipientSnapshotRepository.findAssignmentCampaignRecipientSnapshotsByCampaignId(assignment.campaignId()))
            .thenReturn(List.of(recipientSnapshot(29000L + caseId, assignment.campaignId(), 301L, 39000L + caseId, "/company/scoring")));

        ResultSnapshotFacts facts = provider.provideSnapshotFacts(attempt);

        assertThat(facts.scoringSnapshot().earnedScore()).isEqualByComparingTo(expectedEarnedScore);
        assertThat(facts.scoringSnapshot().maxScore()).isEqualByComparingTo("10.0000");
        assertThat(facts.scoringSnapshot().passed()).isEqualTo(expectedPassed);
    }

    private Assignment assignment(Long id, Long campaignId, Long userId, Instant deadlineAt, Instant closedAt) {
        return new Assignment(
            id,
            campaignId,
            userId,
            401L,
            AssignmentStatus.ASSIGNED,
            FIXED_INSTANT.minusSeconds(3600),
            deadlineAt == null ? FIXED_INSTANT.plusSeconds(3600) : deadlineAt,
            null,
            closedAt,
            FIXED_INSTANT.minusSeconds(3600),
            FIXED_INSTANT.minusSeconds(120)
        );
    }

    private AssignmentTest assignmentTest(Long assignmentTestId, Long assignmentId, Long testId) {
        return assignmentTest(assignmentTestId, assignmentId, testId, AssignmentTestRole.FINAL_TOPIC_CONTROL);
    }

    private AssignmentTest assignmentTest(
        Long assignmentTestId,
        Long assignmentId,
        Long testId,
        AssignmentTestRole assignmentTestRole
    ) {
        return new AssignmentTest(
            assignmentTestId,
            assignmentId,
            testId,
            assignmentTestRole,
            null,
            null,
            false,
            FIXED_INSTANT.minusSeconds(3600),
            FIXED_INSTANT.minusSeconds(120)
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
            FIXED_INSTANT.minusSeconds(7200),
            FIXED_INSTANT.minusSeconds(120)
        );
    }

    private TestQuestion testQuestion(Long id, Long testId, Long questionId, String weight) {
        return new TestQuestion(
            id,
            testId,
            questionId,
            0,
            new BigDecimal(weight),
            FIXED_INSTANT.minusSeconds(7200),
            FIXED_INSTANT.minusSeconds(120)
        );
    }

    private Question question(Long id, QuestionType type) {
        return new Question(id, 201L, "Question " + id, type, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
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
            FIXED_INSTANT,
            FIXED_INSTANT
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
            FIXED_INSTANT,
            FIXED_INSTANT
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
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }

    private AnswerOption orderingOption(Long id, Long questionId, int canonicalOrderPosition, int displayOrder) {
        return new AnswerOption(
            id,
            questionId,
            "Order item " + id,
            AnswerOptionRole.ORDER_ITEM,
            null,
            displayOrder,
            null,
            canonicalOrderPosition,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }

    private AssignmentCampaignRecipientSnapshot recipientSnapshot(
        Long id,
        Long campaignId,
        Long userId,
        Long orgUnitId,
        String path
    ) {
        return new AssignmentCampaignRecipientSnapshot(
            id,
            campaignId,
            userId,
            orgUnitId,
            path,
            "MANDATORY",
            "E-" + userId,
            "User " + userId,
            FIXED_INSTANT.minusSeconds(4000),
            FIXED_INSTANT.minusSeconds(4000)
        );
    }

    private record ScoringTargetObservation(
        ResultSnapshotFacts rootFacts,
        List<ResultQuestionSnapshot> questionSnapshots
    ) {
    }
}

