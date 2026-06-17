package com.vladislav.training.platform.testing.service;

import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.content.domain.AnswerOption;
import com.vladislav.training.platform.content.domain.AnswerOptionRole;
import com.vladislav.training.platform.content.domain.Question;
import com.vladislav.training.platform.content.domain.QuestionType;
import com.vladislav.training.platform.content.repository.AnswerOptionRepository;
import com.vladislav.training.platform.content.repository.QuestionRepository;
import com.vladislav.training.platform.content.repository.TestQuestionRepository;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import com.vladislav.training.platform.testing.domain.UserAnswer;
import com.vladislav.training.platform.testing.domain.UserAnswerItem;
import com.vladislav.training.platform.testing.repository.TestAttemptRepository;
import com.vladislav.training.platform.testing.repository.UserAnswerItemRepository;
import com.vladislav.training.platform.testing.repository.UserAnswerRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Контракт сервиса {@code ActiveAttemptAnswerMutationService}.
 */
@Service
@Transactional
public class ActiveAttemptAnswerMutationService {

    private final TestAttemptRepository testAttemptRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final QuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final UserAnswerItemRepository userAnswerItemRepository;

    public ActiveAttemptAnswerMutationService(
        TestAttemptRepository testAttemptRepository,
        TestQuestionRepository testQuestionRepository,
        QuestionRepository questionRepository,
        AnswerOptionRepository answerOptionRepository,
        UserAnswerRepository userAnswerRepository,
        UserAnswerItemRepository userAnswerItemRepository
    ) {
        this.testAttemptRepository = Objects.requireNonNull(testAttemptRepository, "testAttemptRepository must not be null");
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
    }

    public TestAttempt saveOrReplaceAnswer(
        Long actorUserId,
        Long testAttemptId,
        Long questionId,
        List<ActiveAttemptAnswerItemMutation> answerItems,
        Instant mutatedAt
    ) {
        Objects.requireNonNull(answerItems, "answerItems must not be null");
        if (answerItems.isEmpty()) {
            throw new ConflictException("Answer mutation requires non-empty item collection; use clearAnswer for empty state");
        }
        List<ActiveAttemptAnswerItemMutation> normalizedItems = normalizeAnswerItems(answerItems);

        TestAttempt activeAttempt = requireMutableAttempt(actorUserId, testAttemptId);
        validateAnswerMutation(activeAttempt.testId(), questionId, normalizedItems);
        UserAnswer persistedAnswer = findOrCreateUserAnswer(activeAttempt.id(), questionId, mutatedAt);
        userAnswerItemRepository.deleteUserAnswerItemsByUserAnswerId(persistedAnswer.id());
        saveAnswerItems(persistedAnswer.id(), normalizedItems, mutatedAt);
        return updateAttemptActivity(activeAttempt, mutatedAt);
    }

    public TestAttempt clearAnswer(Long actorUserId, Long testAttemptId, Long questionId, Instant mutatedAt) {
        TestAttempt activeAttempt = requireMutableAttempt(actorUserId, testAttemptId);
        validateQuestionOwnership(activeAttempt.testId(), questionId);
        UserAnswer existingAnswer = userAnswerRepository.findUserAnswerByTestAttemptIdAndQuestionId(activeAttempt.id(), questionId);
        if (existingAnswer != null) {
            userAnswerItemRepository.deleteUserAnswerItemsByUserAnswerId(existingAnswer.id());
            userAnswerRepository.saveUserAnswer(
                new UserAnswer(
                    existingAnswer.id(),
                    existingAnswer.testAttemptId(),
                    existingAnswer.questionId(),
                    existingAnswer.createdAt(),
                    mutatedAt
                )
            );
        }
        return updateAttemptActivity(activeAttempt, mutatedAt);
    }

    private TestAttempt requireMutableAttempt(Long actorUserId, Long testAttemptId) {
        Objects.requireNonNull(actorUserId, "actorUserId must not be null");
        Objects.requireNonNull(testAttemptId, "testAttemptId must not be null");
        TestAttempt attempt = testAttemptRepository.findAndLockTestAttemptByIdAndUserId(testAttemptId, actorUserId);
        if (attempt.status() != TestAttemptStatus.STARTED && attempt.status() != TestAttemptStatus.IN_PROGRESS) {
            throw new ConflictException("Answer mutation is allowed only for active attempts: testAttemptId=" + testAttemptId);
        }
        return attempt;
    }

