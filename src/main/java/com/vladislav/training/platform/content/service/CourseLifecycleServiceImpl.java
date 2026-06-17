package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.application.policy.CapabilityOperationCode;
import com.vladislav.training.platform.application.policy.CapabilityTargetEntityType;
import com.vladislav.training.platform.audit.domain.AuditEventType;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Course;
import com.vladislav.training.platform.content.repository.CourseRepository;
import com.vladislav.training.platform.content.repository.TopicRepository;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса {@code CourseLifecycleServiceImpl}.
 */
@Service
@Transactional
public class CourseLifecycleServiceImpl implements CourseLifecycleService {

    private final CourseRepository repository;
    private final ContentCommandSupport support;
    private final CriticalCommandAuditSupport auditSupport;
    private final TopicRepository topicRepository;
    private final UtcClock utcClock;

    public CourseLifecycleServiceImpl(
        CourseRepository repository,
        ContentCommandSupport support,
        CriticalCommandAuditSupport auditSupport,
        TopicRepository topicRepository,
        UtcClock utcClock
    ) {
        this.repository = repository;
        this.support = support;
        this.auditSupport = auditSupport;
        this.topicRepository = topicRepository;
        this.utcClock = utcClock;
    }

    @Override
    public Course publish(Long id) {
        Course existing = repository.findCourseById(id);
        support.checkUpdate(CapabilityOperationCode.CONTENT_PUBLISH, CapabilityTargetEntityType.COURSE, id);
        support.requireDraft(existing.status(), "Course");
        var now = utcClock.now();
        Course saved = repository.saveCourse(new Course(
            existing.id(),
            existing.name(),
            existing.description(),
            ContentStatus.PUBLISHED,
            existing.sortOrder(),
            existing.createdAt(),
            now
        ));
        auditSupport.recordAudit(
            auditSupport.resolveInteractiveActorUserId(),
            new AuditEventType(ContentAuditCatalog.EVENT_TYPE_CONTENT_COURSE_PUBLISHED),
            "course",
            saved.id(),
            existing,
            saved,
            auditSupport.buildAuditContext(
                "content",
                CapabilityOperationCode.CONTENT_PUBLISH,
                Map.of("entityType", "course")
            )
        );
        return saved;
    }

    @Override
    public Course archive(Long id) {
        Course existing = repository.findCourseById(id);
        support.checkUpdate(CapabilityOperationCode.CONTENT_ARCHIVE, CapabilityTargetEntityType.COURSE, id);
        support.requirePublished(existing.status(), "Course");
        if (topicRepository.existsNonArchivedByCourseId(id)) {
            throw new ConflictException("Course cannot be archived while non-archived topics exist");
        }
        var now = utcClock.now();
        Course saved = repository.saveCourse(new Course(
            existing.id(),
            existing.name(),
            existing.description(),
            ContentStatus.ARCHIVED,
            existing.sortOrder(),
            existing.createdAt(),
            now
        ));
        auditSupport.recordAudit(
            auditSupport.resolveInteractiveActorUserId(),
            new AuditEventType(ContentAuditCatalog.EVENT_TYPE_CONTENT_COURSE_ARCHIVED),
            "course",
            saved.id(),
            existing,
            saved,
            auditSupport.buildAuditContext(
                "content",
                CapabilityOperationCode.CONTENT_ARCHIVE,
                Map.of("entityType", "course")
            )
        );
        return saved;
    }
}
