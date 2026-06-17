package com.vladislav.training.platform.result.service;

import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.content.domain.AnswerOption;
import com.vladislav.training.platform.content.domain.AnswerOptionRole;
import com.vladislav.training.platform.content.domain.Question;
import com.vladislav.training.platform.testing.domain.UserAnswerItem;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
/**
 * Класс {@code ResultQuestionScoringCalculator}.
 */

@Component
class ResultQuestionScoringCalculator implements ResultQuestionScoringEvaluator {

    private static final String DEFAULT_SCORING_POLICY_CODE = "DEFAULT";
    private static final String DEFAULT_PARTIAL_CREDIT_V1_SCORING_POLICY_CODE = "DEFAULT_PARTIAL_CREDIT_V1";
    private static final String STANDARD_SCORING_POLICY_CODE = "STANDARD";
    private static final Set<String> SUPPORTED_SCORING_POLICY_CODES = Set.of(
        DEFAULT_SCORING_POLICY_CODE,
        DEFAULT_PARTIAL_CREDIT_V1_SCORING_POLICY_CODE,
        STANDARD_SCORING_POLICY_CODE
    );

    @Override
    public QuestionScore evaluateQuestion(
        Question question,
        List<AnswerOption> answerOptions,
        List<UserAnswerItem> answerItems,
        BigDecimal maxScore,
        String scoringPolicyCode
    ) {
        validateSupportedScoringPolicy(scoringPolicyCode);
        List<UserAnswerItem> effectiveAnswerItems = answerItems == null ? List.of() : answerItems;
        return switch (question.questionType()) {
            case SINGLE_CHOICE -> evaluateSingleChoice(answerOptions, effectiveAnswerItems, maxScore);
            case MULTIPLE_CHOICE -> evaluateMultipleChoice(answerOptions, effectiveAnswerItems, maxScore);
            case MATCHING -> evaluateMatching(answerOptions, effectiveAnswerItems, maxScore);
            case ORDERING -> evaluateOrdering(answerOptions, effectiveAnswerItems, maxScore);
        };
    }

    private QuestionScore evaluateSingleChoice(
        List<AnswerOption> answerOptions,
        List<UserAnswerItem> answerItems,
        BigDecimal maxScore
    ) {
        Set<Long> correctOptionIds = canonicalChoiceCorrectOptionIds(answerOptions);
        Set<Long> selectedOptionIds = selectedChoiceOptionIds(answerItems);
        boolean correct = selectedOptionIds.equals(correctOptionIds);
        return new QuestionScore(correct ? maxScore : BigDecimal.ZERO.setScale(4), correct);
    }

    private QuestionScore evaluateMultipleChoice(
        List<AnswerOption> answerOptions,
        List<UserAnswerItem> answerItems,
        BigDecimal maxScore
    ) {
        Set<Long> correctOptionIds = canonicalChoiceCorrectOptionIds(answerOptions);
        Set<Long> selectedOptionIds = selectedChoiceOptionIds(answerItems);
        boolean correct = selectedOptionIds.equals(correctOptionIds);
        long correctSelected = selectedOptionIds.stream()
            .filter(correctOptionIds::contains)
            .count();
        long incorrectSelected = selectedOptionIds.size() - correctSelected;
        BigDecimal rawRatio = BigDecimal.valueOf(correctSelected - incorrectSelected)
            .divide(BigDecimal.valueOf(correctOptionIds.size()), 8, RoundingMode.HALF_UP);
        BigDecimal earnedScore = rawRatio.signum() <= 0
            ? BigDecimal.ZERO.setScale(4)
            : maxScore.multiply(rawRatio).setScale(4, RoundingMode.HALF_UP);
        return new QuestionScore(earnedScore, correct);
    }

    private QuestionScore evaluateMatching(
        List<AnswerOption> answerOptions,
        List<UserAnswerItem> answerItems,
        BigDecimal maxScore
    ) {
        Map<Long, Long> expectedPairs = canonicalMatchingPairs(answerOptions);
        Map<Long, Long> actualPairs = userMatchingPairs(answerItems);
        boolean correct = actualPairs.equals(expectedPairs);
        long correctPairs = expectedPairs.entrySet().stream()
            .filter(entry -> Objects.equals(actualPairs.get(entry.getKey()), entry.getValue()))
            .count();
        BigDecimal ratio = BigDecimal.valueOf(correctPairs)
            .divide(BigDecimal.valueOf(expectedPairs.size()), 8, RoundingMode.HALF_UP);
        return new QuestionScore(maxScore.multiply(ratio).setScale(4, RoundingMode.HALF_UP), correct);
    }

