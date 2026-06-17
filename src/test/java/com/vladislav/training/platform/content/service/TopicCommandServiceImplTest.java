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
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Course;
import com.vladislav.training.platform.content.domain.Topic;
import com.vladislav.training.platform.content.repository.CourseRepository;
import com.vladislav.training.platform.content.repository.TopicRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code TopicCommandServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class TopicCommandServiceImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-05T10:00:00Z");

    @Mock private TopicRepository topicRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private ContentCommandSupport support;
    @Mock private CriticalCommandAuditSupport auditSupport;
    @Mock private UtcClock utcClock;

    @Test
    void createTopicCreatesDraftUnderDraftCourse() {
        TopicCommandServiceImpl service = new TopicCommandServiceImpl(topicRepository, courseRepository, support, auditSupport, utcClock);
        Course parent = new Course(20L, "Course", null, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);
        when(courseRepository.findCourseById(20L)).thenReturn(parent);
        when(topicRepository.findTopicsByCourseId(20L)).thenReturn(List.of());
        when(topicRepository.saveTopic(any(Topic.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        Topic result = service.createTopic(new CreateTopicCommand(20L, "Topic", null, 0));

        assertThat(result.courseId()).isEqualTo(20L);
        assertThat(result.status()).isEqualTo(ContentStatus.DRAFT);
        verify(topicRepository).saveTopic(any(Topic.class));
    }

    @Test
    void createTopicRejectsDuplicateSortOrderWithinCourse() {
        TopicCommandServiceImpl service = new TopicCommandServiceImpl(topicRepository, courseRepository, support, auditSupport, utcClock);
        Course parent = new Course(20L, "Course", null, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);
        Topic sibling = new Topic(31L, 20L, "Existing", null, ContentStatus.DRAFT, 1, FIXED_INSTANT, FIXED_INSTANT);
        when(courseRepository.findCourseById(20L)).thenReturn(parent);
        when(topicRepository.findTopicsByCourseId(20L)).thenReturn(List.of(sibling));

        assertThatThrownBy(() -> service.createTopic(new CreateTopicCommand(20L, "Topic", null, 1)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("sortOrder")
            .hasMessageContaining("unique");

        verify(topicRepository).findTopicsByCourseId(20L);
        verify(topicRepository, never()).saveTopic(any(Topic.class));
    }

    @Test
    void publishedCourseStillAllowsNewDraftTopic() {
        TopicCommandServiceImpl service = new TopicCommandServiceImpl(topicRepository, courseRepository, support, auditSupport, utcClock);
        Course parent = new Course(20L, "Course", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        Topic created = new Topic(30L, 20L, "Topic", null, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);
        when(courseRepository.findCourseById(20L)).thenReturn(parent);
        when(topicRepository.findTopicsByCourseId(20L)).thenReturn(List.of());
        when(topicRepository.saveTopic(any(Topic.class))).thenReturn(created);
        when(auditSupport.resolveInteractiveActorUserId()).thenReturn(777L);
        when(auditSupport.buildAuditContext(eq("content"), eq(CapabilityOperationCode.CONTENT_DRAFT_CREATE.code()), any(Map.class)))
            .thenReturn(new AuditContext("{}"));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        Topic result = service.createTopic(new CreateTopicCommand(20L, "Topic", null, 0));

        assertThat(result.id()).isEqualTo(30L);
        assertThat(result.status()).isEqualTo(ContentStatus.DRAFT);
        verify(support).checkChildRootCreate(CapabilityOperationCode.CONTENT_DRAFT_CREATE, CapabilityTargetEntityType.TOPIC, 20L, CapabilityTargetEntityType.COURSE);
        verify(support).validateParentForChildRootAuthoring(ContentStatus.PUBLISHED, "Course");
    }

    @Test
    void createTopicRejectsArchivedParentCourse() {
        TopicCommandServiceImpl service = new TopicCommandServiceImpl(topicRepository, courseRepository, support, auditSupport, utcClock);
        Course parent = new Course(20L, "Course", null, ContentStatus.ARCHIVED, 0, FIXED_INSTANT, FIXED_INSTANT);
        when(courseRepository.findCourseById(20L)).thenReturn(parent);
        doCallRealMethod().when(support).validateParentForChildRootAuthoring(ContentStatus.ARCHIVED, "Course");

        assertThatThrownBy(() -> service.createTopic(new CreateTopicCommand(20L, "Topic", null, 0)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Course ARCHIVED parent cannot accept child-root authoring");

        verify(topicRepository, never()).saveTopic(any(Topic.class));
    }

    @Test
    void createTopicRejectsMissingParentCourse() {
        TopicCommandServiceImpl service = new TopicCommandServiceImpl(topicRepository, courseRepository, support, auditSupport, utcClock);
        when(courseRepository.findCourseById(20L)).thenThrow(new NotFoundException("Course not found"));

        assertThatThrownBy(() -> service.createTopic(new CreateTopicCommand(20L, "Topic", null, 0)))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Course");

        verify(topicRepository, never()).saveTopic(any(Topic.class));
    }

    @Test
    void updateTopicRejectsDuplicateSortOrderWithinCourse() {
        TopicCommandServiceImpl service = new TopicCommandServiceImpl(topicRepository, courseRepository, support, auditSupport, utcClock);
        Topic existing = new Topic(10L, 20L, "Old", null, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);
        Topic sibling = new Topic(11L, 20L, "Other", null, ContentStatus.DRAFT, 1, FIXED_INSTANT, FIXED_INSTANT);
        when(topicRepository.findTopicById(10L)).thenReturn(existing);
        when(topicRepository.findTopicsByCourseId(20L)).thenReturn(List.of(existing, sibling));

        assertThatThrownBy(() -> service.updateTopic(10L, new UpdateTopicCommand("New", null, 1)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("sortOrder")
            .hasMessageContaining("unique");

        verify(topicRepository).findTopicsByCourseId(20L);
        verify(topicRepository, never()).saveTopic(any(Topic.class));
    }

    @Test
    void updateTopicAllowsCurrentSortOrderWithoutSelfConflict() {
        TopicCommandServiceImpl service = new TopicCommandServiceImpl(topicRepository, courseRepository, support, auditSupport, utcClock);
        Topic existing = new Topic(10L, 20L, "Old", null, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);
        Topic sibling = new Topic(11L, 20L, "Other", null, ContentStatus.DRAFT, 1, FIXED_INSTANT, FIXED_INSTANT);
        Topic updated = new Topic(10L, 20L, "New", null, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);
        when(topicRepository.findTopicById(10L)).thenReturn(existing);
        when(topicRepository.findTopicsByCourseId(20L)).thenReturn(List.of(existing, sibling));
        when(topicRepository.saveTopic(any(Topic.class))).thenReturn(updated);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        Topic result = service.updateTopic(10L, new UpdateTopicCommand("New", null, 0));

        assertThat(result.sortOrder()).isEqualTo(0);
        verify(topicRepository).findTopicsByCourseId(20L);
        verify(topicRepository).saveTopic(any(Topic.class));
    }

    @Test
    void updateTopicUpdatesOnlyDraftTopic() {
        TopicCommandServiceImpl service = new TopicCommandServiceImpl(topicRepository, courseRepository, support, auditSupport, utcClock);
        Topic existing = new Topic(10L, 20L, "Old", null, ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT);
        when(topicRepository.findTopicById(10L)).thenReturn(existing);
        when(topicRepository.findTopicsByCourseId(20L)).thenReturn(List.of(existing));
        when(topicRepository.saveTopic(any(Topic.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(utcClock.now()).thenReturn(FIXED_INSTANT);

        Topic result = service.updateTopic(10L, new UpdateTopicCommand("New", null, 1));

        assertThat(result.status()).isEqualTo(ContentStatus.DRAFT);
        assertThat(result.name()).isEqualTo("New");
        assertThat(result.sortOrder()).isEqualTo(1);
        verify(topicRepository).saveTopic(any(Topic.class));
    }

    @Test
    void updateTopicRejectsNonDraftTopic() {
        TopicCommandServiceImpl service = new TopicCommandServiceImpl(topicRepository, courseRepository, support, auditSupport, utcClock);
        Topic existing = new Topic(10L, 20L, "Old", null, ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        when(topicRepository.findTopicById(10L)).thenReturn(existing);
        doCallRealMethod().when(support).requireDraft(ContentStatus.PUBLISHED, "Topic");

        assertThatThrownBy(() -> service.updateTopic(10L, new UpdateTopicCommand("New", null, 1)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Topic must be DRAFT");

        verify(topicRepository, never()).saveTopic(any(Topic.class));
    }
}
