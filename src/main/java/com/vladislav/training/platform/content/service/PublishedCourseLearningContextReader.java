package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Course;
import com.vladislav.training.platform.content.domain.Material;
import com.vladislav.training.platform.content.domain.Topic;
import com.vladislav.training.platform.content.repository.CourseRepository;
import com.vladislav.training.platform.content.repository.MaterialRepository;
import com.vladislav.training.platform.content.repository.TopicRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Читатель {@code PublishedCourseLearningContextReader}.
 */
@Service
@Transactional(readOnly = true)
public class PublishedCourseLearningContextReader {

    private final CourseRepository courseRepository;
    private final TopicRepository topicRepository;
    private final MaterialRepository materialRepository;

    public PublishedCourseLearningContextReader(
        CourseRepository courseRepository,
        TopicRepository topicRepository,
        MaterialRepository materialRepository
    ) {
        this.courseRepository = courseRepository;
        this.topicRepository = topicRepository;
        this.materialRepository = materialRepository;
    }

    public PublishedCourseLearningContext readPublishedCourseLearningContext(Long courseId) {
        Course course = courseRepository.findCourseById(courseId);
        if (course.status() != ContentStatus.PUBLISHED) {
            throw new NotFoundException("Published course not found for assigned learning context: courseId=" + courseId);
        }

        List<Topic> topics = topicRepository.findTopicsByCourseIdAndStatus(courseId, ContentStatus.PUBLISHED);
        List<Material> materials = topics.stream()
            .flatMap(topic -> materialRepository.findMaterialsByTopicIdAndStatus(topic.id(), ContentStatus.PUBLISHED).stream())
            .toList();

        return new PublishedCourseLearningContext(course, topics, materials);
    }
}
