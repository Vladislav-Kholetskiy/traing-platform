package com.vladislav.training.platform.access.service;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.application.actor.ResolvedAuthenticatedActor;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.userorg.service.UserOrgFoundationStateReadService;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Класс {@code JpaAccessSpecificationPolicy}.
 */
@Service("canonicalAccessSpecificationPolicy")
@Transactional(readOnly = true)
public class JpaAccessSpecificationPolicy implements AccessSpecificationPolicy {

    private static final Set<String> READ_ADMIN_ROLE_CODES = Set.of("ADMIN", "SYSTEM_ADMIN", "SUPER_ADMIN");
    private static final Set<String> READ_ADMIN_AUTHORITIES = Set.of(
        "ROLE_ADMIN",
        "ROLE_SYSTEM_ADMIN",
        "ROLE_SUPER_ADMIN"
    );
    private static final Set<String> LEARNER_SELF_READ_ROLE_CODES = Set.of("OPERATOR", "ADMIN", "SYSTEM_ADMIN", "SUPER_ADMIN");
    private static final Set<String> LEARNER_SELF_READ_AUTHORITIES = Set.of(
        "ROLE_OPERATOR",
        "ROLE_ADMIN",
        "ROLE_SYSTEM_ADMIN",
        "ROLE_SUPER_ADMIN"
    );
    private static final Set<String> CONTENT_ROLE_CODES = Set.of("EXPERT", "ADMIN", "SYSTEM_ADMIN", "SUPER_ADMIN");
    private static final Set<String> CONTENT_AUTHORITIES = Set.of(
        "ROLE_EXPERT",
        "ROLE_ADMIN",
        "ROLE_SYSTEM_ADMIN",
        "ROLE_SUPER_ADMIN"
    );
    private static final Set<String> ASSIGNMENT_ROLE_CODES = Set.of("EXPERT", "ADMIN", "SYSTEM_ADMIN", "SUPER_ADMIN");
    private static final Set<String> ASSIGNMENT_AUTHORITIES = Set.of(
        "ROLE_EXPERT",
        "ROLE_ADMIN",
        "ROLE_SYSTEM_ADMIN",
        "ROLE_SUPER_ADMIN"
    );
    private static final Set<String> ASSIGNMENT_CAMPAIGN_TARGET_FAMILIES = Set.of(
        "assignment_campaign_preview",
        "assignment_campaign",
        "assignment_campaign_course"
    );
    private static final Set<String> CONTENT_AUTHORING_TARGET_FAMILIES = Set.of(
        "content",
        "course",
        "topic",
        "test",
        "question",
        "material"
    );
    private static final Set<String> CONTENT_LIFECYCLE_TARGET_FAMILIES = Set.of(
        "content",
        "course",
        "topic",
        "test",
        "question",
        "material"
    );
    private static final Set<String> CONTENT_FINAL_CONTROL_TARGET_FAMILIES = Set.of(
        "content",
        "test"
    );
    private static final Set<String> ASSIGNMENT_TARGET_FAMILIES = Set.of("assignment");
    private static final Set<String> ASSIGNED_ENTITLEMENT_TARGET_FAMILIES = Set.of("assigned_learning_context");
    private static final Set<String> ASSIGNED_CURRENT_ATTEMPT_TARGET_FAMILIES = Set.of("assigned_current_attempt");
    private static final Set<String> SELF_CURRENT_ATTEMPT_TARGET_FAMILIES = Set.of("self_current_attempt");
    private static final Set<String> SELF_VISIBLE_TESTING_TARGET_FAMILIES = Set.of("self_visible_testing");
    private static final Set<String> SELF_RESULT_HISTORY_TARGET_FAMILIES = Set.of("self_result_history");
    private static final Set<String> NOTIFICATION_RECIPIENT_SELF_TARGET_FAMILIES = Set.of("self_notification");
    private static final Set<String> MANAGERIAL_CURRENT_SUPERVISION_TARGET_FAMILIES =
        Set.of("managerial_current_supervision");
    private static final Set<String> MANAGERIAL_HISTORICAL_ANALYTICS_TARGET_FAMILIES =
        Set.of("managerial_historical_analytics");
    private static final Set<String> EXPERT_ANALYTICS_ROLE_CODES = Set.of("EXPERT", "ADMIN", "SYSTEM_ADMIN", "SUPER_ADMIN");
    private static final Set<String> EXPERT_ANALYTICS_AUTHORITIES = Set.of(
        "ROLE_EXPERT",
        "ROLE_ADMIN",
        "ROLE_SYSTEM_ADMIN",
        "ROLE_SUPER_ADMIN"
    );
    private static final Set<String> EXPERT_QUESTION_ANALYTICS_TARGET_FAMILIES =
        Set.of("expert_question_analytics");
    private static final Set<String> NOTIFICATION_ADMINISTRATION_TARGET_FAMILIES = Set.of("notification");
    private static final Set<String> NOTIFICATION_RULE_ADMINISTRATION_TARGET_FAMILIES = Set.of("notification_rule");
    private static final Set<String> IMPORT_JOB_ADMINISTRATION_TARGET_FAMILIES = Set.of("import_job", "import_job_item");
    private static final Set<String> AUDIT_EVENT_ADMINISTRATION_TARGET_FAMILIES = Set.of("audit_event");

