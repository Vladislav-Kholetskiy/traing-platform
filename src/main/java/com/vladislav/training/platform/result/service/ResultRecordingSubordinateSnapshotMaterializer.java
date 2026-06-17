package com.vladislav.training.platform.result.service;

import com.vladislav.training.platform.common.exception.ConflictException;
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
import com.vladislav.training.platform.result.domain.ResultQuestionSnapshot;
import com.vladislav.training.platform.result.domain.ResultQuestionType;
import com.vladislav.training.platform.result.domain.ResultSnapshotFacts;
import com.vladislav.training.platform.result.domain.ResultSnapshotFacts.ResultAnswerOptionSnapshotFact;
import com.vladislav.training.platform.result.domain.ResultSnapshotFacts.ResultQuestionSnapshotFact;
import com.vladislav.training.platform.result.domain.ResultSnapshotFacts.ResultUserAnswerItemSnapshotFact;
import com.vladislav.training.platform.result.repository.ResultAnswerOptionSnapshotRepository;
import com.vladislav.training.platform.result.repository.ResultQuestionSnapshotRepository;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.UserAnswer;
import com.vladislav.training.platform.testing.domain.UserAnswerItem;
import com.vladislav.training.platform.testing.repository.UserAnswerItemRepository;
import com.vladislav.training.platform.testing.repository.UserAnswerRepository;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Класс {@code ResultRecordingSubordinateSnapshotMaterializer}.
 */
@Component
@Transactional
@ConditionalOnBean({
    ResultQuestionSnapshotRepository.class,
    ResultAnswerOptionSnapshotRepository.class,
    TestQuestionRepository.class,
    QuestionRepository.class,
    AnswerOptionRepository.class,
    UserAnswerRepository.class,
    UserAnswerItemRepository.class
})
class ResultRecordingSubordinateSnapshotMaterializer {

    private final ResultQuestionSnapshotRepository resultQuestionSnapshotRepository;
    private final ResultAnswerOptionSnapshotRepository resultAnswerOptionSnapshotRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final QuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final UserAnswerItemRepository userAnswerItemRepository;
    private final ResultQuestionScoringEvaluator resultQuestionScoringEvaluator;

    ResultRecordingSubordinateSnapshotMaterializer(
        ResultQuestionSnapshotRepository resultQuestionSnapshotRepository,
        ResultAnswerOptionSnapshotRepository resultAnswerOptionSnapshotRepository,
        TestQuestionRepository testQuestionRepository,
        QuestionRepository questionRepository,
        AnswerOptionRepository answerOptionRepository,
        UserAnswerRepository userAnswerRepository,
        UserAnswerItemRepository userAnswerItemRepository,
        ResultQuestionScoringEvaluator resultQuestionScoringEvaluator
    ) {
        this.resultQuestionSnapshotRepository = Objects.requireNonNull(
            resultQuestionSnapshotRepository,
            "resultQuestionSnapshotRepository must not be null"
        );
        this.resultAnswerOptionSnapshotRepository = Objects.requireNonNull(
            resultAnswerOptionSnapshotRepository,
            "resultAnswerOptionSnapshotRepository must not be null"
        );
        this.testQuestionRepository = Objects.requireNonNull(
            testQuestionRepository,
            "testQuestionRepository must not be null"
        );
        this.questionRepository = Objects.requireNonNull(questionRepository, "questionRepository must not be null");
        this.answerOptionRepository = Objects.requireNonNull(
            answerOptionRepository,
            "answerOptionRepository must not be null"
        );
        this.userAnswerRepository = Objects.requireNonNull(userAnswerRepository, "userAnswerRepository must not be null");
        this.userAnswerItemRepository = Objects.requireNonNull(
            userAnswerItemRepository,
            "userAnswerItemRepository must not be null"
        );
        this.resultQuestionScoringEvaluator = Objects.requireNonNull(
            resultQuestionScoringEvaluator,
            "resultQuestionScoringEvaluator must not be null"
        );
    }

    void materialize(Result recordedResult, TestAttempt terminalizedAttempt) {
        throw new ConflictException(
            "Result recording subordinate snapshots require frozen historical context facts"
        );
    }

