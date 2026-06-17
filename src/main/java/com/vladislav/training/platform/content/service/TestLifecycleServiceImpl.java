package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.application.policy.CapabilityOperationCode;
import com.vladislav.training.platform.application.policy.CapabilityTargetEntityType;
import com.vladislav.training.platform.audit.domain.AuditEventType;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Test;
import com.vladislav.training.platform.content.repository.TestRepository;
import com.vladislav.training.platform.content.repository.TopicRepository;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса {@code TestLifecycleServiceImpl}.
 */
@Service
@Transactional
public class TestLifecycleServiceImpl implements TestLifecycleService {

    private final TestRepository repository;
    private final ContentCommandSupport support;
    private final CriticalCommandAuditSupport auditSupport;
    private final TopicRepository topicRepository;
    private final ContentPublicationValidationService publicationValidationService;
    private final UtcClock utcClock;

    public TestLifecycleServiceImpl(
            TestRepository repository,
            ContentCommandSupport support,
            CriticalCommandAuditSupport auditSupport,
            TopicRepository topicRepository,
            ContentPublicationValidationService publicationValidationService,
            UtcClock utcClock
    ) {
        this.repository = repository;
        this.support = support;
        this.auditSupport = auditSupport;
        this.topicRepository = topicRepository;
        this.publicationValidationService = publicationValidationService;
        this.utcClock = utcClock;
    }

    @Override
    public Test publish(Long id) {
        Test existing = repository.findTestById(id);
        support.checkUpdate(CapabilityOperationCode.CONTENT_PUBLISH, CapabilityTargetEntityType.TEST, id);
        support.requireDraft(existing.status(), "Test");
        if (topicRepository.findTopicById(existing.topicId()).status() != ContentStatus.PUBLISHED) {
            throw new ConflictException("Parent topic must be PUBLISHED");
        }
        publicationValidationService.validateTestPublishable(id);
        var now = utcClock.now();
        Test saved = repository.saveTest(new Test(
                existing.id(),
                existing.topicId(),
                existing.name(),
                existing.description(),
                existing.testType(),
                ContentStatus.PUBLISHED,
                existing.thresholdPercent(),
                existing.scoringPolicyCode(),
                false,
                existing.sortOrder(),
                existing.createdAt(),
                now
        ));
        auditSupport.recordAudit(
                auditSupport.resolveInteractiveActorUserId(),
                new AuditEventType(ContentAuditCatalog.EVENT_TYPE_CONTENT_TEST_PUBLISHED),
                "test",
                saved.id(),
                existing,
                saved,
                auditSupport.buildAuditContext(
                        "content",
                        CapabilityOperationCode.CONTENT_PUBLISH,
                        Map.of("entityType", "test")
                )
        );
        return saved;
    }

    @Override
    public Test archive(Long id) {
        Test existing = repository.findTestById(id);
        support.checkUpdate(CapabilityOperationCode.CONTENT_ARCHIVE, CapabilityTargetEntityType.TEST, id);
        support.requirePublished(existing.status(), "Test");
        if (existing.isActiveFinalForTopic()) {
            throw new ConflictException("Current active final test cannot be archived via generic lifecycle");
        }
        var now = utcClock.now();
        Test saved = repository.saveTest(new Test(
                existing.id(),
                existing.topicId(),
                existing.name(),
                existing.description(),
                existing.testType(),
                ContentStatus.ARCHIVED,
                existing.thresholdPercent(),
                existing.scoringPolicyCode(),
                false,
                existing.sortOrder(),
                existing.createdAt(),
                now
        ));
        auditSupport.recordAudit(
                auditSupport.resolveInteractiveActorUserId(),
                new AuditEventType(ContentAuditCatalog.EVENT_TYPE_CONTENT_TEST_ARCHIVED),
                "test",
                saved.id(),
                existing,
                saved,
                auditSupport.buildAuditContext(
                        "content",
                        CapabilityOperationCode.CONTENT_ARCHIVE,
                        Map.of("entityType", "test")
                )
        );
        return saved;
    }
}
