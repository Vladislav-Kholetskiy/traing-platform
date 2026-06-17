package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Course;
import java.util.List;

/**
 * Контракт сервиса чтения {@code CourseQueryService}.
 */
public interface CourseQueryService {

    Course findCourseById(Long courseId);

    List<Course> findAllCourses();

    List<Course> findCoursesByStatus(ContentStatus status);
}
