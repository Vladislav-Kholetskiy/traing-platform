package com.vladislav.training.platform.result.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.domain.AssignmentTestRole;
import com.vladislav.training.platform.assignment.infrastructure.persistence.AssignmentCampaignEntity;
import com.vladislav.training.platform.assignment.infrastructure.persistence.AssignmentEntity;
import com.vladislav.training.platform.assignment.infrastructure.persistence.AssignmentTestEntity;
import com.vladislav.training.platform.assignment.infrastructure.persistence.SpringDataAssignmentCampaignJpaRepository;
import com.vladislav.training.platform.assignment.infrastructure.persistence.SpringDataAssignmentJpaRepository;
import com.vladislav.training.platform.assignment.infrastructure.persistence.SpringDataAssignmentTestJpaRepository;
import com.vladislav.training.platform.assignment.service.AssignmentCountedResultHandoffService;
import com.vladislav.training.platform.audit.domain.AuditContext;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.audit.service.SystemActorResolver;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.content.domain.AnswerOption;
import com.vladislav.training.platform.content.domain.AnswerOptionRole;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Question;
import com.vladislav.training.platform.content.domain.QuestionType;
import com.vladislav.training.platform.content.domain.TestQuestion;
import com.vladislav.training.platform.content.domain.TestType;
import com.vladislav.training.platform.content.infrastructure.persistence.CourseEntity;
import com.vladislav.training.platform.content.infrastructure.persistence.QuestionEntity;
import com.vladislav.training.platform.content.infrastructure.persistence.AnswerOptionEntity;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataCourseJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataQuestionJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataAnswerOptionJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataTestJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.SpringDataTopicJpaRepository;
import com.vladislav.training.platform.content.infrastructure.persistence.TestEntity;
import com.vladislav.training.platform.content.infrastructure.persistence.TopicEntity;
import com.vladislav.training.platform.content.repository.AnswerOptionRepository;
import com.vladislav.training.platform.content.repository.QuestionRepository;
import com.vladislav.training.platform.content.repository.TestQuestionRepository;
import com.vladislav.training.platform.result.domain.Result;
import com.vladislav.training.platform.result.domain.ResultOrgContextSnapshot;
import com.vladislav.training.platform.result.domain.ResultAnswerOptionSnapshot;
import com.vladislav.training.platform.result.domain.ResultQuestionSnapshot;
import com.vladislav.training.platform.result.domain.ResultScoringSnapshot;
import com.vladislav.training.platform.result.domain.ResultSnapshotFacts;
import com.vladislav.training.platform.result.domain.ResultSnapshotFacts.ResultAnswerOptionSnapshotFact;
import com.vladislav.training.platform.result.domain.ResultSnapshotFacts.ResultQuestionSnapshotFact;
import com.vladislav.training.platform.result.domain.ResultSnapshotFacts.ResultSubordinateSnapshotFacts;
import com.vladislav.training.platform.result.domain.ResultSnapshotFacts.ResultUserAnswerItemSnapshotFact;
import com.vladislav.training.platform.result.infrastructure.persistence.JpaResultQuestionSnapshotRepositoryAdapter;
import com.vladislav.training.platform.result.infrastructure.persistence.JpaResultRepositoryAdapter;
import com.vladislav.training.platform.result.infrastructure.persistence.SpringDataResultAnswerOptionSnapshotJpaRepository;
import com.vladislav.training.platform.result.infrastructure.persistence.ResultPersistenceMapper;
import com.vladislav.training.platform.result.infrastructure.persistence.SpringDataResultJpaRepository;
import com.vladislav.training.platform.result.infrastructure.persistence.SpringDataResultQuestionSnapshotJpaRepository;
import com.vladislav.training.platform.result.repository.ResultAnswerOptionSnapshotRepository;
import com.vladislav.training.platform.result.repository.ResultQuestionSnapshotRepository;
import com.vladislav.training.platform.result.repository.ResultRepository;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import com.vladislav.training.platform.testing.domain.UserAnswer;
import com.vladislav.training.platform.testing.domain.UserAnswerItem;
import com.vladislav.training.platform.testing.infrastructure.persistence.JpaTestAttemptRepositoryAdapter;
import com.vladislav.training.platform.testing.infrastructure.persistence.SpringDataTestAttemptJpaRepository;
import com.vladislav.training.platform.testing.infrastructure.persistence.TestAttemptEntity;
import com.vladislav.training.platform.testing.infrastructure.persistence.TestingPersistenceMapper;
import com.vladislav.training.platform.testing.repository.TestAttemptRepository;
import com.vladislav.training.platform.testing.repository.UserAnswerItemRepository;
import com.vladislav.training.platform.testing.repository.UserAnswerRepository;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.infrastructure.persistence.AppUserEntity;
import com.vladislav.training.platform.userorg.infrastructure.persistence.SpringDataAppUserJpaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    classes = ResultPersistenceInvariantIntegrationTest.ResultRollbackTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
/**
 * Проверяет {@code ResultPersistenceInvariant} на уровне интеграции.
 * Здесь важна совместная работа нескольких частей приложения.
 */
