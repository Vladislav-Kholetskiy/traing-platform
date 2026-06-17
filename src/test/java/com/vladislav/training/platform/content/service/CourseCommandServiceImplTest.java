package com.vladislav.training.platform.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.application.policy.CapabilityOperationCode;
import com.vladislav.training.platform.application.policy.CapabilityTargetEntityType;
import com.vladislav.training.platform.audit.domain.AuditContext;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Course;
import com.vladislav.training.platform.content.repository.CourseRepository;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code CourseCommandServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class CourseCommandServiceImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-05T10:00:00Z");

    @Mock private CourseRepository repository;
    @Mock private ContentCommandSupport support;
    @Mock private CriticalCommandAuditSupport auditSupport;
    @Mock private UtcClock utcClock;

    @Test
    void createCourseCreatesDraftRoot() {
        CourseCommandServiceImpl service = new CourseCommandServiceImpl(repository, support, auditSupport, utcClock);
        when(repository.saveCourse(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        Course result = service.createCourse(new CreateCourseCommand("Course", null, 0));

        assertThat(result.status()).isEqualTo(ContentStatus.DRAFT);
        assertThat(result.sortOrder()).isEqualTo(0);
        verify(support).checkCreate(CapabilityOperationCode.CONTENT_DRAFT_CREATE, CapabilityTargetEntityType.COURSE);
        verify(repository).saveCourse(any(Course.class));
    }

    @Test
    void updateCourseUpdatesDraftRoot() {
        CourseCommandServiceImpl service = new CourseCommandServiceImpl(repository, support, auditSupport, utcClock);
        Course existing = new Course(10L, "Old", null, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);
        when(repository.findCourseById(10L)).thenReturn(existing);
        when(repository.saveCourse(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        Course result = service.updateCourse(10L, new UpdateCourseCommand("New", null, 1));

        assertThat(result.status()).isEqualTo(ContentStatus.DRAFT);
        assertThat(result.name()).isEqualTo("New");
        assertThat(result.sortOrder()).isEqualTo(1);
        verify(support).checkDraftUpdate(CapabilityOperationCode.CONTENT_DRAFT_UPDATE, CapabilityTargetEntityType.COURSE, 10L);
        verify(repository).findCourseById(10L);
        verify(repository).saveCourse(any(Course.class));
    }

    @Test
    void updateCourseUsesSingleOwnerReadAndClockPolicy() {
        CourseCommandServiceImpl service = new CourseCommandServiceImpl(repository, support, auditSupport, utcClock);
        Course existing = new Course(10L, "Old", null, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);
        Course updated = new Course(10L, "New", null, ContentStatus.DRAFT, 1, FIXED_INSTANT, FIXED_INSTANT);
        when(repository.findCourseById(10L)).thenReturn(existing);
        when(repository.saveCourse(any(Course.class))).thenReturn(updated);
        when(auditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(auditSupport.buildAuditContext(eq("content"), eq(CapabilityOperationCode.CONTENT_DRAFT_UPDATE.code()), any(Map.class)))
            .thenReturn(new AuditContext("{}"));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        Course result = service.updateCourse(10L, new UpdateCourseCommand("New", null, 1));

        assertThat(result.name()).isEqualTo("New");
        verify(support).checkDraftUpdate(CapabilityOperationCode.CONTENT_DRAFT_UPDATE, CapabilityTargetEntityType.COURSE, 10L);
        verify(repository).findCourseById(10L);
        verify(repository).saveCourse(any(Course.class));
    }

    @Test
    void updateCourseRejectsNonDraftCourse() {
        CourseCommandServiceImpl service = new CourseCommandServiceImpl(repository, support, auditSupport, utcClock);
        Course existing = new Course(10L, "Old", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        when(repository.findCourseById(10L)).thenReturn(existing);
        doCallRealMethod().when(support).requireDraft(ContentStatus.PUBLISHED, "Course");

        assertThatThrownBy(() -> service.updateCourse(10L, new UpdateCourseCommand("New", null, 1)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Course must be DRAFT");

        verify(repository).findCourseById(10L);
        verify(repository, never()).saveCourse(any(Course.class));
    }
}
