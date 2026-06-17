package com.vladislav.training.platform.assignment.service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Команда {@code LaunchAssignmentCampaignCommand}.
 */
public record LaunchAssignmentCampaignCommand(
    String name,
    String description,
    String sourceType,
    String sourceRef,
    String sourceNameSnapshot,
    List<Long> courseIds,
    Targeting targeting,
    DeadlinePolicy deadlinePolicy
) {

    public LaunchAssignmentCampaignCommand {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(sourceType, "sourceType must not be null");
        Objects.requireNonNull(courseIds, "courseIds must not be null");
        Objects.requireNonNull(targeting, "targeting must not be null");
        Objects.requireNonNull(deadlinePolicy, "deadlinePolicy must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (sourceType.isBlank()) {
            throw new IllegalArgumentException("sourceType must not be blank");
        }
        if (courseIds.isEmpty()) {
            throw new IllegalArgumentException("courseIds must not be empty");
        }
        for (Long courseId : courseIds) {
            Objects.requireNonNull(courseId, "courseIds must not contain null");
        }
    }

    /**
     * Описание основания и точки организационного охвата, по которым выбираются получатели кампании.
     */
    public record Targeting(
        String basisType,
        String basisRef
    ) {

        public Targeting {
            Objects.requireNonNull(basisType, "basisType must not be null");
            Objects.requireNonNull(basisRef, "basisRef must not be null");
            if (basisType.isBlank()) {
                throw new IllegalArgumentException("basisType must not be blank");
            }
            if (basisRef.isBlank()) {
                throw new IllegalArgumentException("basisRef must not be blank");
            }
        }
    }

    /**
     * Правило срока завершения для всех назначений, созданных в рамках запуска кампании.
     */
    public record DeadlinePolicy(
        Instant deadlineAt
    ) {

        public DeadlinePolicy {
            Objects.requireNonNull(deadlineAt, "deadlineAt must not be null");
        }
    }
}