    void materialize(Result recordedResult, TestAttempt terminalizedAttempt, ResultSnapshotFacts snapshotFacts) {
        Objects.requireNonNull(recordedResult, "recordedResult must not be null");
        Objects.requireNonNull(terminalizedAttempt, "terminalizedAttempt must not be null");
        Objects.requireNonNull(snapshotFacts, "snapshotFacts must not be null");
        if (recordedResult.id() == null) {
            throw new ConflictException("Result recording subordinate snapshots require persisted result id");
        }

        for (ExpectedQuestionSnapshotAggregate materialization : buildExpectedAggregate(recordedResult, snapshotFacts)
            .questionSnapshotAggregates()) {
            ResultQuestionSnapshot savedQuestionSnapshot = resultQuestionSnapshotRepository
                .saveResultQuestionSnapshot(materialization.questionSnapshot());
            Long resultQuestionSnapshotId = requireSavedQuestionSnapshotId(
                savedQuestionSnapshot,
                materialization.questionSnapshot().questionOriginalId()
            );
            for (ResultAnswerOptionSnapshot answerOptionSnapshot : materialization.answerOptionSnapshots()) {
                resultAnswerOptionSnapshotRepository.saveResultAnswerOptionSnapshot(
                    new ResultAnswerOptionSnapshot(
                        null,
                        resultQuestionSnapshotId,
                        answerOptionSnapshot.answerOptionOriginalId(),
                        answerOptionSnapshot.body(),
                        answerOptionSnapshot.displayOrder(),
                        answerOptionSnapshot.isCorrectAtSnapshot(),
                        answerOptionSnapshot.isSelectedByUser(),
                        answerOptionSnapshot.createdAt()
                    )
                );
            }
        }
    }

    ExpectedSubordinateSnapshotAggregate buildExpectedAggregate(
        Result recordedResult,
        ResultSnapshotFacts snapshotFacts
    ) {
        Objects.requireNonNull(recordedResult, "recordedResult must not be null");
        Objects.requireNonNull(snapshotFacts, "snapshotFacts must not be null");
        if (recordedResult.id() == null) {
            throw new ConflictException("Result recording subordinate snapshots require persisted result id");
        }
        List<ResultQuestionSnapshotFact> questionSnapshotFacts =
            snapshotFacts.subordinateSnapshotFacts().questionSnapshotFacts();
        if (questionSnapshotFacts.isEmpty()) {
            throw new ConflictException(
                "Result recording requires frozen subordinate snapshot facts: testAttemptId="
                    + recordedResult.testAttemptId()
            );
        }
        return new ExpectedSubordinateSnapshotAggregate(
            questionSnapshotFacts.stream()
                .map(questionSnapshotFact -> materializeQuestionSnapshot(recordedResult, questionSnapshotFact))
                .toList()
        );
    }

    private ExpectedQuestionSnapshotAggregate materializeQuestionSnapshot(
        Result recordedResult,
        ResultQuestionSnapshotFact questionSnapshotFact
    ) {
        Question question = toQuestion(questionSnapshotFact, recordedResult);
        List<AnswerOption> answerOptions = toAnswerOptions(questionSnapshotFact, recordedResult);
        List<UserAnswerItem> answerItems = toUserAnswerItems(questionSnapshotFact, recordedResult);
        BigDecimal questionWeight = questionSnapshotFact.weight().setScale(4);
        QuestionEvaluation evaluation = evaluateQuestion(
            question,
            answerOptions,
            answerItems,
            questionWeight,
            recordedResult.scoringSnapshot().scoringPolicyCode()
        );
        ResultQuestionSnapshot questionSnapshot = new ResultQuestionSnapshot(
            null,
            recordedResult.id(),
            questionSnapshotFact.questionOriginalId(),
            questionSnapshotFact.topicIdSnapshot(),
            questionSnapshotFact.body(),
            toResultQuestionType(questionSnapshotFact.questionType()),
            questionSnapshotFact.displayOrder(),
            questionWeight,
            evaluation.correctAnswerSnapshot(),
            evaluation.userAnswerSnapshot(),
            evaluation.earnedScore(),
            questionWeight,
            evaluation.correct(),
            evaluation.evaluationNote(),
            recordedResult.createdAt()
        );
        List<ResultAnswerOptionSnapshot> answerOptionSnapshots = evaluation.optionSnapshots().stream()
            .map(snapshot -> new ResultAnswerOptionSnapshot(
                null,
                -1L,
                snapshot.answerOptionOriginalId(),
                snapshot.body(),
                snapshot.displayOrder(),
                snapshot.correctAtSnapshot(),
                snapshot.selectedByUser(),
                recordedResult.createdAt()
            ))
            .toList();
        return new ExpectedQuestionSnapshotAggregate(questionSnapshot, answerOptionSnapshots);
    }

