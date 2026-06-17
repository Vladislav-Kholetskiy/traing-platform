package com.vladislav.training.platform.testing.controller.dto;

import com.vladislav.training.platform.content.domain.MaterialType;
import java.time.Instant;
import java.util.List;

/**
 * Ответ {@code SelfVisibleTopicResponse}.
 */
public record SelfVisibleTopicResponse(
    Long topicId,
    String topicName,
    String topicDescription,
    Long courseId,
    String courseName,
    List<SelfVisibleMaterialResponse> materials
) {
    public record SelfVisibleMaterialResponse(
        Long materialId,
        String name,
        String description,
        String body,
        String videoUrl,
        MaterialType materialType,
        Integer sortOrder,
        Instant updatedAt
    ) {
    }
}
