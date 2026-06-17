package com.vladislav.training.platform.result.service;

import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaignRecipientSnapshot;
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
import com.vladislav.training.platform.content.domain.Question;
import com.vladislav.training.platform.content.domain.QuestionType;
import com.vladislav.training.platform.content.domain.Test;
import com.vladislav.training.platform.content.domain.TestQuestion;
import com.vladislav.training.platform.content.repository.AnswerOptionRepository;
import com.vladislav.training.platform.content.repository.QuestionRepository;
import com.vladislav.training.platform.content.repository.TestQuestionRepository;
import com.vladislav.training.platform.content.repository.TestRepository;
import com.vladislav.training.platform.result.domain.ResultOrgContextSnapshot;
import com.vladislav.training.platform.result.domain.ResultScoringSnapshot;
import com.vladislav.training.platform.result.domain.ResultSnapshotFacts.ResultAnswerOptionSnapshotFact;
import com.vladislav.training.platform.result.domain.ResultSnapshotFacts.ResultQuestionSnapshotFact;
import com.vladislav.training.platform.result.domain.ResultSnapshotFacts.ResultSubordinateSnapshotFacts;
import com.vladislav.training.platform.result.domain.ResultSnapshotFacts.ResultUserAnswerItemSnapshotFact;
import com.vladislav.training.platform.result.domain.ResultSnapshotFacts;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import com.vladislav.training.platform.testing.domain.UserAnswer;
import com.vladislav.training.platform.testing.domain.UserAnswerItem;
import com.vladislav.training.platform.testing.repository.UserAnswerItemRepository;
import com.vladislav.training.platform.testing.repository.UserAnswerRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
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
 * Класс {@code ResultRecordingSnapshotFactsProvider}.
 */
@Component
@Transactional(readOnly = true)
@ConditionalOnBean({
    AssignmentRepository.class,
    AssignmentTestRepository.class,
    AssignmentCampaignRecipientSnapshotRepository.class,
    TestRepository.class,
    TestQuestionRepository.class,
    QuestionRepository.class,
    AnswerOptionRepository.class,
    UserAnswerRepository.class,
    UserAnswerItemRepository.class,
    SelfCompletionOrgSnapshotFactsReader.class,
    UtcClock.class
})
class ResultRecordingSnapshotFactsProvider {

    private static final BigDecimal HUNDRED = new BigDecimal("100.0000");

    private final AssignmentRepository assignmentRepository;
    private final AssignmentTestRepository assignmentTestRepository;
    private final AssignmentCampaignRecipientSnapshotRepository recipientSnapshotRepository;
    private final TestRepository testRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final QuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final UserAnswerItemRepository userAnswerItemRepository;
    private final SelfCompletionOrgSnapshotFactsReader selfCompletionOrgSnapshotFactsReader;
    private final UtcClock utcClock;
    private final ResultDeadlineClassifier resultDeadlineClassifier;
    private final AssignmentCountedResultPolicy assignmentCountedResultPolicy;
    private final ResultQuestionScoringEvaluator resultQuestionScoringEvaluator;

