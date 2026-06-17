package com.vladislav.training.platform.content.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.common.exception.ValidationException;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code ContentPublicationValidationServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class ContentPublicationValidationServiceImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-05T10:00:00Z");

    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private AnswerOptionRepository answerOptionRepository;
    @Mock
    private TestRepository testRepository;
    @Mock
    private TestQuestionRepository testQuestionRepository;

    @Test
    void publishSingleChoiceQuestionRejectsLessThanTwoOptions() {
        ContentPublicationValidationServiceImpl service = service();
        Question question = question(9L, QuestionType.SINGLE_CHOICE, ContentStatus.DRAFT);
        when(questionRepository.findQuestionById(9L)).thenReturn(question);
        when(answerOptionRepository.findAnswerOptionsByQuestionId(9L)).thenReturn(List.of(
            choiceOption(1L, 9L, true)
        ));

        assertThatThrownBy(() -> service.validateQuestionPublishable(9L))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("at least two choice options");
    }

    @Test
    void publishSingleChoiceQuestionRejectsNoCorrectOption() {
        ContentPublicationValidationServiceImpl service = service();
        Question question = question(10L, QuestionType.SINGLE_CHOICE, ContentStatus.DRAFT);
        when(questionRepository.findQuestionById(10L)).thenReturn(question);
        when(answerOptionRepository.findAnswerOptionsByQuestionId(10L)).thenReturn(List.of(
            choiceOption(1L, 10L, false),
            choiceOption(2L, 10L, false)
        ));

        assertThatThrownBy(() -> service.validateQuestionPublishable(10L))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("exactly one correct option");
    }

    @Test
    void publishSingleChoiceQuestionRejectsMoreThanOneCorrectOption() {
        ContentPublicationValidationServiceImpl service = service();
        Question question = question(11L, QuestionType.SINGLE_CHOICE, ContentStatus.DRAFT);
        when(questionRepository.findQuestionById(11L)).thenReturn(question);
        when(answerOptionRepository.findAnswerOptionsByQuestionId(11L)).thenReturn(List.of(
            choiceOption(1L, 11L, true),
            choiceOption(2L, 11L, true)
        ));

        assertThatThrownBy(() -> service.validateQuestionPublishable(11L))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("exactly one correct option");
    }

    @Test
    void publishSingleChoiceQuestionAcceptsExactlyOneCorrectOption() {
        ContentPublicationValidationServiceImpl service = service();
        Question question = question(12L, QuestionType.SINGLE_CHOICE, ContentStatus.DRAFT);
        when(questionRepository.findQuestionById(12L)).thenReturn(question);
        when(answerOptionRepository.findAnswerOptionsByQuestionId(12L)).thenReturn(List.of(
            choiceOption(1L, 12L, true),
            choiceOption(2L, 12L, false)
        ));

        assertThatCode(() -> service.validateQuestionPublishable(12L)).doesNotThrowAnyException();
    }

    @Test
    void publishSingleChoiceQuestionRejectsNonChoiceRole() {
        ContentPublicationValidationServiceImpl service = service();
        Question question = question(17L, QuestionType.SINGLE_CHOICE, ContentStatus.DRAFT);
        when(questionRepository.findQuestionById(17L)).thenReturn(question);
        when(answerOptionRepository.findAnswerOptionsByQuestionId(17L)).thenReturn(List.of(
            choiceOption(1L, 17L, true),
            matchingOption(2L, 17L, AnswerOptionRole.MATCH_LEFT, "A")
        ));

        assertThatThrownBy(() -> service.validateQuestionPublishable(17L))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("allows only CHOICE_OPTION role");
    }

    @Test
    void publishMultipleChoiceQuestionRejectsNoCorrectOption() {
        ContentPublicationValidationServiceImpl service = service();
        Question question = question(13L, QuestionType.MULTIPLE_CHOICE, ContentStatus.DRAFT);
        when(questionRepository.findQuestionById(13L)).thenReturn(question);
        when(answerOptionRepository.findAnswerOptionsByQuestionId(13L)).thenReturn(List.of(
            choiceOption(1L, 13L, false),
            choiceOption(2L, 13L, false)
        ));

        assertThatThrownBy(() -> service.validateQuestionPublishable(13L))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("one or more correct options");
    }

    @Test
    void publishMultipleChoiceQuestionRejectsLessThanTwoOptions() {
        ContentPublicationValidationServiceImpl service = service();
        Question question = question(18L, QuestionType.MULTIPLE_CHOICE, ContentStatus.DRAFT);
        when(questionRepository.findQuestionById(18L)).thenReturn(question);
        when(answerOptionRepository.findAnswerOptionsByQuestionId(18L)).thenReturn(List.of(
            choiceOption(1L, 18L, true)
        ));

        assertThatThrownBy(() -> service.validateQuestionPublishable(18L))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("at least two choice options");
    }

    @Test
    void publishMultipleChoiceQuestionAcceptsOneOrMoreCorrectOptions() {
        ContentPublicationValidationServiceImpl service = service();
        Question question = question(14L, QuestionType.MULTIPLE_CHOICE, ContentStatus.DRAFT);
        when(questionRepository.findQuestionById(14L)).thenReturn(question);
        when(answerOptionRepository.findAnswerOptionsByQuestionId(14L)).thenReturn(List.of(
            choiceOption(1L, 14L, true),
            choiceOption(2L, 14L, false),
            choiceOption(3L, 14L, true)
        ));

        assertThatCode(() -> service.validateQuestionPublishable(14L)).doesNotThrowAnyException();
    }

    @Test
    void publishMultipleChoiceQuestionRejectsNonChoiceRole() {
        ContentPublicationValidationServiceImpl service = service();
        Question question = question(19L, QuestionType.MULTIPLE_CHOICE, ContentStatus.DRAFT);
        when(questionRepository.findQuestionById(19L)).thenReturn(question);
        when(answerOptionRepository.findAnswerOptionsByQuestionId(19L)).thenReturn(List.of(
            choiceOption(1L, 19L, true),
            new AnswerOption(2L, 19L, "Wrong", AnswerOptionRole.ORDER_ITEM, null, 1, null, 0, FIXED_INSTANT, FIXED_INSTANT)
        ));

        assertThatThrownBy(() -> service.validateQuestionPublishable(19L))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("allows only CHOICE_OPTION role");
    }

    @Test
    void validateQuestionPublishableRejectsMatchingRoleMixing() {
        ContentPublicationValidationServiceImpl service = service();
        Question question = matchingQuestion(10L, ContentStatus.DRAFT);
        when(questionRepository.findQuestionById(10L)).thenReturn(question);
        when(answerOptionRepository.findAnswerOptionsByQuestionId(10L)).thenReturn(List.of(
            matchingOption(1L, 10L, AnswerOptionRole.MATCH_LEFT, "A"),
            matchingOption(2L, 10L, AnswerOptionRole.MATCH_RIGHT, "A"),
            new AnswerOption(3L, 10L, "Wrong", AnswerOptionRole.CHOICE_OPTION, Boolean.TRUE, 2, null, null, FIXED_INSTANT, FIXED_INSTANT)
        ));

        assertThatThrownBy(() -> service.validateQuestionPublishable(10L))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("MATCHING allows only MATCH_LEFT and MATCH_RIGHT roles");
    }

    @Test
    void publishMatchingQuestionRejectsMissingLeftSide() {
        ContentPublicationValidationServiceImpl service = service();
        Question question = matchingQuestion(22L, ContentStatus.DRAFT);
        when(questionRepository.findQuestionById(22L)).thenReturn(question);
        when(answerOptionRepository.findAnswerOptionsByQuestionId(22L)).thenReturn(List.of(
            matchingOption(1L, 22L, AnswerOptionRole.MATCH_RIGHT, "A")
        ));

        assertThatThrownBy(() -> service.validateQuestionPublishable(22L))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("non-empty left and right sets");
    }

    @Test
    void publishMatchingQuestionRejectsMissingRightSide() {
        ContentPublicationValidationServiceImpl service = service();
        Question question = matchingQuestion(23L, ContentStatus.DRAFT);
        when(questionRepository.findQuestionById(23L)).thenReturn(question);
        when(answerOptionRepository.findAnswerOptionsByQuestionId(23L)).thenReturn(List.of(
            matchingOption(1L, 23L, AnswerOptionRole.MATCH_LEFT, "A")
        ));

        assertThatThrownBy(() -> service.validateQuestionPublishable(23L))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("non-empty left and right sets");
    }

    @Test
    void publishMatchingQuestionRejectsInvalidPairingComposition() {
        ContentPublicationValidationServiceImpl service = service();
        Question question = matchingQuestion(15L, ContentStatus.DRAFT);
        when(questionRepository.findQuestionById(15L)).thenReturn(question);
        when(answerOptionRepository.findAnswerOptionsByQuestionId(15L)).thenReturn(List.of(
            matchingOption(1L, 15L, AnswerOptionRole.MATCH_LEFT, "A"),
            matchingOption(2L, 15L, AnswerOptionRole.MATCH_RIGHT, "B")
        ));

        assertThatThrownBy(() -> service.validateQuestionPublishable(15L))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("pairingKey sets");
    }

    @Test
    void publishMatchingQuestionRejectsBlankPairingKey() {
        ContentPublicationValidationServiceImpl service = service();
        Question question = matchingQuestion(24L, ContentStatus.DRAFT);
        when(questionRepository.findQuestionById(24L)).thenReturn(question);
        when(answerOptionRepository.findAnswerOptionsByQuestionId(24L)).thenReturn(List.of(
            matchingOption(1L, 24L, AnswerOptionRole.MATCH_LEFT, " "),
            matchingOption(2L, 24L, AnswerOptionRole.MATCH_RIGHT, "A")
        ));

        assertThatThrownBy(() -> service.validateQuestionPublishable(24L))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("non-blank pairingKey");
    }

    @Test
    void publishMatchingQuestionRejectsAmbiguousPairingComposition() {
        ContentPublicationValidationServiceImpl service = service();
        Question question = matchingQuestion(25L, ContentStatus.DRAFT);
        when(questionRepository.findQuestionById(25L)).thenReturn(question);
        when(answerOptionRepository.findAnswerOptionsByQuestionId(25L)).thenReturn(List.of(
            matchingOption(1L, 25L, AnswerOptionRole.MATCH_LEFT, "A"),
            matchingOption(2L, 25L, AnswerOptionRole.MATCH_LEFT, "A"),
            matchingOption(3L, 25L, AnswerOptionRole.MATCH_RIGHT, "A")
        ));

        assertThatThrownBy(() -> service.validateQuestionPublishable(25L))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("unambiguous one-to-one pairing semantics");
    }

    @Test
    void publishMatchingQuestionAcceptsValidPairingComposition() {
        ContentPublicationValidationServiceImpl service = service();
        Question question = matchingQuestion(16L, ContentStatus.DRAFT);
        when(questionRepository.findQuestionById(16L)).thenReturn(question);
        when(answerOptionRepository.findAnswerOptionsByQuestionId(16L)).thenReturn(List.of(
            matchingOption(1L, 16L, AnswerOptionRole.MATCH_LEFT, "A"),
            matchingOption(2L, 16L, AnswerOptionRole.MATCH_RIGHT, "A"),
            matchingOption(3L, 16L, AnswerOptionRole.MATCH_LEFT, "B"),
            matchingOption(4L, 16L, AnswerOptionRole.MATCH_RIGHT, "B")
        ));

        assertThatCode(() -> service.validateQuestionPublishable(16L)).doesNotThrowAnyException();
    }

    @Test
    void publishOrderingQuestionRejectsLessThanTwoItems() {
        ContentPublicationValidationServiceImpl service = service();
        Question question = question(26L, QuestionType.ORDERING, ContentStatus.DRAFT);
        when(questionRepository.findQuestionById(26L)).thenReturn(question);
        when(answerOptionRepository.findAnswerOptionsByQuestionId(26L)).thenReturn(List.of(
            orderingOption(1L, 26L, 0)
        ));

        assertThatThrownBy(() -> service.validateQuestionPublishable(26L))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("at least two items");
    }

    @Test
    void publishOrderingQuestionRejectsInvalidOrderingComposition() {
        ContentPublicationValidationServiceImpl service = service();
        Question question = question(20L, QuestionType.ORDERING, ContentStatus.DRAFT);
        when(questionRepository.findQuestionById(20L)).thenReturn(question);
        when(answerOptionRepository.findAnswerOptionsByQuestionId(20L)).thenReturn(List.of(
            orderingOption(1L, 20L, 0),
            orderingOption(2L, 20L, 2)
        ));

        assertThatThrownBy(() -> service.validateQuestionPublishable(20L))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("normalized canonical sequence");
    }

    @Test
    void publishOrderingQuestionRejectsMissingCanonicalOrderPosition() {
        ContentPublicationValidationServiceImpl service = service();
        Question question = question(27L, QuestionType.ORDERING, ContentStatus.DRAFT);
        when(questionRepository.findQuestionById(27L)).thenReturn(question);
        when(answerOptionRepository.findAnswerOptionsByQuestionId(27L)).thenReturn(List.of(
            new AnswerOption(1L, 27L, "Item1", AnswerOptionRole.ORDER_ITEM, null, 0, null, null, FIXED_INSTANT, FIXED_INSTANT),
            orderingOption(2L, 27L, 1)
        ));

        assertThatThrownBy(() -> service.validateQuestionPublishable(27L))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("requires canonicalOrderPosition");
    }

    @Test
    void publishOrderingQuestionRejectsDuplicateCanonicalPositions() {
        ContentPublicationValidationServiceImpl service = service();
        Question question = question(28L, QuestionType.ORDERING, ContentStatus.DRAFT);
        when(questionRepository.findQuestionById(28L)).thenReturn(question);
        when(answerOptionRepository.findAnswerOptionsByQuestionId(28L)).thenReturn(List.of(
            orderingOption(1L, 28L, 0),
            orderingOption(2L, 28L, 0)
        ));

        assertThatThrownBy(() -> service.validateQuestionPublishable(28L))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("canonical positions must be unique");
    }

    @Test
    void publishOrderingQuestionRejectsNonOrderingRole() {
        ContentPublicationValidationServiceImpl service = service();
        Question question = question(29L, QuestionType.ORDERING, ContentStatus.DRAFT);
        when(questionRepository.findQuestionById(29L)).thenReturn(question);
        when(answerOptionRepository.findAnswerOptionsByQuestionId(29L)).thenReturn(List.of(
            orderingOption(1L, 29L, 0),
            choiceOption(2L, 29L, false)
        ));

        assertThatThrownBy(() -> service.validateQuestionPublishable(29L))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("allows only ORDER_ITEM role");
    }

    @Test
    void publishOrderingQuestionAcceptsNormalizedOrderingComposition() {
        ContentPublicationValidationServiceImpl service = service();
        Question question = question(21L, QuestionType.ORDERING, ContentStatus.DRAFT);
        when(questionRepository.findQuestionById(21L)).thenReturn(question);
        when(answerOptionRepository.findAnswerOptionsByQuestionId(21L)).thenReturn(List.of(
            orderingOption(1L, 21L, 0),
            orderingOption(2L, 21L, 1),
            orderingOption(3L, 21L, 2)
        ));

        assertThatCode(() -> service.validateQuestionPublishable(21L)).doesNotThrowAnyException();
    }

    @Test
    void publishTestRejectsEmptyComposition() {
        ContentPublicationValidationServiceImpl service = service();
        com.vladislav.training.platform.content.domain.Test test = draftTest(30L, 300L);
        when(testRepository.findTestById(30L)).thenReturn(test);
        when(testQuestionRepository.findTestQuestionsByTestId(30L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.validateTestPublishable(30L))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("non-empty composition state");
    }

    @Test
    void publishTestRejectsDuplicateQuestionInLegacyComposition() {
        ContentPublicationValidationServiceImpl service = service();
        com.vladislav.training.platform.content.domain.Test test = draftTest(31L, 300L);
        TestQuestion first = new TestQuestion(51L, 31L, 41L, 0, BigDecimal.ONE, FIXED_INSTANT, FIXED_INSTANT);
        TestQuestion duplicate = new TestQuestion(52L, 31L, 41L, 1, BigDecimal.ONE, FIXED_INSTANT, FIXED_INSTANT);
        when(testRepository.findTestById(31L)).thenReturn(test);
        when(testQuestionRepository.findTestQuestionsByTestId(31L)).thenReturn(List.of(first, duplicate));

        assertThatThrownBy(() -> service.validateTestPublishable(31L))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Duplicate question inside test");
    }

    @Test
    void publishTestRejectsQuestionFromAnotherTopic() {
        ContentPublicationValidationServiceImpl service = service();
        com.vladislav.training.platform.content.domain.Test test = draftTest(32L, 300L);
        TestQuestion item = new TestQuestion(52L, 32L, 42L, 0, BigDecimal.ONE, FIXED_INSTANT, FIXED_INSTANT);
        Question otherTopicQuestion = question(42L, QuestionType.SINGLE_CHOICE, ContentStatus.PUBLISHED, 301L);
        when(testRepository.findTestById(32L)).thenReturn(test);
        when(testQuestionRepository.findTestQuestionsByTestId(32L)).thenReturn(List.of(item));
        when(questionRepository.findQuestionsByIds(java.util.Set.of(42L))).thenReturn(List.of(otherTopicQuestion));

        assertThatThrownBy(() -> service.validateTestPublishable(32L))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("same topic");
    }

    @Test
    void publishTestRejectsInvalidDisplayOrderComposition() {
        ContentPublicationValidationServiceImpl service = service();
        com.vladislav.training.platform.content.domain.Test test = draftTest(33L, 300L);
        TestQuestion first = new TestQuestion(53L, 33L, 43L, 0, BigDecimal.ONE, FIXED_INSTANT, FIXED_INSTANT);
        TestQuestion duplicateDisplayOrder = new TestQuestion(54L, 33L, 44L, 0, BigDecimal.ONE, FIXED_INSTANT, FIXED_INSTANT);
        when(testRepository.findTestById(33L)).thenReturn(test);
        when(testQuestionRepository.findTestQuestionsByTestId(33L)).thenReturn(List.of(first, duplicateDisplayOrder));

        assertThatThrownBy(() -> service.validateTestPublishable(33L))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("displayOrder must be unique");
    }

    @Test
    void publishTestRejectsInvalidWeightComposition() {
        ContentPublicationValidationServiceImpl service = service();
        com.vladislav.training.platform.content.domain.Test test = draftTest(34L, 300L);
        TestQuestion invalidWeight = org.mockito.Mockito.mock(TestQuestion.class);
        when(invalidWeight.weight()).thenReturn(BigDecimal.ZERO);
        when(testRepository.findTestById(34L)).thenReturn(test);
        when(testQuestionRepository.findTestQuestionsByTestId(34L)).thenReturn(List.of(invalidWeight));

        assertThatThrownBy(() -> service.validateTestPublishable(34L))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("weight must be positive");
    }

    @Test
    void validateTestPublishableRejectsDraftQuestionInComposition() {
        ContentPublicationValidationServiceImpl service = service();
        com.vladislav.training.platform.content.domain.Test test = draftTest(35L, 300L);
        Question question = matchingQuestion(46L, ContentStatus.DRAFT);
        TestQuestion item = new TestQuestion(56L, 35L, 46L, 0, BigDecimal.ONE, FIXED_INSTANT, FIXED_INSTANT);
        when(testRepository.findTestById(35L)).thenReturn(test);
        when(testQuestionRepository.findTestQuestionsByTestId(35L)).thenReturn(List.of(item));
        when(questionRepository.findQuestionsByIds(java.util.Set.of(46L))).thenReturn(List.of(question));
        assertThatThrownBy(() -> service.validateTestPublishable(35L))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("only PUBLISHED questions");
    }

    @Test
    void publishAllQuestionsTestRejectsEmptyCompositionEvenWithSemanticLabel() {
        ContentPublicationValidationServiceImpl service = service();
        com.vladislav.training.platform.content.domain.Test test = draftTest(37L, 300L, TestType.ALL_QUESTIONS);
        when(testRepository.findTestById(37L)).thenReturn(test);
        when(testQuestionRepository.findTestQuestionsByTestId(37L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.validateTestPublishable(37L))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("non-empty composition state");
    }

    @Test
    void publishTestAcceptsPublishableComposition() {
        ContentPublicationValidationServiceImpl service = service();
        com.vladislav.training.platform.content.domain.Test test = draftTest(36L, 300L);
        Question firstQuestion = matchingQuestion(47L, ContentStatus.PUBLISHED);
        Question secondQuestion = question(48L, QuestionType.ORDERING, ContentStatus.PUBLISHED);
        TestQuestion firstItem = new TestQuestion(57L, 36L, 47L, 0, BigDecimal.ONE, FIXED_INSTANT, FIXED_INSTANT);
        TestQuestion secondItem = new TestQuestion(58L, 36L, 48L, 1, BigDecimal.TWO, FIXED_INSTANT, FIXED_INSTANT);
        when(testRepository.findTestById(36L)).thenReturn(test);
        when(testQuestionRepository.findTestQuestionsByTestId(36L)).thenReturn(List.of(firstItem, secondItem));
        when(questionRepository.findQuestionsByIds(java.util.Set.of(47L, 48L))).thenReturn(List.of(firstQuestion, secondQuestion));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(47L)).thenReturn(List.of(
            matchingOption(1L, 47L, AnswerOptionRole.MATCH_LEFT, "A"),
            matchingOption(2L, 47L, AnswerOptionRole.MATCH_RIGHT, "A")
        ));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(48L)).thenReturn(List.of(
            orderingOption(3L, 48L, 0),
            orderingOption(4L, 48L, 1)
        ));

        assertThatCode(() -> service.validateTestPublishable(36L)).doesNotThrowAnyException();
    }

    @Test
    void publishAllQuestionsTestStillRequiresExplicitPublishableComposition() {
        ContentPublicationValidationServiceImpl service = service();
        com.vladislav.training.platform.content.domain.Test test = draftTest(38L, 300L, TestType.ALL_QUESTIONS);
        Question firstQuestion = question(49L, QuestionType.SINGLE_CHOICE, ContentStatus.PUBLISHED);
        Question secondQuestion = question(50L, QuestionType.MULTIPLE_CHOICE, ContentStatus.PUBLISHED);
        TestQuestion firstItem = new TestQuestion(59L, 38L, 49L, 0, BigDecimal.ONE, FIXED_INSTANT, FIXED_INSTANT);
        TestQuestion secondItem = new TestQuestion(60L, 38L, 50L, 1, BigDecimal.TWO, FIXED_INSTANT, FIXED_INSTANT);
        when(testRepository.findTestById(38L)).thenReturn(test);
        when(testQuestionRepository.findTestQuestionsByTestId(38L)).thenReturn(List.of(firstItem, secondItem));
        when(questionRepository.findQuestionsByIds(java.util.Set.of(49L, 50L))).thenReturn(List.of(firstQuestion, secondQuestion));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(49L)).thenReturn(List.of(
            choiceOption(1L, 49L, true),
            choiceOption(2L, 49L, false)
        ));
        when(answerOptionRepository.findAnswerOptionsByQuestionId(50L)).thenReturn(List.of(
            choiceOption(3L, 50L, true),
            choiceOption(4L, 50L, false)
        ));

        assertThatCode(() -> service.validateTestPublishable(38L)).doesNotThrowAnyException();
    }

    private ContentPublicationValidationServiceImpl service() {
        return new ContentPublicationValidationServiceImpl(questionRepository, answerOptionRepository, testRepository, testQuestionRepository);
    }

    private Question matchingQuestion(Long questionId, ContentStatus status) {
        return new Question(questionId, 300L, "Match", QuestionType.MATCHING, status, 0, FIXED_INSTANT, FIXED_INSTANT);
    }

    private Question question(Long questionId, QuestionType questionType, ContentStatus status) {
        return question(questionId, questionType, status, 300L);
    }

    private Question question(Long questionId, QuestionType questionType, ContentStatus status, Long topicId) {
        return new Question(questionId, topicId, questionType.name(), questionType, status, 0, FIXED_INSTANT, FIXED_INSTANT);
    }

    private com.vladislav.training.platform.content.domain.Test draftTest(Long testId, Long topicId) {
        return draftTest(testId, topicId, TestType.CONTROL);
    }

    private com.vladislav.training.platform.content.domain.Test draftTest(Long testId, Long topicId, TestType testType) {
        return new com.vladislav.training.platform.content.domain.Test(
            testId,
            topicId,
            "Control",
            null,
            testType,
            ContentStatus.DRAFT,
            BigDecimal.valueOf(80),
            "DEFAULT",
            false,
            0,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }

    private AnswerOption choiceOption(Long id, Long questionId, boolean isCorrect) {
        return new AnswerOption(
            id,
            questionId,
            "Choice" + id,
            AnswerOptionRole.CHOICE_OPTION,
            isCorrect,
            Math.toIntExact(id - 1),
            null,
            null,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }

    private AnswerOption matchingOption(Long id, Long questionId, AnswerOptionRole role, String key) {
        return new AnswerOption(id, questionId, role.name(), role, null, (int) (id - 1), key, null, FIXED_INSTANT, FIXED_INSTANT);
    }

    private AnswerOption orderingOption(Long id, Long questionId, Integer canonicalOrderPosition) {
        return new AnswerOption(id, questionId, "Item" + id, AnswerOptionRole.ORDER_ITEM, null, (int) (id - 1), null,
            canonicalOrderPosition, FIXED_INSTANT, FIXED_INSTANT);
    }
}