    private Question toQuestion(ResultQuestionSnapshotFact questionSnapshotFact, Result recordedResult) {
        return new Question(
            questionSnapshotFact.questionOriginalId(),
            -1L,
            questionSnapshotFact.body(),
            questionSnapshotFact.questionType(),
            ContentStatus.PUBLISHED,
            0,
            recordedResult.createdAt(),
            recordedResult.createdAt()
        );
    }

    private List<AnswerOption> toAnswerOptions(
        ResultQuestionSnapshotFact questionSnapshotFact,
        Result recordedResult
    ) {
        return questionSnapshotFact.answerOptionSnapshotFacts().stream()
            .sorted(Comparator
                .comparingInt(ResultAnswerOptionSnapshotFact::displayOrder)
                .thenComparing(ResultAnswerOptionSnapshotFact::answerOptionOriginalId))
            .map(answerOptionSnapshotFact -> new AnswerOption(
                answerOptionSnapshotFact.answerOptionOriginalId(),
                questionSnapshotFact.questionOriginalId(),
                answerOptionSnapshotFact.body(),
                answerOptionSnapshotFact.answerOptionRole(),
                answerOptionSnapshotFact.correctAtSource(),
                answerOptionSnapshotFact.displayOrder(),
                answerOptionSnapshotFact.pairingKey(),
                answerOptionSnapshotFact.canonicalOrderPosition(),
                recordedResult.createdAt(),
                recordedResult.createdAt()
            ))
            .toList();
    }

    private List<UserAnswerItem> toUserAnswerItems(
        ResultQuestionSnapshotFact questionSnapshotFact,
        Result recordedResult
    ) {
        return questionSnapshotFact.userAnswerItemSnapshotFacts().stream()
            .map(answerItemSnapshotFact -> new UserAnswerItem(
                null,
                -1L,
                answerItemSnapshotFact.answerOptionId(),
                answerItemSnapshotFact.leftAnswerOptionId(),
                answerItemSnapshotFact.rightAnswerOptionId(),
                answerItemSnapshotFact.userOrderPosition(),
                recordedResult.createdAt(),
                recordedResult.createdAt()
            ))
            .toList();
    }

    private QuestionEvaluation evaluateQuestion(
        Question question,
        List<AnswerOption> answerOptions,
        List<UserAnswerItem> answerItems,
        BigDecimal questionWeight,
        String scoringPolicyCode
    ) {
        List<UserAnswerItem> effectiveAnswerItems = answerItems == null ? List.of() : answerItems;
        ResultQuestionScoringEvaluator.QuestionScore score = resultQuestionScoringEvaluator.evaluateQuestion(
            question,
            answerOptions,
            effectiveAnswerItems,
            questionWeight,
            scoringPolicyCode
        );
        return switch (question.questionType()) {
            case SINGLE_CHOICE, MULTIPLE_CHOICE -> evaluateChoice(question, answerOptions, effectiveAnswerItems, score);
            case MATCHING -> evaluateMatching(question, answerOptions, effectiveAnswerItems, score);
            case ORDERING -> evaluateOrdering(question, answerOptions, effectiveAnswerItems, score);
        };
    }