    private final UserOrgFoundationStateReadService userOrgFoundationStateReadService;
    private final AccessFoundationStateReadService accessFoundationStateReadService;
    private final InteractiveActorResolver interactiveActorResolver;

    public JpaAccessSpecificationPolicy(
        UserOrgFoundationStateReadService userOrgFoundationStateReadService,
        AccessFoundationStateReadService accessFoundationStateReadService,
        InteractiveActorResolver interactiveActorResolver
    ) {
        this.userOrgFoundationStateReadService = userOrgFoundationStateReadService;
        this.accessFoundationStateReadService = accessFoundationStateReadService;
        this.interactiveActorResolver = interactiveActorResolver;
    }

    @Override
    public AccessReadScope resolveReadScope(AccessPolicyQueryContext context) {
        if (context == null) {
            return AccessReadScope.denyAll();
        }
        Authentication authentication = requireAuthentication(context.actorUserId());
        UserOrgFoundationStateReadService.UserAccessPolicyFoundationState actor =
            loadActorFoundationState(context.actorUserId(), context.effectiveAt());

        if (!actor.active()) {
            return AccessReadScope.denyAll();
        }

        if (isFoundationAdminContour(context.contour())) {
            if (!hasAdministrativeReadAuthority(authentication, actor, context.actorUserId(), context.effectiveAt())) {
                return AccessReadScope.denyAll();
            }
            return AccessReadScope.fullAccess();
        }

        if (isContentContour(context.contour())) {
            if (!isKnownContentReadContext(context)) {
                return AccessReadScope.denyAll();
            }
            if (!hasContentReadAuthority(authentication, actor, context.actorUserId(), context.effectiveAt())) {
                return AccessReadScope.denyAll();
            }
            return AccessReadScope.fullAccess();
        }

        if (isAssignmentContour(context.contour())) {
            if (!isKnownInternalAssignmentReadContext(context)) {
                return AccessReadScope.denyAll();
            }
            if (isAssignedLearningSelfReadContext(context)) {
                if (!hasLearnerSelfReadAuthority(authentication, actor, context.actorUserId(), context.effectiveAt())) {
                    return AccessReadScope.denyAll();
                }
                return AccessReadScope.fullAccess();
            }
            if (!hasAssignmentReadAuthority(authentication, actor, context.actorUserId(), context.effectiveAt())) {
                return AccessReadScope.denyAll();
            }
            return AccessReadScope.fullAccess();
        }

        if (isAssignedCurrentAttemptContour(context.contour())) {
            if (!isKnownAssignedCurrentAttemptReadContext(context)) {
                return AccessReadScope.denyAll();
            }
            if (!hasLearnerSelfReadAuthority(authentication, actor, context.actorUserId(), context.effectiveAt())) {
                return AccessReadScope.denyAll();
            }
            return AccessReadScope.fullAccess();
        }

        if (isSelfCurrentAttemptContour(context.contour())) {
            if (!isKnownSelfCurrentAttemptReadContext(context)) {
                return AccessReadScope.denyAll();
            }
            return AccessReadScope.fullAccess();
        }

        if (isSelfVisibleTestingContour(context.contour())) {
            if (!isKnownSelfVisibleTestingReadContext(context)) {
                return AccessReadScope.denyAll();
            }
            return AccessReadScope.fullAccess();
        }

        if (isSelfResultHistoryContour(context.contour())) {
            if (!isKnownSelfResultHistoryReadContext(context)) {
                return AccessReadScope.denyAll();
            }
            return AccessReadScope.fullAccess();
        }

        if (isNotificationRecipientSelfContour(context.contour())) {
            if (!isKnownNotificationRecipientSelfReadContext(context)) {
                return AccessReadScope.denyAll();
            }
            return AccessReadScope.fullAccess();
        }

        if (isManagerialCurrentSupervisionContour(context.contour())) {
            if (!isKnownManagerialCurrentSupervisionReadContext(context)) {
                return AccessReadScope.denyAll();
            }
            AccessFoundationStateReadService.ManagerialScopeFoundationState scope =
                accessFoundationStateReadService.findActorManagerialScope(context.actorUserId(), context.effectiveAt());
            if (scope.unitAnchorIds().isEmpty()) {
                return AccessReadScope.denyAll();
            }
            return AccessReadScope.scoped(scope.unitAnchorIds(), scope.subtreePaths());
        }

        if (isManagerialHistoricalAnalyticsContour(context.contour())) {
            if (!isKnownManagerialHistoricalAnalyticsReadContext(context)) {
                return AccessReadScope.denyAll();
            }
            AccessFoundationStateReadService.ManagerialScopeFoundationState scope =
                accessFoundationStateReadService.findActorManagerialScope(context.actorUserId(), context.effectiveAt());
            if (scope.unitAnchorIds().isEmpty()) {
                return AccessReadScope.denyAll();
            }
            return AccessReadScope.scoped(scope.unitAnchorIds(), scope.subtreePaths());
        }

        if (isExpertQuestionAnalyticsContour(context.contour())) {
            if (!isKnownExpertQuestionAnalyticsReadContext(context)) {
                return AccessReadScope.denyAll();
            }
            if (!hasExpertQuestionAnalyticsReadAuthority(authentication, actor, context.actorUserId(), context.effectiveAt())) {
                return AccessReadScope.denyAll();
            }
            return AccessReadScope.fullAccess();
        }

        if (isAdministrativeAdministrativeContour(context.contour())) {
            if (!isKnownAdministrativeAdministrativeReadContext(context)) {
                return AccessReadScope.denyAll();
            }
            if (!hasAdministrativeReadAuthority(authentication, actor, context.actorUserId(), context.effectiveAt())) {
                return AccessReadScope.denyAll();
            }
            return AccessReadScope.fullAccess();
        }

        if (isAnalyticsContour(context.contour())) {
            return AccessReadScope.denyAll();
        }

        return AccessReadScope.denyAll();
    }

