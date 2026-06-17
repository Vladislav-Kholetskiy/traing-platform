package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.content.domain.MaterialType;

/**
 * Команда {@code UpdateMaterialCommand}.
 */
public record UpdateMaterialCommand(
    String name,
    String description,
    String body,
    String videoUrl,
    MaterialType materialType,
    int sortOrder
) {}
