package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.common.exception.ValidationException;
import com.vladislav.training.platform.content.domain.AnswerOption;
import com.vladislav.training.platform.content.domain.AnswerOptionRole;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Question;
import com.vladislav.training.platform.content.domain.Test;
import com.vladislav.training.platform.content.domain.TestQuestion;
import com.vladislav.training.platform.content.repository.AnswerOptionRepository;
import com.vladislav.training.platform.content.repository.QuestionRepository;
import com.vladislav.training.platform.content.repository.TestQuestionRepository;
import com.vladislav.training.platform.content.repository.TestRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса {@code ContentPublicationValidationServiceImpl}.
 */
@Service
@Transactional(readOnly = true)
public class ContentPublicationValidationServiceImpl implements ContentPublicationValidationService {

    private final QuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final TestRepository testRepository;
    private final TestQuestionRepository testQuestionRepository;

    public ContentPublicationValidationServiceImpl(
        QuestionRepository questionRepository,
        AnswerOptionRepository answerOptionRepository,
        TestRepository testRepository,
        TestQuestionRepository testQuestionRepository
    ) {
        this.questionRepository = questionRepository;
        this.answerOptionRepository = answerOptionRepository;
        this.testRepository = testRepository;
        this.testQuestionRepository = testQuestionRepository;
    }

    @Override
    public void validateQuestionPublishable(Long questionId) {
        validateQuestionPublishable(questionRepository.findQuestionById(questionId));
    }

    @Override
    public void validateTestPublishable(Long testId) {
        Test test = testRepository.findTestById(testId);
        List<TestQuestion> items = testQuestionRepository.findTestQuestionsByTestId(testId);
        if (items.isEmpty()) {
            throw new ValidationException("Publishable test must have non-empty composition state");
        }
        Set<Long> questionIds = new HashSet<>();
        Set<Integer> displayOrders = new HashSet<>();
        for (TestQuestion item : items) {
            if (item.weight().signum() <= 0) {
                throw new ValidationException("TestQuestion.weight must be positive");
            }
            if (!questionIds.add(item.questionId())) {
                throw new ValidationException("Duplicate question inside test is not allowed");
            }
            if (!displayOrders.add(item.displayOrder())) {
                throw new ValidationException("displayOrder must be unique inside test");
            }
        }
        List<Question> questions = questionRepository.findQuestionsByIds(questionIds);
        if (questions.size() != questionIds.size()) {
            throw new ValidationException("Some test questions are missing");
        }
        for (Question question : questions) {
            if (!question.topicId().equals(test.topicId())) {
                throw new ValidationException("Test may include only questions from the same topic");
            }
            if (question.status() != ContentStatus.PUBLISHED) {
                throw new ValidationException("Publishable test may include only PUBLISHED questions");
            }
            validateQuestionPublishable(question);
        }
    }

    private void validateQuestionPublishable(Question question) {
        List<AnswerOption> options = answerOptionRepository.findAnswerOptionsByQuestionId(question.id());
        switch (question.questionType()) {
            case SINGLE_CHOICE -> validateSingleChoice(options);
            case MULTIPLE_CHOICE -> validateMultipleChoice(options);
            case MATCHING -> validateMatching(options);
            case ORDERING -> validateOrdering(options);
        }
    }

    private void validateSingleChoice(List<AnswerOption> options) {
        if (options.size() < 2) {
            throw new ValidationException("SINGLE_CHOICE requires at least two choice options");
        }
        long correct = options.stream()
            .peek(this::requireChoiceRole)
            .filter(a -> Boolean.TRUE.equals(a.isCorrect()))
            .count();
        if (correct != 1) {
            throw new ValidationException("SINGLE_CHOICE requires exactly one correct option");
        }
    }

    private void validateMultipleChoice(List<AnswerOption> options) {
        if (options.size() < 2) {
            throw new ValidationException("MULTIPLE_CHOICE requires at least two choice options");
        }
        long correct = options.stream()
            .peek(this::requireChoiceRole)
            .filter(a -> Boolean.TRUE.equals(a.isCorrect()))
            .count();
        if (correct < 1) {
            throw new ValidationException("MULTIPLE_CHOICE requires one or more correct options");
        }
    }

    private void validateMatching(List<AnswerOption> options) {
        if (options.isEmpty()) {
            throw new ValidationException("MATCHING requires non-empty left and right sets");
        }
        Map<String, Integer> leftKeyCounts = new HashMap<>();
        Map<String, Integer> rightKeyCounts = new HashMap<>();
        for (AnswerOption option : options) {
            if (option.answerOptionRole() != AnswerOptionRole.MATCH_LEFT && option.answerOptionRole() != AnswerOptionRole.MATCH_RIGHT) {
                throw new ValidationException("MATCHING allows only MATCH_LEFT and MATCH_RIGHT roles");
            }
            if (option.pairingKey() == null || option.pairingKey().isBlank()) {
                throw new ValidationException("MATCHING requires non-blank pairingKey for all matching items");
            }
            if (option.answerOptionRole() == AnswerOptionRole.MATCH_LEFT) {
                leftKeyCounts.merge(option.pairingKey(), 1, Integer::sum);
            } else {
                rightKeyCounts.merge(option.pairingKey(), 1, Integer::sum);
            }
        }
        if (leftKeyCounts.isEmpty() || rightKeyCounts.isEmpty()) {
            throw new ValidationException("MATCHING requires non-empty left and right sets");
        }
        if (!leftKeyCounts.keySet().equals(rightKeyCounts.keySet())) {
            throw new ValidationException("MATCHING requires identical pairingKey sets on both sides");
        }
        boolean ambiguous = leftKeyCounts.values().stream().anyMatch(count -> count != 1)
            || rightKeyCounts.values().stream().anyMatch(count -> count != 1);
        if (ambiguous) {
            throw new ValidationException("MATCHING requires unambiguous one-to-one pairing semantics");
        }
    }

    private void validateOrdering(List<AnswerOption> options) {
        if (options.size() < 2) {
            throw new ValidationException("ORDERING requires at least two items");
        }
        Set<Integer> positions = new HashSet<>();
        int maxPosition = -1;
        for (AnswerOption option : options) {
            if (option.answerOptionRole() != AnswerOptionRole.ORDER_ITEM) {
                throw new ValidationException("ORDERING allows only ORDER_ITEM role");
            }
            if (option.canonicalOrderPosition() == null) {
                throw new ValidationException("ORDERING requires canonicalOrderPosition");
            }
            if (!positions.add(option.canonicalOrderPosition())) {
                throw new ValidationException("ORDERING canonical positions must be unique");
            }
            maxPosition = Math.max(maxPosition, option.canonicalOrderPosition());
        }
        if (maxPosition != options.size() - 1 || !positions.contains(0)) {
            throw new ValidationException("ORDERING requires a normalized canonical sequence starting at 0 without gaps");
        }
    }

    private void requireChoiceRole(AnswerOption option) {
        if (option.answerOptionRole() != AnswerOptionRole.CHOICE_OPTION) {
            throw new ValidationException("Choice question allows only CHOICE_OPTION role");
        }
    }
}
