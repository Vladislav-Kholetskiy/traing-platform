package com.vladislav.training.platform.content.controller.dto;

import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.QuestionType;
import java.time.Instant;

public record QuestionResponse(Long id, Long topicId, String body, QuestionType questionType, ContentStatus status,
                               Integer sortOrder, Instant createdAt, Instant updatedAt) {}
