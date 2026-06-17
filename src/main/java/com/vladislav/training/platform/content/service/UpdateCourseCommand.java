package com.vladislav.training.platform.content.service;

/**
 * Команда {@code UpdateCourseCommand}.
 */
public record UpdateCourseCommand(
    String name,
    String description,
    Integer sortOrder
) {}
