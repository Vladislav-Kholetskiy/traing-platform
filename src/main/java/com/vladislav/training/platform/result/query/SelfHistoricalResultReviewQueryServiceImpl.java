package com.vladislav.training.platform.result.query;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContext;
import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadType;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.content.domain.AnswerOption;
import com.vladislav.training.platform.content.domain.AnswerOptionRole;
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
import com.vladislav.training.platform.result.repository.ResultAnswerOptionSnapshotRepository;
import com.vladislav.training.platform.result.repository.ResultQuestionSnapshotRepository;
import com.vladislav.training.platform.result.repository.ResultRepository;
import com.vladislav.training.platform.testing.domain.UserAnswer;
import com.vladislav.training.platform.testing.domain.UserAnswerItem;
import com.vladislav.training.platform.testing.repository.UserAnswerItemRepository;
import com.vladislav.training.platform.testing.repository.UserAnswerRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Реализация сервиса чтения {@code SelfHistoricalResultReviewQueryServiceImpl}.
 */

@Service
@Transactional(readOnly = true)
class SelfHistoricalResultReviewQueryServiceImpl implements SelfHistoricalResultReviewQueryService {

    private static final String CHOICE_SELECTION_MISMATCH_NOTE =
        "Р’С‹Р±СЂР°РЅРЅС‹Рµ РїРѕР»СЊР·РѕРІР°С‚РµР»РµРј РІР°СЂРёР°РЅС‚С‹ РЅРµ СЃРѕРІРїР°РґР°СЋС‚ СЃ РїСЂР°РІРёР»СЊРЅС‹Рј РЅР°Р±РѕСЂРѕРј РѕС‚РІРµС‚РѕРІ";

    private final ResultRepository resultRepository;
    private final ResultQuestionSnapshotRepository resultQuestionSnapshotRepository;
    private final ResultAnswerOptionSnapshotRepository resultAnswerOptionSnapshotRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final QuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final UserAnswerItemRepository userAnswerItemRepository;
    private final AccessSpecificationPolicy accessSpecificationPolicy;
    private final AccessPolicyQueryContextResolver contextResolver;

    SelfHistoricalResultReviewQueryServiceImpl(
        ResultRepository resultRepository,
        ResultQuestionSnapshotRepository resultQuestionSnapshotRepository,
        ResultAnswerOptionSnapshotRepository resultAnswerOptionSnapshotRepository,
        TestQuestionRepository testQuestionRepository,
        QuestionRepository questionRepository,
        AnswerOptionRepository answerOptionRepository,
        UserAnswerRepository userAnswerRepository,
        UserAnswerItemRepository userAnswerItemRepository,
        AccessSpecificationPolicy accessSpecificationPolicy,
        AccessPolicyQueryContextResolver contextResolver
    ) {
        this.resultRepository = Objects.requireNonNull(resultRepository, "resultRepository must not be null");
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
        this.accessSpecificationPolicy = Objects.requireNonNull(
            accessSpecificationPolicy,
            "accessSpecificationPolicy must not be null"
        );
        this.contextResolver = Objects.requireNonNull(contextResolver, "contextResolver must not be null");
    }

    @Override
    public SelfHistoricalResultReviewReadModel findSelfHistoricalResultReview(SelfHistoricalResultReviewQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        AccessPolicyQueryContext context = contextResolver.resolveActorSelfScope(
            AccessReadArea.SELF_RESULT_HISTORY,
            AccessReadType.DETAIL,
            "self_result_history"
        );
        requireResolvedContext(context);
        requireMatchingActor(query.actorUserId(), context);
        if (!accessSpecificationPolicy.canRead(context)) {
            throw new PolicyViolationException(
                "SELF_RESULT_HISTORY_DENIED",
                "Read access to self result history is denied"
            );
        }

        Result result = resultRepository.findResultById(query.resultId());
        if (!Objects.equals(result.userIdSnapshot(), query.actorUserId())) {
            throw new PolicyViolationException(
                "SELF_RESULT_HISTORY_DENIED",
                "Read access to self result history is denied because actor context does not match"
            );
        }

        List<SelfHistoricalResultReviewQuestionReadModel> questions =
            resultQuestionSnapshotRepository.findResultQuestionSnapshotsByResultId(result.id()).stream()
                .map(this::toQuestionReadModel)
                .toList();
        if (questions.isEmpty()) {
            questions = reconstructQuestionReadModels(result);
        }

        return new SelfHistoricalResultReviewReadModel(
            result.id(),
            result.completedAt(),
            result.testAttemptId(),
            result.testIdSnapshot(),
            result.testNameSnapshot(),
            result.scoringSnapshot().scorePercent(),
            result.scoringSnapshot().earnedScore(),
            result.scoringSnapshot().passed(),
            result.attemptMode(),
            result.assignmentId(),
            questions
        );
    }