@Testcontainers(disabledWithoutDocker = true)
class ResultPersistenceInvariantIntegrationTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-02T12:00:00Z");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private ResultRecordingService resultRecordingService;
    @Autowired
    private SpringDataAppUserJpaRepository appUserRepository;
    @Autowired
    private SpringDataCourseJpaRepository courseRepository;
    @Autowired
    private SpringDataTopicJpaRepository topicRepository;
    @Autowired
    private SpringDataTestJpaRepository testRepository;
    @Autowired
    private SpringDataQuestionJpaRepository questionJpaRepository;
    @Autowired
    private SpringDataAnswerOptionJpaRepository answerOptionJpaRepository;
    @Autowired
    private SpringDataAssignmentCampaignJpaRepository assignmentCampaignRepository;
    @Autowired
    private SpringDataAssignmentJpaRepository assignmentRepository;
    @Autowired
    private SpringDataAssignmentTestJpaRepository assignmentTestRepository;
    @Autowired
    private SpringDataTestAttemptJpaRepository testAttemptRepository;
    @Autowired
    private SpringDataResultJpaRepository resultRepository;
    @Autowired
    private SpringDataResultQuestionSnapshotJpaRepository resultQuestionSnapshotJpaRepository;
    @Autowired
    private SpringDataResultAnswerOptionSnapshotJpaRepository resultAnswerOptionSnapshotJpaRepository;
    @Autowired
    private ResultRecordingSnapshotFactsProvider snapshotFactsProvider;
    @Autowired
    private ControllableResultQuestionSnapshotRepository resultQuestionSnapshotRepository;
    @Autowired
    private ControllableResultAnswerOptionSnapshotRepository resultAnswerOptionSnapshotRepository;
    @Autowired
    private ResultRecordingIdempotentReplayValidator resultRecordingIdempotentReplayValidator;
    @Autowired
    private TestQuestionRepository testQuestionRepository;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private AnswerOptionRepository answerOptionRepository;
    @Autowired
    private UserAnswerRepository userAnswerRepository;
    @Autowired
    private UserAnswerItemRepository userAnswerItemRepository;
    @Autowired
    private AssignmentCountedResultHandoffService assignmentCountedResultHandoffService;
    @Autowired
    private CriticalCommandAuditSupport criticalCommandAuditSupport;
    @Autowired
    private SystemActorResolver systemActorResolver;

    @AfterEach
    void cleanDatabase() {
        resultQuestionSnapshotRepository.resetBehavior();
        resultAnswerOptionSnapshotRepository.resetBehavior();
        reset(
            snapshotFactsProvider,
            testQuestionRepository,
            questionRepository,
            answerOptionRepository,
            userAnswerRepository,
            userAnswerItemRepository,
            assignmentCountedResultHandoffService,
            criticalCommandAuditSupport,
            systemActorResolver,
            resultRecordingIdempotentReplayValidator
        );
        clearInvocations(resultQuestionSnapshotRepository, resultAnswerOptionSnapshotRepository);
        resultAnswerOptionSnapshotJpaRepository.deleteAllInBatch();
        resultQuestionSnapshotJpaRepository.deleteAllInBatch();
        resultRepository.deleteAllInBatch();
        testAttemptRepository.deleteAllInBatch();
        assignmentTestRepository.deleteAllInBatch();
        assignmentRepository.deleteAllInBatch();
        assignmentCampaignRepository.deleteAllInBatch();
        answerOptionJpaRepository.deleteAllInBatch();
        questionJpaRepository.deleteAllInBatch();
        testRepository.deleteAllInBatch();
        topicRepository.deleteAllInBatch();
        courseRepository.deleteAllInBatch();
        appUserRepository.deleteAllInBatch();
    }

    @Test
    void standardScoringPolicyRecordsExactlyOneCountedAssignedResultAndSupportsIdempotentReplay() {
        AssignedRecordingFixture fixture = createAssignedRecordingFixture();
        QuestionEntity persistedQuestion = questionJpaRepository.saveAndFlush(questionEntity(
            fixture.topicId(),
            "Which option is correct?",
            0
        ));
        AnswerOptionEntity persistedCorrectOption = answerOptionJpaRepository.saveAndFlush(answerOptionEntity(
            persistedQuestion.getId(),
            "Correct option",
            true,
            0
        ));
        AnswerOptionEntity persistedWrongOption = answerOptionJpaRepository.saveAndFlush(answerOptionEntity(
            persistedQuestion.getId(),
            "Wrong option",
            false,
            1
        ));
        TestQuestion testQuestion = new TestQuestion(
            409L,
            fixture.testId(),
            persistedQuestion.getId(),
            0,
            new BigDecimal("2.0000"),
            FIXED_INSTANT.minusSeconds(60),
            FIXED_INSTANT.minusSeconds(60)
        );
        Question question = new Question(
            persistedQuestion.getId(),
            fixture.topicId(),
            "Which option is correct?",
            QuestionType.SINGLE_CHOICE,
            ContentStatus.PUBLISHED,
            0,
            FIXED_INSTANT.minusSeconds(120),
            FIXED_INSTANT.minusSeconds(120)
        );
        AnswerOption correctOption = new AnswerOption(
            persistedCorrectOption.getId(),
            persistedQuestion.getId(),
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
            persistedWrongOption.getId(),
            persistedQuestion.getId(),
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
            929L,
            fixture.attemptId(),
            persistedQuestion.getId(),
            FIXED_INSTANT.minusSeconds(30),
            FIXED_INSTANT.minusSeconds(30)
        );
        UserAnswerItem selectedItem = new UserAnswerItem(
            939L,
            929L,
            persistedCorrectOption.getId(),
            null,
            null,
            null,
            FIXED_INSTANT.minusSeconds(20),
            FIXED_INSTANT.minusSeconds(20)
        );

        when(snapshotFactsProvider.provideSnapshotFacts(any(TestAttempt.class))).thenReturn(new ResultSnapshotFacts(
            fixture.assignmentId(),
            fixture.assignmentTestId(),
            fixture.testId(),
            "Test",
            new ResultScoringSnapshot(
                new BigDecimal("70.0000"),
                new BigDecimal("2.0000"),
                new BigDecimal("2.0000"),
                new BigDecimal("100.0000"),
                true,
                "STANDARD",
                "{\"policy\":\"standard\"}"
            ),
            true,
            true,
            new ResultOrgContextSnapshot(501L, "/company/ops"),
            true,
            FIXED_INSTANT,
            frozenSubordinateSnapshotFacts(
                testQuestion,
                question,
                List.of(correctOption, wrongOption),
                List.of(selectedItem)
            )
        ));
        when(testQuestionRepository.findTestQuestionsByTestId(fixture.testId())).thenReturn(List.of(testQuestion));
        when(questionRepository.findQuestionsByIds(List.of(persistedQuestion.getId()))).thenReturn(List.of(question));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(persistedQuestion.getId()))
            .thenReturn(List.of(correctOption, wrongOption));
        when(userAnswerRepository.findUserAnswersByTestAttemptId(fixture.attemptId())).thenReturn(List.of(userAnswer));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(929L)).thenReturn(List.of(selectedItem));
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(901L);
        when(criticalCommandAuditSupport.buildAuditContext(any(), any(String.class), any()))
            .thenReturn(new AuditContext("{\"operationCode\":\"RESULT_RECORDING\"}"));

        Long firstResultId = resultRecordingService.recordResult(fixture.attemptId());
        var persistedBeforeReplay = resultRepository.findByTestAttemptId(fixture.attemptId()).orElseThrow();
        long resultRowsBeforeReplay = resultRepository.count();
        long questionSnapshotsBeforeReplay = resultQuestionSnapshotJpaRepository.count();
        long answerOptionSnapshotsBeforeReplay = resultAnswerOptionSnapshotJpaRepository.count();
        verify(assignmentCountedResultHandoffService).acceptValidCountedAssignmentResult(firstResultId);
        verify(criticalCommandAuditSupport).recordAudit(any(), any(), any(), any(), any(), any(), any());

        clearInvocations(
            snapshotFactsProvider,
            testQuestionRepository,
            questionRepository,
            answerOptionRepository,
            userAnswerRepository,
            userAnswerItemRepository,
            resultQuestionSnapshotRepository,
            resultAnswerOptionSnapshotRepository,
            resultRecordingIdempotentReplayValidator
        );
        doReturn(true)
            .when(resultRecordingIdempotentReplayValidator)
            .isIdenticalReplay(any(Result.class), any(Result.class));

        Long replayedResultId = resultRecordingService.recordResult(fixture.attemptId());
        var persistedAfterReplay = resultRepository.findByTestAttemptId(fixture.attemptId()).orElseThrow();

        assertThat(replayedResultId).isEqualTo(firstResultId);
        assertThat(persistedBeforeReplay.getId()).isEqualTo(firstResultId);
        assertThat(persistedBeforeReplay.getScoringPolicyCode()).isEqualTo("STANDARD");
        assertThat(persistedBeforeReplay.getEarnedScore()).isEqualByComparingTo("2.0000");
        assertThat(persistedBeforeReplay.getMaxScore()).isEqualByComparingTo("2.0000");
        assertThat(persistedBeforeReplay.getScorePercent()).isEqualByComparingTo("100.0000");
        assertThat(persistedBeforeReplay.isPassed()).isTrue();
        assertThat(persistedBeforeReplay.getCountedInAssignment()).isTrue();
        assertThat(persistedBeforeReplay.getAssignmentId()).isEqualTo(fixture.assignmentId());
        assertThat(persistedBeforeReplay.getAssignmentTestId()).isEqualTo(fixture.assignmentTestId());
        assertThat(persistedAfterReplay.getId()).isEqualTo(persistedBeforeReplay.getId());
        assertThat(resultRepository.count()).isEqualTo(resultRowsBeforeReplay);
        assertThat(resultQuestionSnapshotJpaRepository.count()).isEqualTo(questionSnapshotsBeforeReplay);
        assertThat(resultAnswerOptionSnapshotJpaRepository.count()).isEqualTo(answerOptionSnapshotsBeforeReplay);
        verify(resultRecordingIdempotentReplayValidator).isIdenticalReplay(any(Result.class), any(Result.class));
        verify(resultQuestionSnapshotRepository, never()).saveResultQuestionSnapshot(any(ResultQuestionSnapshot.class));
        verify(
            resultAnswerOptionSnapshotRepository,
            never()
        ).saveResultAnswerOptionSnapshot(any(ResultAnswerOptionSnapshot.class));
        verifyNoMoreInteractions(assignmentCountedResultHandoffService);
    }

    @Test
    void questionSnapshotFailureRollsBackRootResult() {
        AssignedRecordingFixture fixture = createAssignedRecordingFixture();
        TestQuestion testQuestion = new TestQuestion(
            410L,
            fixture.testId(),
            910L,
            0,
            new BigDecimal("2.0000"),
            FIXED_INSTANT.minusSeconds(60),
            FIXED_INSTANT.minusSeconds(60)
        );
        Question question = new Question(
            910L,
            810L,
            "Which option is correct?",
            QuestionType.SINGLE_CHOICE,
            ContentStatus.PUBLISHED,
            0,
            FIXED_INSTANT.minusSeconds(120),
            FIXED_INSTANT.minusSeconds(120)
        );
        AnswerOption correctOption = new AnswerOption(
            920L,
            910L,
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
            930L,
            fixture.attemptId(),
            910L,
            FIXED_INSTANT.minusSeconds(30),
            FIXED_INSTANT.minusSeconds(30)
        );
        UserAnswerItem selectedItem = new UserAnswerItem(
            940L,
            930L,
            920L,
            null,
            null,
            null,
            FIXED_INSTANT.minusSeconds(20),
            FIXED_INSTANT.minusSeconds(20)
        );

        when(snapshotFactsProvider.provideSnapshotFacts(any(TestAttempt.class))).thenReturn(new ResultSnapshotFacts(
            fixture.assignmentId(),
            fixture.assignmentTestId(),
            fixture.testId(),
            "Test",
            new ResultScoringSnapshot(
                new BigDecimal("70.0000"),
                new BigDecimal("2.0000"),
                new BigDecimal("2.0000"),
                new BigDecimal("100.0000"),
                true,
                "DEFAULT_PARTIAL_CREDIT_V1",
                "{\"policy\":\"v1\"}"
            ),
            true,
            true,
            new ResultOrgContextSnapshot(501L, "/company/ops"),
            true,
            FIXED_INSTANT,
            frozenSubordinateSnapshotFacts(testQuestion, question, List.of(correctOption), List.of(selectedItem))
        ));
        when(testQuestionRepository.findTestQuestionsByTestId(fixture.testId())).thenReturn(List.of(testQuestion));
        when(questionRepository.findQuestionsByIds(List.of(910L))).thenReturn(List.of(question));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(910L)).thenReturn(List.of(correctOption));
        when(userAnswerRepository.findUserAnswersByTestAttemptId(fixture.attemptId())).thenReturn(List.of(userAnswer));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(930L)).thenReturn(List.of(selectedItem));
        resultQuestionSnapshotRepository.failWith(
            new PersistenceConstraintViolationException("Failed to persist result_question_snapshot")
        );

        assertThatThrownBy(() -> resultRecordingService.recordResult(fixture.attemptId()))
            .isInstanceOf(PersistenceConstraintViolationException.class)
            .hasMessageContaining("result_question_snapshot");

        assertThat(resultRepository.findByTestAttemptId(fixture.attemptId())).isEmpty();
        verify(resultQuestionSnapshotRepository).saveResultQuestionSnapshot(any(ResultQuestionSnapshot.class));
        verify(resultAnswerOptionSnapshotRepository, never()).saveResultAnswerOptionSnapshot(any());
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
    }

    @Test
    void answerOptionSnapshotFailureRollsBackRootResultAndQuestionSnapshots() {
        AssignedRecordingFixture fixture = createAssignedRecordingFixture();
        QuestionEntity persistedQuestion = questionJpaRepository.saveAndFlush(questionEntity(
            fixture.topicId(),
            "Which option is correct?",
            0
        ));
        AnswerOptionEntity persistedCorrectOption = answerOptionJpaRepository.saveAndFlush(answerOptionEntity(
            persistedQuestion.getId(),
            "Correct option",
            true,
            0
        ));
        AnswerOptionEntity persistedWrongOption = answerOptionJpaRepository.saveAndFlush(answerOptionEntity(
            persistedQuestion.getId(),
            "Wrong option",
            false,
            1
        ));
        TestQuestion testQuestion = new TestQuestion(
            411L,
            fixture.testId(),
            persistedQuestion.getId(),
            0,
            new BigDecimal("2.0000"),
            FIXED_INSTANT.minusSeconds(60),
            FIXED_INSTANT.minusSeconds(60)
        );
        Question question = new Question(
            persistedQuestion.getId(),
            fixture.topicId(),
            "Which option is correct?",
            QuestionType.SINGLE_CHOICE,
            ContentStatus.PUBLISHED,
            0,
            FIXED_INSTANT.minusSeconds(120),
            FIXED_INSTANT.minusSeconds(120)
        );
        AnswerOption correctOption = new AnswerOption(
            persistedCorrectOption.getId(),
            persistedQuestion.getId(),
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
            persistedWrongOption.getId(),
            persistedQuestion.getId(),
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
            931L,
            fixture.attemptId(),
            persistedQuestion.getId(),
            FIXED_INSTANT.minusSeconds(30),
            FIXED_INSTANT.minusSeconds(30)
        );
        UserAnswerItem selectedItem = new UserAnswerItem(
            941L,
            931L,
            persistedCorrectOption.getId(),
            null,
            null,
            null,
            FIXED_INSTANT.minusSeconds(20),
            FIXED_INSTANT.minusSeconds(20)
        );

        when(snapshotFactsProvider.provideSnapshotFacts(any(TestAttempt.class))).thenReturn(new ResultSnapshotFacts(
            fixture.assignmentId(),
            fixture.assignmentTestId(),
            fixture.testId(),
            "Test",
            new ResultScoringSnapshot(
                new BigDecimal("70.0000"),
                new BigDecimal("2.0000"),
                new BigDecimal("2.0000"),
                new BigDecimal("100.0000"),
                true,
                "DEFAULT_PARTIAL_CREDIT_V1",
                "{\"policy\":\"v1\"}"
            ),
            true,
            true,
            new ResultOrgContextSnapshot(501L, "/company/ops"),
            true,
            FIXED_INSTANT,
            frozenSubordinateSnapshotFacts(
                testQuestion,
                question,
                List.of(correctOption, wrongOption),
                List.of(selectedItem)
            )
        ));
        when(testQuestionRepository.findTestQuestionsByTestId(fixture.testId())).thenReturn(List.of(testQuestion));
        when(questionRepository.findQuestionsByIds(List.of(persistedQuestion.getId()))).thenReturn(List.of(question));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(persistedQuestion.getId()))
            .thenReturn(List.of(correctOption, wrongOption));
        when(userAnswerRepository.findUserAnswersByTestAttemptId(fixture.attemptId())).thenReturn(List.of(userAnswer));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(931L)).thenReturn(List.of(selectedItem));
        resultAnswerOptionSnapshotRepository.failOnFirstSave(
            new PersistenceConstraintViolationException("Failed to persist result_answer_option_snapshot")
        );

        assertThatThrownBy(() -> resultRecordingService.recordResult(fixture.attemptId()))
            .isInstanceOf(PersistenceConstraintViolationException.class)
            .hasMessageContaining("result_answer_option_snapshot");

        assertThat(resultRepository.findByTestAttemptId(fixture.attemptId())).isEmpty();
        assertThat(resultQuestionSnapshotJpaRepository.count()).isZero();
        assertThat(resultAnswerOptionSnapshotJpaRepository.count()).isZero();
        verify(resultQuestionSnapshotRepository).saveResultQuestionSnapshot(any(ResultQuestionSnapshot.class));
        verify(resultAnswerOptionSnapshotRepository).saveResultAnswerOptionSnapshot(any(ResultAnswerOptionSnapshot.class));
        assertThat(resultAnswerOptionSnapshotRepository.saveAttempts()).isEqualTo(1);
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
    }

    @Test
    void secondRecordingForSameAttemptKeepsOriginalResultRowUnchanged() {
        AssignedRecordingFixture fixture = createAssignedRecordingFixture();
        QuestionEntity persistedQuestion = questionJpaRepository.saveAndFlush(questionEntity(
            fixture.topicId(),
            "Which option is correct?",
            0
        ));
        AnswerOptionEntity persistedCorrectOption = answerOptionJpaRepository.saveAndFlush(answerOptionEntity(
            persistedQuestion.getId(),
            "Correct option",
            true,
            0
        ));
        AnswerOptionEntity persistedWrongOption = answerOptionJpaRepository.saveAndFlush(answerOptionEntity(
            persistedQuestion.getId(),
            "Wrong option",
            false,
            1
        ));
        TestQuestion testQuestion = new TestQuestion(
            412L,
            fixture.testId(),
            persistedQuestion.getId(),
            0,
            new BigDecimal("2.0000"),
            FIXED_INSTANT.minusSeconds(60),
            FIXED_INSTANT.minusSeconds(60)
        );
        Question question = new Question(
            persistedQuestion.getId(),
            fixture.topicId(),
            "Which option is correct?",
            QuestionType.SINGLE_CHOICE,
            ContentStatus.PUBLISHED,
            0,
            FIXED_INSTANT.minusSeconds(120),
            FIXED_INSTANT.minusSeconds(120)
        );
        AnswerOption correctOption = new AnswerOption(
            persistedCorrectOption.getId(),
            persistedQuestion.getId(),
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
            persistedWrongOption.getId(),
            persistedQuestion.getId(),
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
            932L,
            fixture.attemptId(),
            persistedQuestion.getId(),
            FIXED_INSTANT.minusSeconds(30),
            FIXED_INSTANT.minusSeconds(30)
        );
        UserAnswerItem selectedItem = new UserAnswerItem(
            942L,
            932L,
            persistedCorrectOption.getId(),
            null,
            null,
            null,
            FIXED_INSTANT.minusSeconds(20),
            FIXED_INSTANT.minusSeconds(20)
        );

        when(snapshotFactsProvider.provideSnapshotFacts(any(TestAttempt.class))).thenReturn(new ResultSnapshotFacts(
            fixture.assignmentId(),
            fixture.assignmentTestId(),
            fixture.testId(),
            "Test",
            new ResultScoringSnapshot(
                new BigDecimal("70.0000"),
                new BigDecimal("1.0000"),
                new BigDecimal("2.0000"),
                new BigDecimal("50.0000"),
                false,
                "DEFAULT_PARTIAL_CREDIT_V1",
                "{\"policy\":\"v1\"}"
            ),
            true,
            false,
            new ResultOrgContextSnapshot(501L, "/company/ops"),
            true,
            FIXED_INSTANT,
            frozenSubordinateSnapshotFacts(
                testQuestion,
                question,
                List.of(correctOption, wrongOption),
                List.of(selectedItem)
            )
        ));
        when(testQuestionRepository.findTestQuestionsByTestId(fixture.testId())).thenReturn(List.of(testQuestion));
        when(questionRepository.findQuestionsByIds(List.of(persistedQuestion.getId()))).thenReturn(List.of(question));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(persistedQuestion.getId()))
            .thenReturn(List.of(correctOption, wrongOption));
        when(userAnswerRepository.findUserAnswersByTestAttemptId(fixture.attemptId())).thenReturn(List.of(userAnswer));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(932L)).thenReturn(List.of(selectedItem));
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(903L);
        when(criticalCommandAuditSupport.buildAuditContext(any(), any(String.class), any()))
            .thenReturn(new AuditContext("{\"operationCode\":\"RESULT_RECORDING\"}"));

        Long firstResultId = resultRecordingService.recordResult(fixture.attemptId());
        var firstPersisted = resultRepository.findByTestAttemptId(fixture.attemptId()).orElseThrow();
        long resultRowsBeforeReplay = resultRepository.count();
        long questionSnapshotsBeforeReplay = resultQuestionSnapshotJpaRepository.count();
        long answerOptionSnapshotsBeforeReplay = resultAnswerOptionSnapshotJpaRepository.count();

        clearInvocations(
            snapshotFactsProvider,
            testQuestionRepository,
            questionRepository,
            answerOptionRepository,
            userAnswerRepository,
            userAnswerItemRepository,
            resultQuestionSnapshotRepository,
            resultAnswerOptionSnapshotRepository,
            resultRecordingIdempotentReplayValidator,
            assignmentCountedResultHandoffService,
            criticalCommandAuditSupport
        );
        doReturn(true)
            .when(resultRecordingIdempotentReplayValidator)
            .isIdenticalReplay(any(Result.class), any(Result.class));

        Long replayedResultId = resultRecordingService.recordResult(fixture.attemptId());
        var persistedAfterReplay = resultRepository.findByTestAttemptId(fixture.attemptId()).orElseThrow();

        assertThat(replayedResultId).isEqualTo(firstResultId);
        assertThat(persistedAfterReplay.getId()).isEqualTo(firstPersisted.getId());
        assertThat(persistedAfterReplay.getCreatedAt()).isEqualTo(firstPersisted.getCreatedAt());
        assertThat(persistedAfterReplay.getEarnedScore()).isEqualByComparingTo(firstPersisted.getEarnedScore());
        assertThat(persistedAfterReplay.getMaxScore()).isEqualByComparingTo(firstPersisted.getMaxScore());
        assertThat(resultRepository.count()).isEqualTo(resultRowsBeforeReplay);
        assertThat(resultQuestionSnapshotJpaRepository.count()).isEqualTo(questionSnapshotsBeforeReplay);
        assertThat(resultAnswerOptionSnapshotJpaRepository.count()).isEqualTo(answerOptionSnapshotsBeforeReplay);
        verify(resultQuestionSnapshotRepository, never()).saveResultQuestionSnapshot(any(ResultQuestionSnapshot.class));
        verify(
            resultAnswerOptionSnapshotRepository,
            never()
        ).saveResultAnswerOptionSnapshot(any(ResultAnswerOptionSnapshot.class));
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void differentSecondRecordingForSameAttemptFailsClosedAndKeepsOriginalResultRowUnchanged() {
        AssignedRecordingFixture fixture = createAssignedRecordingFixture();
        QuestionEntity persistedQuestion = questionJpaRepository.saveAndFlush(questionEntity(
            fixture.topicId(),
            "Which option is correct?",
            0
        ));
        AnswerOptionEntity persistedCorrectOption = answerOptionJpaRepository.saveAndFlush(answerOptionEntity(
            persistedQuestion.getId(),
            "Correct option",
            true,
            0
        ));
        AnswerOptionEntity persistedWrongOption = answerOptionJpaRepository.saveAndFlush(answerOptionEntity(
            persistedQuestion.getId(),
            "Wrong option",
            false,
            1
        ));
        TestQuestion testQuestion = new TestQuestion(
            413L,
            fixture.testId(),
            persistedQuestion.getId(),
            0,
            new BigDecimal("2.0000"),
            FIXED_INSTANT.minusSeconds(60),
            FIXED_INSTANT.minusSeconds(60)
        );
        Question question = new Question(
            persistedQuestion.getId(),
            fixture.topicId(),
            "Which option is correct?",
            QuestionType.SINGLE_CHOICE,
            ContentStatus.PUBLISHED,
            0,
            FIXED_INSTANT.minusSeconds(120),
            FIXED_INSTANT.minusSeconds(120)
        );
        AnswerOption correctOption = new AnswerOption(
            persistedCorrectOption.getId(),
            persistedQuestion.getId(),
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
            persistedWrongOption.getId(),
            persistedQuestion.getId(),
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
            933L,
            fixture.attemptId(),
            persistedQuestion.getId(),
            FIXED_INSTANT.minusSeconds(30),
            FIXED_INSTANT.minusSeconds(30)
        );
        UserAnswerItem selectedItem = new UserAnswerItem(
            943L,
            933L,
            persistedCorrectOption.getId(),
            null,
            null,
            null,
            FIXED_INSTANT.minusSeconds(20),
            FIXED_INSTANT.minusSeconds(20)
        );

        ResultSnapshotFacts originalFacts = new ResultSnapshotFacts(
            fixture.assignmentId(),
            fixture.assignmentTestId(),
            fixture.testId(),
            "Test",
            new ResultScoringSnapshot(
                new BigDecimal("70.0000"),
                new BigDecimal("1.0000"),
                new BigDecimal("2.0000"),
                new BigDecimal("50.0000"),
                false,
                "DEFAULT_PARTIAL_CREDIT_V1",
                "{\"policy\":\"v1\"}"
            ),
            true,
            false,
            new ResultOrgContextSnapshot(501L, "/company/ops"),
            true,
            FIXED_INSTANT,
            frozenSubordinateSnapshotFacts(
                testQuestion,
                question,
                List.of(correctOption, wrongOption),
                List.of(selectedItem)
            )
        );
        ResultSnapshotFacts mismatchedReplayFacts = new ResultSnapshotFacts(
            fixture.assignmentId(),
            fixture.assignmentTestId(),
            fixture.testId(),
            "Test",
            new ResultScoringSnapshot(
                new BigDecimal("70.0000"),
                new BigDecimal("2.0000"),
                new BigDecimal("2.0000"),
                new BigDecimal("100.0000"),
                true,
                "DEFAULT_PARTIAL_CREDIT_V1",
                "{\"policy\":\"v2\"}"
            ),
            false,
            false,
            new ResultOrgContextSnapshot(501L, "/company/ops/changed"),
            true,
            FIXED_INSTANT.plusSeconds(30),
            frozenSubordinateSnapshotFacts(
                testQuestion,
                question,
                List.of(correctOption, wrongOption),
                List.of(selectedItem)
            )
        );

        when(snapshotFactsProvider.provideSnapshotFacts(any(TestAttempt.class))).thenReturn(originalFacts);
        when(testQuestionRepository.findTestQuestionsByTestId(fixture.testId())).thenReturn(List.of(testQuestion));
        when(questionRepository.findQuestionsByIds(List.of(persistedQuestion.getId()))).thenReturn(List.of(question));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(persistedQuestion.getId()))
            .thenReturn(List.of(correctOption, wrongOption));
        when(userAnswerRepository.findUserAnswersByTestAttemptId(fixture.attemptId())).thenReturn(List.of(userAnswer));
        when(userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(933L)).thenReturn(List.of(selectedItem));
        when(criticalCommandAuditSupport.resolveInteractiveActorUserId()).thenReturn(904L);
        when(criticalCommandAuditSupport.buildAuditContext(any(), any(String.class), any()))
            .thenReturn(new AuditContext("{\"operationCode\":\"RESULT_RECORDING\"}"));

        Long firstResultId = resultRecordingService.recordResult(fixture.attemptId());
        var firstPersisted = resultRepository.findByTestAttemptId(fixture.attemptId()).orElseThrow();
        long resultRowsBeforeReplay = resultRepository.count();
        long questionSnapshotsBeforeReplay = resultQuestionSnapshotJpaRepository.count();
        long answerOptionSnapshotsBeforeReplay = resultAnswerOptionSnapshotJpaRepository.count();

        clearInvocations(
            snapshotFactsProvider,
            testQuestionRepository,
            questionRepository,
            answerOptionRepository,
            userAnswerRepository,
            userAnswerItemRepository,
            resultQuestionSnapshotRepository,
            resultAnswerOptionSnapshotRepository,
            resultRecordingIdempotentReplayValidator,
            assignmentCountedResultHandoffService,
            criticalCommandAuditSupport
        );
        doReturn(mismatchedReplayFacts)
            .when(snapshotFactsProvider)
            .provideSnapshotFacts(any(TestAttempt.class));

        assertThatThrownBy(() -> resultRecordingService.recordResult(fixture.attemptId()))
            .isInstanceOf(com.vladislav.training.platform.common.exception.ConflictException.class)
            .hasMessageContaining("not idempotent")
            .hasMessageContaining(fixture.attemptId().toString());

        var persistedAfterReplay = resultRepository.findByTestAttemptId(fixture.attemptId()).orElseThrow();

        assertThat(firstResultId).isEqualTo(firstPersisted.getId());
        assertThat(persistedAfterReplay.getId()).isEqualTo(firstPersisted.getId());
        assertThat(persistedAfterReplay.getCreatedAt()).isEqualTo(firstPersisted.getCreatedAt());
        assertThat(persistedAfterReplay.getEarnedScore()).isEqualByComparingTo(firstPersisted.getEarnedScore());
        assertThat(persistedAfterReplay.getMaxScore()).isEqualByComparingTo(firstPersisted.getMaxScore());
        assertThat(resultRepository.count()).isEqualTo(resultRowsBeforeReplay);
        assertThat(resultQuestionSnapshotJpaRepository.count()).isEqualTo(questionSnapshotsBeforeReplay);
        assertThat(resultAnswerOptionSnapshotJpaRepository.count()).isEqualTo(answerOptionSnapshotsBeforeReplay);
        verify(resultRecordingIdempotentReplayValidator).isIdenticalReplay(any(Result.class), any(Result.class));
        verify(resultQuestionSnapshotRepository, never()).saveResultQuestionSnapshot(any(ResultQuestionSnapshot.class));
        verify(
            resultAnswerOptionSnapshotRepository,
            never()
        ).saveResultAnswerOptionSnapshot(any(ResultAnswerOptionSnapshot.class));
        verify(criticalCommandAuditSupport, never()).recordAudit(any(), any(), any(), any(), any(), any(), any());
        verify(assignmentCountedResultHandoffService, never()).acceptValidCountedAssignmentResult(any());
    }

    private AssignedRecordingFixture createAssignedRecordingFixture() {
        AppUserEntity user = appUserRepository.saveAndFlush(appUserEntity("EMP-RESULT-ROLLBACK"));
        CourseEntity course = courseRepository.saveAndFlush(courseEntity());
        TopicEntity topic = topicRepository.saveAndFlush(topicEntity(course.getId()));
        TestEntity test = testRepository.saveAndFlush(testEntity(topic.getId()));
        AssignmentCampaignEntity campaign = assignmentCampaignRepository.saveAndFlush(assignmentCampaignEntity());
        AssignmentEntity assignment = assignmentRepository.saveAndFlush(
            assignmentEntity(campaign.getId(), user.getId(), course.getId())
        );
        AssignmentTestEntity assignmentTest = assignmentTestRepository.saveAndFlush(
            assignmentTestEntity(assignment.getId(), test.getId())
        );
        TestAttemptEntity attempt = testAttemptRepository.saveAndFlush(
            testAttemptEntity(user.getId(), test.getId(), assignmentTest.getId())
        );
        return new AssignedRecordingFixture(
            assignment.getId(),
            assignmentTest.getId(),
            attempt.getId(),
            test.getId(),
            topic.getId()
        );
    }

    private ResultSubordinateSnapshotFacts frozenSubordinateSnapshotFacts(
        TestQuestion testQuestion,
        Question question,
        List<AnswerOption> answerOptions,
        List<UserAnswerItem> answerItems
    ) {
        return new ResultSubordinateSnapshotFacts(List.of(
            new ResultQuestionSnapshotFact(
                question.id(),
                question.body(),
                question.questionType(),
                testQuestion.displayOrder(),
                testQuestion.weight(),
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
        ));
    }

    private AppUserEntity appUserEntity(String employeeNumber) {
        AppUserEntity entity = instantiate(AppUserEntity.class);
        entity.setEmployeeNumber(employeeNumber);
        entity.setExternalId(null);
        entity.setLastName("User");
        entity.setFirstName(employeeNumber);
        entity.setMiddleName(null);
        entity.setStatus(UserStatus.ACTIVE);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private CourseEntity courseEntity() {
        CourseEntity entity = instantiate(CourseEntity.class);
        entity.setName("Course");
        entity.setDescription("Course");
        entity.setStatus(ContentStatus.PUBLISHED);
        entity.setSortOrder(0);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private TopicEntity topicEntity(Long courseId) {
        TopicEntity entity = instantiate(TopicEntity.class);
        entity.setCourseId(courseId);
        entity.setName("Topic");
        entity.setDescription("Topic");
        entity.setStatus(ContentStatus.PUBLISHED);
        entity.setSortOrder(0);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private TestEntity testEntity(Long topicId) {
        TestEntity entity = instantiate(TestEntity.class);
        entity.setTopicId(topicId);
        entity.setName("Test");
        entity.setDescription("Test");
        entity.setTestType(TestType.CONTROL);
        entity.setStatus(ContentStatus.PUBLISHED);
        entity.setThresholdPercent(new BigDecimal("70.0000"));
            entity.setScoringPolicyCode("DEFAULT_PARTIAL_CREDIT_V1");
        entity.setActiveFinalForTopic(true);
        entity.setSortOrder(0);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AssignmentCampaignEntity assignmentCampaignEntity() {
        AssignmentCampaignEntity entity = instantiate(AssignmentCampaignEntity.class);
        entity.setName("Campaign");
        entity.setDescription("Campaign");
        entity.setSourceType("ORG_UNIT");
        entity.setSourceRef("UNIT-RESULT");
        entity.setSourceNameSnapshot("Operations");
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AssignmentEntity assignmentEntity(Long campaignId, Long userId, Long courseId) {
        AssignmentEntity entity = instantiate(AssignmentEntity.class);
        entity.setCampaignId(campaignId);
        entity.setUserId(userId);
        entity.setCourseId(courseId);
        entity.setStatus(AssignmentStatus.ASSIGNED);
        entity.setAssignedAt(FIXED_INSTANT);
        entity.setDeadlineAt(FIXED_INSTANT.plusSeconds(86_400));
        entity.setCancelledAt(null);
        entity.setClosedAt(null);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AssignmentTestEntity assignmentTestEntity(Long assignmentId, Long testId) {
        AssignmentTestEntity entity = instantiate(AssignmentTestEntity.class);
        entity.setAssignmentId(assignmentId);
        entity.setTestId(testId);
        entity.setAssignmentTestRole(AssignmentTestRole.FINAL_TOPIC_CONTROL);
        entity.setCountedResultId(null);
        entity.setClosedAt(null);
        entity.setClosed(false);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private TestAttemptEntity testAttemptEntity(Long userId, Long testId, Long assignmentTestId) {
        TestAttemptEntity entity = instantiate(TestAttemptEntity.class);
        entity.setUserId(userId);
        entity.setTestId(testId);
        entity.setAssignmentTestId(assignmentTestId);
        entity.setAttemptMode(AttemptMode.ASSIGNED);
        entity.setStatus(TestAttemptStatus.COMPLETED);
        entity.setStartedAt(FIXED_INSTANT);
        entity.setCompletedAt(FIXED_INSTANT.plusSeconds(120));
        entity.setExpiredAt(null);
        entity.setAbandonedAt(null);
        entity.setLastActivityAt(FIXED_INSTANT.plusSeconds(60));
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private QuestionEntity questionEntity(Long topicId, String body, int sortOrder) {
        QuestionEntity entity = instantiate(QuestionEntity.class);
        entity.setTopicId(topicId);
        entity.setBody(body);
        entity.setQuestionType(QuestionType.SINGLE_CHOICE);
        entity.setStatus(ContentStatus.PUBLISHED);
        entity.setSortOrder(sortOrder);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AnswerOptionEntity answerOptionEntity(Long questionId, String body, boolean isCorrect, int displayOrder) {
        AnswerOptionEntity entity = instantiate(AnswerOptionEntity.class);
        entity.setQuestionId(questionId);
        entity.setBody(body);
        entity.setAnswerOptionRole(AnswerOptionRole.CHOICE_OPTION);
        entity.setIsCorrect(isCorrect);
        entity.setDisplayOrder(displayOrder);
        entity.setPairingKey(null);
        entity.setCanonicalOrderPosition(null);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private <T> T instantiate(Class<T> type) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to instantiate test entity: " + type.getName(), exception);
        }
    }

    private record AssignedRecordingFixture(
        Long assignmentId,
        Long assignmentTestId,
        Long attemptId,
        Long testId,
        Long topicId
    ) {
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.vladislav.training.platform")
    @EnableJpaRepositories(basePackages = "com.vladislav.training.platform")
    static class ResultRollbackTestApplication {

        @Bean
        ResultPersistenceMapper resultPersistenceMapper() {
            return new ResultPersistenceMapper();
        }

        @Bean
        TestingPersistenceMapper testingPersistenceMapper() {
            return new TestingPersistenceMapper();
        }

        @Bean
        ResultRepository resultRepository(
            SpringDataResultJpaRepository repository,
            ResultPersistenceMapper mapper
        ) {
            return new JpaResultRepositoryAdapter(repository, mapper);
        }

        @Bean
        TestAttemptRepository testAttemptRepository(
            SpringDataTestAttemptJpaRepository repository,
            TestingPersistenceMapper mapper
        ) {
            return new JpaTestAttemptRepositoryAdapter(repository, mapper);
        }

        @Bean
        CountedAssignmentResultValidityGate countedAssignmentResultValidityGate() {
            return new CountedAssignmentResultValidator();
        }

        @Bean
        ResultQuestionScoringEvaluator resultQuestionScoringEvaluator() {
            return new ResultQuestionScoringCalculator();
        }

        @Bean
        ResultRecordingIdempotentReplayValidator resultRecordingIdempotentReplayValidator() {
            return org.mockito.Mockito.spy(new ResultRecordingIdempotentReplayValidator());
        }

        @Bean
        ResultRecordingSnapshotFactsProvider snapshotFactsProvider() {
            return org.mockito.Mockito.mock(ResultRecordingSnapshotFactsProvider.class);
        }

        @Bean
        ControllableResultQuestionSnapshotRepository resultQuestionSnapshotRepository(
            SpringDataResultQuestionSnapshotJpaRepository repository,
            ResultPersistenceMapper mapper
        ) {
            return org.mockito.Mockito.spy(new ControllableResultQuestionSnapshotRepository(repository, mapper));
        }

        @Bean
        ControllableResultAnswerOptionSnapshotRepository resultAnswerOptionSnapshotRepository(
            SpringDataResultAnswerOptionSnapshotJpaRepository repository,
            ResultPersistenceMapper mapper
        ) {
            return org.mockito.Mockito.spy(new ControllableResultAnswerOptionSnapshotRepository(repository, mapper));
        }

        @Bean
        TestQuestionRepository testQuestionRepository() {
            return org.mockito.Mockito.mock(TestQuestionRepository.class);
        }

        @Bean
        QuestionRepository questionRepository() {
            return org.mockito.Mockito.mock(QuestionRepository.class);
        }

        @Bean
        AnswerOptionRepository answerOptionRepository() {
            return org.mockito.Mockito.mock(AnswerOptionRepository.class);
        }

        @Bean
        UserAnswerRepository userAnswerRepository() {
            return org.mockito.Mockito.mock(UserAnswerRepository.class);
        }

        @Bean
        UserAnswerItemRepository userAnswerItemRepository() {
            return org.mockito.Mockito.mock(UserAnswerItemRepository.class);
        }

        @Bean
        AssignmentCountedResultHandoffService assignmentCountedResultHandoffService() {
            return org.mockito.Mockito.mock(AssignmentCountedResultHandoffService.class);
        }

        @Bean
        CriticalCommandAuditSupport criticalCommandAuditSupport() {
            return org.mockito.Mockito.mock(CriticalCommandAuditSupport.class);
        }

        @Bean
        SystemActorResolver systemActorResolver() {
            return org.mockito.Mockito.mock(SystemActorResolver.class);
        }

        @Bean
        ResultRecordingSubordinateSnapshotMaterializer subordinateSnapshotMaterializer(
            ResultQuestionSnapshotRepository resultQuestionSnapshotRepository,
            ResultAnswerOptionSnapshotRepository resultAnswerOptionSnapshotRepository,
            TestQuestionRepository testQuestionRepository,
            QuestionRepository questionRepository,
            AnswerOptionRepository answerOptionRepository,
            UserAnswerRepository userAnswerRepository,
            UserAnswerItemRepository userAnswerItemRepository,
            ResultQuestionScoringEvaluator resultQuestionScoringEvaluator
        ) {
            return new ResultRecordingSubordinateSnapshotMaterializer(
                resultQuestionSnapshotRepository,
                resultAnswerOptionSnapshotRepository,
                testQuestionRepository,
                questionRepository,
                answerOptionRepository,
                userAnswerRepository,
                userAnswerItemRepository,
                resultQuestionScoringEvaluator
            );
        }

        @Bean
        ResultRecordingChildSnapshotCompletenessValidator childSnapshotCompletenessValidator(
            ResultQuestionSnapshotRepository resultQuestionSnapshotRepository,
            ResultAnswerOptionSnapshotRepository resultAnswerOptionSnapshotRepository,
            ResultRecordingSubordinateSnapshotMaterializer subordinateSnapshotMaterializer
        ) {
            return new ResultRecordingChildSnapshotCompletenessValidator(
                resultQuestionSnapshotRepository,
                resultAnswerOptionSnapshotRepository,
                subordinateSnapshotMaterializer
            );
        }

        @Bean
        ResultRecordingService resultRecordingService(
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
            return new ResultRecordingServiceImpl(
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
    }

    static class ControllableResultQuestionSnapshotRepository extends JpaResultQuestionSnapshotRepositoryAdapter {

        private RuntimeException failure;

        ControllableResultQuestionSnapshotRepository(
            SpringDataResultQuestionSnapshotJpaRepository repository,
            ResultPersistenceMapper mapper
        ) {
            super(repository, mapper);
        }

        void failWith(RuntimeException failure) {
            this.failure = failure;
        }

        void resetBehavior() {
            this.failure = null;
        }

        @Override
        public ResultQuestionSnapshot saveResultQuestionSnapshot(ResultQuestionSnapshot resultQuestionSnapshot) {
            if (failure != null) {
                throw failure;
            }
            return super.saveResultQuestionSnapshot(resultQuestionSnapshot);
        }
    }

    static class ControllableResultAnswerOptionSnapshotRepository implements ResultAnswerOptionSnapshotRepository {

        private final SpringDataResultAnswerOptionSnapshotJpaRepository repository;
        private final ResultPersistenceMapper mapper;
        private RuntimeException failureOnFirstSave;
        private int saveAttempts;

        ControllableResultAnswerOptionSnapshotRepository(
            SpringDataResultAnswerOptionSnapshotJpaRepository repository,
            ResultPersistenceMapper mapper
        ) {
            this.repository = repository;
            this.mapper = mapper;
        }

        void failOnFirstSave(RuntimeException failure) {
            this.failureOnFirstSave = failure;
        }

        void resetBehavior() {
            this.failureOnFirstSave = null;
            this.saveAttempts = 0;
        }

        int saveAttempts() {
            return saveAttempts;
        }

        @Override
        public ResultAnswerOptionSnapshot findResultAnswerOptionSnapshotById(Long resultAnswerOptionSnapshotId) {
            return repository.findById(resultAnswerOptionSnapshotId)
                .map(mapper::toDomain)
                .orElseThrow();
        }

        @Override
        public List<ResultAnswerOptionSnapshot> findResultAnswerOptionSnapshotsByResultQuestionSnapshotId(
            Long resultQuestionSnapshotId
        ) {
            return mapper.toResultAnswerOptionSnapshots(
                repository.findAllByResultQuestionSnapshotIdOrderByDisplayOrderAscIdAsc(resultQuestionSnapshotId)
            );
        }

        @Override
        public ResultAnswerOptionSnapshot saveResultAnswerOptionSnapshot(
            ResultAnswerOptionSnapshot resultAnswerOptionSnapshot
        ) {
            saveAttempts++;
            if (failureOnFirstSave != null && saveAttempts == 1) {
                throw failureOnFirstSave;
            }
            return mapper.toDomain(repository.save(mapper.toEntity(resultAnswerOptionSnapshot)));
        }
    }
}