    private void validateAnswerMutation(
        Long testId,
        Long questionId,
        List<ActiveAttemptAnswerItemMutation> normalizedItems
    ) {
        Question question = validateQuestionOwnership(testId, questionId);
        AnswerItemShape shape = detectShape(normalizedItems);
        validatePayloadShape(question.questionType(), normalizedItems, shape);
        Map<Long, AnswerOption> optionsById = loadQuestionOptionsById(question.id());
        validateOptionOwnershipAndShape(question.questionType(), normalizedItems, optionsById);
    }

    private Question validateQuestionOwnership(Long testId, Long questionId) {
        Objects.requireNonNull(questionId, "questionId must not be null");
        boolean belongsToTest = testQuestionRepository.findTestQuestionsByTestId(testId).stream()
            .anyMatch(testQuestion -> questionId.equals(testQuestion.questionId()));
        if (!belongsToTest) {
            throw new ConflictException(
                "Answer mutation question does not belong to attempt test: testId=" + testId + ", questionId=" + questionId
            );
        }
        return questionRepository.findQuestionById(questionId);
    }

    private void validatePayloadShape(
        QuestionType questionType,
        List<ActiveAttemptAnswerItemMutation> normalizedItems,
        AnswerItemShape shape
    ) {
        switch (questionType) {
            case SINGLE_CHOICE -> {
                if (shape != AnswerItemShape.CHOICE || normalizedItems.size() != 1) {
                    throw new ConflictException("SINGLE_CHOICE answer payload must contain exactly one choice item");
                }
            }
            case MULTIPLE_CHOICE -> {
                if (shape != AnswerItemShape.CHOICE) {
                    throw new ConflictException("MULTIPLE_CHOICE answer payload must contain choice items only");
                }
            }
            case MATCHING -> {
                if (shape != AnswerItemShape.MATCHING) {
                    throw new ConflictException("MATCHING answer payload must contain left/right pairs only");
                }
            }
            case ORDERING -> {
                if (shape != AnswerItemShape.ORDERING) {
                    throw new ConflictException("ORDERING answer payload must contain ordered items only");
                }
            }
        }
    }

    private Map<Long, AnswerOption> loadQuestionOptionsById(Long questionId) {
        Map<Long, AnswerOption> optionsById = new HashMap<>();
        for (AnswerOption option : answerOptionRepository.findAnswerOptionsByQuestionId(questionId)) {
            optionsById.put(option.id(), option);
        }
        return optionsById;
    }

    private void validateOptionOwnershipAndShape(
        QuestionType questionType,
        List<ActiveAttemptAnswerItemMutation> normalizedItems,
        Map<Long, AnswerOption> optionsById
    ) {
        switch (questionType) {
            case SINGLE_CHOICE, MULTIPLE_CHOICE -> validateChoiceOptions(normalizedItems, optionsById);
            case MATCHING -> validateMatchingOptions(normalizedItems, optionsById);
            case ORDERING -> validateOrderingOptions(normalizedItems, optionsById);
        }
    }

    private void validateChoiceOptions(
        List<ActiveAttemptAnswerItemMutation> normalizedItems,
        Map<Long, AnswerOption> optionsById
    ) {
        for (ActiveAttemptAnswerItemMutation answerItem : normalizedItems) {
            AnswerOption option = requireQuestionOption(optionsById, answerItem.answerOptionId());
            if (option.answerOptionRole() != AnswerOptionRole.CHOICE_OPTION) {
                throw new ConflictException("Choice answer payload requires CHOICE_OPTION ownership");
            }
        }
    }

    private void validateMatchingOptions(
        List<ActiveAttemptAnswerItemMutation> normalizedItems,
        Map<Long, AnswerOption> optionsById
    ) {
        Set<Long> usedLeftOptions = new HashSet<>();
        Set<Long> usedRightOptions = new HashSet<>();
        Set<String> usedPairs = new HashSet<>();
        for (ActiveAttemptAnswerItemMutation answerItem : normalizedItems) {
            AnswerOption leftOption = requireQuestionOption(optionsById, answerItem.leftAnswerOptionId());
            AnswerOption rightOption = requireQuestionOption(optionsById, answerItem.rightAnswerOptionId());
            if (leftOption.answerOptionRole() != AnswerOptionRole.MATCH_LEFT) {
                throw new ConflictException("MATCHING answer payload requires MATCH_LEFT ownership on left side");
            }
            if (rightOption.answerOptionRole() != AnswerOptionRole.MATCH_RIGHT) {
                throw new ConflictException("MATCHING answer payload requires MATCH_RIGHT ownership on right side");
            }
            String pairKey = leftOption.id() + ":" + rightOption.id();
            if (!usedPairs.add(pairKey)) {
                throw new ConflictException("MATCHING answer payload must not contain duplicate left/right pairs");
            }
            if (!usedLeftOptions.add(leftOption.id())) {
                throw new ConflictException("MATCHING answer payload requires unique left-side ownership");
            }
            if (!usedRightOptions.add(rightOption.id())) {
                throw new ConflictException("MATCHING answer payload requires unique right-side ownership");
            }
        }
    }

