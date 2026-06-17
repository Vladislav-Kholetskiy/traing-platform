package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.application.policy.CapabilityOperationCode;
import com.vladislav.training.platform.audit.domain.AuditEventType;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.content.domain.Test;
import com.vladislav.training.platform.content.domain.Topic;
import com.vladislav.training.platform.content.repository.TestRepository;
import com.vladislav.training.platform.content.repository.TopicRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса {@code TopicFinalControlServiceImpl}.
 */
@Service
@Transactional
public class TopicFinalControlServiceImpl implements TopicFinalControlService {

    private static final String MUTATION_KIND_ASSIGN = "ASSIGN";
    private static final String MUTATION_KIND_REPLACE = "REPLACE";
    private static final String MUTATION_KIND_CLEAR = "CLEAR";

    private final TestRepository testRepository;
    private final TopicRepository topicRepository;
    private final ContentCommandSupport support;
    private final CriticalCommandAuditSupport auditSupport;
    private final UtcClock utcClock;
    private final TestQueryService testQueryService;

    public TopicFinalControlServiceImpl(
        TestRepository testRepository,
        TopicRepository topicRepository,
        ContentCommandSupport support,
        CriticalCommandAuditSupport auditSupport,
        UtcClock utcClock,
        TestQueryService testQueryService
    ) {
        this.testRepository = testRepository;
        this.topicRepository = topicRepository;
        this.support = support;
        this.auditSupport = auditSupport;
        this.utcClock = utcClock;
        this.testQueryService = testQueryService;
    }

    @Override
    public void assignActiveFinalTest(Long topicId, Long testId) {
        support.checkFinal(CapabilityOperationCode.CONTENT_FINAL_ASSIGN, topicId, testId);
        doAssignOrReplace(topicId, testId, false);
    }

    @Override
    public void replaceActiveFinalTest(Long topicId, Long testId) {
        support.checkFinal(CapabilityOperationCode.CONTENT_FINAL_REPLACE, topicId, testId);
        doAssignOrReplace(topicId, testId, true);
    }

