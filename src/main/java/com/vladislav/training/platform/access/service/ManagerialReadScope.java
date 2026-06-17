package com.vladislav.training.platform.access.service;

import java.time.Instant;
import java.util.Objects;

/**
 * Запись данных {@code ManagerialReadScope}.
 */
public record ManagerialReadScope(
    Long actorUserId,
    Instant effectiveAt,
    AccessReadArea contour,
    AccessReadScope readScope
) {

    public ManagerialReadScope {
        Objects.requireNonNull(actorUserId, "actorUserId must not be null");
        Objects.requireNonNull(effectiveAt, "effectiveAt must not be null");
        Objects.requireNonNull(contour, "contour must not be null");
        Objects.requireNonNull(readScope, "readScope must not be null");

        if (contour != AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION
            && contour != AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS) {
            throw new IllegalArgumentException("Managerial read scope supports only managerial contours");
        }
    }

    public static ManagerialReadScope denyAll(Long actorUserId, Instant effectiveAt, AccessReadArea contour) {
        return new ManagerialReadScope(actorUserId, effectiveAt, contour, AccessReadScope.denyAll());
    }

    public AccessReadSubjectSemantics subjectSemantics() {
        return AccessReadSubjectSemantics.MANAGER;
    }
}
