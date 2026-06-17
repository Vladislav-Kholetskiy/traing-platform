package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.content.domain.Course;
import com.vladislav.training.platform.content.domain.Material;
import com.vladislav.training.platform.content.domain.Question;
import com.vladislav.training.platform.content.domain.Test;
import com.vladislav.training.platform.content.domain.Topic;

/**
 * Контракт сервиса чтения {@code ContentLifecycleQueryService}.
 */
public interface ContentLifecycleQueryService {

    Course findCourseById(Long courseId);

    Topic findTopicById(Long topicId);

    Material findMaterialById(Long materialId);

    Question findQuestionById(Long questionId);

    Test findTestById(Long testId);
}