    @Override
    public boolean canReadUserAdministrationData(Long userId, java.time.Instant effectiveAt) {
        return resolveReadScope(new AccessPolicyQueryContext(
            userId,
            AccessReadArea.USER_ADMINISTRATION,
            AccessReadType.LIST,
            effectiveAt,
            null,
            null,
            "app_user"
        )).readAllowed();
    }

    @Override
    public boolean canReadAccessManagementData(Long userId, java.time.Instant effectiveAt) {
        return resolveReadScope(new AccessPolicyQueryContext(
            userId,
            AccessReadArea.ACCESS_MANAGEMENT,
            AccessReadType.HISTORY,
            effectiveAt,
            null,
            null,
            "user_access_area"
        )).readAllowed();
    }

    @Override
    public boolean canReadAssignmentData(Long userId, java.time.Instant effectiveAt) {
        return resolveReadScope(new AccessPolicyQueryContext(
            userId,
            AccessReadArea.ASSIGNMENT,
            AccessReadType.DETAIL,
            effectiveAt,
            null,
            null,
            "assignment",
            AccessReadSubjectScope.ACTOR_SELF
        )).readAllowed();
    }

    @Override
    public boolean canReadAssignmentCampaignData(Long userId, java.time.Instant effectiveAt) {
        return resolveReadScope(new AccessPolicyQueryContext(
            userId,
            AccessReadArea.ASSIGNMENT_CAMPAIGN,
            AccessReadType.DETAIL,
            effectiveAt,
            null,
            null,
            "assignment_campaign"
        )).readAllowed();
    }

    @Override
    public boolean canReadContentAuthoringData(Long userId, java.time.Instant effectiveAt) {
        return resolveReadScope(new AccessPolicyQueryContext(
            userId,
            AccessReadArea.CONTENT_AUTHORING,
            AccessReadType.DETAIL,
            effectiveAt,
            null,
            null,
            "content"
        )).readAllowed();
    }

