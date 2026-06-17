package com.vladislav.training.platform.access.service;

import com.vladislav.training.platform.common.exception.DomainException;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Контракт сервиса {@code ManagerialReadScopeProjectionService}.
 */
@Service
@Transactional(readOnly = true)
public class ManagerialReadScopeProjectionService {

    private static final String MANAGERIAL_CURRENT_SUPERVISION_TARGET_FAMILY = "managerial_current_supervision";
    private static final String MANAGERIAL_HISTORICAL_ANALYTICS_TARGET_FAMILY = "managerial_historical_analytics";

    private final AccessSpecificationPolicy accessSpecificationPolicy;

    public ManagerialReadScopeProjectionService(AccessSpecificationPolicy accessSpecificationPolicy) {
        this.accessSpecificationPolicy = Objects.requireNonNull(
            accessSpecificationPolicy,
            "accessSpecificationPolicy must not be null"
        );
    }

    public ManagerialReadScope project(Long actorUserId, Instant effectiveAt, AccessReadArea contour) {
        Objects.requireNonNull(actorUserId, "actorUserId must not be null");
        Objects.requireNonNull(effectiveAt, "effectiveAt must not be null");
        Objects.requireNonNull(contour, "contour must not be null");
        requireManagerialContour(contour);

        try {
            AccessReadScope scope = accessSpecificationPolicy.resolveReadScope(
                new AccessPolicyQueryContext(
                    actorUserId,
                    contour,
                    readTypeFor(contour),
                    effectiveAt,
                    null,
                    null,
                    targetFamilyFor(contour),
                    AccessReadSubjectScope.UNSPECIFIED,
                    AccessReadSubjectSemantics.MANAGER
                )
            );
            if (scope == null || !scope.readAllowed()) {
                return ManagerialReadScope.denyAll(actorUserId, effectiveAt, contour);
            }
            return new ManagerialReadScope(actorUserId, effectiveAt, contour, scope);
        } catch (DomainException exception) {
            return ManagerialReadScope.denyAll(actorUserId, effectiveAt, contour);
        }
    }

    private void requireManagerialContour(AccessReadArea contour) {
        if (contour != AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION
            && contour != AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS) {
            throw new IllegalArgumentException("Managerial scope projection supports only managerial contours");
        }
    }

    private AccessReadType readTypeFor(AccessReadArea contour) {
        if (contour == AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION) {
            return AccessReadType.LIST;
        }
        return AccessReadType.ANALYTICS;
    }

    private String targetFamilyFor(AccessReadArea contour) {
        if (contour == AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION) {
            return MANAGERIAL_CURRENT_SUPERVISION_TARGET_FAMILY;
        }
        return MANAGERIAL_HISTORICAL_ANALYTICS_TARGET_FAMILY;
    }
}