    private void requireResolvedContext(AccessPolicyQueryContext context) {
        if (context == null) {
            throw new PolicyViolationException(
                "SELF_RESULT_HISTORY_DENIED",
                "Read access to self result history is denied because policy context is unavailable"
            );
        }
    }

    private void requireMatchingActor(Long actorUserId, AccessPolicyQueryContext context) {
        if (!Objects.equals(actorUserId, context.actorUserId())) {
            throw new PolicyViolationException(
                "SELF_RESULT_HISTORY_DENIED",
                "Read access to self result history is denied because actor context does not match"
            );
        }
    }

    private SelfHistoricalResultReviewQuestionReadModel toQuestionReadModel(ResultQuestionSnapshot questionSnapshot) {
        List<SelfHistoricalResultReviewOptionReadModel> options =
            resultAnswerOptionSnapshotRepository.findResultAnswerOptionSnapshotsByResultQuestionSnapshotId(
                questionSnapshot.id()
            ).stream()
                .map(this::toOptionReadModel)
                .toList();

        return new SelfHistoricalResultReviewQuestionReadModel(
            questionSnapshot.id(),
            questionSnapshot.questionOriginalId(),
            questionSnapshot.body(),
            questionSnapshot.questionType(),
            questionSnapshot.displayOrder(),
            questionSnapshot.earnedScore(),
            questionSnapshot.maxScore(),
            questionSnapshot.isCorrect(),
            sanitizeEvaluationNote(questionSnapshot.evaluationNote()),
            questionSnapshot.correctAnswerSnapshot(),
            questionSnapshot.userAnswerSnapshot(),
            options
        );
    }

    private String sanitizeEvaluationNote(String evaluationNote) {
        if (Objects.equals(evaluationNote, CHOICE_SELECTION_MISMATCH_NOTE)) {
            return null;
        }
        return evaluationNote;
    }

    private SelfHistoricalResultReviewOptionReadModel toOptionReadModel(ResultAnswerOptionSnapshot optionSnapshot) {
        return new SelfHistoricalResultReviewOptionReadModel(
            optionSnapshot.id(),
            optionSnapshot.answerOptionOriginalId(),
            optionSnapshot.body(),
            optionSnapshot.displayOrder(),
            optionSnapshot.isCorrectAtSnapshot(),
            optionSnapshot.isSelectedByUser()
        );
    }

    private List<SelfHistoricalResultReviewQuestionReadModel> reconstructQuestionReadModels(Result result) {
        List<TestQuestion> testQuestions = testQuestionRepository.findTestQuestionsByTestId(result.testIdSnapshot()).stream()
            .sorted(Comparator.comparingInt(TestQuestion::displayOrder).thenComparing(TestQuestion::id))
            .toList();
        if (testQuestions.isEmpty()) {
            return List.of();
        }

        Map<Long, Question> questionsById = questionRepository.findQuestionsByIds(
            testQuestions.stream().map(TestQuestion::questionId).toList()
        ).stream().collect(Collectors.toMap(Question::id, Function.identity()));

        Map<Long, UserAnswer> answersByQuestionId = userAnswerRepository.findUserAnswersByTestAttemptId(result.testAttemptId()).stream()
            .collect(Collectors.toMap(UserAnswer::questionId, Function.identity(), (left, right) -> right, LinkedHashMap::new));

        return testQuestions.stream()
            .map(testQuestion -> reconstructQuestionReadModel(testQuestion, questionsById.get(testQuestion.questionId()), answersByQuestionId))
            .filter(Objects::nonNull)
            .toList();
    }

    private SelfHistoricalResultReviewQuestionReadModel reconstructQuestionReadModel(
        TestQuestion testQuestion,
        Question question,
        Map<Long, UserAnswer> answersByQuestionId
    ) {
        if (question == null) {
            return null;
        }

        List<AnswerOption> answerOptions = answerOptionRepository.findAnswerOptionsByQuestionId(question.id()).stream()
            .sorted(Comparator.comparingInt(AnswerOption::displayOrder).thenComparing(AnswerOption::id))
            .toList();

        UserAnswer userAnswer = answersByQuestionId.get(question.id());
        List<UserAnswerItem> answerItems = userAnswer == null
            ? List.of()
            : userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(userAnswer.id());

        QuestionReviewFallback fallback = buildQuestionFallback(question, answerOptions, answerItems, testQuestion.weight());
        List<SelfHistoricalResultReviewOptionReadModel> optionReadModels = answerOptions.stream()
            .map(option -> new SelfHistoricalResultReviewOptionReadModel(
                null,
                option.id(),
                option.body(),
                option.displayOrder(),
                fallback.correctOptionIds().contains(option.id()),
                fallback.selectedOptionIds().contains(option.id())
            ))
            .toList();

        return new SelfHistoricalResultReviewQuestionReadModel(
            null,
            question.id(),
            question.body(),
            ResultQuestionType.valueOf(question.questionType().name()),
            testQuestion.displayOrder(),
            fallback.earnedScore(),
            testQuestion.weight(),
            fallback.correct(),
            fallback.evaluationNote(),
            fallback.correctAnswerSnapshot(),
            fallback.userAnswerSnapshot(),
            optionReadModels
        );
    }