    @Override
    public boolean canReadContentLifecycleData(Long userId, java.time.Instant effectiveAt) {
        return resolveReadScope(new AccessPolicyQueryContext(
            userId,
            AccessReadArea.CONTENT_LIFECYCLE,
            AccessReadType.DETAIL,
            effectiveAt,
            null,
            null,
            "content"
        )).readAllowed();
    }

    @Override
    public boolean canReadContentFinalControlData(Long userId, java.time.Instant effectiveAt) {
        return resolveReadScope(new AccessPolicyQueryContext(
            userId,
            AccessReadArea.CONTENT_FINAL_CONTROL,
            AccessReadType.DETAIL,
            effectiveAt,
            null,
            null,
            "content"
        )).readAllowed();
    }

    @Override
    public boolean canReadManagerialCurrentSupervision(Long userId, java.time.Instant effectiveAt) {
        return resolveReadScope(new AccessPolicyQueryContext(
            userId,
            AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION,
            AccessReadType.LIST,
            effectiveAt,
            null,
            null,
            "managerial_current_supervision",
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.MANAGER
        )).readAllowed();
    }

    @Override
    public boolean canReadManagerialHistoricalAnalytics(Long userId, java.time.Instant effectiveAt) {
        return resolveReadScope(new AccessPolicyQueryContext(
            userId,
            AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS,
            AccessReadType.ANALYTICS,
            effectiveAt,
            null,
            null,
            "managerial_historical_analytics",
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.MANAGER
        )).readAllowed();
    }

    @Override
    public boolean canReadExpertQuestionAnalytics(Long userId, java.time.Instant effectiveAt) {
        return resolveReadScope(new AccessPolicyQueryContext(
            userId,
            AccessReadArea.EXPERT_QUESTION_ANALYTICS,
            AccessReadType.ANALYTICS,
            effectiveAt,
            null,
            null,
            "expert_question_analytics",
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.EXPERT
        )).readAllowed();
    }

    private Authentication requireAuthentication(Long actorUserId) {
        ResolvedAuthenticatedActor resolvedActor = interactiveActorResolver.resolveActor();
        if (!resolvedActor.actorUserId().equals(actorUserId)) {
            throw new PolicyViolationException(
                "AccessSpecificationPolicy actorUserId does not match the current authenticated principal"
            );
        }
        return resolvedActor.authentication();
    }

    private UserOrgFoundationStateReadService.UserAccessPolicyFoundationState loadActorFoundationState(
        Long actorUserId,
        java.time.Instant effectiveAt
    ) {
        try {
            return userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(actorUserId, effectiveAt);
        } catch (NotFoundException exception) {
            throw new PolicyViolationException("Actor user is not readable for policy-aware read flow");
        }
    }

    private boolean isFoundationAdminContour(AccessReadArea contour) {
        return contour == AccessReadArea.USER_ADMINISTRATION
            || contour == AccessReadArea.ORGANIZATION
            || contour == AccessReadArea.ACCESS_MANAGEMENT
            || contour == AccessReadArea.TEMPORARY_AUTHORITY;
    }

    private boolean isContentContour(AccessReadArea contour) {
        return contour == AccessReadArea.CONTENT_AUTHORING
            || contour == AccessReadArea.CONTENT_LIFECYCLE
            || contour == AccessReadArea.CONTENT_FINAL_CONTROL;
    }

    private boolean isAssignmentContour(AccessReadArea contour) {
        return contour == AccessReadArea.ASSIGNMENT
            || contour == AccessReadArea.ASSIGNMENT_CAMPAIGN;
    }

    private boolean isAssignedCurrentAttemptContour(AccessReadArea contour) {
        return contour == AccessReadArea.ASSIGNED_CURRENT_ATTEMPT;
    }

    private boolean isSelfCurrentAttemptContour(AccessReadArea contour) {
        return contour == AccessReadArea.SELF_CURRENT_ATTEMPT;
    }

    private boolean isSelfVisibleTestingContour(AccessReadArea contour) {
        return contour == AccessReadArea.SELF_VISIBLE_TESTING;
    }

    private boolean isSelfResultHistoryContour(AccessReadArea contour) {
        return contour == AccessReadArea.SELF_RESULT_HISTORY;
    }