    @Override
    public void clearActiveFinalTest(Long topicId) {
        support.checkFinal(CapabilityOperationCode.CONTENT_FINAL_CLEAR, topicId);
        Topic topic = topicRepository.findTopicById(topicId);
        support.requirePublished(topic.status(), "Topic");
        Optional<Test> current = testRepository.findActiveFinalTestByTopicIdForUpdate(topicId);
        if (current.isEmpty()) {
            recordActiveFinalAudit(
                new AuditEventType(ContentAuditCatalog.EVENT_TYPE_CONTENT_TOPIC_ACTIVE_FINAL_CLEARED),
                CapabilityOperationCode.CONTENT_FINAL_CLEAR,
                topicId,
                null,
                null,
                MUTATION_KIND_CLEAR
            );
            return;
        }
        Test existing = current.get();
        var now = utcClock.now();
        Test cleared = new Test(
            existing.id(),
            existing.topicId(),
            existing.name(),
            existing.description(),
            existing.testType(),
            existing.status(),
            existing.thresholdPercent(),
            existing.scoringPolicyCode(),
            false,
            existing.sortOrder(),
            existing.createdAt(),
            now
        );
        testRepository.saveTest(cleared);
        recordActiveFinalAudit(
            new AuditEventType(ContentAuditCatalog.EVENT_TYPE_CONTENT_TOPIC_ACTIVE_FINAL_CLEARED),
            CapabilityOperationCode.CONTENT_FINAL_CLEAR,
            topicId,
            existing,
            cleared,
            MUTATION_KIND_CLEAR
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Test> findActiveFinalTestByTopicId(Long topicId) {
        return testQueryService.findActiveFinalTestByTopicId(topicId);
    }

    private void doAssignOrReplace(Long topicId, Long testId, boolean replace) {
        Topic topic = topicRepository.findTopicById(topicId);
        support.requirePublished(topic.status(), "Topic");
        Test candidate = testRepository.lockTestById(testId);
        if (!candidate.topicId().equals(topicId)) {
            throw new ConflictException("Test must belong to the same topic");
        }
        support.validateActiveFinalEligibility(candidate);
        Optional<Test> current = testRepository.findActiveFinalTestByTopicIdForUpdate(topicId);
        if (current.isPresent() && current.get().id().equals(testId)) {
            recordActiveFinalAudit(
                new AuditEventType(replace
                    ? ContentAuditCatalog.EVENT_TYPE_CONTENT_TOPIC_ACTIVE_FINAL_REPLACED
                    : ContentAuditCatalog.EVENT_TYPE_CONTENT_TOPIC_ACTIVE_FINAL_ASSIGNED),
                replace
                    ? CapabilityOperationCode.CONTENT_FINAL_REPLACE
                    : CapabilityOperationCode.CONTENT_FINAL_ASSIGN,
                topicId,
                current.get(),
                current.get(),
                replace ? MUTATION_KIND_REPLACE : MUTATION_KIND_ASSIGN
            );
            return;
        }
        if (!replace && current.isPresent()) {
            throw new ConflictException("Topic already has active final test");
        }
        var now = utcClock.now();
        Test updated;
        try {
            current.ifPresent(existing -> testRepository.saveTest(new Test(
                existing.id(),
                existing.topicId(),
                existing.name(),
                existing.description(),
                existing.testType(),
                existing.status(),
                existing.thresholdPercent(),
                existing.scoringPolicyCode(),
                false,
                existing.sortOrder(),
                existing.createdAt(),
                now
            )));
            updated = new Test(
                candidate.id(),
                candidate.topicId(),
                candidate.name(),
                candidate.description(),
                candidate.testType(),
                candidate.status(),
                candidate.thresholdPercent(),
                candidate.scoringPolicyCode(),
                true,
                candidate.sortOrder(),
                candidate.createdAt(),
                now
            );
            testRepository.saveTest(updated);
        } catch (PersistenceConstraintViolationException exception) {
            throw new ConflictException("Concurrent active final test change detected");
        }
        recordActiveFinalAudit(
            new AuditEventType(current.isPresent()
                ? ContentAuditCatalog.EVENT_TYPE_CONTENT_TOPIC_ACTIVE_FINAL_REPLACED
                : ContentAuditCatalog.EVENT_TYPE_CONTENT_TOPIC_ACTIVE_FINAL_ASSIGNED),
            current.isPresent()
                ? CapabilityOperationCode.CONTENT_FINAL_REPLACE
                : CapabilityOperationCode.CONTENT_FINAL_ASSIGN,
            topicId,
            current.orElse(null),
            updated,
            current.isPresent() ? MUTATION_KIND_REPLACE : MUTATION_KIND_ASSIGN
        );
    }

    private void recordActiveFinalAudit(
        AuditEventType eventType,
        CapabilityOperationCode operationCode,
        Long topicId,
        Test payloadBefore,
        Test payloadAfter,
        String mutationKind
    ) {
        auditSupport.recordAudit(
            auditSupport.resolveInteractiveActorUserId(),
            eventType,
            "topic",
            topicId,
            payloadBefore,
            payloadAfter,
            auditSupport.buildAuditContext(
                "content",
                operationCode,
                buildAuditDetails(topicId, payloadBefore, payloadAfter, mutationKind)
            )
        );
    }

    private Map<String, Object> buildAuditDetails(Long topicId, Test payloadBefore, Test payloadAfter, String mutationKind) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("topicId", topicId);
        details.put("previousActiveTestId", activeFinalTestId(payloadBefore));
        details.put("newActiveTestId", activeFinalTestId(payloadAfter));
        details.put("mutationKind", mutationKind);
        return details;
    }

    private Long activeFinalTestId(Test payload) {
        if (payload == null || !payload.isActiveFinalForTopic()) {
            return null;
        }
        return payload.id();
    }
}
