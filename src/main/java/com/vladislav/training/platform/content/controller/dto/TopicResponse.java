package com.vladislav.training.platform.content.controller.dto;

import com.vladislav.training.platform.content.domain.ContentStatus;
import java.time.Instant;

public record TopicResponse(Long id, Long courseId, String name, String description, ContentStatus status, int sortOrder,
                            Instant createdAt, Instant updatedAt) {}
