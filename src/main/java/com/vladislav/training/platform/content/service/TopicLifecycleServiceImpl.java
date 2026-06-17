package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.application.policy.CapabilityOperationCode;
import com.vladislav.training.platform.application.policy.CapabilityTargetEntityType;
import com.vladislav.training.platform.audit.domain.AuditEventType;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Topic;
import com.vladislav.training.platform.content.repository.CourseRepository;
import com.vladislav.training.platform.content.repository.MaterialRepository;
import com.vladislav.training.platform.content.repository.QuestionRepository;
import com.vladislav.training.platform.content.repository.TestRepository;
import com.vladislav.training.platform.content.repository.TopicRepository;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса {@code TopicLifecycleServiceImpl}.
 */
@Service
@Transactional
public class TopicLifecycleServiceImpl implements TopicLifecycleService {

    private final TopicRepository repository;
    private final ContentCommandSupport support;
    private final CriticalCommandAuditSupport auditSupport;
    private final CourseRepository courseRepository;
    private final MaterialRepository materialRepository;
    private final QuestionRepository questionRepository;
    private final TestRepository testRepository;
    private final UtcClock utcClock;

    public TopicLifecycleServiceImpl(
            TopicRepository repository,
            ContentCommandSupport support,
            CriticalCommandAuditSupport auditSupport,
            CourseRepository courseRepository,
            MaterialRepository materialRepository,
            QuestionRepository questionRepository,
            TestRepository testRepository,
            UtcClock utcClock
    ) {
        this.repository = repository;
        this.support = support;
        this.auditSupport = auditSupport;
        this.courseRepository = courseRepository;
        this.materialRepository = materialRepository;
        this.questionRepository = questionRepository;
        this.testRepository = testRepository;
        this.utcClock = utcClock;
    }

    @Override
    public Topic publish(Long id) {
        Topic existing = repository.findTopicById(id);
        support.checkUpdate(CapabilityOperationCode.CONTENT_PUBLISH, CapabilityTargetEntityType.TOPIC, id);
        support.requireDraft(existing.status(), "Topic");
        if (courseRepository.findCourseById(existing.courseId()).status() != ContentStatus.PUBLISHED) {
            throw new ConflictException("Parent course must be PUBLISHED");
        }
        var now = utcClock.now();
        Topic saved = repository.saveTopic(new Topic(
                existing.id(),
                existing.courseId(),
                existing.name(),
                existing.description(),
                ContentStatus.PUBLISHED,
                existing.sortOrder(),
                existing.createdAt(),
                now
        ));
        auditSupport.recordAudit(
                auditSupport.resolveInteractiveActorUserId(),
                new AuditEventType(ContentAuditCatalog.EVENT_TYPE_CONTENT_TOPIC_PUBLISHED),
                "topic",
                saved.id(),
                existing,
                saved,
                auditSupport.buildAuditContext(
                        "content",
                        CapabilityOperationCode.CONTENT_PUBLISH,
                        Map.of("entityType", "topic")
                )
        );
        return saved;
    }

    @Override
    public Topic archive(Long id) {
        Topic existing = repository.findTopicById(id);
        support.checkUpdate(CapabilityOperationCode.CONTENT_ARCHIVE, CapabilityTargetEntityType.TOPIC, id);
        support.requirePublished(existing.status(), "Topic");
        if (materialRepository.existsNonArchivedByTopicId(id)
                || questionRepository.existsNonArchivedByTopicId(id)
                || testRepository.existsNonArchivedByTopicId(id)) {
            throw new ConflictException("Topic cannot be archived while non-archived child content exists");
        }
        var now = utcClock.now();
        Topic saved = repository.saveTopic(new Topic(
                existing.id(),
                existing.courseId(),
                existing.name(),
                existing.description(),
                ContentStatus.ARCHIVED,
                existing.sortOrder(),
                existing.createdAt(),
                now
        ));
        auditSupport.recordAudit(
                auditSupport.resolveInteractiveActorUserId(),
                new AuditEventType(ContentAuditCatalog.EVENT_TYPE_CONTENT_TOPIC_ARCHIVED),
                "topic",
                saved.id(),
                existing,
                saved,
                auditSupport.buildAuditContext(
                        "content",
                        CapabilityOperationCode.CONTENT_ARCHIVE,
                        Map.of("entityType", "topic")
                )
        );
        return saved;
    }
}