    ResultRecordingSnapshotFactsProvider(
        AssignmentRepository assignmentRepository,
        AssignmentTestRepository assignmentTestRepository,
        AssignmentCampaignRecipientSnapshotRepository recipientSnapshotRepository,
        TestRepository testRepository,
        TestQuestionRepository testQuestionRepository,
        QuestionRepository questionRepository,
        AnswerOptionRepository answerOptionRepository,
        UserAnswerRepository userAnswerRepository,
        UserAnswerItemRepository userAnswerItemRepository,
        SelfCompletionOrgSnapshotFactsReader selfCompletionOrgSnapshotFactsReader,
        UtcClock utcClock,
        ResultDeadlineClassifier resultDeadlineClassifier,
        AssignmentCountedResultPolicy assignmentCountedResultPolicy,
        ResultQuestionScoringEvaluator resultQuestionScoringEvaluator
    ) {
        this.assignmentRepository = Objects.requireNonNull(assignmentRepository, "assignmentRepository must not be null");
        this.assignmentTestRepository = Objects.requireNonNull(
            assignmentTestRepository,
            "assignmentTestRepository must not be null"
        );
        this.recipientSnapshotRepository = Objects.requireNonNull(
            recipientSnapshotRepository,
            "recipientSnapshotRepository must not be null"
        );
        this.testRepository = Objects.requireNonNull(testRepository, "testRepository must not be null");
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
        this.selfCompletionOrgSnapshotFactsReader = Objects.requireNonNull(
            selfCompletionOrgSnapshotFactsReader,
            "selfCompletionOrgSnapshotFactsReader must not be null"
        );
        this.utcClock = Objects.requireNonNull(utcClock, "utcClock must not be null");
        this.resultDeadlineClassifier = Objects.requireNonNull(
            resultDeadlineClassifier,
            "resultDeadlineClassifier must not be null"
        );
        this.assignmentCountedResultPolicy = Objects.requireNonNull(
            assignmentCountedResultPolicy,
            "assignmentCountedResultPolicy must not be null"
        );
        this.resultQuestionScoringEvaluator = Objects.requireNonNull(
            resultQuestionScoringEvaluator,
            "resultQuestionScoringEvaluator must not be null"
        );
    }

    ResultSnapshotFacts provideSnapshotFacts(TestAttempt terminalizedAttempt) {
        Objects.requireNonNull(terminalizedAttempt, "terminalizedAttempt must not be null");

        Instant recordedAt = utcClock.now();
        return switch (terminalizedAttempt.attemptMode()) {
            case SELF -> selfSnapshotFacts(terminalizedAttempt, recordedAt);
            case ASSIGNED -> assignedSnapshotFacts(terminalizedAttempt, recordedAt);
        };
    }

    private ResultSnapshotFacts selfSnapshotFacts(TestAttempt terminalizedAttempt, Instant recordedAt) {
        FrozenAttemptSnapshotSource frozenAttemptSnapshotSource = freezeAttemptSnapshotSource(terminalizedAttempt);
        ResultScoringSnapshot scoringSnapshot = computeScoringSnapshot(frozenAttemptSnapshotSource);
        SelfCompletionOrgSnapshotFactsReader.SelfCompletionOrgSnapshotFacts orgSnapshotFacts =
            selfCompletionOrgSnapshotFactsReader.readSelfCompletionOrgSnapshotFacts(terminalizedAttempt.id());
        if (orgSnapshotFacts == null) {
            throw new ConflictException(
                "Для записи результата не удалось получить данные о подразделении пользователя: testAttemptId="
                    + terminalizedAttempt.id()
            );
        }

        return new ResultSnapshotFacts(
            null,
            null,
            frozenAttemptSnapshotSource.test().id(),
            frozenAttemptSnapshotSource.test().name(),
            scoringSnapshot,
            null,
            null,
            new ResultOrgContextSnapshot(
                orgSnapshotFacts.organizationalUnitIdSnapshot(),
                orgSnapshotFacts.organizationalPathSnapshot()
            ),
            false,
            recordedAt,
            toSubordinateSnapshotFacts(frozenAttemptSnapshotSource)
        );
    }