    private QuestionScore evaluateOrdering(
        List<AnswerOption> answerOptions,
        List<UserAnswerItem> answerItems,
        BigDecimal maxScore
    ) {
        Map<Long, Integer> expectedOrdering = canonicalOrdering(answerOptions);
        Map<Long, Integer> actualOrdering = userOrdering(answerItems);
        boolean correct = actualOrdering.equals(expectedOrdering);
        long correctPositions = expectedOrdering.entrySet().stream()
            .filter(entry -> Objects.equals(actualOrdering.get(entry.getKey()), entry.getValue()))
            .count();
        BigDecimal ratio = BigDecimal.valueOf(correctPositions)
            .divide(BigDecimal.valueOf(expectedOrdering.size()), 8, RoundingMode.HALF_UP);
        return new QuestionScore(maxScore.multiply(ratio).setScale(4, RoundingMode.HALF_UP), correct);
    }

    private Set<Long> canonicalChoiceCorrectOptionIds(List<AnswerOption> answerOptions) {
        Set<Long> correctOptionIds = new HashSet<>();
        for (AnswerOption answerOption : answerOptions) {
            if (answerOption.answerOptionRole() == AnswerOptionRole.CHOICE_OPTION
                && Boolean.TRUE.equals(answerOption.isCorrect())) {
                correctOptionIds.add(answerOption.id());
            }
        }
        if (correctOptionIds.isEmpty()) {
            throw new ConflictException("Result recording requires canonical correct choice options");
        }
        return correctOptionIds;
    }

    private Set<Long> selectedChoiceOptionIds(List<UserAnswerItem> answerItems) {
        Set<Long> selectedOptionIds = new HashSet<>();
        for (UserAnswerItem answerItem : answerItems) {
            Long answerOptionId = answerItem.answerOptionId();
            if (answerOptionId != null && !selectedOptionIds.add(answerOptionId)) {
                throw new ConflictException(
                    "Result recording rejects duplicate persisted choice option state: answerOptionId=" + answerOptionId
                );
            }
        }
        return selectedOptionIds;
    }

    private Map<Long, Long> canonicalMatchingPairs(List<AnswerOption> answerOptions) {
        validateCanonicalMatchingOptions(answerOptions);
        Map<String, Long> leftByPairingKey = answerOptions.stream()
            .filter(option -> option.answerOptionRole() == AnswerOptionRole.MATCH_LEFT)
            .collect(Collectors.toMap(
                AnswerOption::pairingKey,
                AnswerOption::id,
                (left, right) -> {
                    throw new ConflictException("Result recording requires unique MATCH_LEFT pairingKey");
                },
                LinkedHashMap::new
            ));
        Map<String, Long> rightByPairingKey = answerOptions.stream()
            .filter(option -> option.answerOptionRole() == AnswerOptionRole.MATCH_RIGHT)
            .collect(Collectors.toMap(
                AnswerOption::pairingKey,
                AnswerOption::id,
                (left, right) -> {
                    throw new ConflictException("Result recording requires unique MATCH_RIGHT pairingKey");
                },
                LinkedHashMap::new
            ));
        if (leftByPairingKey.isEmpty() || !leftByPairingKey.keySet().equals(rightByPairingKey.keySet())) {
            throw new ConflictException("Result recording requires canonical one-to-one matching options");
        }

        Map<Long, Long> expectedPairs = new LinkedHashMap<>();
        leftByPairingKey.forEach((pairingKey, leftId) -> expectedPairs.put(leftId, rightByPairingKey.get(pairingKey)));
        return expectedPairs;
    }

