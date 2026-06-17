package com.vladislav.training.platform.result.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.content.domain.AnswerOption;
import com.vladislav.training.platform.content.domain.AnswerOptionRole;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Question;
import com.vladislav.training.platform.content.domain.QuestionType;
import com.vladislav.training.platform.testing.domain.UserAnswerItem;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code ResultQuestionScoringCalculator}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class ResultQuestionScoringCalculatorTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-21T12:00:00Z");
    private static final String DEFAULT = "DEFAULT";
    private static final String DEFAULT_PARTIAL_CREDIT_V1 = "DEFAULT_PARTIAL_CREDIT_V1";
    private static final String STANDARD = "STANDARD";

    private final ResultQuestionScoringCalculator evaluator = new ResultQuestionScoringCalculator();

    @Test
    void acceptsDefaultPartialCreditV1PolicyCode() {
        Question question = new Question(
            600L,
            101L,
            "Select the correct option",
            QuestionType.SINGLE_CHOICE,
            ContentStatus.PUBLISHED,
            0,
            FIXED_INSTANT.minusSeconds(60),
            FIXED_INSTANT.minusSeconds(60)
        );
        List<AnswerOption> answerOptions = List.of(
            choiceOption(700L, 600L, true, 0),
            choiceOption(701L, 600L, false, 1)
        );
        List<UserAnswerItem> answerItems = List.of(choiceAnswerItem(700L));

        ResultQuestionScoringEvaluator.QuestionScore score = evaluator.evaluateQuestion(
            question,
            answerOptions,
            answerItems,
            new BigDecimal("4.0000"),
            DEFAULT_PARTIAL_CREDIT_V1
        );

        assertThat(score.earnedScore()).isEqualByComparingTo("4.0000");
        assertThat(score.correct()).isTrue();
    }

    @Test
    void acceptsDefaultAliasPolicyCode() {
        Question question = new Question(
            600L,
            101L,
            "Select the correct option",
            QuestionType.SINGLE_CHOICE,
            ContentStatus.PUBLISHED,
            0,
            FIXED_INSTANT.minusSeconds(60),
            FIXED_INSTANT.minusSeconds(60)
        );
        List<AnswerOption> answerOptions = List.of(
            choiceOption(700L, 600L, true, 0),
            choiceOption(701L, 600L, false, 1)
        );
        List<UserAnswerItem> answerItems = List.of(choiceAnswerItem(700L));

        ResultQuestionScoringEvaluator.QuestionScore score = evaluator.evaluateQuestion(
            question,
            answerOptions,
            answerItems,
            new BigDecimal("4.0000"),
            DEFAULT
        );

        assertThat(score.earnedScore()).isEqualByComparingTo("4.0000");
        assertThat(score.correct()).isTrue();
    }

    @Test
    void acceptsStandardPolicyCode() {
        Question question = new Question(
            600L,
            101L,
            "Select the correct option",
            QuestionType.SINGLE_CHOICE,
            ContentStatus.PUBLISHED,
            0,
            FIXED_INSTANT.minusSeconds(60),
            FIXED_INSTANT.minusSeconds(60)
        );
        List<AnswerOption> answerOptions = List.of(
            choiceOption(700L, 600L, true, 0),
            choiceOption(701L, 600L, false, 1)
        );
        List<UserAnswerItem> answerItems = List.of(choiceAnswerItem(700L));

        ResultQuestionScoringEvaluator.QuestionScore score = evaluator.evaluateQuestion(
            question,
            answerOptions,
            answerItems,
            new BigDecimal("4.0000"),
            STANDARD
        );

        assertThat(score.earnedScore()).isEqualByComparingTo("4.0000");
        assertThat(score.correct()).isTrue();
    }

    @Test
    void rejectsUnknownScoringPolicyCodeFailClosed() {
        Question question = new Question(
            601L,
            101L,
            "Select all correct options",
            QuestionType.MULTIPLE_CHOICE,
            ContentStatus.PUBLISHED,
            0,
            FIXED_INSTANT.minusSeconds(60),
            FIXED_INSTANT.minusSeconds(60)
        );
        List<AnswerOption> answerOptions = List.of(
            choiceOption(701L, 601L, true, 0),
            choiceOption(702L, 601L, true, 1),
            choiceOption(703L, 601L, false, 2)
        );
        List<UserAnswerItem> answerItems = List.of(
            new UserAnswerItem(801L, 901L, 701L, null, null, 0, FIXED_INSTANT.minusSeconds(10), FIXED_INSTANT.minusSeconds(10))
        );

        assertThatThrownBy(() -> evaluator.evaluateQuestion(
            question,
            answerOptions,
            answerItems,
            new BigDecimal("12.0000"),
            "EXPERIMENTAL_UNKNOWN_POLICY"
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("canonical scoring policy support")
            .hasMessageContaining("EXPERIMENTAL_UNKNOWN_POLICY");
    }

    @Test
    void rejectsDuplicateChoiceAnswerOptionIdWithoutSilentDeduplication() {
        assertThatThrownBy(() -> evaluator.evaluateQuestion(
            choiceQuestion(601L, QuestionType.MULTIPLE_CHOICE),
            List.of(
                choiceOption(701L, 601L, true, 0),
                choiceOption(702L, 601L, false, 1)
            ),
            List.of(
                choiceAnswerItem(701L),
                choiceAnswerItem(701L)
            ),
            new BigDecimal("12.0000"),
            DEFAULT_PARTIAL_CREDIT_V1
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("duplicate persisted choice option");
    }

    @Test
    void rejectsDuplicateMatchingPairWithoutSilentMerge() {
        assertThatThrownBy(() -> evaluator.evaluateQuestion(
            choiceQuestion(602L, QuestionType.MATCHING),
            List.of(
                matchingLeftOption(711L, 602L, "A", 0),
                matchingRightOption(712L, 602L, "A", 1)
            ),
            List.of(
                matchingAnswerItem(711L, 712L),
                matchingAnswerItem(711L, 712L)
            ),
            new BigDecimal("5.0000"),
            DEFAULT_PARTIAL_CREDIT_V1
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("duplicate persisted matching pair");
    }

    @Test
    void rejectsDuplicateMatchingLeftWithoutSilentMerge() {
        assertThatThrownBy(() -> evaluator.evaluateQuestion(
            choiceQuestion(603L, QuestionType.MATCHING),
            List.of(
                matchingLeftOption(721L, 603L, "A", 0),
                matchingLeftOption(724L, 603L, "B", 1),
                matchingRightOption(722L, 603L, "A", 1),
                matchingRightOption(723L, 603L, "B", 2)
            ),
            List.of(
                matchingAnswerItem(721L, 722L),
                matchingAnswerItem(721L, 723L)
            ),
            new BigDecimal("5.0000"),
            DEFAULT_PARTIAL_CREDIT_V1
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("duplicate persisted matching left");
    }

    @Test
    void rejectsDuplicateMatchingRightWithoutSilentMerge() {
        assertThatThrownBy(() -> evaluator.evaluateQuestion(
            choiceQuestion(604L, QuestionType.MATCHING),
            List.of(
                matchingLeftOption(731L, 604L, "A", 0),
                matchingLeftOption(732L, 604L, "B", 1),
                matchingRightOption(733L, 604L, "A", 2),
                matchingRightOption(734L, 604L, "B", 3)
            ),
            List.of(
                matchingAnswerItem(731L, 733L),
                matchingAnswerItem(732L, 733L)
            ),
            new BigDecimal("5.0000"),
            DEFAULT_PARTIAL_CREDIT_V1
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("duplicate persisted matching right");
    }

    @Test
    void rejectsDuplicateOrderingAnswerOptionIdWithoutSilentMerge() {
        assertThatThrownBy(() -> evaluator.evaluateQuestion(
            choiceQuestion(605L, QuestionType.ORDERING),
            List.of(
                orderingOption(741L, 605L, 1, 0),
                orderingOption(742L, 605L, 2, 1)
            ),
            List.of(
                orderingAnswerItem(741L, 1),
                orderingAnswerItem(741L, 2)
            ),
            new BigDecimal("5.0000"),
            DEFAULT_PARTIAL_CREDIT_V1
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("duplicate persisted ordering option");
    }

    @Test
    void rejectsDuplicateOrderingPositionWithoutSilentMerge() {
        assertThatThrownBy(() -> evaluator.evaluateQuestion(
            choiceQuestion(606L, QuestionType.ORDERING),
            List.of(
                orderingOption(751L, 606L, 1, 0),
                orderingOption(752L, 606L, 2, 1)
            ),
            List.of(
                orderingAnswerItem(751L, 1),
                orderingAnswerItem(752L, 1)
            ),
            new BigDecimal("5.0000"),
            DEFAULT_PARTIAL_CREDIT_V1
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("duplicate persisted ordering position");
    }

    @Test
    void sourceGuardForbidsSilentMergeFallbacksInCanonicalScoringEvaluator() throws IOException {
        String source = Files.readString(Path.of(
            "src",
            "main",
            "java",
            "com",
            "vladislav",
            "training",
            "platform",
            "result",
            "service",
            "ResultQuestionScoringCalculator.java"
        ));

        assertThat(source).doesNotContain("(left, right) -> left");
        assertThat(source).doesNotContain("(left, right) -> right");
        assertThat(source).doesNotContain("Collectors.toCollection(HashSet::new)");
    }

    @Test
    void singleChoiceScoresFullOnlyForExactCorrectSelection() {
        Question question = choiceQuestion(610L, QuestionType.SINGLE_CHOICE);
        List<AnswerOption> answerOptions = List.of(
            choiceOption(710L, 610L, true, 0),
            choiceOption(711L, 610L, false, 1)
        );

        ResultQuestionScoringEvaluator.QuestionScore exactCorrect = evaluator.evaluateQuestion(
            question,
            answerOptions,
            List.of(choiceAnswerItem(710L)),
            new BigDecimal("4.0000"),
            DEFAULT_PARTIAL_CREDIT_V1
        );
        ResultQuestionScoringEvaluator.QuestionScore wrongSelection = evaluator.evaluateQuestion(
            question,
            answerOptions,
            List.of(choiceAnswerItem(711L)),
            new BigDecimal("4.0000"),
            DEFAULT_PARTIAL_CREDIT_V1
        );
        ResultQuestionScoringEvaluator.QuestionScore missingSelection = evaluator.evaluateQuestion(
            question,
            answerOptions,
            List.of(),
            new BigDecimal("4.0000"),
            DEFAULT_PARTIAL_CREDIT_V1
        );

        assertThat(exactCorrect.earnedScore()).isEqualByComparingTo("4.0000");
        assertThat(exactCorrect.correct()).isTrue();
        assertThat(wrongSelection.earnedScore()).isEqualByComparingTo("0.0000");
        assertThat(wrongSelection.correct()).isFalse();
        assertThat(missingSelection.earnedScore()).isEqualByComparingTo("0.0000");
        assertThat(missingSelection.correct()).isFalse();
    }

    @Test
    void multipleChoiceUsesPartialCreditWithIncorrectPenaltyAndZeroFloor() {
        Question question = choiceQuestion(611L, QuestionType.MULTIPLE_CHOICE);
        List<AnswerOption> answerOptions = List.of(
            choiceOption(721L, 611L, true, 0),
            choiceOption(722L, 611L, true, 1),
            choiceOption(723L, 611L, true, 2),
            choiceOption(724L, 611L, false, 3),
            choiceOption(725L, 611L, false, 4)
        );

        ResultQuestionScoringEvaluator.QuestionScore partialCredit = evaluator.evaluateQuestion(
            question,
            answerOptions,
            List.of(
                choiceAnswerItem(721L),
                choiceAnswerItem(722L),
                choiceAnswerItem(724L)
            ),
            new BigDecimal("12.0000"),
            DEFAULT_PARTIAL_CREDIT_V1
        );
        ResultQuestionScoringEvaluator.QuestionScore zeroFloor = evaluator.evaluateQuestion(
            question,
            answerOptions,
            List.of(
                choiceAnswerItem(721L),
                choiceAnswerItem(724L),
                choiceAnswerItem(725L)
            ),
            new BigDecimal("12.0000"),
            DEFAULT_PARTIAL_CREDIT_V1
        );

        assertThat(partialCredit.earnedScore()).isEqualByComparingTo("4.0000");
        assertThat(partialCredit.correct()).isFalse();
        assertThat(zeroFloor.earnedScore()).isEqualByComparingTo("0.0000");
        assertThat(zeroFloor.correct()).isFalse();
    }

    @Test
    void matchingUsesPartialCreditByCorrectPairs() {
        Question question = choiceQuestion(612L, QuestionType.MATCHING);
        List<AnswerOption> answerOptions = List.of(
            matchingLeftOption(731L, 612L, "A", 0),
            matchingLeftOption(732L, 612L, "B", 1),
            matchingLeftOption(733L, 612L, "C", 2),
            matchingLeftOption(734L, 612L, "D", 3),
            matchingRightOption(741L, 612L, "A", 4),
            matchingRightOption(742L, 612L, "B", 5),
            matchingRightOption(743L, 612L, "C", 6),
            matchingRightOption(744L, 612L, "D", 7)
        );

        ResultQuestionScoringEvaluator.QuestionScore score = evaluator.evaluateQuestion(
            question,
            answerOptions,
            List.of(
                matchingAnswerItem(731L, 741L),
                matchingAnswerItem(732L, 742L),
                matchingAnswerItem(733L, 744L),
                matchingAnswerItem(734L, 743L)
            ),
            new BigDecimal("12.0000"),
            DEFAULT_PARTIAL_CREDIT_V1
        );

        assertThat(score.earnedScore()).isEqualByComparingTo("6.0000");
        assertThat(score.correct()).isFalse();
    }

    @Test
    void orderingUsesPartialCreditByCorrectPositions() {
        Question question = choiceQuestion(613L, QuestionType.ORDERING);
        List<AnswerOption> answerOptions = List.of(
            orderingOption(751L, 613L, 1, 0),
            orderingOption(752L, 613L, 2, 1),
            orderingOption(753L, 613L, 3, 2),
            orderingOption(754L, 613L, 4, 3)
        );

        ResultQuestionScoringEvaluator.QuestionScore score = evaluator.evaluateQuestion(
            question,
            answerOptions,
            List.of(
                orderingAnswerItem(751L, 1),
                orderingAnswerItem(752L, 4),
                orderingAnswerItem(753L, 3),
                orderingAnswerItem(754L, 2)
            ),
            new BigDecimal("12.0000"),
            DEFAULT_PARTIAL_CREDIT_V1
        );

        assertThat(score.earnedScore()).isEqualByComparingTo("6.0000");
        assertThat(score.correct()).isFalse();
    }

    @Test
    void exactlyThresholdPercentIsPassed() {
        ResultQuestionScoringEvaluator.QuestionScore fullScore = evaluator.evaluateQuestion(
            choiceQuestion(614L, QuestionType.SINGLE_CHOICE),
            List.of(
                choiceOption(761L, 614L, true, 0),
                choiceOption(762L, 614L, false, 1)
            ),
            List.of(choiceAnswerItem(761L)),
            new BigDecimal("5.0000"),
            DEFAULT_PARTIAL_CREDIT_V1
        );
        ResultQuestionScoringEvaluator.QuestionScore zeroScore = evaluator.evaluateQuestion(
            choiceQuestion(615L, QuestionType.SINGLE_CHOICE),
            List.of(
                choiceOption(763L, 615L, true, 0),
                choiceOption(764L, 615L, false, 1)
            ),
            List.of(choiceAnswerItem(764L)),
            new BigDecimal("5.0000"),
            DEFAULT_PARTIAL_CREDIT_V1
        );

        assertThat(passesThreshold(
            sumScores(fullScore, zeroScore),
            new BigDecimal("10.0000"),
            new BigDecimal("50.00")
        )).isTrue();
    }

    @Test
    void belowThresholdPercentIsFailed() {
        ResultQuestionScoringEvaluator.QuestionScore partialScore = evaluator.evaluateQuestion(
            choiceQuestion(616L, QuestionType.MULTIPLE_CHOICE),
            List.of(
                choiceOption(771L, 616L, true, 0),
                choiceOption(772L, 616L, true, 1),
                choiceOption(773L, 616L, true, 2),
                choiceOption(774L, 616L, false, 3)
            ),
            List.of(
                choiceAnswerItem(771L),
                choiceAnswerItem(772L),
                choiceAnswerItem(774L)
            ),
            new BigDecimal("12.0000"),
            DEFAULT_PARTIAL_CREDIT_V1
        );

        assertThat(partialScore.earnedScore()).isEqualByComparingTo("4.0000");
        assertThat(passesThreshold(
            partialScore.earnedScore(),
            new BigDecimal("12.0000"),
            new BigDecimal("50.00")
        )).isFalse();
    }

    private BigDecimal sumScores(ResultQuestionScoringEvaluator.QuestionScore first, ResultQuestionScoringEvaluator.QuestionScore second) {
        return first.earnedScore().add(second.earnedScore()).setScale(4, RoundingMode.HALF_UP);
    }

    private boolean passesThreshold(BigDecimal earnedScore, BigDecimal maxScore, BigDecimal thresholdPercent) {
        BigDecimal scorePercent = earnedScore.multiply(new BigDecimal("100"))
            .divide(maxScore, 4, RoundingMode.HALF_UP);
        return scorePercent.compareTo(thresholdPercent.setScale(4, RoundingMode.HALF_UP)) >= 0;
    }

    private Question choiceQuestion(Long questionId, QuestionType questionType) {
        return new Question(
            questionId,
            101L,
            "Question " + questionId,
            questionType,
            ContentStatus.PUBLISHED,
            0,
            FIXED_INSTANT.minusSeconds(60),
            FIXED_INSTANT.minusSeconds(60)
        );
    }

    private UserAnswerItem choiceAnswerItem(Long answerOptionId) {
        return new UserAnswerItem(
            801L,
            901L,
            answerOptionId,
            null,
            null,
            null,
            FIXED_INSTANT.minusSeconds(10),
            FIXED_INSTANT.minusSeconds(10)
        );
    }

    private UserAnswerItem matchingAnswerItem(Long leftAnswerOptionId, Long rightAnswerOptionId) {
        return new UserAnswerItem(
            802L,
            902L,
            null,
            leftAnswerOptionId,
            rightAnswerOptionId,
            null,
            FIXED_INSTANT.minusSeconds(10),
            FIXED_INSTANT.minusSeconds(10)
        );
    }

    private UserAnswerItem orderingAnswerItem(Long answerOptionId, Integer userOrderPosition) {
        return new UserAnswerItem(
            803L,
            903L,
            answerOptionId,
            null,
            null,
            userOrderPosition,
            FIXED_INSTANT.minusSeconds(10),
            FIXED_INSTANT.minusSeconds(10)
        );
    }

    private AnswerOption choiceOption(Long id, Long questionId, boolean isCorrect, int displayOrder) {
        return new AnswerOption(
            id,
            questionId,
            "Option " + id,
            AnswerOptionRole.CHOICE_OPTION,
            isCorrect,
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

    private AnswerOption orderingOption(Long id, Long questionId, Integer canonicalOrderPosition, int displayOrder) {
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
}