    private ResultSnapshotFacts assignedSnapshotFacts(TestAttempt terminalizedAttempt, Instant recordedAt) {
        Long assignmentTestId = requireAssignmentTestId(terminalizedAttempt);
        AssignmentTest assignmentTest = assignmentTestRepository.findAssignmentTestById(assignmentTestId);
        Assignment assignment = assignmentRepository.findAssignmentById(assignmentTest.assignmentId());
        requireConsistentAssignedAnchor(terminalizedAttempt, assignmentTest, assignment);

        FrozenAttemptSnapshotSource frozenAttemptSnapshotSource = freezeAttemptSnapshotSource(terminalizedAttempt);
        ResultScoringSnapshot scoringSnapshot = computeScoringSnapshot(frozenAttemptSnapshotSource);
        Instant terminalInstant = terminalInstantOf(terminalizedAttempt);
        boolean snapshotFinalTopicControlFlag =
            assignmentTest.assignmentTestRole() == AssignmentTestRole.FINAL_TOPIC_CONTROL;
        boolean withinDeadline = resultDeadlineClassifier.isWithinDeadline(
            assignment,
            assignmentTest,
            terminalInstant
        );
        AssignmentCountedResultDecision countedDecision = assignmentCountedResultPolicy.decide(
            snapshotFinalTopicControlFlag,
            scoringSnapshot,
            withinDeadline
        );
        boolean countedInAssignment = countedDecision.countedInAssignment();
        ResultOrgContextSnapshot orgContextSnapshot = resolveAssignedOrgContextSnapshot(assignment);

        return new ResultSnapshotFacts(
            assignment.id(),
            assignmentTest.id(),
            frozenAttemptSnapshotSource.test().id(),
            frozenAttemptSnapshotSource.test().name(),
            scoringSnapshot,
            withinDeadline,
            countedInAssignment,
            orgContextSnapshot,
            snapshotFinalTopicControlFlag,
            recordedAt,
            toSubordinateSnapshotFacts(frozenAttemptSnapshotSource)
        );
    }

    private FrozenAttemptSnapshotSource freezeAttemptSnapshotSource(TestAttempt terminalizedAttempt) {
        Test test = testRepository.findTestById(terminalizedAttempt.testId());
        List<TestQuestion> testQuestions = testQuestionRepository.findTestQuestionsByTestId(test.id()).stream()
            .sorted(Comparator.comparingInt(TestQuestion::displayOrder).thenComparing(TestQuestion::id))
            .toList();
        if (testQuestions.isEmpty()) {
            throw new ConflictException("Result recording requires non-empty test composition: testId=" + test.id());
        }

        Map<Long, Question> questionsById = questionRepository.findQuestionsByIds(
            testQuestions.stream().map(TestQuestion::questionId).toList()
        ).stream().collect(Collectors.toMap(Question::id, question -> question));
        if (questionsById.size() != testQuestions.size()) {
            throw new ConflictException("Result recording requires complete question set for test: testId=" + test.id());
        }

        Map<Long, List<AnswerOption>> optionsByQuestionId = new HashMap<>();
        for (TestQuestion testQuestion : testQuestions) {
            List<AnswerOption> answerOptions = answerOptionRepository.findAnswerOptionsByQuestionId(testQuestion.questionId());
            if (answerOptions.isEmpty()) {
                throw new ConflictException(
                    "Result recording requires answer options for test question: questionId=" + testQuestion.questionId()
                );
            }
            optionsByQuestionId.put(testQuestion.questionId(), answerOptions);
        }

        Map<Long, List<UserAnswerItem>> answerItemsByQuestionId = loadAnswerItemsByQuestionId(terminalizedAttempt.id());
        List<FrozenQuestionSnapshotSource> questionSnapshotSources = testQuestions.stream()
            .map(testQuestion -> {
                Question question = questionsById.get(testQuestion.questionId());
                if (question == null) {
                    throw new ConflictException(
                        "Result recording question lookup is incomplete: questionId=" + testQuestion.questionId()
                    );
                }
                return new FrozenQuestionSnapshotSource(
                    testQuestion,
                    question,
                    List.copyOf(optionsByQuestionId.get(question.id())),
                    List.copyOf(answerItemsByQuestionId.getOrDefault(question.id(), List.of()))
                );
            })
            .toList();
        return new FrozenAttemptSnapshotSource(test, questionSnapshotSources);
    }

