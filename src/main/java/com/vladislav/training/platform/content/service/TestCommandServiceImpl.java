package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.application.policy.CapabilityOperationCode;
import com.vladislav.training.platform.application.policy.CapabilityTargetEntityType;
import com.vladislav.training.platform.audit.domain.AuditEventType;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Test;
import com.vladislav.training.platform.content.domain.TestQuestion;
import com.vladislav.training.platform.content.repository.QuestionRepository;
import com.vladislav.training.platform.content.repository.TestQuestionRepository;
import com.vladislav.training.platform.content.repository.TestRepository;
import com.vladislav.training.platform.content.repository.TopicRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация командного сервиса {@code TestCommandServiceImpl}.
 */
@Service
@Transactional
public class TestCommandServiceImpl implements TestCommandService {

    private final TestRepository testRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final QuestionRepository questionRepository;
    private final TopicRepository topicRepository;
    private final ContentCommandSupport support;
    private final CriticalCommandAuditSupport auditSupport;
    private final UtcClock utcClock;

    public TestCommandServiceImpl(
        TestRepository testRepository,
        TestQuestionRepository testQuestionRepository,
        QuestionRepository questionRepository,
        TopicRepository topicRepository,
        ContentCommandSupport support,
        CriticalCommandAuditSupport auditSupport,
        UtcClock utcClock
    ) {
        this.testRepository = testRepository;
        this.testQuestionRepository = testQuestionRepository;
        this.questionRepository = questionRepository;
        this.topicRepository = topicRepository;
        this.support = support;
        this.auditSupport = auditSupport;
        this.utcClock = utcClock;
    }

    @Override
    public Test createTest(CreateTestCommand command) {
        support.requireNonNull("topicId", command.topicId());
        support.requireNotBlank("name", command.name());
        support.requireNonNull("testType", command.testType());
        support.requireNotBlank("scoringPolicyCode", command.scoringPolicyCode());
        support.validateNonNegative("sortOrder", command.sortOrder());
        support.checkChildRootCreate(
            CapabilityOperationCode.CONTENT_DRAFT_CREATE,
            CapabilityTargetEntityType.TEST,
            command.topicId(),
            CapabilityTargetEntityType.TOPIC
        );
        var parent = topicRepository.findTopicById(command.topicId());
        support.validateParentForChildRootAuthoring(parent.status(), "Topic");
        support.validateThreshold(command.thresholdPercent());
        validateTestSortOrderAvailable(command.topicId(), command.sortOrder(), null);
        var now = utcClock.now();
        Test saved = testRepository.saveTest(new Test(
            null,
            command.topicId(),
            command.name(),
            command.description(),
            command.testType(),
            ContentStatus.DRAFT,
            command.thresholdPercent(),
            command.scoringPolicyCode(),
            false,
            command.sortOrder(),
            now,
            now
        ));
        auditSupport.recordAudit(
            auditSupport.resolveInteractiveActorUserId(),
            new AuditEventType(ContentAuditCatalog.EVENT_TYPE_CONTENT_TEST_DRAFT_CREATED),
            "test",
            saved.id(),
            null,
            saved,
            auditSupport.buildAuditContext(
                "content",
                CapabilityOperationCode.CONTENT_DRAFT_CREATE.code(),
                Map.of("entityType", "test", "topicId", saved.topicId())
            )
        );
        return saved;
    }

