package com.vladislav.training.platform.access.service;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.common.time.UtcClock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Разрешитель {@code AccessPolicyQueryContextResolver}.
 */
@Component
public class AccessPolicyQueryContextResolver {

    private final InteractiveActorResolver interactiveActorResolver;
    private final UtcClock utcClock;

    public AccessPolicyQueryContextResolver(
        InteractiveActorResolver interactiveActorResolver,
        UtcClock utcClock
    ) {
        this.interactiveActorResolver = interactiveActorResolver;
        this.utcClock = utcClock;
    }

    public AccessPolicyQueryContext resolve(
        AccessReadArea contour,
        AccessReadType readType,
        String targetEntityFamily
    ) {
        return resolve(contour, readType, null, null, targetEntityFamily);
    }

    public AccessPolicyQueryContext resolve(
        AccessReadArea contour,
        AccessReadType readType,
        Long targetUserId,
        Long targetOrganizationalUnitId,
        String targetEntityFamily
    ) {
        return new AccessPolicyQueryContext(
            interactiveActorResolver.resolveActorUserId(),
            contour,
            readType,
            utcClock.now(),
            targetUserId,
            targetOrganizationalUnitId,
            targetEntityFamily,
            null,
            null,
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.UNSPECIFIED
        );
    }

    public AccessPolicyQueryContext resolveActorSelfScope(
        AccessReadArea contour,
        AccessReadType readType,
        String targetEntityFamily
    ) {
        return new AccessPolicyQueryContext(
            interactiveActorResolver.resolveActorUserId(),
            contour,
            readType,
            utcClock.now(),
            null,
            null,
            targetEntityFamily,
            null,
            null,
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        );
    }

    public AccessPolicyQueryContext resolveSelfCurrentAttemptContext(Long actorUserId, Long testId) {
        return resolveSelfCurrentAttemptContext(actorUserId, testId, null, null);
    }

    public AccessPolicyQueryContext resolveSelfCurrentAttemptContext(
        Long actorUserId,
        Long testId,
        Long currentAttemptId,
        Instant effectiveAt
    ) {
        Long resolvedActorUserId = interactiveActorResolver.resolveActorUserId();
        if (!Objects.equals(resolvedActorUserId, actorUserId)) {
            throw new IllegalArgumentException("actorUserId must match the current authenticated actor");
        }
        return new AccessPolicyQueryContext(
            actorUserId,
            AccessReadArea.SELF_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            effectiveAt == null ? utcClock.now() : effectiveAt,
            null,
            null,
            "self_current_attempt",
            testId,
            currentAttemptId,
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        );
    }

    public AccessPolicyQueryContext resolveNotificationAdministrationContext(Long actorUserId) {
        validateCurrentActor(actorUserId);
        return new AccessPolicyQueryContext(
            actorUserId,
            AccessReadArea.NOTIFICATION_ADMINISTRATION,
            AccessReadType.LIST,
            administrativeEffectiveAt(),
            null,
            null,
            "notification",
            null,
            null,
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.UNSPECIFIED
        );
    }

    public AccessPolicyQueryContext resolveNotificationAdministrationDetailContext(Long actorUserId, Long notificationId) {
        validateCurrentActor(actorUserId);
        return new AccessPolicyQueryContext(
            actorUserId,
            AccessReadArea.NOTIFICATION_ADMINISTRATION,
            AccessReadType.DETAIL,
            administrativeEffectiveAt(),
            null,
            null,
            "notification",
            notificationId,
            null,
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.UNSPECIFIED
        );
    }

    public AccessPolicyQueryContext resolveNotificationRecipientSelfContext(Long actorUserId) {
        validateCurrentActor(actorUserId);
        return new AccessPolicyQueryContext(
            actorUserId,
            AccessReadArea.NOTIFICATION_RECIPIENT_SELF,
            AccessReadType.LIST,
            administrativeEffectiveAt(),
            null,
            null,
            "self_notification",
            null,
            null,
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        );
    }

    public AccessPolicyQueryContext resolveNotificationRecipientSelfDetailContext(Long actorUserId, Long notificationId) {
        validateCurrentActor(actorUserId);
        return new AccessPolicyQueryContext(
            actorUserId,
            AccessReadArea.NOTIFICATION_RECIPIENT_SELF,
            AccessReadType.DETAIL,
            administrativeEffectiveAt(),
            null,
            null,
            "self_notification",
            notificationId,
            null,
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        );
    }