    private QuestionEvaluation evaluateChoice(
        Question question,
        List<AnswerOption> answerOptions,
        List<UserAnswerItem> answerItems,
        ResultQuestionScoringEvaluator.QuestionScore score
    ) {
        List<AnswerOption> choiceOptions = requireOnlyRole(
            answerOptions,
            AnswerOptionRole.CHOICE_OPTION,
            "choice options",
            question.id()
        );
        List<Long> correctOptionIds = choiceOptions.stream()
            .filter(option -> Boolean.TRUE.equals(option.isCorrect()))
            .map(AnswerOption::id)
            .toList();
        if (correctOptionIds.isEmpty()) {
            throw new ConflictException("Для записи результата нужны корректно настроенные правильные варианты ответа: questionId=" + question.id());
        }

        List<Long> selectedOptionIds = answerItems.stream()
            .map(UserAnswerItem::answerOptionId)
            .map(answerOptionId -> requireBelongsToQuestion(answerOptionId, choiceOptions, question.id()))
            .distinct()
            .toList();

        List<OptionSnapshotDraft> optionSnapshots = choiceOptions.stream()
            .map(option -> new OptionSnapshotDraft(
                option.id(),
                option.body(),
                option.displayOrder(),
                Boolean.TRUE.equals(option.isCorrect()),
                selectedOptionIds.contains(option.id())
            ))
            .toList();
        return new QuestionEvaluation(
            score.earnedScore(),
            score.correct(),
            score.correct()
                ? null
                : null,
            "{\"type\":\"CHOICE\",\"correctOptionIds\":" + jsonLongArray(correctOptionIds) + "}",
            "{\"type\":\"CHOICE\",\"selectedOptionIds\":" + jsonLongArray(selectedOptionIds) + "}",
            optionSnapshots
        );
    }

    private QuestionEvaluation evaluateMatching(
        Question question,
        List<AnswerOption> answerOptions,
        List<UserAnswerItem> answerItems,
        ResultQuestionScoringEvaluator.QuestionScore score
    ) {
        List<AnswerOption> leftOptions = answerOptions.stream()
            .filter(option -> option.answerOptionRole() == AnswerOptionRole.MATCH_LEFT)
            .toList();
        List<AnswerOption> rightOptions = answerOptions.stream()
            .filter(option -> option.answerOptionRole() == AnswerOptionRole.MATCH_RIGHT)
            .toList();
        if (leftOptions.isEmpty() || rightOptions.isEmpty() || leftOptions.size() + rightOptions.size() != answerOptions.size()) {
            throw new ConflictException("Для записи результата нужны корректно настроенные пары для сопоставления: questionId=" + question.id());
        }

        Map<String, AnswerOption> leftByPairingKey = uniqueMatchingOptions(leftOptions, question.id(), "MATCH_LEFT");
        Map<String, AnswerOption> rightByPairingKey = uniqueMatchingOptions(rightOptions, question.id(), "MATCH_RIGHT");
        if (!leftByPairingKey.keySet().equals(rightByPairingKey.keySet())) {
            throw new ConflictException("Для записи результата сопоставление должно быть однозначным: questionId=" + question.id());
        }

        List<PairSnapshot> expectedPairs = leftOptions.stream()
            .map(left -> new PairSnapshot(left.id(), rightByPairingKey.get(left.pairingKey()).id()))
            .toList();
        Map<Long, Long> actualPairs = new LinkedHashMap<>();
        for (UserAnswerItem answerItem : answerItems) {
            Long leftId = requireBelongsToCollection(answerItem.leftAnswerOptionId(), leftOptions, question.id(), "MATCH_LEFT");
            Long rightId = requireBelongsToCollection(answerItem.rightAnswerOptionId(), rightOptions, question.id(), "MATCH_RIGHT");
            Long previous = actualPairs.put(leftId, rightId);
            if (previous != null) {
                throw new ConflictException("Result recording requires unique user matching left anchors: questionId=" + question.id());
            }
        }
        Set<Long> actualRightIds = new HashSet<>(actualPairs.values());
        if (actualRightIds.size() != actualPairs.size()) {
            throw new ConflictException("Result recording requires unique user matching right anchors: questionId=" + question.id());
        }

        List<OptionSnapshotDraft> optionSnapshots = answerOptions.stream()
            .map(option -> new OptionSnapshotDraft(
                option.id(),
                option.body(),
                option.displayOrder(),
                true,
                actualPairs.containsKey(option.id()) || actualRightIds.contains(option.id())
            ))
            .toList();
        return new QuestionEvaluation(
            score.earnedScore(),
            score.correct(),
            score.correct()
                ? null
                : "Пары сопоставления пользователя не совпадают с ожидаемыми",
            "{\"type\":\"MATCHING\",\"pairs\":" + jsonPairs(expectedPairs) + "}",
            "{\"type\":\"MATCHING\",\"pairs\":" + jsonPairs(actualPairs.entrySet().stream()
                .map(entry -> new PairSnapshot(entry.getKey(), entry.getValue()))
                .toList()) + "}",
            optionSnapshots
        );
    }

