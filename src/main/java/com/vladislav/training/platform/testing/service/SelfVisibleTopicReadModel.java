package com.vladislav.training.platform.testing.service;

import com.vladislav.training.platform.content.domain.MaterialType;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Запись данных {@code SelfVisibleTopicReadModel}.
 */
public record SelfVisibleTopicReadModel(
    Long topicId,
    String topicName,
    String topicDescription,
    Long courseId,
    String courseName,
    List<SelfVisibleMaterialReadModel> materials
) {

    public SelfVisibleTopicReadModel {
        Objects.requireNonNull(topicId, "topicId must not be null");
        Objects.requireNonNull(topicName, "topicName must not be null");
        Objects.requireNonNull(courseId, "courseId must not be null");
        Objects.requireNonNull(courseName, "courseName must not be null");
        materials = materials == null ? List.of() : List.copyOf(materials);
        if (topicName.isBlank()) {
            throw new IllegalArgumentException("topicName must not be blank");
        }
        if (courseName.isBlank()) {
            throw new IllegalArgumentException("courseName must not be blank");
        }
    }

    public record SelfVisibleMaterialReadModel(
        Long materialId,
        String name,
        String description,
        String body,
        String videoUrl,
        MaterialType materialType,
        Integer sortOrder,
        Instant updatedAt
    ) {

        public SelfVisibleMaterialReadModel {
            Objects.requireNonNull(materialId, "materialId must not be null");
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(materialType, "materialType must not be null");
            if (name.isBlank()) {
                throw new IllegalArgumentException("name must not be blank");
            }
        }
    }
}
