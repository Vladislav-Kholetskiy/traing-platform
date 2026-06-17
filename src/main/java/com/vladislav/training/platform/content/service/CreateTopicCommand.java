package com.vladislav.training.platform.content.service;

/**
 * Команда {@code CreateTopicCommand}.
 */
public record CreateTopicCommand(
    Long courseId,
    String name,
    String description,
    int sortOrder
) {}
