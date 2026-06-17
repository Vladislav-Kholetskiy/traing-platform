package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.content.domain.Course;

/**
 * Контракт сервиса {@code CourseLifecycleService}.
 */
public interface CourseLifecycleService {

    Course publish(Long courseId);

    Course archive(Long courseId);
}
