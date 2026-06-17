package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.content.domain.Course;

/**
 * Контракт командного сервиса {@code CourseCommandService}.
 */
public interface CourseCommandService {

    Course createCourse(CreateCourseCommand command);

    Course updateCourse(Long courseId, UpdateCourseCommand command);
}