    private boolean isNotificationRecipientSelfContour(AccessReadArea contour) {
        return contour == AccessReadArea.NOTIFICATION_RECIPIENT_SELF;
    }

    private boolean isManagerialCurrentSupervisionContour(AccessReadArea contour) {
        return contour == AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION;
    }

    private boolean isManagerialHistoricalAnalyticsContour(AccessReadArea contour) {
        return contour == AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS;
    }

    private boolean isExpertQuestionAnalyticsContour(AccessReadArea contour) {
        return contour == AccessReadArea.EXPERT_QUESTION_ANALYTICS;
    }

    private boolean isAnalyticsContour(AccessReadArea contour) {
        return false;
    }

    private boolean isAdministrativeAdministrativeContour(AccessReadArea contour) {
        return contour == AccessReadArea.NOTIFICATION_ADMINISTRATION
            || contour == AccessReadArea.NOTIFICATION_RULE_ADMINISTRATION
            || contour == AccessReadArea.IMPORT_JOB_ADMINISTRATION
            || contour == AccessReadArea.AUDIT_EVENT_ADMINISTRATION;
    }

    private boolean isKnownContentReadContext(AccessPolicyQueryContext context) {
        if (context.subjectScope() != AccessReadSubjectScope.UNSPECIFIED
            || context.subjectSemantics() != AccessReadSubjectSemantics.UNSPECIFIED) {
            return false;
        }

        if (context.contour() == AccessReadArea.CONTENT_AUTHORING) {
            return (context.readType() == AccessReadType.LIST || context.readType() == AccessReadType.DETAIL)
                && CONTENT_AUTHORING_TARGET_FAMILIES.contains(context.targetEntityFamily());
        }
        if (context.contour() == AccessReadArea.CONTENT_LIFECYCLE) {
            return context.readType() == AccessReadType.DETAIL
                && CONTENT_LIFECYCLE_TARGET_FAMILIES.contains(context.targetEntityFamily());
        }
        if (context.contour() == AccessReadArea.CONTENT_FINAL_CONTROL) {
            return (context.readType() == AccessReadType.LIST || context.readType() == AccessReadType.DETAIL)
                && CONTENT_FINAL_CONTROL_TARGET_FAMILIES.contains(context.targetEntityFamily());
        }
        return false;
    }

    private boolean isKnownInternalAssignmentReadContext(AccessPolicyQueryContext context) {
        if (context.contour() == AccessReadArea.ASSIGNMENT_CAMPAIGN) {
            return context.subjectScope() == AccessReadSubjectScope.UNSPECIFIED
                && (context.readType() == AccessReadType.LIST || context.readType() == AccessReadType.DETAIL)
                && ASSIGNMENT_CAMPAIGN_TARGET_FAMILIES.contains(context.targetEntityFamily());
        }
        if (context.contour() == AccessReadArea.ASSIGNMENT) {
            if (context.subjectScope() != AccessReadSubjectScope.ACTOR_SELF
                || context.subjectSemantics() != AccessReadSubjectSemantics.SELF) {
                return false;
            }
            if (ASSIGNMENT_TARGET_FAMILIES.contains(context.targetEntityFamily())) {
                return context.readType() == AccessReadType.DETAIL || context.readType() == AccessReadType.LIST;
            }
            if (ASSIGNED_ENTITLEMENT_TARGET_FAMILIES.contains(context.targetEntityFamily())) {
                return context.readType() == AccessReadType.DETAIL || context.readType() == AccessReadType.LIST;
            }
            return false;
        }
        return false;
    }

    private boolean isAssignedLearningSelfReadContext(AccessPolicyQueryContext context) {
        return context.contour() == AccessReadArea.ASSIGNMENT
            && context.subjectScope() == AccessReadSubjectScope.ACTOR_SELF
            && context.subjectSemantics() == AccessReadSubjectSemantics.SELF
            && ASSIGNED_ENTITLEMENT_TARGET_FAMILIES.contains(context.targetEntityFamily());
    }

    private boolean isKnownAssignedCurrentAttemptReadContext(AccessPolicyQueryContext context) {
        return context.subjectScope() == AccessReadSubjectScope.ACTOR_SELF
            && context.subjectSemantics() == AccessReadSubjectSemantics.SELF
            && context.readType() == AccessReadType.DETAIL
            && ASSIGNED_CURRENT_ATTEMPT_TARGET_FAMILIES.contains(context.targetEntityFamily());
    }

