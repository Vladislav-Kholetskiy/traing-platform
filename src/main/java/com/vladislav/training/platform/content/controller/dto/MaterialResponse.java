package com.vladislav.training.platform.content.controller.dto;

import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.MaterialType;
import java.time.Instant;

public record MaterialResponse(Long id, Long topicId, String name, String description, String body, String videoUrl,
                               MaterialType materialType, ContentStatus status, int sortOrder,
                               Instant createdAt, Instant updatedAt) {}