    @Override
    public Test updateTest(Long testId, UpdateTestCommand command) {
        var existing = testRepository.findTestById(testId);
        support.checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.TEST,
            testId,
            existing.topicId(),
            CapabilityTargetEntityType.TOPIC
        );
        support.requireDraft(existing.status(), "Test");
        support.requireNotBlank("name", command.name());
        support.requireNonNull("testType", command.testType());
        support.requireNotBlank("scoringPolicyCode", command.scoringPolicyCode());
        support.validateNonNegative("sortOrder", command.sortOrder());
        support.validateThreshold(command.thresholdPercent());
        validateTestSortOrderAvailable(existing.topicId(), command.sortOrder(), existing.id());
        var now = utcClock.now();
        Test saved = testRepository.saveTest(new Test(
            existing.id(),
            existing.topicId(),
            command.name(),
            command.description(),
            command.testType(),
            existing.status(),
            command.thresholdPercent(),
            command.scoringPolicyCode(),
            existing.isActiveFinalForTopic(),
            command.sortOrder(),
            existing.createdAt(),
            now
        ));
        auditSupport.recordAudit(
            auditSupport.resolveInteractiveActorUserId(),
            new AuditEventType(ContentAuditCatalog.EVENT_TYPE_CONTENT_TEST_DRAFT_UPDATED),
            "test",
            saved.id(),
            existing,
            saved,
            auditSupport.buildAuditContext(
                "content",
                CapabilityOperationCode.CONTENT_DRAFT_UPDATE.code(),
                Map.of("entityType", "test", "topicId", saved.topicId())
            )
        );
        return saved;
    }

    @Override
    public TestQuestion createTestQuestion(Long testId, CreateTestQuestionCommand command) {
        var test = testRepository.findTestById(testId);
        support.checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.TEST,
            test.id(),
            test.id(),
            CapabilityTargetEntityType.TEST
        );
        support.requireDraft(test.status(), "Test");
        support.requireNonNull("questionId", command.questionId());
        support.validateNonNegative("displayOrder", command.displayOrder());
        support.validatePositive("weight", command.weight());
        var question = questionRepository.findQuestionById(command.questionId());
        if (!test.topicId().equals(question.topicId())) {
            throw new ConflictException("Test may include only questions from the same topic");
        }
        var now = utcClock.now();
        TestQuestion candidate = new TestQuestion(
            null,
            testId,
            command.questionId(),
            command.displayOrder(),
            command.weight(),
            now,
            now
        );
        List<TestQuestion> resultingQuestions = new ArrayList<>(testQuestionRepository.findTestQuestionsByTestId(testId));
        resultingQuestions.add(candidate);
        validateUniqueTestQuestionIdentity(resultingQuestions);
        validateUniqueTestQuestionDisplayOrder(resultingQuestions);
        TestQuestion saved = testQuestionRepository.saveTestQuestion(candidate);
        recordTestCompositionAudit(test.id(), CapabilityOperationCode.CONTENT_DRAFT_UPDATE.code(), "create", null, saved);
        return saved;
    }

    @Override
    public TestQuestion updateTestQuestion(Long testId, Long testQuestionId, UpdateTestQuestionCommand command) {
        var existing = testQuestionRepository.findTestQuestionById(testQuestionId);
        if (!testId.equals(existing.testId())) {
            throw new ConflictException("Test question does not belong to test");
        }
        var test = testRepository.findTestById(existing.testId());
        support.checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.TEST,
            test.id(),
            test.id(),
            CapabilityTargetEntityType.TEST
        );
        support.requireDraft(test.status(), "Test");
        support.requireNonNull("questionId", command.questionId());
        support.validateNonNegative("displayOrder", command.displayOrder());
        support.validatePositive("weight", command.weight());
        var question = questionRepository.findQuestionById(command.questionId());
        if (!test.topicId().equals(question.topicId())) {
            throw new ConflictException("Test may include only questions from the same topic");
        }
        var now = utcClock.now();
        TestQuestion candidate = new TestQuestion(
            existing.id(),
            existing.testId(),
            command.questionId(),
            command.displayOrder(),
            command.weight(),
            existing.createdAt(),
            now
        );
        List<TestQuestion> resultingQuestions = loadTestCompositionEnsuringExistingPresent(testId, existing);
        replaceTestQuestion(resultingQuestions, candidate);
        validateUniqueTestQuestionIdentity(resultingQuestions);
        validateUniqueTestQuestionDisplayOrder(resultingQuestions);
        TestQuestion saved = testQuestionRepository.saveTestQuestion(candidate);
        recordTestCompositionAudit(test.id(), CapabilityOperationCode.CONTENT_DRAFT_UPDATE.code(), "update", existing, saved);
        return saved;
    }

    @Override
    public void deleteTestQuestion(Long testId, Long testQuestionId) {
        var existing = testQuestionRepository.findTestQuestionById(testQuestionId);
        if (!testId.equals(existing.testId())) {
            throw new ConflictException("Test question does not belong to test");
        }
        var test = testRepository.findTestById(existing.testId());
        support.checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.TEST,
            test.id(),
            test.id(),
            CapabilityTargetEntityType.TEST
        );
        support.requireDraft(test.status(), "Test");
        testQuestionRepository.deleteTestQuestion(testQuestionId);
        recordTestCompositionAudit(test.id(), CapabilityOperationCode.CONTENT_DRAFT_UPDATE.code(), "delete", existing, null);
    }

    private void recordTestCompositionAudit(
        Long testId,
        String operationCode,
        String action,
        TestQuestion payloadBefore,
        TestQuestion payloadAfter
    ) {
        auditSupport.recordAudit(
            auditSupport.resolveInteractiveActorUserId(),
            new AuditEventType(ContentAuditCatalog.EVENT_TYPE_CONTENT_TEST_DRAFT_COMPOSITION_UPDATED),
            "test",
            testId,
            payloadBefore,
            payloadAfter,
            auditSupport.buildAuditContext(
                "content",
                operationCode,
                Map.of("entityType", "test_question", "testId", testId, "action", action)
            )
        );
    }

    private List<TestQuestion> loadTestCompositionEnsuringExistingPresent(Long testId, TestQuestion existing) {
        List<TestQuestion> testQuestions = new ArrayList<>(testQuestionRepository.findTestQuestionsByTestId(testId));
        boolean alreadyPresent = testQuestions.stream()
            .anyMatch(testQuestion -> testQuestion.id().equals(existing.id()));
        if (!alreadyPresent) {
            testQuestions.add(existing);
        }
        return testQuestions;
    }

    private void replaceTestQuestion(List<TestQuestion> testQuestions, TestQuestion replacement) {
        for (int i = 0; i < testQuestions.size(); i++) {
            if (testQuestions.get(i).id().equals(replacement.id())) {
                testQuestions.set(i, replacement);
                return;
            }
        }
        throw new ConflictException("Test question does not belong to test");
    }

    private void validateUniqueTestQuestionDisplayOrder(List<TestQuestion> testQuestions) {
        var seenDisplayOrders = new HashSet<Integer>();
        for (TestQuestion testQuestion : testQuestions) {
            if (!seenDisplayOrders.add(testQuestion.displayOrder())) {
                throw new ConflictException("Test question displayOrder must be unique within test");
            }
        }
    }

    private void validateUniqueTestQuestionIdentity(List<TestQuestion> testQuestions) {
        var seenQuestionIds = new HashSet<Long>();
        for (TestQuestion testQuestion : testQuestions) {
            if (!seenQuestionIds.add(testQuestion.questionId())) {
                throw new ConflictException("Test question questionId must be unique within test");
            }
        }
    }

    private void validateTestSortOrderAvailable(Long topicId, int sortOrder, Long currentTestId) {
        boolean conflict = testRepository.findTestsByTopicId(topicId).stream()
            .anyMatch(test -> test.sortOrder() == sortOrder && !Objects.equals(test.id(), currentTestId));
        if (conflict) {
            throw new ConflictException("Test sortOrder must be unique within topic");
        }
    }
}
