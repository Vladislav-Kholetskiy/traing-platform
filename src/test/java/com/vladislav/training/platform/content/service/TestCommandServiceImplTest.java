package com.vladislav.training.platform.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.application.policy.CapabilityOperationCode;
import com.vladislav.training.platform.application.policy.CapabilityTargetEntityType;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.ValidationException;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Question;
import com.vladislav.training.platform.content.domain.QuestionType;
import com.vladislav.training.platform.content.domain.TestQuestion;
import com.vladislav.training.platform.content.domain.TestType;
import com.vladislav.training.platform.content.repository.QuestionRepository;
import com.vladislav.training.platform.content.repository.TestQuestionRepository;
import com.vladislav.training.platform.content.repository.TestRepository;
import com.vladislav.training.platform.content.repository.TopicRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code TestCommandServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class TestCommandServiceImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-05T10:00:00Z");

    @Mock private TestRepository testRepository;
    @Mock private TestQuestionRepository testQuestionRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private ContentCommandSupport support;
    @Mock private CriticalCommandAuditSupport auditSupport;
    @Mock private UtcClock utcClock;

    @Test
    void createTestCreatesDraftUnderDraftTopic() {
        TestCommandServiceImpl service = new TestCommandServiceImpl(
            testRepository,
            testQuestionRepository,
            questionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        when(topicRepository.findTopicById(200L)).thenReturn(new com.vladislav.training.platform.content.domain.Topic(
            200L,
            20L,
            "Topic",
            null,
            ContentStatus.DRAFT,
            0,
            FIXED_INSTANT,
            FIXED_INSTANT
        ));
        when(testRepository.findTestsByTopicId(200L)).thenReturn(List.of());
        when(testRepository.saveTest(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        com.vladislav.training.platform.content.domain.Test result = service.createTest(new CreateTestCommand(
            200L,
            "Test",
            null,
            TestType.TRAINING,
            BigDecimal.valueOf(80),
            "DEFAULT",
            0
        ));

        assertThat(result.status()).isEqualTo(ContentStatus.DRAFT);
        assertThat(result.isActiveFinalForTopic()).isFalse();
        verify(testRepository).saveTest(any());
    }

    @Test
    void publishedTopicStillAllowsNewDraftTest() {
        TestCommandServiceImpl service = new TestCommandServiceImpl(
            testRepository,
            testQuestionRepository,
            questionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        com.vladislav.training.platform.content.domain.Topic parent = new com.vladislav.training.platform.content.domain.Topic(
            200L,
            20L,
            "Topic",
            null,
            ContentStatus.PUBLISHED,
            0,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
        com.vladislav.training.platform.content.domain.Test created = new com.vladislav.training.platform.content.domain.Test(
            100L,
            200L,
            "Test",
            null,
            TestType.TRAINING,
            ContentStatus.DRAFT,
            BigDecimal.valueOf(80),
            "DEFAULT",
            false,
            0,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
        when(topicRepository.findTopicById(200L)).thenReturn(parent);
        when(testRepository.findTestsByTopicId(200L)).thenReturn(List.of());
        when(testRepository.saveTest(any())).thenReturn(created);
        when(auditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        com.vladislav.training.platform.content.domain.Test result = service.createTest(new CreateTestCommand(
            200L,
            "Test",
            null,
            TestType.TRAINING,
            BigDecimal.valueOf(80),
            "DEFAULT",
            0
        ));

        assertThat(result.status()).isEqualTo(ContentStatus.DRAFT);
        assertThat(result.isActiveFinalForTopic()).isFalse();
        verify(support).checkChildRootCreate(
            CapabilityOperationCode.CONTENT_DRAFT_CREATE,
            CapabilityTargetEntityType.TEST,
            200L,
            CapabilityTargetEntityType.TOPIC
        );
        verify(support).validateParentForChildRootAuthoring(ContentStatus.PUBLISHED, "Topic");
        verify(testRepository).findTestsByTopicId(200L);
        verify(testRepository).saveTest(any());
    }

    @Test
    void createTestRejectsArchivedParentTopic() {
        TestCommandServiceImpl service = new TestCommandServiceImpl(
            testRepository,
            testQuestionRepository,
            questionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        when(topicRepository.findTopicById(200L)).thenReturn(new com.vladislav.training.platform.content.domain.Topic(
            200L,
            20L,
            "Topic",
            null,
            ContentStatus.ARCHIVED,
            0,
            FIXED_INSTANT,
            FIXED_INSTANT
        ));
        doCallRealMethod().when(support).validateParentForChildRootAuthoring(ContentStatus.ARCHIVED, "Topic");

        assertThatThrownBy(() -> service.createTest(new CreateTestCommand(
            200L,
            "Test",
            null,
            TestType.CONTROL,
            BigDecimal.valueOf(80),
            "DEFAULT",
            0
        )))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Topic ARCHIVED parent cannot accept child-root authoring");

        verify(testRepository, never()).saveTest(any());
    }

    @Test
    void createTestRejectsDuplicateSortOrderWithinTopic() {
        TestCommandServiceImpl service = new TestCommandServiceImpl(
            testRepository,
            testQuestionRepository,
            questionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        when(topicRepository.findTopicById(200L)).thenReturn(new com.vladislav.training.platform.content.domain.Topic(
            200L,
            20L,
            "Topic",
            null,
            ContentStatus.DRAFT,
            0,
            FIXED_INSTANT,
            FIXED_INSTANT
        ));
        when(testRepository.findTestsByTopicId(200L)).thenReturn(List.of(
            new com.vladislav.training.platform.content.domain.Test(
                101L,
                200L,
                "Existing",
                null,
                TestType.CONTROL,
                ContentStatus.DRAFT,
                BigDecimal.valueOf(80),
                "DEFAULT",
                false,
                1,
                FIXED_INSTANT,
                FIXED_INSTANT
            )
        ));

        assertThatThrownBy(() -> service.createTest(new CreateTestCommand(
            200L,
            "Test",
            null,
            TestType.CONTROL,
            BigDecimal.valueOf(80),
            "DEFAULT",
            1
        )))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("sortOrder")
            .hasMessageContaining("unique");

        verify(testRepository).findTestsByTopicId(200L);
        verify(testRepository, never()).saveTest(any());
    }

    @Test
    void createTestRejectsThresholdPercentOutOfRange() {
        TestCommandServiceImpl service = new TestCommandServiceImpl(
            testRepository,
            testQuestionRepository,
            questionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        when(topicRepository.findTopicById(200L)).thenReturn(new com.vladislav.training.platform.content.domain.Topic(
            200L,
            20L,
            "Topic",
            null,
            ContentStatus.DRAFT,
            0,
            FIXED_INSTANT,
            FIXED_INSTANT
        ));
        doThrow(new ValidationException("thresholdPercent must be between 0 and 100"))
            .when(support)
            .validateThreshold(BigDecimal.valueOf(101));

        assertThatThrownBy(() -> service.createTest(new CreateTestCommand(
            200L,
            "Test",
            null,
            TestType.CONTROL,
            BigDecimal.valueOf(101),
            "DEFAULT",
            0
        )))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("thresholdPercent")
            .hasMessageContaining("between 0 and 100");

        verify(testRepository, never()).saveTest(any());
    }

    @Test
    void updateTestRejectsDuplicateSortOrderWithinTopic() {
        TestCommandServiceImpl service = new TestCommandServiceImpl(
            testRepository,
            testQuestionRepository,
            questionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        com.vladislav.training.platform.content.domain.Test existing = test(100L, 200L, ContentStatus.DRAFT);
        com.vladislav.training.platform.content.domain.Test sibling = new com.vladislav.training.platform.content.domain.Test(
            101L,
            200L,
            "Other",
            null,
            TestType.CONTROL,
            ContentStatus.DRAFT,
            BigDecimal.valueOf(80),
            "DEFAULT",
            false,
            1,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
        when(testRepository.findTestById(100L)).thenReturn(existing);
        when(testRepository.findTestsByTopicId(200L)).thenReturn(List.of(existing, sibling));

        assertThatThrownBy(() -> service.updateTest(100L, new UpdateTestCommand(
            "Test",
            null,
            TestType.CONTROL,
            BigDecimal.valueOf(80),
            "DEFAULT",
            1
        )))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("sortOrder")
            .hasMessageContaining("unique");

        verify(testRepository).findTestsByTopicId(200L);
        verify(testRepository, never()).saveTest(any());
    }

    @Test
    void updateTestAllowsCurrentSortOrderWithoutSelfConflict() {
        TestCommandServiceImpl service = new TestCommandServiceImpl(
            testRepository,
            testQuestionRepository,
            questionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        com.vladislav.training.platform.content.domain.Test existing = test(100L, 200L, ContentStatus.DRAFT);
        com.vladislav.training.platform.content.domain.Test sibling = new com.vladislav.training.platform.content.domain.Test(
            101L,
            200L,
            "Other",
            null,
            TestType.CONTROL,
            ContentStatus.DRAFT,
            BigDecimal.valueOf(80),
            "DEFAULT",
            false,
            1,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
        com.vladislav.training.platform.content.domain.Test updated = test(100L, 200L, ContentStatus.DRAFT);
        when(testRepository.findTestById(100L)).thenReturn(existing);
        when(testRepository.findTestsByTopicId(200L)).thenReturn(List.of(existing, sibling));
        when(testRepository.saveTest(any())).thenReturn(updated);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.updateTest(100L, new UpdateTestCommand(
            "Test",
            null,
            TestType.CONTROL,
            BigDecimal.valueOf(80),
            "DEFAULT",
            0
        ));

        verify(testRepository).findTestsByTopicId(200L);
        verify(testRepository).saveTest(any());
    }

    @Test
    void updateTestUpdatesOnlyDraftTest() {
        TestCommandServiceImpl service = new TestCommandServiceImpl(
            testRepository,
            testQuestionRepository,
            questionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        com.vladislav.training.platform.content.domain.Test existing = test(100L, 200L, ContentStatus.DRAFT);
        when(testRepository.findTestById(100L)).thenReturn(existing);
        when(testRepository.findTestsByTopicId(200L)).thenReturn(List.of(existing));
        when(testRepository.saveTest(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        com.vladislav.training.platform.content.domain.Test result = service.updateTest(100L, new UpdateTestCommand(
            "Updated",
            null,
            TestType.CONTROL,
            BigDecimal.valueOf(80),
            "DEFAULT",
            1
        ));

        assertThat(result.status()).isEqualTo(ContentStatus.DRAFT);
        assertThat(result.isActiveFinalForTopic()).isFalse();
        assertThat(result.sortOrder()).isEqualTo(1);
        verify(testRepository).saveTest(any());
    }

    @Test
    void updateTestRejectsNonDraftTest() {
        TestCommandServiceImpl service = new TestCommandServiceImpl(
            testRepository,
            testQuestionRepository,
            questionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        com.vladislav.training.platform.content.domain.Test existing = test(100L, 200L, ContentStatus.PUBLISHED);
        when(testRepository.findTestById(100L)).thenReturn(existing);
        doCallRealMethod().when(support).requireDraft(ContentStatus.PUBLISHED, "Test");

        assertThatThrownBy(() -> service.updateTest(100L, new UpdateTestCommand(
            "Updated",
            null,
            TestType.CONTROL,
            BigDecimal.valueOf(80),
            "DEFAULT",
            1
        )))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Test must be DRAFT");

        verify(testRepository, never()).saveTest(any());
    }

    @Test
    void updateTestRejectsThresholdPercentOutOfRange() {
        TestCommandServiceImpl service = new TestCommandServiceImpl(
            testRepository,
            testQuestionRepository,
            questionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        when(testRepository.findTestById(100L)).thenReturn(test(100L, 200L, ContentStatus.DRAFT));
        doThrow(new ValidationException("thresholdPercent must be between 0 and 100"))
            .when(support)
            .validateThreshold(BigDecimal.valueOf(-1));

        assertThatThrownBy(() -> service.updateTest(100L, new UpdateTestCommand(
            "Test",
            null,
            TestType.CONTROL,
            BigDecimal.valueOf(-1),
            "DEFAULT",
            0
        )))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("thresholdPercent")
            .hasMessageContaining("between 0 and 100");

        verify(testRepository, never()).saveTest(any());
    }

    @Test
    void createTestQuestionRunsAdmissionOnParentTestContour() {
        TestCommandServiceImpl service = new TestCommandServiceImpl(
            testRepository,
            testQuestionRepository,
            questionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        com.vladislav.training.platform.content.domain.Test parentTest = test(100L, 200L, ContentStatus.DRAFT);
        Question question = new Question(300L, 200L, "Question", QuestionType.SINGLE_CHOICE, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);
        TestQuestion saved = new TestQuestion(400L, 100L, 300L, 0, BigDecimal.ONE, FIXED_INSTANT, FIXED_INSTANT);
        when(testRepository.findTestById(100L)).thenReturn(parentTest);
        when(questionRepository.findQuestionById(300L)).thenReturn(question);
        when(testQuestionRepository.findTestQuestionsByTestId(100L)).thenReturn(List.of());
        when(testQuestionRepository.saveTestQuestion(any(TestQuestion.class))).thenReturn(saved);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.createTestQuestion(100L, new CreateTestQuestionCommand(300L, 0, BigDecimal.ONE));

        verify(support).checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.TEST,
            100L,
            100L,
            CapabilityTargetEntityType.TEST
        );
        verify(testQuestionRepository).saveTestQuestion(any(TestQuestion.class));
        verify(testRepository, never()).saveTest(any());
    }

    @Test
    void createTestQuestionCreatesCompositionOnlyForDraftTest() {
        TestCommandServiceImpl service = new TestCommandServiceImpl(
            testRepository,
            testQuestionRepository,
            questionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        com.vladislav.training.platform.content.domain.Test parentTest = test(100L, 200L, ContentStatus.DRAFT);
        Question question = new Question(300L, 200L, "Question", QuestionType.SINGLE_CHOICE, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);
        when(testRepository.findTestById(100L)).thenReturn(parentTest);
        when(questionRepository.findQuestionById(300L)).thenReturn(question);
        when(testQuestionRepository.findTestQuestionsByTestId(100L)).thenReturn(List.of());
        when(testQuestionRepository.saveTestQuestion(any(TestQuestion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        TestQuestion result = service.createTestQuestion(100L, new CreateTestQuestionCommand(300L, 0, BigDecimal.ONE));

        assertThat(result.testId()).isEqualTo(100L);
        assertThat(result.questionId()).isEqualTo(300L);
        verify(testQuestionRepository).saveTestQuestion(any(TestQuestion.class));
        verify(testRepository, never()).saveTest(any());
    }

    @Test
    void createTestQuestionRejectsNonDraftTest() {
        TestCommandServiceImpl service = new TestCommandServiceImpl(
            testRepository,
            testQuestionRepository,
            questionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        com.vladislav.training.platform.content.domain.Test parentTest = test(100L, 200L, ContentStatus.PUBLISHED);
        when(testRepository.findTestById(100L)).thenReturn(parentTest);
        doCallRealMethod().when(support).requireDraft(ContentStatus.PUBLISHED, "Test");

        assertThatThrownBy(() -> service.createTestQuestion(100L, new CreateTestQuestionCommand(300L, 0, BigDecimal.ONE)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Test must be DRAFT");

        verify(questionRepository, never()).findQuestionById(any());
        verify(testQuestionRepository, never()).saveTestQuestion(any(TestQuestion.class));
    }

    @Test
    void createTestQuestionRejectsDuplicateDisplayOrderWithinTest() {
        TestCommandServiceImpl service = new TestCommandServiceImpl(
            testRepository,
            testQuestionRepository,
            questionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        com.vladislav.training.platform.content.domain.Test parentTest = test(100L, 200L, ContentStatus.DRAFT);
        Question question = new Question(300L, 200L, "Question", QuestionType.SINGLE_CHOICE, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);
        TestQuestion sibling = new TestQuestion(401L, 100L, 301L, 1, BigDecimal.ONE, FIXED_INSTANT, FIXED_INSTANT);
        when(testRepository.findTestById(100L)).thenReturn(parentTest);
        when(questionRepository.findQuestionById(300L)).thenReturn(question);
        when(testQuestionRepository.findTestQuestionsByTestId(100L)).thenReturn(List.of(sibling));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        assertThatThrownBy(() -> service.createTestQuestion(100L, new CreateTestQuestionCommand(300L, 1, BigDecimal.ONE)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("displayOrder")
            .hasMessageContaining("unique");

        verify(testQuestionRepository, never()).saveTestQuestion(any(TestQuestion.class));
    }

    @Test
    void createTestQuestionRejectsDuplicateQuestionWithinTest() {
        TestCommandServiceImpl service = new TestCommandServiceImpl(
            testRepository,
            testQuestionRepository,
            questionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        com.vladislav.training.platform.content.domain.Test parentTest = test(100L, 200L, ContentStatus.DRAFT);
        Question question = new Question(300L, 200L, "Question", QuestionType.SINGLE_CHOICE, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);
        TestQuestion sibling = new TestQuestion(401L, 100L, 300L, 0, BigDecimal.ONE, FIXED_INSTANT, FIXED_INSTANT);
        when(testRepository.findTestById(100L)).thenReturn(parentTest);
        when(questionRepository.findQuestionById(300L)).thenReturn(question);
        when(testQuestionRepository.findTestQuestionsByTestId(100L)).thenReturn(List.of(sibling));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        assertThatThrownBy(() -> service.createTestQuestion(100L, new CreateTestQuestionCommand(300L, 1, BigDecimal.ONE)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("questionId")
            .hasMessageContaining("unique");

        verify(testQuestionRepository, never()).saveTestQuestion(any(TestQuestion.class));
    }

    @Test
    void createTestQuestionRejectsQuestionFromAnotherTopic() {
        TestCommandServiceImpl service = new TestCommandServiceImpl(
            testRepository,
            testQuestionRepository,
            questionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        com.vladislav.training.platform.content.domain.Test parentTest = test(100L, 200L, ContentStatus.DRAFT);
        Question question = new Question(300L, 201L, "Question", QuestionType.SINGLE_CHOICE, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);
        when(testRepository.findTestById(100L)).thenReturn(parentTest);
        when(questionRepository.findQuestionById(300L)).thenReturn(question);

        assertThatThrownBy(() -> service.createTestQuestion(100L, new CreateTestQuestionCommand(300L, 0, BigDecimal.ONE)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("same topic");

        verify(testQuestionRepository, never()).saveTestQuestion(any(TestQuestion.class));
    }

    @Test
    void createTestQuestionRejectsNonPositiveWeight() {
        TestCommandServiceImpl service = new TestCommandServiceImpl(
            testRepository,
            testQuestionRepository,
            questionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        com.vladislav.training.platform.content.domain.Test parentTest = test(100L, 200L, ContentStatus.DRAFT);
        when(testRepository.findTestById(100L)).thenReturn(parentTest);
        doThrow(new ValidationException("weight must be positive"))
            .when(support)
            .validatePositive("weight", BigDecimal.ZERO);

        assertThatThrownBy(() -> service.createTestQuestion(100L, new CreateTestQuestionCommand(300L, 0, BigDecimal.ZERO)))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("weight")
            .hasMessageContaining("positive");

        verify(questionRepository, never()).findQuestionById(any());
        verify(testQuestionRepository, never()).saveTestQuestion(any(TestQuestion.class));
    }

    @Test
    void updateTestQuestionRejectsDuplicateDisplayOrderWithinTest() {
        TestCommandServiceImpl service = new TestCommandServiceImpl(
            testRepository,
            testQuestionRepository,
            questionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        TestQuestion existing = new TestQuestion(401L, 101L, 301L, 0, BigDecimal.ONE, FIXED_INSTANT, FIXED_INSTANT);
        TestQuestion sibling = new TestQuestion(402L, 101L, 302L, 1, BigDecimal.ONE, FIXED_INSTANT, FIXED_INSTANT);
        when(testQuestionRepository.findTestQuestionById(401L)).thenReturn(existing);
        when(testRepository.findTestById(101L)).thenReturn(test(101L, 201L, ContentStatus.DRAFT));
        when(questionRepository.findQuestionById(301L)).thenReturn(new Question(301L, 201L, "Question", QuestionType.SINGLE_CHOICE, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT));
        when(testQuestionRepository.findTestQuestionsByTestId(101L)).thenReturn(List.of(existing, sibling));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        assertThatThrownBy(() -> service.updateTestQuestion(101L, 401L, new UpdateTestQuestionCommand(301L, 1, BigDecimal.ONE)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("displayOrder")
            .hasMessageContaining("unique");

        verify(testQuestionRepository, never()).saveTestQuestion(any(TestQuestion.class));
    }

    @Test
    void updateTestQuestionRejectsDuplicateQuestionWithinTest() {
        TestCommandServiceImpl service = new TestCommandServiceImpl(
            testRepository,
            testQuestionRepository,
            questionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        TestQuestion existing = new TestQuestion(401L, 101L, 301L, 0, BigDecimal.ONE, FIXED_INSTANT, FIXED_INSTANT);
        TestQuestion sibling = new TestQuestion(402L, 101L, 302L, 1, BigDecimal.ONE, FIXED_INSTANT, FIXED_INSTANT);
        when(testQuestionRepository.findTestQuestionById(401L)).thenReturn(existing);
        when(testRepository.findTestById(101L)).thenReturn(test(101L, 201L, ContentStatus.DRAFT));
        when(questionRepository.findQuestionById(302L)).thenReturn(new Question(302L, 201L, "Question", QuestionType.SINGLE_CHOICE, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT));
        when(testQuestionRepository.findTestQuestionsByTestId(101L)).thenReturn(List.of(existing, sibling));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        assertThatThrownBy(() -> service.updateTestQuestion(101L, 401L, new UpdateTestQuestionCommand(302L, 0, BigDecimal.ONE)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("questionId")
            .hasMessageContaining("unique");

        verify(testQuestionRepository, never()).saveTestQuestion(any(TestQuestion.class));
    }

    @Test
    void updateTestQuestionRejectsQuestionFromAnotherTopic() {
        TestCommandServiceImpl service = new TestCommandServiceImpl(
            testRepository,
            testQuestionRepository,
            questionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        TestQuestion existing = new TestQuestion(401L, 101L, 301L, 0, BigDecimal.ONE, FIXED_INSTANT, FIXED_INSTANT);
        when(testQuestionRepository.findTestQuestionById(401L)).thenReturn(existing);
        when(testRepository.findTestById(101L)).thenReturn(test(101L, 201L, ContentStatus.DRAFT));
        when(questionRepository.findQuestionById(302L)).thenReturn(new Question(302L, 202L, "Question", QuestionType.SINGLE_CHOICE, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT));

        assertThatThrownBy(() -> service.updateTestQuestion(101L, 401L, new UpdateTestQuestionCommand(302L, 0, BigDecimal.ONE)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("same topic");

        verify(testQuestionRepository, never()).saveTestQuestion(any(TestQuestion.class));
    }

    @Test
    void updateTestQuestionRejectsNonPositiveWeight() {
        TestCommandServiceImpl service = new TestCommandServiceImpl(
            testRepository,
            testQuestionRepository,
            questionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        TestQuestion existing = new TestQuestion(401L, 101L, 301L, 0, BigDecimal.ONE, FIXED_INSTANT, FIXED_INSTANT);
        when(testQuestionRepository.findTestQuestionById(401L)).thenReturn(existing);
        when(testRepository.findTestById(101L)).thenReturn(test(101L, 201L, ContentStatus.DRAFT));
        doThrow(new ValidationException("weight must be positive"))
            .when(support)
            .validatePositive("weight", BigDecimal.ZERO);

        assertThatThrownBy(() -> service.updateTestQuestion(101L, 401L, new UpdateTestQuestionCommand(301L, 0, BigDecimal.ZERO)))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("weight")
            .hasMessageContaining("positive");

        verify(questionRepository, never()).findQuestionById(any());
        verify(testQuestionRepository, never()).saveTestQuestion(any(TestQuestion.class));
    }

    @Test
    void updateTestQuestionRejectsNonDraftTest() {
        TestCommandServiceImpl service = new TestCommandServiceImpl(
            testRepository,
            testQuestionRepository,
            questionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        TestQuestion existing = new TestQuestion(401L, 101L, 301L, 0, BigDecimal.ONE, FIXED_INSTANT, FIXED_INSTANT);
        when(testQuestionRepository.findTestQuestionById(401L)).thenReturn(existing);
        when(testRepository.findTestById(101L)).thenReturn(test(101L, 201L, ContentStatus.PUBLISHED));
        doCallRealMethod().when(support).requireDraft(ContentStatus.PUBLISHED, "Test");

        assertThatThrownBy(() -> service.updateTestQuestion(101L, 401L, new UpdateTestQuestionCommand(301L, 0, BigDecimal.ONE)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Test must be DRAFT");

        verify(questionRepository, never()).findQuestionById(any());
        verify(testQuestionRepository, never()).saveTestQuestion(any(TestQuestion.class));
    }

    @Test
    void updateTestQuestionAllowsCurrentQuestionAndDisplayOrderWithoutSelfConflict() {
        TestCommandServiceImpl service = new TestCommandServiceImpl(
            testRepository,
            testQuestionRepository,
            questionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        TestQuestion existing = new TestQuestion(401L, 101L, 301L, 0, BigDecimal.ONE, FIXED_INSTANT, FIXED_INSTANT);
        TestQuestion sibling = new TestQuestion(402L, 101L, 302L, 1, BigDecimal.ONE, FIXED_INSTANT, FIXED_INSTANT);
        TestQuestion updated = new TestQuestion(401L, 101L, 301L, 0, BigDecimal.ONE, FIXED_INSTANT, FIXED_INSTANT);
        when(testQuestionRepository.findTestQuestionById(401L)).thenReturn(existing);
        when(testRepository.findTestById(101L)).thenReturn(test(101L, 201L, ContentStatus.DRAFT));
        when(questionRepository.findQuestionById(301L)).thenReturn(new Question(301L, 201L, "Question", QuestionType.SINGLE_CHOICE, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT));
        when(testQuestionRepository.findTestQuestionsByTestId(101L)).thenReturn(List.of(existing, sibling));
        when(testQuestionRepository.saveTestQuestion(any(TestQuestion.class))).thenReturn(updated);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        service.updateTestQuestion(101L, 401L, new UpdateTestQuestionCommand(301L, 0, BigDecimal.ONE));

        verify(testQuestionRepository).saveTestQuestion(any(TestQuestion.class));
    }

    @Test
    void deleteTestQuestionRunsAdmissionBeforeDelete() {
        TestCommandServiceImpl service = new TestCommandServiceImpl(
            testRepository,
            testQuestionRepository,
            questionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        TestQuestion existing = new TestQuestion(401L, 101L, 301L, 0, BigDecimal.ONE, FIXED_INSTANT, FIXED_INSTANT);
        when(testQuestionRepository.findTestQuestionById(401L)).thenReturn(existing);
        when(testRepository.findTestById(101L)).thenReturn(test(101L, 201L, ContentStatus.DRAFT));

        service.deleteTestQuestion(101L, 401L);

        verify(support).checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.TEST,
            101L,
            101L,
            CapabilityTargetEntityType.TEST
        );
        verify(testQuestionRepository).deleteTestQuestion(401L);
        verify(testRepository, never()).saveTest(any());
    }

    @Test
    void deleteTestQuestionRejectsNonDraftTest() {
        TestCommandServiceImpl service = new TestCommandServiceImpl(
            testRepository,
            testQuestionRepository,
            questionRepository,
            topicRepository,
            support,
            auditSupport,
            utcClock
        );
        TestQuestion existing = new TestQuestion(401L, 101L, 301L, 0, BigDecimal.ONE, FIXED_INSTANT, FIXED_INSTANT);
        when(testQuestionRepository.findTestQuestionById(401L)).thenReturn(existing);
        when(testRepository.findTestById(101L)).thenReturn(test(101L, 201L, ContentStatus.PUBLISHED));
        doCallRealMethod().when(support).requireDraft(ContentStatus.PUBLISHED, "Test");

        assertThatThrownBy(() -> service.deleteTestQuestion(101L, 401L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Test must be DRAFT");

        verify(testQuestionRepository, never()).deleteTestQuestion(401L);
    }

    private com.vladislav.training.platform.content.domain.Test test(Long testId, Long topicId, ContentStatus status) {
        return new com.vladislav.training.platform.content.domain.Test(
            testId,
            topicId,
            "Test",
            null,
            TestType.CONTROL,
            status,
            BigDecimal.valueOf(80),
            "DEFAULT",
            false,
            0,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }
}
