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
import com.vladislav.training.platform.content.repository.TopicRepository;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация командного сервиса {@code TopicCommandServiceImpl}.
 */
@Service
@Transactional
public class TopicCommandServiceImpl implements TopicCommandService {

    private final TopicRepository topicRepository;
    private final CourseRepository courseRepository;
    private final ContentCommandSupport support;
    private final CriticalCommandAuditSupport auditSupport;
    private final UtcClock utcClock;

    public TopicCommandServiceImpl(
        TopicRepository topicRepository,
        CourseRepository courseRepository,
        ContentCommandSupport support,
        CriticalCommandAuditSupport auditSupport,
        UtcClock utcClock
    ) {
        this.topicRepository = topicRepository;
        this.courseRepository = courseRepository;
        this.support = support;
        this.auditSupport = auditSupport;
        this.utcClock = utcClock;
    }

    @Override
    public Topic createTopic(CreateTopicCommand command) {
        support.requireNonNull("courseId", command.courseId());
        support.requireNotBlank("name", command.name());
        support.validateNonNegative("sortOrder", command.sortOrder());
        support.checkChildRootCreate(
            CapabilityOperationCode.CONTENT_DRAFT_CREATE,
            CapabilityTargetEntityType.TOPIC,
            command.courseId(),
            CapabilityTargetEntityType.COURSE
        );
        var parent = courseRepository.findCourseById(command.courseId());
        support.validateParentForChildRootAuthoring(parent.status(), "Course");
        validateTopicSortOrderAvailable(command.courseId(), command.sortOrder(), null);
        var now = utcClock.now();
        Topic saved = topicRepository.saveTopic(new Topic(
            null,
            command.courseId(),
            command.name(),
            command.description(),
            ContentStatus.DRAFT,
            command.sortOrder(),
            now,
            now
        ));
        auditSupport.recordAudit(
            auditSupport.resolveInteractiveActorUserId(),
            new AuditEventType(ContentAuditCatalog.EVENT_TYPE_CONTENT_TOPIC_DRAFT_CREATED),
            "topic",
            saved.id(),
            null,
            saved,
            auditSupport.buildAuditContext(
                "content",
                CapabilityOperationCode.CONTENT_DRAFT_CREATE.code(),
                Map.of("entityType", "topic", "courseId", saved.courseId())
            )
        );
        return saved;
    }

    @Override
    public Topic updateTopic(Long topicId, UpdateTopicCommand command) {
        var existing = topicRepository.findTopicById(topicId);
        support.checkDraftUpdate(
            CapabilityOperationCode.CONTENT_DRAFT_UPDATE,
            CapabilityTargetEntityType.TOPIC,
            topicId,
            existing.courseId(),
            CapabilityTargetEntityType.COURSE
        );
        support.requireDraft(existing.status(), "Topic");
        support.requireNotBlank("name", command.name());
        support.validateNonNegative("sortOrder", command.sortOrder());
        validateTopicSortOrderAvailable(existing.courseId(), command.sortOrder(), existing.id());
        var now = utcClock.now();
        Topic saved = topicRepository.saveTopic(new Topic(
            existing.id(),
            existing.courseId(),
            command.name(),
            command.description(),
            existing.status(),
            command.sortOrder(),
            existing.createdAt(),
            now
        ));
        auditSupport.recordAudit(
            auditSupport.resolveInteractiveActorUserId(),
            new AuditEventType(ContentAuditCatalog.EVENT_TYPE_CONTENT_TOPIC_DRAFT_UPDATED),
            "topic",
            saved.id(),
            existing,
            saved,
            auditSupport.buildAuditContext(
                "content",
                CapabilityOperationCode.CONTENT_DRAFT_UPDATE.code(),
                Map.of("entityType", "topic", "courseId", saved.courseId())
            )
        );
        return saved;
    }

    private void validateTopicSortOrderAvailable(Long courseId, int sortOrder, Long currentTopicId) {
        boolean conflict = topicRepository.findTopicsByCourseId(courseId).stream()
            .anyMatch(topic -> topic.sortOrder() == sortOrder && !Objects.equals(topic.id(), currentTopicId));
        if (conflict) {
            throw new ConflictException("Topic sortOrder must be unique within course");
        }
    }
}