    private QuestionReviewFallback buildQuestionFallback(
        Question question,
        List<AnswerOption> answerOptions,
        List<UserAnswerItem> answerItems,
        BigDecimal maxScore
    ) {
        return switch (question.questionType()) {
            case SINGLE_CHOICE, MULTIPLE_CHOICE -> buildChoiceFallback(question.questionType(), answerOptions, answerItems, maxScore);
            case MATCHING -> buildMatchingFallback(answerOptions, answerItems, maxScore);
            case ORDERING -> buildOrderingFallback(answerOptions, answerItems, maxScore);
        };
    }

    private QuestionReviewFallback buildChoiceFallback(
        QuestionType questionType,
        List<AnswerOption> answerOptions,
        List<UserAnswerItem> answerItems,
        BigDecimal maxScore
    ) {
        List<Long> correctOptionIds = answerOptions.stream()
            .filter(option -> option.answerOptionRole() == AnswerOptionRole.CHOICE_OPTION && Boolean.TRUE.equals(option.isCorrect()))
            .map(AnswerOption::id)
            .toList();
        if (correctOptionIds.isEmpty()) {
            return QuestionReviewFallback.empty();
        }
        List<Long> selectedOptionIds = answerItems.stream()
            .map(UserAnswerItem::answerOptionId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        boolean correct = Set.copyOf(selectedOptionIds).equals(Set.copyOf(correctOptionIds));
        BigDecimal earnedScore = questionType == QuestionType.SINGLE_CHOICE
            ? (correct ? maxScore : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
            : calculateMultipleChoiceScore(correctOptionIds, selectedOptionIds, maxScore);
        return new QuestionReviewFallback(
            earnedScore,
            correct,
            null,
            "{\"type\":\"CHOICE\",\"correctOptionIds\":" + jsonLongArray(correctOptionIds) + "}",
            "{\"type\":\"CHOICE\",\"selectedOptionIds\":" + jsonLongArray(selectedOptionIds) + "}",
            correctOptionIds,
            selectedOptionIds
        );
    }

    private BigDecimal calculateMultipleChoiceScore(
        List<Long> correctOptionIds,
        List<Long> selectedOptionIds,
        BigDecimal maxScore
    ) {
        long correctSelected = selectedOptionIds.stream().filter(correctOptionIds::contains).count();
        long incorrectSelected = selectedOptionIds.size() - correctSelected;
        BigDecimal rawRatio = BigDecimal.valueOf(correctSelected - incorrectSelected)
            .divide(BigDecimal.valueOf(correctOptionIds.size()), 8, RoundingMode.HALF_UP);
        return rawRatio.signum() <= 0
            ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
            : maxScore.multiply(rawRatio).setScale(4, RoundingMode.HALF_UP);
    }

    private QuestionReviewFallback buildMatchingFallback(
        List<AnswerOption> answerOptions,
        List<UserAnswerItem> answerItems,
        BigDecimal maxScore
    ) {
        Map<String, Long> leftByPairingKey = answerOptions.stream()
            .filter(option -> option.answerOptionRole() == AnswerOptionRole.MATCH_LEFT)
            .collect(Collectors.toMap(AnswerOption::pairingKey, AnswerOption::id, (left, right) -> left, LinkedHashMap::new));
        Map<String, Long> rightByPairingKey = answerOptions.stream()
            .filter(option -> option.answerOptionRole() == AnswerOptionRole.MATCH_RIGHT)
            .collect(Collectors.toMap(AnswerOption::pairingKey, AnswerOption::id, (left, right) -> left, LinkedHashMap::new));
        if (leftByPairingKey.isEmpty() || !leftByPairingKey.keySet().equals(rightByPairingKey.keySet())) {
            return QuestionReviewFallback.empty();
        }

        List<PairSnapshot> expectedPairs = leftByPairingKey.entrySet().stream()
            .map(entry -> new PairSnapshot(entry.getValue(), rightByPairingKey.get(entry.getKey())))
            .toList();
        Map<Long, Long> actualPairs = answerItems.stream()
            .filter(item -> item.leftAnswerOptionId() != null && item.rightAnswerOptionId() != null)
            .collect(Collectors.toMap(
                UserAnswerItem::leftAnswerOptionId,
                UserAnswerItem::rightAnswerOptionId,
                (left, right) -> right,
                LinkedHashMap::new
            ));
        boolean correct = actualPairs.equals(expectedPairs.stream().collect(Collectors.toMap(
            PairSnapshot::leftAnswerOptionId,
            PairSnapshot::rightAnswerOptionId,
            (left, right) -> left,
            LinkedHashMap::new
        )));
        long correctPairs = expectedPairs.stream()
            .filter(pair -> Objects.equals(actualPairs.get(pair.leftAnswerOptionId()), pair.rightAnswerOptionId()))
            .count();
        BigDecimal earnedScore = maxScore.multiply(
            BigDecimal.valueOf(correctPairs).divide(BigDecimal.valueOf(expectedPairs.size()), 8, RoundingMode.HALF_UP)
        ).setScale(4, RoundingMode.HALF_UP);
        List<Long> selectedOptionIds = answerOptions.stream()
            .map(AnswerOption::id)
            .filter(optionId -> actualPairs.containsKey(optionId) || actualPairs.containsValue(optionId))
            .toList();
        return new QuestionReviewFallback(
            earnedScore,
            correct,
            correct ? null : "Пары сопоставления пользователя не совпадают с ожидаемыми",
            "{\"type\":\"MATCHING\",\"pairs\":" + jsonPairs(expectedPairs) + "}",
            "{\"type\":\"MATCHING\",\"pairs\":" + jsonPairs(actualPairs.entrySet().stream()
                .map(entry -> new PairSnapshot(entry.getKey(), entry.getValue()))
                .toList()) + "}",
            answerOptions.stream().map(AnswerOption::id).toList(),
            selectedOptionIds
        );
    }

    private QuestionReviewFallback buildOrderingFallback(
        List<AnswerOption> answerOptions,
        List<UserAnswerItem> answerItems,
        BigDecimal maxScore
    ) {
        List<AnswerOption> orderingOptions = answerOptions.stream()
            .filter(option -> option.answerOptionRole() == AnswerOptionRole.ORDER_ITEM)
            .toList();
        if (orderingOptions.isEmpty()) {
            return QuestionReviewFallback.empty();
        }

        List<OrderSnapshot> expectedOrdering = orderingOptions.stream()
            .filter(option -> option.canonicalOrderPosition() != null)
            .map(option -> new OrderSnapshot(option.id(), option.canonicalOrderPosition()))
            .sorted(Comparator.comparingInt(OrderSnapshot::position).thenComparing(OrderSnapshot::answerOptionId))
            .toList();
        Map<Long, Integer> actualOrderingByOptionId = answerItems.stream()
            .filter(item -> item.answerOptionId() != null && item.userOrderPosition() != null)
            .collect(Collectors.toMap(
                UserAnswerItem::answerOptionId,
                UserAnswerItem::userOrderPosition,
                (left, right) -> right,
                LinkedHashMap::new
            ));
        List<OrderSnapshot> actualOrdering = actualOrderingByOptionId.entrySet().stream()
            .map(entry -> new OrderSnapshot(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparingInt(OrderSnapshot::position).thenComparing(OrderSnapshot::answerOptionId))
            .toList();
        boolean correct = actualOrdering.equals(expectedOrdering);
        long correctPositions = expectedOrdering.stream()
            .filter(item -> Objects.equals(actualOrderingByOptionId.get(item.answerOptionId()), item.position()))
            .count();
        BigDecimal earnedScore = maxScore.multiply(
            BigDecimal.valueOf(correctPositions).divide(BigDecimal.valueOf(expectedOrdering.size()), 8, RoundingMode.HALF_UP)
        ).setScale(4, RoundingMode.HALF_UP);
        return new QuestionReviewFallback(
            earnedScore,
            correct,
            correct ? null : "Порядок пользователя не совпадает с ожидаемым",
            "{\"type\":\"ORDERING\",\"items\":" + jsonOrdering(expectedOrdering) + "}",
            "{\"type\":\"ORDERING\",\"items\":" + jsonOrdering(actualOrdering) + "}",
            orderingOptions.stream().map(AnswerOption::id).toList(),
            actualOrderingByOptionId.keySet().stream().toList()
        );
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

    private record QuestionReviewFallback(
        BigDecimal earnedScore,
        boolean correct,
        String evaluationNote,
        String correctAnswerSnapshot,
        String userAnswerSnapshot,
        List<Long> correctOptionIds,
        List<Long> selectedOptionIds
    ) {
        private static QuestionReviewFallback empty() {
            return new QuestionReviewFallback(
                BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                false,
                null,
                null,
                null,
                List.of(),
                List.of()
            );
        }
    }

    private record PairSnapshot(Long leftAnswerOptionId, Long rightAnswerOptionId) {
    }

    private record OrderSnapshot(Long answerOptionId, int position) {
    }
}
