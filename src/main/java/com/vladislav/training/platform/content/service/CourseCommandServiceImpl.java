package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.application.policy.CapabilityOperationCode;
import com.vladislav.training.platform.application.policy.CapabilityTargetEntityType;
import com.vladislav.training.platform.audit.domain.AuditEventType;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Course;
import com.vladislav.training.platform.content.repository.CourseRepository;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация командного сервиса {@code CourseCommandServiceImpl}.
 */
@Service
@Transactional
public class CourseCommandServiceImpl implements CourseCommandService {

    private final CourseRepository repository;
    private final ContentCommandSupport support;
    private final CriticalCommandAuditSupport auditSupport;
    private final UtcClock utcClock;

    public CourseCommandServiceImpl(
        CourseRepository repository,
        ContentCommandSupport support,
        CriticalCommandAuditSupport auditSupport,
        UtcClock utcClock
    ) {
        this.repository = repository;
        this.support = support;
        this.auditSupport = auditSupport;
        this.utcClock = utcClock;
    }

    @Override
    public Course createCourse(CreateCourseCommand command) {
        support.checkCreate(CapabilityOperationCode.CONTENT_DRAFT_CREATE, CapabilityTargetEntityType.COURSE);
        support.requireNotBlank("name", command.name());
        support.validateNonNegative("sortOrder", command.sortOrder());
        var now = utcClock.now();
        Course saved = repository.saveCourse(new Course(
            null,
            command.name(),
            command.description(),
            ContentStatus.DRAFT,
            command.sortOrder(),
            now,
            now
        ));
        auditSupport.recordAudit(
            auditSupport.resolveInteractiveActorUserId(),
            new AuditEventType(ContentAuditCatalog.EVENT_TYPE_CONTENT_COURSE_DRAFT_CREATED),
            "course",
            saved.id(),
            null,
            saved,
            auditSupport.buildAuditContext(
                "content",
                CapabilityOperationCode.CONTENT_DRAFT_CREATE.code(),
                Map.of("entityType", "course")
            )
        );
        return saved;
    }

    @Override
    public Course updateCourse(Long courseId, UpdateCourseCommand command) {
        support.checkDraftUpdate(CapabilityOperationCode.CONTENT_DRAFT_UPDATE, CapabilityTargetEntityType.COURSE, courseId);
        Course existing = repository.findCourseById(courseId);
        support.requireDraft(existing.status(), "Course");
        support.requireNotBlank("name", command.name());
        support.validateNonNegative("sortOrder", command.sortOrder());
        var now = utcClock.now();
        Course saved = repository.saveCourse(new Course(
            existing.id(),
            command.name(),
            command.description(),
            existing.status(),
            command.sortOrder(),
            existing.createdAt(),
            now
        ));
        auditSupport.recordAudit(
            auditSupport.resolveInteractiveActorUserId(),
            new AuditEventType(ContentAuditCatalog.EVENT_TYPE_CONTENT_COURSE_DRAFT_UPDATED),
            "course",
            saved.id(),
            existing,
            saved,
            auditSupport.buildAuditContext(
                "content",
                CapabilityOperationCode.CONTENT_DRAFT_UPDATE.code(),
                Map.of("entityType", "course")
            )
        );
        return saved;
    }
}
