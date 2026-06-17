package com.vladislav.training.platform.content.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Запись данных {@code Test}.
 */
public record Test(
    Long id,
    Long topicId,
    String name,
    String description,
    TestType testType,
    ContentStatus status,
    BigDecimal thresholdPercent,
    String scoringPolicyCode,
    boolean isActiveFinalForTopic,
    int sortOrder,
    Instant createdAt,
    Instant updatedAt
) {

    public Test {
        Objects.requireNonNull(topicId, "topicId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(testType, "testType must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(thresholdPercent, "thresholdPercent must not be null");
        Objects.requireNonNull(scoringPolicyCode, "scoringPolicyCode must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (thresholdPercent.compareTo(BigDecimal.ZERO) < 0
            || thresholdPercent.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("thresholdPercent must be between 0 and 100");
        }
        if (scoringPolicyCode.isBlank()) {
            throw new IllegalArgumentException("scoringPolicyCode must not be blank");
        }
        if (sortOrder < 0) {
            throw new IllegalArgumentException("sortOrder must be non-negative");
        }
        if (isActiveFinalForTopic && (testType != TestType.CONTROL || status != ContentStatus.PUBLISHED)) {
            throw new IllegalArgumentException(
                "active final test must be CONTROL and PUBLISHED"
            );
        }
    }
}
