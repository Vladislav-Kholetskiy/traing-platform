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
import com.vladislav.training.platform.content.domain.Topic;
import com.vladislav.training.platform.content.repository.CourseRepository;
import com.vladislav.training.platform.content.repository.MaterialRepository;
import com.vladislav.training.platform.content.repository.QuestionRepository;
import com.vladislav.training.platform.content.repository.TestRepository;
import com.vladislav.training.platform.content.repository.TopicRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code TopicLifecycleServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class TopicLifecycleServiceImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-05T10:00:00Z");

    @Mock private TopicRepository repository;
    @Mock private ContentCommandSupport support;
    @Mock private com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport auditSupport;
    @Mock private CourseRepository courseRepository;
    @Mock private MaterialRepository materialRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private TestRepository testRepository;
    @Mock private UtcClock utcClock;

    @Test
    void publishTopicAllowsOnlyDraft() {
        TopicLifecycleServiceImpl service = new TopicLifecycleServiceImpl(repository, support, auditSupport, courseRepository, materialRepository, questionRepository, testRepository, utcClock);
        Topic existing = topic(10L, 20L, ContentStatus.DRAFT);
        Topic published = topic(10L, 20L, ContentStatus.PUBLISHED);
        when(repository.findTopicById(10L)).thenReturn(existing);
        when(courseRepository.findCourseById(20L)).thenReturn(course(20L, ContentStatus.PUBLISHED));
        when(repository.saveTopic(org.mockito.ArgumentMatchers.any())).thenReturn(published);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        Topic result = service.publish(10L);

        assertThat(result.status()).isEqualTo(ContentStatus.PUBLISHED);
        verify(repository).saveTopic(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void publishTopicRejectsNonDraftState() {
        TopicLifecycleServiceImpl service = new TopicLifecycleServiceImpl(repository, support, auditSupport, courseRepository, materialRepository, questionRepository, testRepository, utcClock);
        when(repository.findTopicById(10L)).thenReturn(new Topic(10L, 20L, "Topic", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT));
        doCallRealMethod().when(support).requireDraft(org.mockito.ArgumentMatchers.any(), anyString());

        assertThatThrownBy(() -> service.publish(10L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Topic must be DRAFT");

        org.mockito.Mockito.verify(repository, never()).saveTopic(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void publishTopicRequiresPublishedParentCourse() {
        TopicLifecycleServiceImpl service = new TopicLifecycleServiceImpl(repository, support, auditSupport, courseRepository, materialRepository, questionRepository, testRepository, utcClock);
        when(repository.findTopicById(10L)).thenReturn(topic(10L, 20L, ContentStatus.DRAFT));
        when(courseRepository.findCourseById(20L)).thenReturn(course(20L, ContentStatus.DRAFT));

        assertThatThrownBy(() -> service.publish(10L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Parent course must be PUBLISHED");

        verify(repository, never()).saveTopic(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void archiveTopicRejectsNonPublishedState() {
        TopicLifecycleServiceImpl service = new TopicLifecycleServiceImpl(repository, support, auditSupport, courseRepository, materialRepository, questionRepository, testRepository, utcClock);
        when(repository.findTopicById(11L)).thenReturn(new Topic(11L, 21L, "Topic", null, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT));
        doCallRealMethod().when(support).requirePublished(org.mockito.ArgumentMatchers.any(), anyString());

        assertThatThrownBy(() -> service.archive(11L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Topic must be PUBLISHED");

        org.mockito.Mockito.verify(repository, never()).saveTopic(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void archiveTopicRejectsWhenActiveChildrenRemain() {
        TopicLifecycleServiceImpl service = new TopicLifecycleServiceImpl(repository, support, auditSupport, courseRepository, materialRepository, questionRepository, testRepository, utcClock);
        when(repository.findTopicById(10L)).thenReturn(topic(10L, 20L, ContentStatus.PUBLISHED));
        when(materialRepository.existsNonArchivedByTopicId(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.archive(10L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("child content exists");

        verify(repository, never()).saveTopic(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void archiveTopicRejectsWhenActiveChildQuestionExists() {
        TopicLifecycleServiceImpl service = new TopicLifecycleServiceImpl(repository, support, auditSupport, courseRepository, materialRepository, questionRepository, testRepository, utcClock);
        when(repository.findTopicById(12L)).thenReturn(topic(12L, 20L, ContentStatus.PUBLISHED));
        when(questionRepository.existsNonArchivedByTopicId(12L)).thenReturn(true);

        assertThatThrownBy(() -> service.archive(12L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("child content exists");

        verify(repository, never()).saveTopic(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void archiveTopicRejectsWhenActiveChildTestExists() {
        TopicLifecycleServiceImpl service = new TopicLifecycleServiceImpl(repository, support, auditSupport, courseRepository, materialRepository, questionRepository, testRepository, utcClock);
        when(repository.findTopicById(13L)).thenReturn(topic(13L, 20L, ContentStatus.PUBLISHED));
        when(testRepository.existsNonArchivedByTopicId(13L)).thenReturn(true);

        assertThatThrownBy(() -> service.archive(13L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("child content exists");

        verify(repository, never()).saveTopic(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void archiveTopicAllowsWhenAllChildRootsArchived() {
        TopicLifecycleServiceImpl service = new TopicLifecycleServiceImpl(repository, support, auditSupport, courseRepository, materialRepository, questionRepository, testRepository, utcClock);
        Topic existing = topic(14L, 20L, ContentStatus.PUBLISHED);
        Topic archived = topic(14L, 20L, ContentStatus.ARCHIVED);
        when(repository.findTopicById(14L)).thenReturn(existing);
        when(materialRepository.existsNonArchivedByTopicId(14L)).thenReturn(false);
        when(questionRepository.existsNonArchivedByTopicId(14L)).thenReturn(false);
        when(testRepository.existsNonArchivedByTopicId(14L)).thenReturn(false);
        when(repository.saveTopic(org.mockito.ArgumentMatchers.any())).thenReturn(archived);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        Topic result = service.archive(14L);

        assertThat(result.status()).isEqualTo(ContentStatus.ARCHIVED);
        verify(materialRepository).existsNonArchivedByTopicId(14L);
        verify(questionRepository).existsNonArchivedByTopicId(14L);
        verify(testRepository).existsNonArchivedByTopicId(14L);
        verify(repository).saveTopic(org.mockito.ArgumentMatchers.any());
    }

    private Course course(Long id, ContentStatus status) {
        return new Course(id, "Course", null, status, 0, FIXED_INSTANT, FIXED_INSTANT);
    }

    private Topic topic(Long id, Long courseId, ContentStatus status) {
        return new Topic(id, courseId, "Topic", null, status, 0, FIXED_INSTANT, FIXED_INSTANT);
    }
}