    private boolean isKnownSelfCurrentAttemptReadContext(AccessPolicyQueryContext context) {
        return context.subjectScope() == AccessReadSubjectScope.ACTOR_SELF
            && context.subjectSemantics() == AccessReadSubjectSemantics.SELF
            && context.readType() == AccessReadType.DETAIL
            && context.targetTestId() != null
            && SELF_CURRENT_ATTEMPT_TARGET_FAMILIES.contains(context.targetEntityFamily());
    }

    private boolean isKnownSelfVisibleTestingReadContext(AccessPolicyQueryContext context) {
        return context.subjectScope() == AccessReadSubjectScope.ACTOR_SELF
            && context.subjectSemantics() == AccessReadSubjectSemantics.SELF
            && (context.readType() == AccessReadType.LIST || context.readType() == AccessReadType.DETAIL)
            && SELF_VISIBLE_TESTING_TARGET_FAMILIES.contains(context.targetEntityFamily());
    }

    private boolean isKnownSelfResultHistoryReadContext(AccessPolicyQueryContext context) {
        return context.subjectScope() == AccessReadSubjectScope.ACTOR_SELF
            && context.subjectSemantics() == AccessReadSubjectSemantics.SELF
            && (context.readType() == AccessReadType.LIST || context.readType() == AccessReadType.DETAIL)
            && SELF_RESULT_HISTORY_TARGET_FAMILIES.contains(context.targetEntityFamily());
    }

    private boolean isKnownNotificationRecipientSelfReadContext(AccessPolicyQueryContext context) {
        return context.subjectScope() == AccessReadSubjectScope.ACTOR_SELF
            && context.subjectSemantics() == AccessReadSubjectSemantics.SELF
            && (context.readType() == AccessReadType.LIST || context.readType() == AccessReadType.DETAIL)
            && NOTIFICATION_RECIPIENT_SELF_TARGET_FAMILIES.contains(context.targetEntityFamily());
    }

    private boolean isKnownManagerialCurrentSupervisionReadContext(AccessPolicyQueryContext context) {
        return context.subjectScope() == AccessReadSubjectScope.UNSPECIFIED
            && context.subjectSemantics() == AccessReadSubjectSemantics.MANAGER
            && context.readType() == AccessReadType.LIST
            && MANAGERIAL_CURRENT_SUPERVISION_TARGET_FAMILIES.contains(context.targetEntityFamily());
    }

    private boolean isKnownManagerialHistoricalAnalyticsReadContext(AccessPolicyQueryContext context) {
        return context.subjectScope() == AccessReadSubjectScope.UNSPECIFIED
            && context.subjectSemantics() == AccessReadSubjectSemantics.MANAGER
            && context.readType() == AccessReadType.ANALYTICS
            && MANAGERIAL_HISTORICAL_ANALYTICS_TARGET_FAMILIES.contains(context.targetEntityFamily());
    }

    private boolean isKnownExpertQuestionAnalyticsReadContext(AccessPolicyQueryContext context) {
        return context.subjectScope() == AccessReadSubjectScope.UNSPECIFIED
            && context.subjectSemantics() == AccessReadSubjectSemantics.EXPERT
            && context.readType() == AccessReadType.ANALYTICS
            && EXPERT_QUESTION_ANALYTICS_TARGET_FAMILIES.contains(context.targetEntityFamily());
    }

    private boolean isKnownAdministrativeAdministrativeReadContext(AccessPolicyQueryContext context) {
        if (context.subjectScope() != AccessReadSubjectScope.UNSPECIFIED
            || context.subjectSemantics() != AccessReadSubjectSemantics.UNSPECIFIED) {
            return false;
        }

        if (context.contour() == AccessReadArea.NOTIFICATION_ADMINISTRATION) {
            return (context.readType() == AccessReadType.LIST || context.readType() == AccessReadType.DETAIL)
                && NOTIFICATION_ADMINISTRATION_TARGET_FAMILIES.contains(context.targetEntityFamily());
        }
        if (context.contour() == AccessReadArea.NOTIFICATION_RULE_ADMINISTRATION) {
            return (context.readType() == AccessReadType.LIST || context.readType() == AccessReadType.DETAIL)
                && NOTIFICATION_RULE_ADMINISTRATION_TARGET_FAMILIES.contains(context.targetEntityFamily());
        }
        if (context.contour() == AccessReadArea.IMPORT_JOB_ADMINISTRATION) {
            return (context.readType() == AccessReadType.LIST || context.readType() == AccessReadType.DETAIL)
                && IMPORT_JOB_ADMINISTRATION_TARGET_FAMILIES.contains(context.targetEntityFamily());
        }
        if (context.contour() == AccessReadArea.AUDIT_EVENT_ADMINISTRATION) {
            return (context.readType() == AccessReadType.LIST || context.readType() == AccessReadType.DETAIL)
                && AUDIT_EVENT_ADMINISTRATION_TARGET_FAMILIES.contains(context.targetEntityFamily());
        }
        return false;
    }