    private void validateOrderingOptions(
        List<ActiveAttemptAnswerItemMutation> normalizedItems,
        Map<Long, AnswerOption> optionsById
    ) {
        for (ActiveAttemptAnswerItemMutation answerItem : normalizedItems) {
            AnswerOption option = requireQuestionOption(optionsById, answerItem.answerOptionId());
            if (option.answerOptionRole() != AnswerOptionRole.ORDER_ITEM) {
                throw new ConflictException("ORDERING answer payload requires ORDER_ITEM ownership");
            }
        }
    }

    private AnswerOption requireQuestionOption(Map<Long, AnswerOption> optionsById, Long answerOptionId) {
        AnswerOption option = optionsById.get(answerOptionId);
        if (option == null) {
            throw new ConflictException("Answer option does not belong to question: answerOptionId=" + answerOptionId);
        }
        return option;
    }

    private UserAnswer findOrCreateUserAnswer(Long testAttemptId, Long questionId, Instant mutatedAt) {
        Objects.requireNonNull(questionId, "questionId must not be null");
        Objects.requireNonNull(mutatedAt, "mutatedAt must not be null");

        UserAnswer existingAnswer = userAnswerRepository.findUserAnswerByTestAttemptIdAndQuestionId(testAttemptId, questionId);
        if (existingAnswer != null) {
            return userAnswerRepository.saveUserAnswer(
                new UserAnswer(
                    existingAnswer.id(),
                    existingAnswer.testAttemptId(),
                    existingAnswer.questionId(),
                    existingAnswer.createdAt(),
                    mutatedAt
                )
            );
        }
        return userAnswerRepository.saveUserAnswer(
            new UserAnswer(null, testAttemptId, questionId, mutatedAt, mutatedAt)
        );
    }

    private void saveAnswerItems(Long userAnswerId, List<ActiveAttemptAnswerItemMutation> answerItems, Instant mutatedAt) {
        for (ActiveAttemptAnswerItemMutation answerItem : answerItems) {
            userAnswerItemRepository.saveUserAnswerItem(
                new UserAnswerItem(
                    null,
                    userAnswerId,
                    answerItem.answerOptionId(),
                    answerItem.leftAnswerOptionId(),
                    answerItem.rightAnswerOptionId(),
                    answerItem.userOrderPosition(),
                    mutatedAt,
                    mutatedAt
                )
            );
        }
    }

    private List<ActiveAttemptAnswerItemMutation> normalizeAnswerItems(List<ActiveAttemptAnswerItemMutation> answerItems) {
        AnswerItemShape shape = detectShape(answerItems);
        return switch (shape) {
            case CHOICE -> normalizeChoiceItems(answerItems);
            case MATCHING -> normalizeMatchingItems(answerItems);
            case ORDERING -> normalizeOrderingItems(answerItems);
        };
    }

    private AnswerItemShape detectShape(List<ActiveAttemptAnswerItemMutation> answerItems) {
        Set<AnswerItemShape> shapes = answerItems.stream()
            .map(this::shapeOf)
            .collect(java.util.stream.Collectors.toSet());
        if (shapes.size() != 1) {
            throw new ConflictException("Answer mutation does not allow mixed item shapes");
        }
        return shapes.iterator().next();
    }

    private AnswerItemShape shapeOf(ActiveAttemptAnswerItemMutation answerItem) {
        boolean hasAnswerOptionId = answerItem.answerOptionId() != null;
        boolean hasMatchingPair = answerItem.leftAnswerOptionId() != null || answerItem.rightAnswerOptionId() != null;
        boolean hasOrderPosition = answerItem.userOrderPosition() != null;

        if (hasMatchingPair) {
            if (!hasBothMatchingSides(answerItem) || hasAnswerOptionId || hasOrderPosition) {
                throw new ConflictException("Matching answer items must contain only left/right pair");
            }
            return AnswerItemShape.MATCHING;
        }
        if (hasOrderPosition) {
            if (!hasAnswerOptionId) {
                throw new ConflictException("Ordering answer items require answerOptionId with userOrderPosition");
            }
            return AnswerItemShape.ORDERING;
        }
        if (hasAnswerOptionId) {
            return AnswerItemShape.CHOICE;
        }
        throw new ConflictException("Answer item shape is not recognized");
    }

