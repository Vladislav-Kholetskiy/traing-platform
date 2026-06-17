package com.vladislav.training.platform.content.controller.dto;

import com.vladislav.training.platform.content.domain.ContentStatus;
import java.time.Instant;

public record CourseResponse(Long id, String name, String description, ContentStatus status, Integer sortOrder,
                             Instant createdAt, Instant updatedAt) {}
