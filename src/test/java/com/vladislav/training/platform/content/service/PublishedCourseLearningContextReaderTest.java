package com.vladislav.training.platform.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Course;
import com.vladislav.training.platform.content.domain.Material;
import com.vladislav.training.platform.content.domain.MaterialType;
import com.vladislav.training.platform.content.domain.Topic;
import com.vladislav.training.platform.content.repository.CourseRepository;
import com.vladislav.training.platform.content.repository.MaterialRepository;
import com.vladislav.training.platform.content.repository.TopicRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет чтение данных в {@code PublishedCourseLearningContext}.
 * Тест держит под контролем выборку и состав возвращаемых данных.
 */
@ExtendWith(MockitoExtension.class)
class PublishedCourseLearningContextReaderTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-20T09:00:00Z");

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private TopicRepository topicRepository;
    @Mock
    private MaterialRepository materialRepository;

    @Test
    void readsOnlyPublishedCourseTopicsAndMaterialsForAssignedLearningContext() {
        PublishedCourseLearningContextReader reader = reader();
        Course course = new Course(301L, "Course", "Published", ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        Topic topic = new Topic(401L, 301L, "Topic", "Published", ContentStatus.PUBLISHED, 0, FIXED_INSTANT, FIXED_INSTANT);
        Material material = new Material(
            501L,
            401L,
            "Material",
            "Published",
            null,
            null,
            MaterialType.TEXT,
            ContentStatus.PUBLISHED,
            0,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
        when(courseRepository.findCourseById(301L)).thenReturn(course);
        when(topicRepository.findTopicsByCourseIdAndStatus(301L, ContentStatus.PUBLISHED)).thenReturn(List.of(topic));
        when(materialRepository.findMaterialsByTopicIdAndStatus(401L, ContentStatus.PUBLISHED)).thenReturn(List.of(material));

        PublishedCourseLearningContext context = reader.readPublishedCourseLearningContext(301L);

        assertThat(context.course()).isEqualTo(course);
        assertThat(context.topics()).containsExactly(topic);
        assertThat(context.materials()).containsExactly(material);

        verify(courseRepository).findCourseById(301L);
        verify(topicRepository).findTopicsByCourseIdAndStatus(301L, ContentStatus.PUBLISHED);
        verify(materialRepository).findMaterialsByTopicIdAndStatus(401L, ContentStatus.PUBLISHED);
        verifyNoMoreInteractions(courseRepository, topicRepository, materialRepository);
    }

    @Test
    void failsClosedWhenAssignedCourseIsNotPublished() {
        PublishedCourseLearningContextReader reader = reader();
        when(courseRepository.findCourseById(301L)).thenReturn(
            new Course(301L, "Course", "Draft", ContentStatus.DRAFT, 0, FIXED_INSTANT, FIXED_INSTANT)
        );

        assertThatThrownBy(() -> reader.readPublishedCourseLearningContext(301L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Published course not found");

        verify(courseRepository).findCourseById(301L);
        verifyNoMoreInteractions(courseRepository, topicRepository, materialRepository);
    }

    private PublishedCourseLearningContextReader reader() {
        return new PublishedCourseLearningContextReader(courseRepository, topicRepository, materialRepository);
    }
}
