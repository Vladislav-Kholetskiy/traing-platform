package com.vladislav.training.platform.content.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateTopicRequest(
    @NotNull Long courseId,
    @NotBlank String name,
    String description,
    @PositiveOrZero int sortOrder
) {}
