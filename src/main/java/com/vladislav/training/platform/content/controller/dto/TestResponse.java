package com.vladislav.training.platform.content.controller.dto;

import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.TestType;
import java.math.BigDecimal;
import java.time.Instant;

public record TestResponse(Long id, Long topicId, String name, String description, TestType testType,
                           ContentStatus status, BigDecimal thresholdPercent, String scoringPolicyCode,
                           int sortOrder, Instant createdAt, Instant updatedAt) {}