    private ResultScoringSnapshot computeScoringSnapshot(FrozenAttemptSnapshotSource frozenAttemptSnapshotSource) {
        Test test = frozenAttemptSnapshotSource.test();
        List<FrozenQuestionSnapshotSource> testQuestions = frozenAttemptSnapshotSource.questionSnapshotSources();
        BigDecimal maxScore = testQuestions.stream()
            .map(questionSnapshotSource -> questionSnapshotSource.testQuestion().weight())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (maxScore.signum() <= 0) {
            throw new ConflictException("Result recording requires positive maxScore: testId=" + test.id());
        }

        BigDecimal earnedScore = BigDecimal.ZERO;
        for (FrozenQuestionSnapshotSource frozenQuestionSnapshotSource : testQuestions) {
            earnedScore = earnedScore.add(evaluateQuestionScore(
                frozenQuestionSnapshotSource.question(),
                frozenQuestionSnapshotSource.answerOptions(),
                frozenQuestionSnapshotSource.answerItems(),
                frozenQuestionSnapshotSource.testQuestion().weight(),
                test.scoringPolicyCode()
            ));
        }

        BigDecimal scorePercent = earnedScore.multiply(HUNDRED)
            .divide(maxScore, 4, RoundingMode.HALF_UP);
        boolean passed = scorePercent.compareTo(test.thresholdPercent().setScale(4, RoundingMode.HALF_UP)) >= 0;

        return new ResultScoringSnapshot(
            test.thresholdPercent().setScale(4, RoundingMode.HALF_UP),
            earnedScore.setScale(4, RoundingMode.HALF_UP),
            maxScore.setScale(4, RoundingMode.HALF_UP),
            scorePercent,
            passed,
            test.scoringPolicyCode(),
            "{\"source\":\"content-test\",\"testId\":"
                + test.id()
                + ",\"thresholdPercent\":\""
                + test.thresholdPercent().setScale(4, RoundingMode.HALF_UP).toPlainString()
                + "\",\"scoringPolicyCode\":\""
                + test.scoringPolicyCode()
                + "\"}"
        );
    }

    private ResultSubordinateSnapshotFacts toSubordinateSnapshotFacts(
        FrozenAttemptSnapshotSource frozenAttemptSnapshotSource
    ) {
        return new ResultSubordinateSnapshotFacts(
            frozenAttemptSnapshotSource.questionSnapshotSources().stream()
                .map(this::toQuestionSnapshotFact)
                .toList()
        );
    }

    private ResultQuestionSnapshotFact toQuestionSnapshotFact(FrozenQuestionSnapshotSource frozenQuestionSnapshotSource) {
        return new ResultQuestionSnapshotFact(
            frozenQuestionSnapshotSource.question().id(),
            frozenQuestionSnapshotSource.question().topicId(),
            frozenQuestionSnapshotSource.question().body(),
            frozenQuestionSnapshotSource.question().questionType(),
            frozenQuestionSnapshotSource.testQuestion().displayOrder(),
            frozenQuestionSnapshotSource.testQuestion().weight().setScale(4, RoundingMode.HALF_UP),
            frozenQuestionSnapshotSource.answerOptions().stream()
                .map(this::toAnswerOptionSnapshotFact)
                .toList(),
            frozenQuestionSnapshotSource.answerItems().stream()
                .map(answerItem -> new ResultUserAnswerItemSnapshotFact(
                    answerItem.answerOptionId(),
                    answerItem.leftAnswerOptionId(),
                    answerItem.rightAnswerOptionId(),
                    answerItem.userOrderPosition()
                ))
                .toList()
        );
    }

    private ResultAnswerOptionSnapshotFact toAnswerOptionSnapshotFact(AnswerOption answerOption) {
        return new ResultAnswerOptionSnapshotFact(
            answerOption.id(),
            answerOption.body(),
            answerOption.answerOptionRole(),
            answerOption.isCorrect(),
            answerOption.displayOrder(),
            answerOption.pairingKey(),
            answerOption.canonicalOrderPosition()
        );
    }