    private boolean hasAdministrativeReadAuthority(
        Authentication authentication,
        UserOrgFoundationStateReadService.UserAccessPolicyFoundationState actor,
        Long actorUserId,
        java.time.Instant effectiveAt
    ) {
        return hasAdminAuthority(authentication) || hasAdminRoleAssignment(actor, actorUserId, effectiveAt);
    }

    private boolean hasAdminAuthority(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .map(grantedAuthority -> grantedAuthority.getAuthority())
            .anyMatch(READ_ADMIN_AUTHORITIES::contains);
    }

    private boolean hasAdminRoleAssignment(
        UserOrgFoundationStateReadService.UserAccessPolicyFoundationState actor,
        Long actorUserId,
        java.time.Instant effectiveAt
    ) {
        Set<String> roleCodes = new LinkedHashSet<>(actor.activePermanentRoleCodes());
        if (!hasAdminRoleCode(roleCodes)) {
            Set<Long> temporaryRoleIds = accessFoundationStateReadService.findActiveTemporaryRoleIds(actorUserId, effectiveAt);
            if (!temporaryRoleIds.isEmpty()) {
                roleCodes.addAll(userOrgFoundationStateReadService.findRoleCodesByIds(temporaryRoleIds));
            }
        }
        return hasAdminRoleCode(roleCodes);
    }

    private boolean hasContentReadAuthority(
        Authentication authentication,
        UserOrgFoundationStateReadService.UserAccessPolicyFoundationState actor,
        Long actorUserId,
        java.time.Instant effectiveAt
    ) {
        return hasContentAuthority(authentication) || hasContentRoleAssignment(actor, actorUserId, effectiveAt);
    }

    private boolean hasContentAuthority(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .map(grantedAuthority -> grantedAuthority.getAuthority())
            .anyMatch(CONTENT_AUTHORITIES::contains);
    }

    private boolean hasContentRoleAssignment(
        UserOrgFoundationStateReadService.UserAccessPolicyFoundationState actor,
        Long actorUserId,
        java.time.Instant effectiveAt
    ) {
        Set<String> roleCodes = new LinkedHashSet<>(actor.activePermanentRoleCodes());
        if (!hasContentRoleCode(roleCodes)) {
            Set<Long> temporaryRoleIds = accessFoundationStateReadService.findActiveTemporaryRoleIds(actorUserId, effectiveAt);
            if (!temporaryRoleIds.isEmpty()) {
                roleCodes.addAll(userOrgFoundationStateReadService.findRoleCodesByIds(temporaryRoleIds));
            }
        }
        return hasContentRoleCode(roleCodes);
    }

    private boolean hasAssignmentReadAuthority(
        Authentication authentication,
        UserOrgFoundationStateReadService.UserAccessPolicyFoundationState actor,
        Long actorUserId,
        java.time.Instant effectiveAt
    ) {
        return hasAssignmentAuthority(authentication) || hasAssignmentRoleAssignment(actor, actorUserId, effectiveAt);
    }

    private boolean hasAssignmentAuthority(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .map(grantedAuthority -> grantedAuthority.getAuthority())
            .anyMatch(ASSIGNMENT_AUTHORITIES::contains);
    }

    private boolean hasAssignmentRoleAssignment(
        UserOrgFoundationStateReadService.UserAccessPolicyFoundationState actor,
        Long actorUserId,
        java.time.Instant effectiveAt
    ) {
        Set<String> roleCodes = new LinkedHashSet<>(actor.activePermanentRoleCodes());
        if (!hasAssignmentRoleCode(roleCodes)) {
            Set<Long> temporaryRoleIds = accessFoundationStateReadService.findActiveTemporaryRoleIds(actorUserId, effectiveAt);
            if (!temporaryRoleIds.isEmpty()) {
                roleCodes.addAll(userOrgFoundationStateReadService.findRoleCodesByIds(temporaryRoleIds));
            }
        }
        return hasAssignmentRoleCode(roleCodes);
    }

