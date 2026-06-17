package com.vladislav.training.platform.assignment.controller.dto;

import com.vladislav.training.platform.content.controller.dto.CourseResponse;
import com.vladislav.training.platform.content.controller.dto.MaterialResponse;
import com.vladislav.training.platform.content.controller.dto.TopicResponse;
import java.util.List;

/**
 * Ответ {@code AssignedLearningContextResponse}.
 */
public record AssignedLearningContextResponse(
    AssignmentResponse assignment,
    List<AssignmentTestResponse> assignmentTests,
    CourseResponse publishedCourse,
    List<TopicResponse> publishedTopics,
    List<MaterialResponse> publishedMaterials
) {
}
