package com.vladislav.training.platform.content.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record SaveCourseRequest(@NotBlank String name, String description, @PositiveOrZero Integer sortOrder) {}