    private QuestionEvaluation evaluateOrdering(
        Question question,
        List<AnswerOption> answerOptions,
        List<UserAnswerItem> answerItems,
        ResultQuestionScoringEvaluator.QuestionScore score
    ) {
        List<AnswerOption> orderingOptions = requireOnlyRole(
            answerOptions,
            AnswerOptionRole.ORDER_ITEM,
            "ordering options",
            question.id()
        );
        List<OrderSnapshot> expectedOrdering = orderingOptions.stream()
            .map(option -> new OrderSnapshot(
                option.id(),
                requireCanonicalOrderPosition(option, question.id())
            ))
            .sorted(Comparator.comparingInt(OrderSnapshot::position).thenComparing(OrderSnapshot::answerOptionId))
            .toList();
        if (expectedOrdering.isEmpty()) {
            throw new ConflictException("Для записи результата нужны корректно настроенные элементы порядка: questionId=" + question.id());
        }

        Map<Long, Integer> actualOrderingByOptionId = new LinkedHashMap<>();
        for (UserAnswerItem answerItem : answerItems) {
            Long answerOptionId = requireBelongsToQuestion(answerItem.answerOptionId(), orderingOptions, question.id());
            if (answerItem.userOrderPosition() == null) {
                throw new ConflictException("Result recording requires user order positions: questionId=" + question.id());
            }
            Integer previous = actualOrderingByOptionId.put(answerOptionId, answerItem.userOrderPosition());
            if (previous != null) {
                throw new ConflictException("Result recording requires unique user ordering answers: questionId=" + question.id());
            }
        }
        Set<Integer> actualPositions = new HashSet<>(actualOrderingByOptionId.values());
        if (actualPositions.size() != actualOrderingByOptionId.size()) {
            throw new ConflictException("Result recording requires unique user ordering positions: questionId=" + question.id());
        }

        List<OrderSnapshot> actualOrdering = actualOrderingByOptionId.entrySet().stream()
            .map(entry -> new OrderSnapshot(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparingInt(OrderSnapshot::position).thenComparing(OrderSnapshot::answerOptionId))
            .toList();
        List<OptionSnapshotDraft> optionSnapshots = orderingOptions.stream()
            .map(option -> new OptionSnapshotDraft(
                option.id(),
                option.body(),
                option.displayOrder(),
                true,
                actualOrderingByOptionId.containsKey(option.id())
            ))
            .toList();
        return new QuestionEvaluation(
            score.earnedScore(),
            score.correct(),
            score.correct()
                ? null
                : "Порядок пользователя не совпадает с ожидаемым",
            "{\"type\":\"ORDERING\",\"items\":" + jsonOrdering(expectedOrdering) + "}",
            "{\"type\":\"ORDERING\",\"items\":" + jsonOrdering(actualOrdering) + "}",
            optionSnapshots
        );
    }

    private List<AnswerOption> requireOnlyRole(
        List<AnswerOption> answerOptions,
        AnswerOptionRole expectedRole,
        String description,
        Long questionId
    ) {
        List<AnswerOption> filtered = answerOptions.stream()
            .filter(option -> option.answerOptionRole() == expectedRole)
            .toList();
        if (filtered.isEmpty() || filtered.size() != answerOptions.size()) {
            throw new ConflictException(
                "Для записи результата нужны корректно настроенные данные: " + description + ", questionId=" + questionId
            );
        }
        return filtered;
    }

    private Map<String, AnswerOption> uniqueMatchingOptions(
        List<AnswerOption> options,
        Long questionId,
        String roleName
    ) {
        Map<String, AnswerOption> optionsByPairingKey = new LinkedHashMap<>();
        for (AnswerOption option : options) {
            if (option.pairingKey() == null || option.pairingKey().isBlank()) {
                throw new ConflictException(
                    "Result recording requires pairingKey for " + roleName + ": questionId=" + questionId
                );
            }
            AnswerOption previous = optionsByPairingKey.put(option.pairingKey(), option);
            if (previous != null) {
                throw new ConflictException(
                    "Result recording requires unique " + roleName + " pairingKey: questionId=" + questionId
                );
            }
        }
        return optionsByPairingKey;
    }

    private Long requireBelongsToQuestion(Long answerOptionId, List<AnswerOption> answerOptions, Long questionId) {
        return requireBelongsToCollection(answerOptionId, answerOptions, questionId, "answer option");
    }

    private Long requireBelongsToCollection(
        Long answerOptionId,
        Collection<AnswerOption> answerOptions,
        Long questionId,
        String description
    ) {
        if (answerOptionId == null) {
            throw new ConflictException(
                "Result recording requires non-null " + description + " reference: questionId=" + questionId
            );
        }
        boolean belongs = answerOptions.stream().anyMatch(option -> Objects.equals(option.id(), answerOptionId));
        if (!belongs) {
            throw new ConflictException(
                "Для вопроса указаны неверные связанные данные: " + description + ", questionId=" + questionId
            );
        }
        return answerOptionId;
    }

    private int requireCanonicalOrderPosition(AnswerOption option, Long questionId) {
        if (option.canonicalOrderPosition() == null) {
            throw new ConflictException(
                "Для элементов порядка не заданы позиции: questionId=" + questionId
            );
        }
        return option.canonicalOrderPosition();
    }

    private Long requireSavedQuestionSnapshotId(ResultQuestionSnapshot savedQuestionSnapshot, Long questionId) {
        if (savedQuestionSnapshot == null || savedQuestionSnapshot.id() == null) {
            throw new ConflictException(
                "Result recording requires persisted result_question_snapshot id: questionId=" + questionId
            );
        }
        return savedQuestionSnapshot.id();
    }

    private ResultQuestionType toResultQuestionType(QuestionType questionType) {
        return switch (questionType) {
            case SINGLE_CHOICE -> ResultQuestionType.SINGLE_CHOICE;
            case MULTIPLE_CHOICE -> ResultQuestionType.MULTIPLE_CHOICE;
            case MATCHING -> ResultQuestionType.MATCHING;
            case ORDERING -> ResultQuestionType.ORDERING;
        };
    }

    private String jsonLongArray(List<Long> values) {
        return values.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(",", "[", "]"));
    }

