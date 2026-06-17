package com.vladislav.training.platform.content.repository;

import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Course;
import java.util.List;

/**
 * Контракт репозитория {@code CourseRepository}.
 */
public interface CourseRepository {

    Course findCourseById(Long courseId);

    List<Course> findAllCourses();

    List<Course> findCoursesByStatus(ContentStatus status);

    Course saveCourse(Course course);
}
