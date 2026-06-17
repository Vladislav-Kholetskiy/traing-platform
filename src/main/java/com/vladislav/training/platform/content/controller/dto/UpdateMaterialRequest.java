package com.vladislav.training.platform.content.controller.dto;

import com.vladislav.training.platform.content.domain.MaterialType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateMaterialRequest(
    @NotBlank String name,
    String description,
    String body,
    String videoUrl,
    @NotNull MaterialType materialType,
    @PositiveOrZero int sortOrder
) {}
