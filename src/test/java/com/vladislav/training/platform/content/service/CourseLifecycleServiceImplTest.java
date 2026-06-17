package com.vladislav.training.platform.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Course;
import com.vladislav.training.platform.content.repository.CourseRepository;
import com.vladislav.training.platform.content.repository.TopicRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code CourseLifecycleServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class CourseLifecycleServiceImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-05T10:00:00Z");

    @Mock private CourseRepository repository;
    @Mock private ContentCommandSupport support;
    @Mock private com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport auditSupport;
    @Mock private TopicRepository topicRepository;
    @Mock private UtcClock utcClock;

    @Test
    void publishCourseAllowsOnlyDraft() {
        CourseLifecycleServiceImpl service = new CourseLifecycleServiceImpl(repository, support, auditSupport, topicRepository, utcClock);
        Course existing = course(10L, ContentStatus.DRAFT);
        Course published = course(10L, ContentStatus.PUBLISHED);
        when(repository.findCourseById(10L)).thenReturn(existing);
        when(repository.saveCourse(org.mockito.ArgumentMatchers.any())).thenReturn(published);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        Course result = service.publish(10L);

        assertThat(result.status()).isEqualTo(ContentStatus.PUBLISHED);
        verify(repository).saveCourse(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void publishCourseRejectsNonDraftState() {
        CourseLifecycleServiceImpl service = new CourseLifecycleServiceImpl(repository, support, auditSupport, topicRepository, utcClock);
        when(repository.findCourseById(10L)).thenReturn(new Course(10L, "Course", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT));
        doCallRealMethod().when(support).requireDraft(org.mockito.ArgumentMatchers.any(), anyString());

        assertThatThrownBy(() -> service.publish(10L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Course must be DRAFT");

        verify(repository, never()).saveCourse(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void archiveCourseAllowsWhenAllChildTopicsArchived() {
        CourseLifecycleServiceImpl service = new CourseLifecycleServiceImpl(repository, support, auditSupport, topicRepository, utcClock);
        Course existing = course(11L, ContentStatus.PUBLISHED);
        Course archived = course(11L, ContentStatus.ARCHIVED);
        when(repository.findCourseById(11L)).thenReturn(existing);
        when(topicRepository.existsNonArchivedByCourseId(11L)).thenReturn(false);
        when(repository.saveCourse(org.mockito.ArgumentMatchers.any())).thenReturn(archived);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        Course result = service.archive(11L);

        assertThat(result.status()).isEqualTo(ContentStatus.ARCHIVED);
        verify(topicRepository).existsNonArchivedByCourseId(11L);
        verify(repository).saveCourse(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void archiveCourseRejectsNonPublishedState() {
        CourseLifecycleServiceImpl service = new CourseLifecycleServiceImpl(repository, support, auditSupport, topicRepository, utcClock);
        when(repository.findCourseById(11L)).thenReturn(new Course(11L, "Course", null, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT));
        doCallRealMethod().when(support).requirePublished(org.mockito.ArgumentMatchers.any(), anyString());

        assertThatThrownBy(() -> service.archive(11L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Course must be PUBLISHED");

        verify(repository, never()).saveCourse(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void archiveCourseRejectsWhenActiveTopicsRemain() {
        CourseLifecycleServiceImpl service = new CourseLifecycleServiceImpl(repository, support, auditSupport, topicRepository, utcClock);
        when(repository.findCourseById(10L)).thenReturn(new Course(10L, "Course", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT));
        when(topicRepository.existsNonArchivedByCourseId(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.archive(10L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("non-archived topics");

        verify(repository, never()).saveCourse(org.mockito.ArgumentMatchers.any());
    }

    private Course course(Long id, ContentStatus status) {
        return new Course(id, "Course", null, status, 0, FIXED_INSTANT, FIXED_INSTANT);
    }
}