    private Map<Long, Long> userMatchingPairs(List<UserAnswerItem> answerItems) {
        Map<Long, Long> matchingPairs = new LinkedHashMap<>();
        Set<String> seenPairs = new HashSet<>();
        Set<Long> seenLeftIds = new HashSet<>();
        Set<Long> seenRightIds = new HashSet<>();
        for (UserAnswerItem answerItem : answerItems) {
            Long leftAnswerOptionId = answerItem.leftAnswerOptionId();
            Long rightAnswerOptionId = answerItem.rightAnswerOptionId();
            if (leftAnswerOptionId == null || rightAnswerOptionId == null) {
                continue;
            }

            String pairKey = leftAnswerOptionId + ":" + rightAnswerOptionId;
            if (!seenPairs.add(pairKey)) {
                throw new ConflictException(
                    "Result recording rejects duplicate persisted matching pair state: leftAnswerOptionId="
                        + leftAnswerOptionId
                        + ", rightAnswerOptionId="
                        + rightAnswerOptionId
                );
            }
            if (!seenLeftIds.add(leftAnswerOptionId)) {
                throw new ConflictException(
                    "Result recording rejects duplicate persisted matching left state: leftAnswerOptionId="
                        + leftAnswerOptionId
                );
            }
            if (!seenRightIds.add(rightAnswerOptionId)) {
                throw new ConflictException(
                    "Result recording rejects duplicate persisted matching right state: rightAnswerOptionId="
                        + rightAnswerOptionId
                );
            }

            matchingPairs.put(leftAnswerOptionId, rightAnswerOptionId);
        }
        return matchingPairs;
    }

    private Map<Long, Integer> canonicalOrdering(List<AnswerOption> answerOptions) {
        validateCanonicalOrderingOptions(answerOptions);
        Map<Long, Integer> expectedOrdering = new LinkedHashMap<>();
        for (AnswerOption answerOption : answerOptions) {
            if (answerOption.answerOptionRole() != AnswerOptionRole.ORDER_ITEM) {
                continue;
            }
            if (answerOption.canonicalOrderPosition() == null) {
                throw new ConflictException("Result recording requires canonical ORDER_ITEM positions");
            }
            Integer previous = expectedOrdering.putIfAbsent(answerOption.id(), answerOption.canonicalOrderPosition());
            if (previous != null) {
                throw new ConflictException(
                    "Result recording requires unique canonical ORDER_ITEM ids: answerOptionId=" + answerOption.id()
                );
            }
        }
        if (expectedOrdering.isEmpty()) {
            throw new ConflictException("Result recording requires canonical ordering options");
        }
        return expectedOrdering;
    }

    private Map<Long, Integer> userOrdering(List<UserAnswerItem> answerItems) {
        Map<Long, Integer> ordering = new LinkedHashMap<>();
        Set<Integer> seenOrderPositions = new HashSet<>();
        for (UserAnswerItem answerItem : answerItems) {
            Long answerOptionId = answerItem.answerOptionId();
            Integer userOrderPosition = answerItem.userOrderPosition();
            if (answerOptionId != null) {
                Integer previous = ordering.putIfAbsent(answerOptionId, userOrderPosition);
                if (previous != null) {
                    throw new ConflictException(
                        "Result recording rejects duplicate persisted ordering option state: answerOptionId="
                            + answerOptionId
                    );
                }
            }
            if (userOrderPosition != null && !seenOrderPositions.add(userOrderPosition)) {
                throw new ConflictException(
                    "Result recording rejects duplicate persisted ordering position state: userOrderPosition="
                        + userOrderPosition
                );
            }
        }
        return ordering;
    }

    private void validateCanonicalMatchingOptions(List<AnswerOption> answerOptions) {
        if (answerOptions == null || answerOptions.isEmpty()) {
            throw new ConflictException("Result recording requires canonical one-to-one matching options");
        }
    }

    private void validateCanonicalOrderingOptions(List<AnswerOption> answerOptions) {
        if (answerOptions == null || answerOptions.isEmpty()) {
            throw new ConflictException("Result recording requires canonical ordering options");
        }
    }

    private void validateSupportedScoringPolicy(String scoringPolicyCode) {
        if (scoringPolicyCode == null
            || scoringPolicyCode.isBlank()
            || !SUPPORTED_SCORING_POLICY_CODES.contains(scoringPolicyCode)) {
            throw new ConflictException(
                "Result recording requires canonical scoring policy support: scoringPolicyCode=" + scoringPolicyCode
            );
        }
    }
}

