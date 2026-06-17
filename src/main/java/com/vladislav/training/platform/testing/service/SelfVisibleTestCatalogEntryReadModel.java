package com.vladislav.training.platform.testing.service;

import com.vladislav.training.platform.content.domain.TestType;
import java.util.Objects;

/**
 * Запись данных {@code SelfVisibleTestCatalogEntryReadModel}.
 */
public record SelfVisibleTestCatalogEntryReadModel(
    Long id,
    Long courseId,
    String courseName,
    Long topicId,
    String topicName,
    String name,
    String description,
    TestType testType
) {

    public SelfVisibleTestCatalogEntryReadModel {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(courseId, "courseId must not be null");
        Objects.requireNonNull(courseName, "courseName must not be null");
        Objects.requireNonNull(topicId, "topicId must not be null");
        Objects.requireNonNull(topicName, "topicName must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(testType, "testType must not be null");
        if (courseName.isBlank()) {
            throw new IllegalArgumentException("courseName must not be blank");
        }
        if (topicName.isBlank()) {
            throw new IllegalArgumentException("topicName must not be blank");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }
}