    private String jsonPairs(List<PairSnapshot> pairs) {
        return pairs.stream()
            .map(pair -> "{\"leftAnswerOptionId\":" + pair.leftAnswerOptionId()
                + ",\"rightAnswerOptionId\":" + pair.rightAnswerOptionId() + "}")
            .collect(Collectors.joining(",", "[", "]"));
    }

    private String jsonOrdering(List<OrderSnapshot> ordering) {
        return ordering.stream()
            .map(item -> "{\"answerOptionId\":" + item.answerOptionId() + ",\"position\":" + item.position() + "}")
            .collect(Collectors.joining(",", "[", "]"));
    }

    static record ExpectedSubordinateSnapshotAggregate(
        List<ExpectedQuestionSnapshotAggregate> questionSnapshotAggregates
    ) {
    }

    static record ExpectedQuestionSnapshotAggregate(
        ResultQuestionSnapshot questionSnapshot,
        List<ResultAnswerOptionSnapshot> answerOptionSnapshots
    ) {
    }

    private record QuestionEvaluation(
        BigDecimal earnedScore,
        boolean correct,
        String evaluationNote,
        String correctAnswerSnapshot,
        String userAnswerSnapshot,
        List<OptionSnapshotDraft> optionSnapshots
    ) {
    }

    private record OptionSnapshotDraft(
        Long answerOptionOriginalId,
        String body,
        int displayOrder,
        boolean correctAtSnapshot,
        boolean selectedByUser
    ) {
    }

    private record PairSnapshot(Long leftAnswerOptionId, Long rightAnswerOptionId) {
    }

    private record OrderSnapshot(Long answerOptionId, int position) {
    }
}