    private Map<Long, List<UserAnswerItem>> loadAnswerItemsByQuestionId(Long testAttemptId) {
        Map<Long, List<UserAnswerItem>> itemsByQuestionId = new HashMap<>();
        for (UserAnswer userAnswer : userAnswerRepository.findUserAnswersByTestAttemptId(testAttemptId)) {
            itemsByQuestionId.put(
                userAnswer.questionId(),
                userAnswerItemRepository.findUserAnswerItemsByUserAnswerId(userAnswer.id())
            );
        }
        return itemsByQuestionId;
    }

    private BigDecimal evaluateQuestionScore(
        Question question,
        List<AnswerOption> answerOptions,
        List<UserAnswerItem> answerItems,
        BigDecimal maxScore,
        String scoringPolicyCode
    ) {
        validatePersistedAnswerItems(question, answerItems == null ? List.of() : answerItems);
        return resultQuestionScoringEvaluator.evaluateQuestion(
            question,
            answerOptions,
            answerItems == null ? List.of() : answerItems,
            maxScore,
            scoringPolicyCode
        ).earnedScore();
    }

    private void validatePersistedAnswerItems(Question question, List<UserAnswerItem> answerItems) {
        switch (question.questionType()) {
            case SINGLE_CHOICE, MULTIPLE_CHOICE -> validatePersistedChoiceItems(answerItems);
            case MATCHING -> validatePersistedMatchingItems(answerItems);
            case ORDERING -> validatePersistedOrderingItems(answerItems);
        }
    }

    private void validatePersistedChoiceItems(List<UserAnswerItem> answerItems) {
        Set<Long> seenOptionIds = new HashSet<>();
        for (UserAnswerItem answerItem : answerItems) {
            Long answerOptionId = answerItem.answerOptionId();
            if (answerOptionId != null && !seenOptionIds.add(answerOptionId)) {
                throw new ConflictException(
                    "Result recording rejects duplicate persisted choice option state: answerOptionId=" + answerOptionId
                );
            }
        }
    }

    private void validatePersistedMatchingItems(List<UserAnswerItem> answerItems) {
        Set<String> seenPairs = new HashSet<>();
        for (UserAnswerItem answerItem : answerItems) {
            Long leftAnswerOptionId = answerItem.leftAnswerOptionId();
            Long rightAnswerOptionId = answerItem.rightAnswerOptionId();
            if (leftAnswerOptionId != null && rightAnswerOptionId != null) {
                String pairKey = leftAnswerOptionId + ":" + rightAnswerOptionId;
                if (!seenPairs.add(pairKey)) {
                    throw new ConflictException(
                        "Result recording rejects duplicate persisted matching pair state: leftAnswerOptionId="
                            + leftAnswerOptionId
                            + ", rightAnswerOptionId="
                            + rightAnswerOptionId
                    );
                }
            }
        }

        Set<Long> seenLeftAnswerOptionIds = new HashSet<>();
        for (UserAnswerItem answerItem : answerItems) {
            Long leftAnswerOptionId = answerItem.leftAnswerOptionId();
            if (leftAnswerOptionId != null && !seenLeftAnswerOptionIds.add(leftAnswerOptionId)) {
                throw new ConflictException(
                    "Result recording rejects duplicate persisted matching left state: leftAnswerOptionId="
                        + leftAnswerOptionId
                );
            }
        }

        Set<Long> seenRightAnswerOptionIds = new HashSet<>();
        for (UserAnswerItem answerItem : answerItems) {
            Long rightAnswerOptionId = answerItem.rightAnswerOptionId();
            if (rightAnswerOptionId != null && !seenRightAnswerOptionIds.add(rightAnswerOptionId)) {
                throw new ConflictException(
                    "Result recording rejects duplicate persisted matching right state: rightAnswerOptionId="
                        + rightAnswerOptionId
                );
            }
        }
    }