    private boolean hasExpertQuestionAnalyticsReadAuthority(
        Authentication authentication,
        UserOrgFoundationStateReadService.UserAccessPolicyFoundationState actor,
        Long actorUserId,
        java.time.Instant effectiveAt
    ) {
        return hasExpertQuestionAnalyticsAuthority(authentication)
            || hasExpertQuestionAnalyticsRoleAssignment(actor, actorUserId, effectiveAt);
    }

    private boolean hasExpertQuestionAnalyticsAuthority(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .map(grantedAuthority -> grantedAuthority.getAuthority())
            .anyMatch(EXPERT_ANALYTICS_AUTHORITIES::contains);
    }

    private boolean hasExpertQuestionAnalyticsRoleAssignment(
        UserOrgFoundationStateReadService.UserAccessPolicyFoundationState actor,
        Long actorUserId,
        java.time.Instant effectiveAt
    ) {
        Set<String> roleCodes = new LinkedHashSet<>(actor.activePermanentRoleCodes());
        if (!hasExpertQuestionAnalyticsRoleCode(roleCodes)) {
            Set<Long> temporaryRoleIds = accessFoundationStateReadService.findActiveTemporaryRoleIds(actorUserId, effectiveAt);
            if (!temporaryRoleIds.isEmpty()) {
                roleCodes.addAll(userOrgFoundationStateReadService.findRoleCodesByIds(temporaryRoleIds));
            }
        }
        return hasExpertQuestionAnalyticsRoleCode(roleCodes);
    }

    private boolean hasAdminRoleCode(Set<String> roleCodes) {
        return roleCodes.stream()
            .map(roleCode -> roleCode.toUpperCase())
            .anyMatch(READ_ADMIN_ROLE_CODES::contains);
    }

    private boolean hasContentRoleCode(Set<String> roleCodes) {
        return roleCodes.stream()
            .map(roleCode -> roleCode.toUpperCase())
            .anyMatch(CONTENT_ROLE_CODES::contains);
    }

    private boolean hasLearnerSelfReadAuthority(
        Authentication authentication,
        UserOrgFoundationStateReadService.UserAccessPolicyFoundationState actor,
        Long actorUserId,
        java.time.Instant effectiveAt
    ) {
        return hasLearnerSelfAuthority(authentication) || hasLearnerSelfRoleAssignment(actor, actorUserId, effectiveAt);
    }

    private boolean hasLearnerSelfAuthority(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .map(grantedAuthority -> grantedAuthority.getAuthority())
            .anyMatch(LEARNER_SELF_READ_AUTHORITIES::contains);
    }

    private boolean hasLearnerSelfRoleAssignment(
        UserOrgFoundationStateReadService.UserAccessPolicyFoundationState actor,
        Long actorUserId,
        java.time.Instant effectiveAt
    ) {
        Set<String> roleCodes = new LinkedHashSet<>(actor.activePermanentRoleCodes());
        if (!hasLearnerSelfRoleCode(roleCodes)) {
            Set<Long> temporaryRoleIds = accessFoundationStateReadService.findActiveTemporaryRoleIds(actorUserId, effectiveAt);
            if (!temporaryRoleIds.isEmpty()) {
                roleCodes.addAll(userOrgFoundationStateReadService.findRoleCodesByIds(temporaryRoleIds));
            }
        }
        return hasLearnerSelfRoleCode(roleCodes);
    }

    private boolean hasAssignmentRoleCode(Set<String> roleCodes) {
        return roleCodes.stream()
            .map(roleCode -> roleCode.toUpperCase())
            .anyMatch(ASSIGNMENT_ROLE_CODES::contains);
    }

    private boolean hasLearnerSelfRoleCode(Set<String> roleCodes) {
        return roleCodes.stream()
            .map(roleCode -> roleCode.toUpperCase())
            .anyMatch(LEARNER_SELF_READ_ROLE_CODES::contains);
    }

    private boolean hasExpertQuestionAnalyticsRoleCode(Set<String> roleCodes) {
        return roleCodes.stream()
            .map(roleCode -> roleCode.toUpperCase())
            .anyMatch(EXPERT_ANALYTICS_ROLE_CODES::contains);
    }
}


