package com.vladislav.training.platform.assignment.controller.dto;

import com.vladislav.training.platform.content.controller.dto.CourseResponse;
import com.vladislav.training.platform.content.controller.dto.MaterialResponse;
import com.vladislav.training.platform.content.controller.dto.TopicResponse;

public record AssignedMaterialContentResponse(
    Long assignmentId,
    CourseResponse publishedCourse,
    TopicResponse publishedTopic,
    MaterialResponse publishedMaterial
) {
}
