package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.content.domain.Course;
import com.vladislav.training.platform.content.domain.Material;
import com.vladislav.training.platform.content.domain.Topic;
import java.util.Objects;

public record AssignedMaterialContent(
    Long assignmentId,
    Course course,
    Topic topic,
    Material material
) {

    public AssignedMaterialContent {
        Objects.requireNonNull(assignmentId, "assignmentId must not be null");
        Objects.requireNonNull(course, "course must not be null");
        Objects.requireNonNull(topic, "topic must not be null");
        Objects.requireNonNull(material, "material must not be null");
    }
}
