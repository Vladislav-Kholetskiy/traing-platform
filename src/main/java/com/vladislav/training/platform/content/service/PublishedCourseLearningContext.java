package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.content.domain.Course;
import com.vladislav.training.platform.content.domain.Material;
import com.vladislav.training.platform.content.domain.Topic;
import java.util.List;
import java.util.Objects;

/**
 * Запись данных {@code PublishedCourseLearningContext}.
 */
public record PublishedCourseLearningContext(
    Course course,
    List<Topic> topics,
    List<Material> materials
) {

    public PublishedCourseLearningContext {
        Objects.requireNonNull(course, "course must not be null");
        topics = topics == null ? List.of() : List.copyOf(topics);
        materials = materials == null ? List.of() : List.copyOf(materials);
    }
}
