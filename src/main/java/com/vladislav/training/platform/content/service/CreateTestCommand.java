package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.content.domain.TestType;
import java.math.BigDecimal;

/**
 * Команда {@code CreateTestCommand}.
 */
public record CreateTestCommand(
    Long topicId,
    String name,
    String description,
    TestType testType,
    BigDecimal thresholdPercent,
    String scoringPolicyCode,
    int sortOrder
) {}
