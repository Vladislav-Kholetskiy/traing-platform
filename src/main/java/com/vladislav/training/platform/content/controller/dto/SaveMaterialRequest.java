package com.vladislav.training.platform.content.controller.dto;

import com.vladislav.training.platform.content.domain.MaterialType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record SaveMaterialRequest(@NotNull Long topicId, @NotBlank String name, String description,
                                  @NotNull MaterialType materialType, @PositiveOrZero int sortOrder) {}