    private boolean hasBothMatchingSides(ActiveAttemptAnswerItemMutation answerItem) {
        return answerItem.leftAnswerOptionId() != null && answerItem.rightAnswerOptionId() != null;
    }

    private List<ActiveAttemptAnswerItemMutation> normalizeChoiceItems(List<ActiveAttemptAnswerItemMutation> answerItems) {
        ensureUniqueChoiceOptions(answerItems);
        Map<Long, ActiveAttemptAnswerItemMutation> itemsByAnswerOptionId = new LinkedHashMap<>();
        for (ActiveAttemptAnswerItemMutation answerItem : answerItems) {
            ActiveAttemptAnswerItemMutation previous = itemsByAnswerOptionId.putIfAbsent(
                answerItem.answerOptionId(),
                answerItem
            );
            if (previous != null) {
                throw new ConflictException("Choice answer items must not contain duplicate answerOptionId values");
            }
        }
        return itemsByAnswerOptionId.values()
            .stream()
            .sorted(Comparator.comparing(ActiveAttemptAnswerItemMutation::answerOptionId))
            .toList();
    }

    private void ensureUniqueChoiceOptions(List<ActiveAttemptAnswerItemMutation> answerItems) {
        long distinctAnswerOptions = answerItems.stream()
            .map(ActiveAttemptAnswerItemMutation::answerOptionId)
            .distinct()
            .count();
        if (distinctAnswerOptions != answerItems.size()) {
            throw new ConflictException("Choice answer items must not contain duplicate answerOptionId values");
        }
    }

    private List<ActiveAttemptAnswerItemMutation> normalizeMatchingItems(List<ActiveAttemptAnswerItemMutation> answerItems) {
        return answerItems.stream()
            .sorted(Comparator
                .comparing(ActiveAttemptAnswerItemMutation::leftAnswerOptionId)
                .thenComparing(ActiveAttemptAnswerItemMutation::rightAnswerOptionId))
            .toList();
    }

    private List<ActiveAttemptAnswerItemMutation> normalizeOrderingItems(List<ActiveAttemptAnswerItemMutation> answerItems) {
        ensureUniqueOrderingByOption(answerItems);
        ensureUniqueOrderingByPosition(answerItems);
        return answerItems.stream()
            .sorted(Comparator
                .comparing(ActiveAttemptAnswerItemMutation::userOrderPosition)
                .thenComparing(ActiveAttemptAnswerItemMutation::answerOptionId))
            .toList();
    }

    private void ensureUniqueOrderingByOption(List<ActiveAttemptAnswerItemMutation> answerItems) {
        long distinctAnswerOptions = answerItems.stream()
            .map(ActiveAttemptAnswerItemMutation::answerOptionId)
            .distinct()
            .count();
        if (distinctAnswerOptions != answerItems.size()) {
            throw new ConflictException("Ordering answer items require unique answerOptionId values");
        }
    }

    private void ensureUniqueOrderingByPosition(List<ActiveAttemptAnswerItemMutation> answerItems) {
        long distinctPositions = answerItems.stream()
            .map(ActiveAttemptAnswerItemMutation::userOrderPosition)
            .distinct()
            .count();
        if (distinctPositions != answerItems.size()) {
            throw new ConflictException("Ordering answer items require unique userOrderPosition values");
        }
    }

    private TestAttempt updateAttemptActivity(TestAttempt attempt, Instant mutatedAt) {
        Objects.requireNonNull(mutatedAt, "mutatedAt must not be null");
        return testAttemptRepository.saveTestAttempt(
            new TestAttempt(
                attempt.id(),
                attempt.userId(),
                attempt.testId(),
                attempt.assignmentTestId(),
                attempt.attemptMode(),
                attempt.status() == TestAttemptStatus.STARTED ? TestAttemptStatus.IN_PROGRESS : attempt.status(),
                attempt.startedAt(),
                attempt.completedAt(),
                attempt.expiredAt(),
                attempt.abandonedAt(),
                mutatedAt,
                attempt.createdAt(),
                mutatedAt
            )
        );
    }

    public record ActiveAttemptAnswerItemMutation(
        Long answerOptionId,
        Long leftAnswerOptionId,
        Long rightAnswerOptionId,
        Integer userOrderPosition
    ) {
    }

    private enum AnswerItemShape {
        CHOICE,
        MATCHING,
        ORDERING
    }
}
