package com.vladislav.training.platform.access.service;

import java.time.Instant;
import java.util.Objects;

/**
 * Запись данных {@code AccessPolicyQueryContext}.
 */
public record AccessPolicyQueryContext(
    Long actorUserId,
    AccessReadArea contour,
    AccessReadType readType,
    Instant effectiveAt,
    Long targetUserId,
    Long targetOrganizationalUnitId,
    String targetEntityFamily,
    Long targetTestId,
    Long currentAttemptId,
    AccessReadSubjectScope subjectScope,
    AccessReadSubjectSemantics subjectSemantics
) {

    public AccessPolicyQueryContext(
        Long actorUserId,
        AccessReadArea contour,
        AccessReadType readType,
        Instant effectiveAt,
        Long targetUserId,
        Long targetOrganizationalUnitId,
        String targetEntityFamily
    ) {
        this(
            actorUserId,
            contour,
            readType,
            effectiveAt,
            targetUserId,
            targetOrganizationalUnitId,
            targetEntityFamily,
            null,
            null,
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.UNSPECIFIED
        );
    }

    public AccessPolicyQueryContext(
        Long actorUserId,
        AccessReadArea contour,
        AccessReadType readType,
        Instant effectiveAt,
        Long targetUserId,
        Long targetOrganizationalUnitId,
        String targetEntityFamily,
        AccessReadSubjectScope subjectScope,
        AccessReadSubjectSemantics subjectSemantics
    ) {
        this(
            actorUserId,
            contour,
            readType,
            effectiveAt,
            targetUserId,
            targetOrganizationalUnitId,
            targetEntityFamily,
            null,
            null,
            subjectScope,
            subjectSemantics
        );
    }

    public AccessPolicyQueryContext(
        Long actorUserId,
        AccessReadArea contour,
        AccessReadType readType,
        Instant effectiveAt,
        Long targetUserId,
        Long targetOrganizationalUnitId,
        String targetEntityFamily,
        AccessReadSubjectScope subjectScope
    ) {
        this(
            actorUserId,
            contour,
            readType,
            effectiveAt,
            targetUserId,
            targetOrganizationalUnitId,
            targetEntityFamily,
            null,
            null,
            subjectScope,
            AccessReadSubjectSemantics.UNSPECIFIED
        );
    }

    public AccessPolicyQueryContext(
        Long actorUserId,
        AccessReadArea contour,
        AccessReadType readType,
        Instant effectiveAt,
        Long targetUserId,
        Long targetOrganizationalUnitId,
        String targetEntityFamily,
        Long targetTestId,
        Long currentAttemptId,
        AccessReadSubjectScope subjectScope
    ) {
        this(
            actorUserId,
            contour,
            readType,
            effectiveAt,
            targetUserId,
            targetOrganizationalUnitId,
            targetEntityFamily,
            targetTestId,
            currentAttemptId,
            subjectScope,
            AccessReadSubjectSemantics.UNSPECIFIED
        );
    }

    public AccessPolicyQueryContext {
        Objects.requireNonNull(actorUserId, "actorUserId must not be null");
        Objects.requireNonNull(contour, "contour must not be null");
        Objects.requireNonNull(readType, "readType must not be null");
        Objects.requireNonNull(effectiveAt, "effectiveAt must not be null");
        Objects.requireNonNull(subjectScope, "subjectScope must not be null");
        Objects.requireNonNull(subjectSemantics, "subjectSemantics must not be null");

        if (subjectScope == AccessReadSubjectScope.ACTOR_SELF && targetUserId != null) {
            throw new IllegalArgumentException("ACTOR_SELF subjectScope must not be combined with targetUserId");
        }
    }
}

