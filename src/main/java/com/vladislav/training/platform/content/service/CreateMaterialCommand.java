package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.content.domain.MaterialType;

/**
 * Команда {@code CreateMaterialCommand}.
 */
public record CreateMaterialCommand(
    Long topicId,
    String name,
    String description,
    String body,
    String videoUrl,
    MaterialType materialType,
    int sortOrder
) {}
