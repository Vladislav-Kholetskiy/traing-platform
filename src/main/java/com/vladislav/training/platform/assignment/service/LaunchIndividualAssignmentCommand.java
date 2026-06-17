package com.vladislav.training.platform.assignment.service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record LaunchIndividualAssignmentCommand(
    String name,
    String description,
    Long userId,
    List<Long> courseIds,
    DeadlinePolicy deadlinePolicy
) {

    public LaunchIndividualAssignmentCommand {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(courseIds, "courseIds must not be null");
        Objects.requireNonNull(deadlinePolicy, "deadlinePolicy must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (courseIds.isEmpty()) {
            throw new IllegalArgumentException("courseIds must not be empty");
        }
        for (Long courseId : courseIds) {
            Objects.requireNonNull(courseId, "courseIds must not contain null");
        }
    }

    public record DeadlinePolicy(
        Instant deadlineAt
    ) {

        public DeadlinePolicy {
            Objects.requireNonNull(deadlineAt, "deadlineAt must not be null");
        }
    }
}