    public AccessPolicyQueryContext resolveNotificationRuleAdministrationContext(Long actorUserId) {
        validateCurrentActor(actorUserId);
        return new AccessPolicyQueryContext(
            actorUserId,
            AccessReadArea.NOTIFICATION_RULE_ADMINISTRATION,
            AccessReadType.LIST,
            administrativeEffectiveAt(),
            null,
            null,
            "notification_rule",
            null,
            null,
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.UNSPECIFIED
        );
    }

    public AccessPolicyQueryContext resolveNotificationRuleAdministrationDetailContext(Long actorUserId, Long notificationRuleId) {
        validateCurrentActor(actorUserId);
        return new AccessPolicyQueryContext(
            actorUserId,
            AccessReadArea.NOTIFICATION_RULE_ADMINISTRATION,
            AccessReadType.DETAIL,
            administrativeEffectiveAt(),
            null,
            null,
            "notification_rule",
            notificationRuleId,
            null,
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.UNSPECIFIED
        );
    }

    public AccessPolicyQueryContext resolveImportJobAdministrationContext(Long actorUserId) {
        validateCurrentActor(actorUserId);
        return new AccessPolicyQueryContext(
            actorUserId,
            AccessReadArea.IMPORT_JOB_ADMINISTRATION,
            AccessReadType.LIST,
            administrativeEffectiveAt(),
            null,
            null,
            "import_job",
            null,
            null,
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.UNSPECIFIED
        );
    }

    public AccessPolicyQueryContext resolveImportJobAdministrationDetailContext(Long actorUserId, Long importJobId) {
        validateCurrentActor(actorUserId);
        return new AccessPolicyQueryContext(
            actorUserId,
            AccessReadArea.IMPORT_JOB_ADMINISTRATION,
            AccessReadType.DETAIL,
            administrativeEffectiveAt(),
            null,
            null,
            "import_job",
            importJobId,
            null,
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.UNSPECIFIED
        );
    }

    public AccessPolicyQueryContext resolveImportJobItemAdministrationContext(Long actorUserId) {
        validateCurrentActor(actorUserId);
        return new AccessPolicyQueryContext(
            actorUserId,
            AccessReadArea.IMPORT_JOB_ADMINISTRATION,
            AccessReadType.LIST,
            administrativeEffectiveAt(),
            null,
            null,
            "import_job_item",
            null,
            null,
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.UNSPECIFIED
        );
    }

    public AccessPolicyQueryContext resolveImportJobItemAdministrationDetailContext(Long actorUserId, Long importJobItemId) {
        validateCurrentActor(actorUserId);
        return new AccessPolicyQueryContext(
            actorUserId,
            AccessReadArea.IMPORT_JOB_ADMINISTRATION,
            AccessReadType.DETAIL,
            administrativeEffectiveAt(),
            null,
            null,
            "import_job_item",
            importJobItemId,
            null,
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.UNSPECIFIED
        );
    }

    public AccessPolicyQueryContext resolveAuditEventAdministrationContext(Long actorUserId) {
        validateCurrentActor(actorUserId);
        return new AccessPolicyQueryContext(
            actorUserId,
            AccessReadArea.AUDIT_EVENT_ADMINISTRATION,
            AccessReadType.LIST,
            administrativeEffectiveAt(),
            null,
            null,
            "audit_event",
            null,
            null,
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.UNSPECIFIED
        );
    }

    public AccessPolicyQueryContext resolveAuditEventAdministrationDetailContext(Long actorUserId, Long auditEventId) {
        validateCurrentActor(actorUserId);
        return new AccessPolicyQueryContext(
            actorUserId,
            AccessReadArea.AUDIT_EVENT_ADMINISTRATION,
            AccessReadType.DETAIL,
            administrativeEffectiveAt(),
            null,
            null,
            "audit_event",
            auditEventId,
            null,
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.UNSPECIFIED
        );
    }

    private void validateCurrentActor(Long actorUserId) {
        Objects.requireNonNull(actorUserId, "actorUserId must not be null");
        Long resolvedActorUserId = interactiveActorResolver.resolveActorUserId();
        if (!Objects.equals(resolvedActorUserId, actorUserId)) {
            throw new IllegalArgumentException("actorUserId must match the current authenticated actor");
        }
    }

    private Instant administrativeEffectiveAt() {
        return utcClock.now();
    }
}
