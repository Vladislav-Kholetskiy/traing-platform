package com.vladislav.training.platform.content.service;

/**
 * Команда {@code CreateCourseCommand}.
 */
public record CreateCourseCommand(
    String name,
    String description,
    Integer sortOrder
) {}
