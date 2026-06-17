package com.vladislav.training.platform.content.service;

/**
 * Команда {@code UpdateTopicCommand}.
 */
public record UpdateTopicCommand(
    String name,
    String description,
    int sortOrder
) {}
