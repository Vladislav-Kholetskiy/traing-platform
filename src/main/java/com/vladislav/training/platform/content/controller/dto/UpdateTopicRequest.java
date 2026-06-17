package com.vladislav.training.platform.content.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateTopicRequest(
    @NotBlank String name,
    String description,
    @PositiveOrZero int sortOrder
) {}
