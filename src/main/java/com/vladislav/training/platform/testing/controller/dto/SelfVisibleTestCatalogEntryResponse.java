package com.vladislav.training.platform.testing.controller.dto;

import com.vladislav.training.platform.content.domain.TestType;

/**
 * Ответ {@code SelfVisibleTestCatalogEntryResponse}.
 */
public record SelfVisibleTestCatalogEntryResponse(
    Long id,
    Long courseId,
    String courseName,
    Long topicId,
    String topicName,
    String name,
    String description,
    TestType testType
) {
}