    private void validatePersistedOrderingItems(List<UserAnswerItem> answerItems) {
        Set<Long> seenOptionIds = new HashSet<>();
        Set<Integer> seenOrderPositions = new HashSet<>();
        for (UserAnswerItem answerItem : answerItems) {
            Long answerOptionId = answerItem.answerOptionId();
            if (answerOptionId != null && !seenOptionIds.add(answerOptionId)) {
                throw new ConflictException(
                    "Result recording rejects duplicate persisted ordering option state: answerOptionId=" + answerOptionId
                );
            }

            Integer userOrderPosition = answerItem.userOrderPosition();
            if (userOrderPosition != null && !seenOrderPositions.add(userOrderPosition)) {
                throw new ConflictException(
                    "Result recording rejects duplicate persisted ordering position state: userOrderPosition="
                        + userOrderPosition
                );
            }
        }
    }

    private ResultOrgContextSnapshot resolveAssignedOrgContextSnapshot(Assignment assignment) {
        List<AssignmentCampaignRecipientSnapshot> matchingSnapshots = recipientSnapshotRepository
            .findAssignmentCampaignRecipientSnapshotsByCampaignId(assignment.campaignId()).stream()
            .filter(snapshot -> Objects.equals(snapshot.userId(), assignment.userId()))
            .toList();
        if (matchingSnapshots.size() != 1) {
            throw new ConflictException(
                "Assigned result recording requires exactly one recipient org snapshot: assignmentId=" + assignment.id()
            );
        }
        AssignmentCampaignRecipientSnapshot snapshot = matchingSnapshots.get(0);
        return new ResultOrgContextSnapshot(
            snapshot.organizationalUnitIdSnapshot(),
            snapshot.organizationalPathSnapshot()
        );
    }

    private void requireConsistentAssignedAnchor(
        TestAttempt terminalizedAttempt,
        AssignmentTest assignmentTest,
        Assignment assignment
    ) {
        if (!Objects.equals(assignmentTest.id(), terminalizedAttempt.assignmentTestId())) {
            throw new ConflictException("Assigned result recording anchor mismatch: assignmentTestId=" + terminalizedAttempt.assignmentTestId());
        }
        if (!Objects.equals(assignmentTest.testId(), terminalizedAttempt.testId())) {
            throw new ConflictException("Assigned result recording test mismatch: assignmentTestId=" + assignmentTest.id());
        }
        if (!Objects.equals(assignment.userId(), terminalizedAttempt.userId())) {
            throw new ConflictException("Assigned result recording user mismatch: assignmentId=" + assignment.id());
        }
    }

    private Long requireAssignmentTestId(TestAttempt terminalizedAttempt) {
        if (terminalizedAttempt.assignmentTestId() == null) {
            throw new ConflictException("Assigned result recording requires assignmentTestId: " + terminalizedAttempt.id());
        }
        return terminalizedAttempt.assignmentTestId();
    }

    private Instant terminalInstantOf(TestAttempt terminalizedAttempt) {
        return switch (terminalizedAttempt.status()) {
            case COMPLETED -> requireTerminalInstant(terminalizedAttempt.completedAt(), terminalizedAttempt.status());
            case EXPIRED -> requireTerminalInstant(terminalizedAttempt.expiredAt(), terminalizedAttempt.status());
            case ABANDONED -> requireTerminalInstant(terminalizedAttempt.abandonedAt(), terminalizedAttempt.status());
            case STARTED, IN_PROGRESS -> throw new ConflictException(
                "Result recording snapshot facts require a terminalized attempt: " + terminalizedAttempt.id()
            );
        };
    }

    private Instant requireTerminalInstant(Instant terminalInstant, TestAttemptStatus status) {
        if (terminalInstant == null) {
            throw new ConflictException("Terminalized attempt must expose terminal timestamp for status " + status);
        }
        return terminalInstant;
    }

    private record FrozenAttemptSnapshotSource(
        Test test,
        List<FrozenQuestionSnapshotSource> questionSnapshotSources
    ) {
    }

    private record FrozenQuestionSnapshotSource(
        TestQuestion testQuestion,
        Question question,
        List<AnswerOption> answerOptions,
        List<UserAnswerItem> answerItems
    ) {
    }
}
