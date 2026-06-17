package com.vladislav.training.platform.application.policy;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.application.actor.ResolvedAuthenticatedActor;
import com.vladislav.training.platform.access.service.AccessFoundationStateReadService;
import com.vladislav.training.platform.assignment.service.AssignmentAdministrativeAdmissionFoundationStateReadService;
import com.vladislav.training.platform.assignment.service.AssignmentAssignedExecutionAdmissionFoundationStateReadService;
import com.vladislav.training.platform.assignment.service.AssignmentCampaignLaunchFoundationStateReadService;
import com.vladislav.training.platform.audit.service.SystemActorResolver;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.testing.admission.SelfExecutionAdmissionFoundationStateReadService;
import com.vladislav.training.platform.userorg.service.UserOrgFoundationStateReadService;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Класс {@code DefaultCapabilityAdmissionPolicy}.
 */
@Service("DefaultCapabilityAdmissionPolicy")
@Transactional(readOnly = true)
public class DefaultCapabilityAdmissionPolicy implements CapabilityAdmissionPolicy {

    private static final Set<String> COMMAND_ADMIN_ROLE_CODES = Set.of("ADMIN", "SYSTEM_ADMIN", "SUPER_ADMIN");
    private static final Set<String> CONTENT_COMMAND_ROLE_CODES = Set.of("EXPERT", "ADMIN", "SYSTEM_ADMIN", "SUPER_ADMIN");
    private static final Set<String> CONTENT_AUTHORITIES = Set.of("ROLE_EXPERT", "ROLE_ADMIN", "ROLE_SYSTEM_ADMIN", "ROLE_SUPER_ADMIN");

    private final UserOrgFoundationStateReadService userOrgFoundationStateReadService;
    private final AccessFoundationStateReadService accessFoundationStateReadService;
    private final InteractiveActorResolver interactiveActorResolver;
    private AssignmentAdministrativeAdmissionFoundationStateReadService assignmentAdministrativeAdmissionFoundationStateReadService;
    private AssignmentCampaignLaunchFoundationStateReadService assignmentCampaignLaunchFoundationStateReadService;
    private AssignmentAssignedExecutionAdmissionFoundationStateReadService
        assignmentAssignedExecutionAdmissionFoundationStateReadService;
    private SelfExecutionAdmissionFoundationStateReadService selfExecutionAdmissionFoundationStateReadService;
    private SystemActorResolver systemActorResolver;

    public DefaultCapabilityAdmissionPolicy(
        UserOrgFoundationStateReadService userOrgFoundationStateReadService,
        AccessFoundationStateReadService accessFoundationStateReadService,
        InteractiveActorResolver interactiveActorResolver
    ) {
        this.userOrgFoundationStateReadService = userOrgFoundationStateReadService;
        this.accessFoundationStateReadService = accessFoundationStateReadService;
        this.interactiveActorResolver = interactiveActorResolver;
    }

    @Autowired(required = false)
    void setAssignmentAdministrativeAdmissionFoundationStateReadService(
        AssignmentAdministrativeAdmissionFoundationStateReadService assignmentAdministrativeAdmissionFoundationStateReadService
    ) {
        this.assignmentAdministrativeAdmissionFoundationStateReadService = assignmentAdministrativeAdmissionFoundationStateReadService;
    }

    @Autowired(required = false)
    void setAssignmentCampaignLaunchFoundationStateReadService(
        AssignmentCampaignLaunchFoundationStateReadService assignmentCampaignLaunchFoundationStateReadService
    ) {
        this.assignmentCampaignLaunchFoundationStateReadService = assignmentCampaignLaunchFoundationStateReadService;
    }

    @Autowired(required = false)
    void setAssignmentAssignedExecutionAdmissionFoundationStateReadService(
        AssignmentAssignedExecutionAdmissionFoundationStateReadService
            assignmentAssignedExecutionAdmissionFoundationStateReadService
    ) {
        this.assignmentAssignedExecutionAdmissionFoundationStateReadService =
            assignmentAssignedExecutionAdmissionFoundationStateReadService;
    }

    @Autowired(required = false)
    void setSelfExecutionAdmissionFoundationStateReadService(
        SelfExecutionAdmissionFoundationStateReadService selfExecutionAdmissionFoundationStateReadService
    ) {
        this.selfExecutionAdmissionFoundationStateReadService = selfExecutionAdmissionFoundationStateReadService;
    }

    @Autowired(required = false)
    public void setSystemActorResolver(SystemActorResolver systemActorResolver) {
        this.systemActorResolver = systemActorResolver;
    }

    @Override
    public void check(CapabilityAdmissionRequest request) {
        Objects.requireNonNull(request, "CapabilityAdmissionRequest must not be null");

        boolean technicalSystemAdmission = isAllowedTechnicalSystemAdmission(request);
        Authentication authentication = technicalSystemAdmission ? null : requireAuthentication(request.actorUserId());
        UserOrgFoundationStateReadService.ActorCommandFoundationState actor =
            userOrgFoundationStateReadService.findActorCommandFoundationState(request.actorUserId(), request.requestedAt());
        if (!actor.active()) {
            throw new PolicyViolationException(CapabilityViolationCode.ACTOR_INACTIVE.name(),
                "Inactive actor cannot execute command-flow capability admission");
        }
        if (!technicalSystemAdmission && !hasCommandAuthority(authentication, actor, request.requestedAt(), request.operationCode())) {
            throw new PolicyViolationException(CapabilityViolationCode.ACTOR_NOT_AUTHORIZED.name(),
                "Authenticated actor is not authorized for command-flow capability admission");
        }

        CapabilityAdmissionFoundationFacts foundationFacts = new CapabilityAdmissionFoundationFacts(
            request,
            assignmentCampaignLaunchFoundationStateReadService,
            assignmentAdministrativeAdmissionFoundationStateReadService,
            assignmentAssignedExecutionAdmissionFoundationStateReadService,
            selfExecutionAdmissionFoundationStateReadService
        );
        CapabilityOperationCode operationCode = CapabilityOperationCode.fromCode(request.operationCode());
        switch (operationCode) {
            case ASSIGNMENT_CAMPAIGN_LAUNCH -> validateAssignmentCampaignLaunch(request, foundationFacts);
            case ASSIGNMENT_CANCEL -> validateAssignmentCancel(foundationFacts);
            case ASSIGNMENT_DEADLINE_EXTEND -> validateAssignmentDeadlineExtend(foundationFacts);
            case ASSIGNMENT_REPLACE_WITH_NEW -> validateAssignmentReplaceWithNew(foundationFacts);
            case TESTING_ASSIGNED_ATTEMPT_START, TESTING_ASSIGNED_ATTEMPT_CONTINUE,
                TESTING_ASSIGNED_ATTEMPT_SUBMIT, TESTING_ASSIGNED_ANSWER_MUTATION ->
                validateAssignedExecution(foundationFacts);
            case TESTING_SELF_ATTEMPT_START, TESTING_SELF_ATTEMPT_CONTINUE, TESTING_SELF_ATTEMPT_SUBMIT, TESTING_SELF_ATTEMPT_ABANDON,
                TESTING_SELF_ANSWER_MUTATION -> validateSelfExecution(foundationFacts);
            case USERORG_USER_CREATE -> validateCreateUser();
            case USERORG_USER_UPDATE -> validateUpdateUser(foundationFacts);
            case USERORG_USER_DEACTIVATE -> validateDeactivateUser(foundationFacts);
            case USERORG_USER_ROLE_ASSIGN -> validatePermanentRoleAssign(foundationFacts);
            case USERORG_USER_ROLE_CLOSE -> validatePermanentRoleClose(foundationFacts);
            case USERORG_USER_ORGANIZATION_ASSIGN -> validateOrganizationAssign(foundationFacts);
            case USERORG_USER_ORGANIZATION_CLOSE -> validateOrganizationClose(foundationFacts);
            case USERORG_USER_PRIMARY_HOME_REPLACE -> validatePrimaryHomeReplace(foundationFacts);
            case USERORG_ORGANIZATIONAL_UNIT_TYPE_CREATE -> validateUnitTypeCreate();
            case USERORG_ORGANIZATIONAL_UNIT_TYPE_UPDATE -> validateUnitTypeUpdate(foundationFacts);
            case USERORG_ORGANIZATIONAL_UNIT_CREATE -> validateUnitCreate();
            case USERORG_ORGANIZATIONAL_UNIT_UPDATE -> validateUnitUpdate(foundationFacts);
            case USERORG_ORGANIZATIONAL_UNIT_MOVE -> validateUnitMove(request, foundationFacts);
            case USERORG_ORGANIZATIONAL_UNIT_ARCHIVE -> validateUnitArchive(foundationFacts);
            case ACCESS_USER_ACCESS_AREA_ASSIGN -> validateUserAccessAreaAssign(foundationFacts);
            case ACCESS_USER_ACCESS_AREA_CLOSE -> validateUserAccessAreaClose(foundationFacts);
            case ACCESS_MANAGEMENT_RELATION_ASSIGN -> validateManagementRelationAssign(foundationFacts);
            case ACCESS_MANAGEMENT_RELATION_CLOSE -> validateManagementRelationClose(foundationFacts);
            case ACCESS_TEMPORARY_ROLE_ASSIGN -> validateTemporaryRoleAssign(foundationFacts);
            case ACCESS_TEMPORARY_ROLE_CLOSE -> validateTemporaryRoleClose(foundationFacts);
            case ACCESS_TEMPORARY_ACCESS_ASSIGN -> validateTemporaryAccessAreaAssign(foundationFacts);
            case ACCESS_TEMPORARY_ACCESS_CLOSE -> validateTemporaryAccessAreaClose(foundationFacts);
            case ACCESS_TEMPORARY_MANAGEMENT_ASSIGN -> validateTemporaryManagementAssign(foundationFacts);
            case ACCESS_TEMPORARY_MANAGEMENT_CLOSE -> validateTemporaryManagementClose(foundationFacts);
            case CONTENT_DRAFT_CREATE -> validateContentDraftCreate(foundationFacts);
            case CONTENT_DRAFT_UPDATE -> validateContentDraftUpdate(foundationFacts);
            case CONTENT_PUBLISH -> validateContentPublish(foundationFacts);
            case CONTENT_ARCHIVE -> validateContentArchive(foundationFacts);
            case CONTENT_FINAL_ASSIGN -> validateContentFinalAssign(foundationFacts);
            case CONTENT_FINAL_REPLACE -> validateContentFinalReplace(foundationFacts);
            case CONTENT_FINAL_CLEAR -> validateContentFinalClear(foundationFacts);
            case NOTIFICATION_RULE_CREATE -> validateNotificationRuleCreate(request, foundationFacts);
            case NOTIFICATION_RULE_UPDATE -> validateNotificationRuleUpdate(foundationFacts);
            case NOTIFICATION_RULE_ENABLE -> validateNotificationRuleEnable(foundationFacts);
            case NOTIFICATION_RULE_DISABLE -> validateNotificationRuleDisable(foundationFacts);
            case ANALYTICS_RESULT_REBUILD -> validateAnalyticsResultRebuild(foundationFacts);
            case PERSONNEL_EXCEL_DRY_RUN -> validatePersonnelExcelDryRun();
            case PERSONNEL_EXCEL_APPLY -> validatePersonnelExcelApply();
            case IMPORT_JOB_LAUNCH -> validateImportJobLaunch(request, foundationFacts);
            case IMPORT_ITEM_REVIEW_APPLY -> validateImportItemReviewApply(foundationFacts);
            case IMPORT_ITEM_REVIEW_REJECT -> validateImportItemReviewReject(foundationFacts);
            default -> throw new PolicyViolationException(CapabilityViolationCode.UNKNOWN_OPERATION.name(),
                "Capability admission rejects unknown operationCode: " + request.operationCode());
        }
    }

    private void validateCreateUser() {}
    private void validateAssignmentCampaignLaunch(CapabilityAdmissionRequest request, CapabilityAdmissionFoundationFacts foundationFacts) {
        requireAssignmentCampaignLaunchTarget(request, foundationFacts.targetEntityType());
        foundationFacts.resolveAssignmentCampaignLaunchFoundationState();
    }
    private void validateAssignmentCancel(CapabilityAdmissionFoundationFacts foundationFacts) {
        foundationFacts.assignmentCancel();
        Long assignmentId = requireAssignmentAdministrativeTarget(foundationFacts);
        foundationFacts.resolveAssignmentAdministrativeFoundationState(assignmentId);
    }
    private void validateAssignmentDeadlineExtend(CapabilityAdmissionFoundationFacts foundationFacts) {
        var payload = foundationFacts.assignmentDeadlineExtend();
        foundationFacts.requiredNewDeadlineAt(payload);
        Long assignmentId = requireAssignmentAdministrativeTarget(foundationFacts);
        foundationFacts.resolveAssignmentAdministrativeFoundationState(assignmentId);
    }
    private void validateAssignmentReplaceWithNew(CapabilityAdmissionFoundationFacts foundationFacts) {
        var payload = foundationFacts.assignmentReplaceWithNew();
        Long assignmentId = requireAssignmentAdministrativeTarget(foundationFacts);
        var foundationState = foundationFacts.resolveAssignmentAdministrativeFoundationState(assignmentId);
        foundationFacts.requireAssignmentReplacementCampaignCompatibility(payload, foundationState);
    }
    private void validateAssignedExecution(CapabilityAdmissionFoundationFacts foundationFacts) {
        if (foundationFacts.targetEntityType() != CapabilityTargetEntityType.ASSIGNMENT_TEST) {
            throw new PolicyViolationException(
                CapabilityViolationCode.REQUEST_PAYLOAD_INVALID.name(),
                "Assigned execution command requires ASSIGNMENT_TEST target type"
            );
        }
        var foundationState = foundationFacts.resolveAssignmentAssignedExecutionFoundationState();
        if (foundationState.assignmentCancelled()) {
            throw new ConflictException(
                "Assigned execution is not allowed for CANCELLED assignment: " + foundationState.assignmentId()
            );
        }
        if (foundationState.assignmentClosed()) {
            throw new ConflictException(
                "Assigned execution is not allowed for CLOSED assignment: " + foundationState.assignmentId()
            );
        }
        if (foundationState.assignmentTestClosed()) {
            throw new ConflictException(
                "Assigned execution is not allowed for CLOSED assignment test: " + foundationState.assignmentTestId()
            );
        }
    }
    private void validateSelfExecution(CapabilityAdmissionFoundationFacts foundationFacts) {
        if (foundationFacts.targetEntityType() != CapabilityTargetEntityType.TEST) {
            throw new PolicyViolationException(
                CapabilityViolationCode.REQUEST_PAYLOAD_INVALID.name(),
                "Self execution command requires TEST target type"
            );
        }
        foundationFacts.resolveSelfExecutionFoundationState();
    }
    private void validateUpdateUser(CapabilityAdmissionFoundationFacts foundationFacts) { requireExistingUser(foundationFacts.targetEntityId()); }
    private void validateDeactivateUser(CapabilityAdmissionFoundationFacts foundationFacts) {
        var targetUser = requireExistingUser(foundationFacts.targetEntityId());
        if (!targetUser.active()) throw new ConflictException("User is already INACTIVE: " + targetUser.userId());
    }
    private void validatePermanentRoleAssign(CapabilityAdmissionFoundationFacts foundationFacts) {
        var payload = foundationFacts.roleAssignment();
        requireActiveUser(foundationFacts.requiredUserId(payload));
        requireRoleExists(foundationFacts.requiredRoleId(payload));
    }
    private void validatePermanentRoleClose(CapabilityAdmissionFoundationFacts foundationFacts) { foundationFacts.targetEntityId(); }
    private void validateAnalyticsResultRebuild(CapabilityAdmissionFoundationFacts foundationFacts) {
        if (foundationFacts.targetEntityType() != CapabilityTargetEntityType.ANALYTICS_AGGREGATE) {
            throw new PolicyViolationException(
                CapabilityViolationCode.REQUEST_PAYLOAD_INVALID.name(),
                "Analytics rebuild command requires ANALYTICS_AGGREGATE target type"
            );
        }
    }
    private void validateOrganizationAssign(CapabilityAdmissionFoundationFacts foundationFacts) {
        var payload = foundationFacts.organizationAssignment();
        requireActiveUser(foundationFacts.requiredUserId(payload));
        requireNonArchivedOrganizationalUnitTarget(foundationFacts.requiredOrganizationalUnitId(payload));
    }
    private void validateOrganizationClose(CapabilityAdmissionFoundationFacts foundationFacts) { foundationFacts.targetEntityId(); }
    private void validatePrimaryHomeReplace(CapabilityAdmissionFoundationFacts foundationFacts) {
        var payload = foundationFacts.organizationAssignment();
        requireActiveUser(foundationFacts.requiredUserId(payload));
        requireNonArchivedOrganizationalUnitTarget(foundationFacts.requiredOrganizationalUnitId(payload));
    }
    private void validateUnitTypeCreate() {}
    private void validateUnitTypeUpdate(CapabilityAdmissionFoundationFacts foundationFacts) {
        Long targetId = foundationFacts.targetEntityId();
        if (!userOrgFoundationStateReadService.organizationalUnitTypeExists(targetId)) throw new NotFoundException("Organizational unit type not found: " + targetId);
    }
    private void validateUnitCreate() {}
    private void validateUnitUpdate(CapabilityAdmissionFoundationFacts foundationFacts) {
        var unit = userOrgFoundationStateReadService.findOrganizationalUnitFoundationState(foundationFacts.targetEntityId());
        if (!unit.active()) throw new ConflictException("Archived organizational unit cannot be edited: " + unit.organizationalUnitId());
    }
    private void validateUnitMove(CapabilityAdmissionRequest request, CapabilityAdmissionFoundationFacts foundationFacts) {
        var unit = userOrgFoundationStateReadService.findOrganizationalUnitFoundationState(foundationFacts.targetEntityId());
        if (!unit.active()) throw new ConflictException("Archived organizational unit cannot be edited: " + unit.organizationalUnitId());
        if (request.payloadContext() instanceof CapabilityAdmissionPayload.Empty) return;
        var payload = foundationFacts.organizationalUnitMutation();
        Long newParentUnitId = foundationFacts.optionalNewParentUnitId(payload);
        if (newParentUnitId != null) requireNonArchivedOrganizationalUnitTarget(newParentUnitId);
    }
    private void validateUnitArchive(CapabilityAdmissionFoundationFacts foundationFacts) { userOrgFoundationStateReadService.findOrganizationalUnitFoundationState(foundationFacts.targetEntityId()); }
    private void validateUserAccessAreaAssign(CapabilityAdmissionFoundationFacts foundationFacts) {
        var payload = foundationFacts.accessArea();
        requireActiveUser(foundationFacts.requiredUserId(payload));
        requireNonArchivedOrganizationalUnitTarget(foundationFacts.optionalOrganizationalUnitId(payload));
    }
    private void validateUserAccessAreaClose(CapabilityAdmissionFoundationFacts foundationFacts) { foundationFacts.targetEntityId(); }
    private void validateManagementRelationAssign(CapabilityAdmissionFoundationFacts foundationFacts) {
        var payload = foundationFacts.managementRelation();
        requireActiveUser(foundationFacts.requiredUserId(payload));
        requireExistingManagementRelationType(foundationFacts.requiredManagementRelationTypeId(payload));
        requireNonArchivedOrganizationalUnitTarget(foundationFacts.requiredOrganizationalUnitId(payload));
    }
    private void validateManagementRelationClose(CapabilityAdmissionFoundationFacts foundationFacts) { foundationFacts.targetEntityId(); }
    private void validateTemporaryRoleAssign(CapabilityAdmissionFoundationFacts foundationFacts) {
        var payload = foundationFacts.roleAssignment();
        requireActiveUser(foundationFacts.requiredUserId(payload));
        requireRoleExists(foundationFacts.requiredRoleId(payload));
    }
    private void validateTemporaryRoleClose(CapabilityAdmissionFoundationFacts foundationFacts) { foundationFacts.targetEntityId(); }
    private void validateTemporaryAccessAreaAssign(CapabilityAdmissionFoundationFacts foundationFacts) {
        var payload = foundationFacts.accessArea();
        requireActiveUser(foundationFacts.requiredUserId(payload));
        requireNonArchivedOrganizationalUnitTarget(foundationFacts.optionalOrganizationalUnitId(payload));
    }
    private void validateTemporaryAccessAreaClose(CapabilityAdmissionFoundationFacts foundationFacts) { foundationFacts.targetEntityId(); }
    private void validateTemporaryManagementAssign(CapabilityAdmissionFoundationFacts foundationFacts) {
        var payload = foundationFacts.managementRelation();
        requireActiveUser(foundationFacts.requiredUserId(payload));
        requireExistingManagementRelationType(foundationFacts.requiredManagementRelationTypeId(payload));
        requireNonArchivedOrganizationalUnitTarget(foundationFacts.requiredOrganizationalUnitId(payload));
    }
    private void validateTemporaryManagementClose(CapabilityAdmissionFoundationFacts foundationFacts) { foundationFacts.targetEntityId(); }

    private void validateContentDraftCreate(CapabilityAdmissionFoundationFacts foundationFacts) {
        CapabilityTargetEntityType targetType = foundationFacts.targetEntityType();
        validateContentTargetType(targetType);
        if (targetType == CapabilityTargetEntityType.COURSE) {
            return;
        }
        CapabilityAdmissionPayload.ContentMutation mutation = foundationFacts.contentMutation();
        validateContentParentShape(targetType, foundationFacts.requiredParentEntityType(mutation));
        foundationFacts.requiredParentEntityId(mutation);
    }

    private void validateContentDraftUpdate(CapabilityAdmissionFoundationFacts foundationFacts) {
        CapabilityTargetEntityType targetType = foundationFacts.targetEntityType();
        validateContentTargetType(targetType);
        foundationFacts.targetEntityId();
        if (targetType == CapabilityTargetEntityType.COURSE) {
            return;
        }
        CapabilityAdmissionPayload.ContentMutation mutation = foundationFacts.contentMutation();
        validateContentParentShapeForUpdate(targetType, foundationFacts.requiredParentEntityType(mutation));
        foundationFacts.requiredParentEntityId(mutation);
    }

    private void validateContentPublish(CapabilityAdmissionFoundationFacts foundationFacts) {
        validateContentTargetType(foundationFacts.targetEntityType());
        foundationFacts.targetEntityId();
    }

    private void validateContentArchive(CapabilityAdmissionFoundationFacts foundationFacts) {
        validateContentTargetType(foundationFacts.targetEntityType());
        foundationFacts.targetEntityId();
    }

    private void validateContentFinalAssign(CapabilityAdmissionFoundationFacts foundationFacts) {
        validateFinalControlMutation(foundationFacts, true);
    }

    private void validateContentFinalReplace(CapabilityAdmissionFoundationFacts foundationFacts) {
        validateFinalControlMutation(foundationFacts, true);
    }

    private void validateContentFinalClear(CapabilityAdmissionFoundationFacts foundationFacts) {
        validateFinalControlMutation(foundationFacts, false);
    }

    private void validateNotificationRuleCreate(
        CapabilityAdmissionRequest request,
        CapabilityAdmissionFoundationFacts foundationFacts
    ) {
        requireAdministrativeTargetType(
            foundationFacts.targetEntityType(),
            CapabilityTargetEntityType.NOTIFICATION_RULE,
            "Notification rule create command requires NOTIFICATION_RULE target type"
        );
        if (request.targetEntityId() != null) {
            throw new PolicyViolationException(
                CapabilityViolationCode.REQUEST_PAYLOAD_INVALID.name(),
                "Notification rule create must not reference an already materialized notification_rule id"
            );
        }
    }

    private void validateNotificationRuleUpdate(CapabilityAdmissionFoundationFacts foundationFacts) {
        requireAdministrativeTargetType(
            foundationFacts.targetEntityType(),
            CapabilityTargetEntityType.NOTIFICATION_RULE,
            "Notification rule update command requires NOTIFICATION_RULE target type"
        );
        foundationFacts.targetEntityId();
    }

    private void validateNotificationRuleEnable(CapabilityAdmissionFoundationFacts foundationFacts) {
        requireAdministrativeTargetType(
            foundationFacts.targetEntityType(),
            CapabilityTargetEntityType.NOTIFICATION_RULE,
            "Notification rule enable command requires NOTIFICATION_RULE target type"
        );
        foundationFacts.targetEntityId();
    }

    private void validateNotificationRuleDisable(CapabilityAdmissionFoundationFacts foundationFacts) {
        requireAdministrativeTargetType(
            foundationFacts.targetEntityType(),
            CapabilityTargetEntityType.NOTIFICATION_RULE,
            "Notification rule disable command requires NOTIFICATION_RULE target type"
        );
        foundationFacts.targetEntityId();
    }

    private void validateImportJobLaunch(
        CapabilityAdmissionRequest request,
        CapabilityAdmissionFoundationFacts foundationFacts
    ) {
        requireAdministrativeTargetType(
            foundationFacts.targetEntityType(),
            CapabilityTargetEntityType.IMPORT_JOB,
            "Import job launch command requires IMPORT_JOB target type"
        );
        if (request.targetEntityId() != null) {
            throw new PolicyViolationException(
                CapabilityViolationCode.REQUEST_PAYLOAD_INVALID.name(),
                "Import job launch must not reference an already materialized import_job id"
            );
        }
    }

    private void validateImportItemReviewApply(CapabilityAdmissionFoundationFacts foundationFacts) {
        requireAdministrativeTargetType(
            foundationFacts.targetEntityType(),
            CapabilityTargetEntityType.IMPORT_JOB_ITEM,
            "Import item review apply command requires IMPORT_JOB_ITEM target type"
        );
        foundationFacts.targetEntityId();
    }

    private void validateImportItemReviewReject(CapabilityAdmissionFoundationFacts foundationFacts) {
        requireAdministrativeTargetType(
            foundationFacts.targetEntityType(),
            CapabilityTargetEntityType.IMPORT_JOB_ITEM,
            "Import item review reject command requires IMPORT_JOB_ITEM target type"
        );
        foundationFacts.targetEntityId();
    }

    private void validateFinalControlMutation(CapabilityAdmissionFoundationFacts foundationFacts, boolean requiresTestId) {
        if (foundationFacts.targetEntityType() != CapabilityTargetEntityType.TOPIC_FINAL_CONTROL) {
            throw new PolicyViolationException(
                CapabilityViolationCode.REQUEST_PAYLOAD_INVALID.name(),
                "Final-control target type must be TOPIC_FINAL_CONTROL"
            );
        }
        Long targetTopicId = foundationFacts.targetEntityId();
        CapabilityAdmissionPayload.TopicFinalControlMutation mutation = foundationFacts.topicFinalControlMutation();
        Long payloadTopicId = foundationFacts.requiredTopicId(mutation);
        if (!Objects.equals(targetTopicId, payloadTopicId)) {
            throw new PolicyViolationException(
                CapabilityViolationCode.REQUEST_PAYLOAD_INVALID.name(),
                "Final-control topicId must match targetEntityId"
            );
        }
        if (requiresTestId) {
            foundationFacts.requiredTestId(mutation);
        }
    }

    private void validateContentParentShape(CapabilityTargetEntityType targetType, CapabilityTargetEntityType parentType) {
        switch (targetType) {
            case TOPIC -> requireContentParentType(parentType, CapabilityTargetEntityType.COURSE, targetType);
            case MATERIAL, QUESTION, TEST -> requireContentParentType(parentType, CapabilityTargetEntityType.TOPIC, targetType);
            case COURSE -> {
            }
            default -> throw new PolicyViolationException(
                CapabilityViolationCode.REQUEST_PAYLOAD_INVALID.name(),
                "Unsupported content draft create target type: " + targetType
            );
        }
    }

    private void validateContentParentShapeForUpdate(CapabilityTargetEntityType targetType, CapabilityTargetEntityType parentType) {
        switch (targetType) {
            case TOPIC -> requireContentParentType(parentType, CapabilityTargetEntityType.COURSE, targetType);
            case MATERIAL -> requireContentParentType(parentType, CapabilityTargetEntityType.TOPIC, targetType);
            case QUESTION -> requireOneOfContentParentTypes(targetType, parentType, CapabilityTargetEntityType.TOPIC, CapabilityTargetEntityType.QUESTION);
            case TEST -> requireOneOfContentParentTypes(targetType, parentType, CapabilityTargetEntityType.TOPIC, CapabilityTargetEntityType.TEST);
            case COURSE -> {
            }
            default -> throw new PolicyViolationException(
                CapabilityViolationCode.REQUEST_PAYLOAD_INVALID.name(),
                "Unsupported content draft update target type: " + targetType
            );
        }
    }

    private void requireContentParentType(
        CapabilityTargetEntityType actualParentType,
        CapabilityTargetEntityType expectedParentType,
        CapabilityTargetEntityType targetType
    ) {
        if (actualParentType != expectedParentType) {
            throw new PolicyViolationException(
                CapabilityViolationCode.REQUEST_PAYLOAD_INVALID.name(),
                "Content mutation for " + targetType + " requires parent type " + expectedParentType
            );
        }
    }

    private void requireOneOfContentParentTypes(
        CapabilityTargetEntityType targetType,
        CapabilityTargetEntityType actualParentType,
        CapabilityTargetEntityType firstAllowedParentType,
        CapabilityTargetEntityType secondAllowedParentType
    ) {
        if (actualParentType != firstAllowedParentType && actualParentType != secondAllowedParentType) {
            throw new PolicyViolationException(
                CapabilityViolationCode.REQUEST_PAYLOAD_INVALID.name(),
                "Content mutation for " + targetType + " requires parent type "
                    + firstAllowedParentType + " or " + secondAllowedParentType
            );
        }
    }

    private void validateContentTargetType(CapabilityTargetEntityType targetType) {
        if (targetType != CapabilityTargetEntityType.COURSE
            && targetType != CapabilityTargetEntityType.TOPIC
            && targetType != CapabilityTargetEntityType.MATERIAL
            && targetType != CapabilityTargetEntityType.QUESTION
            && targetType != CapabilityTargetEntityType.TEST) {
            throw new PolicyViolationException(CapabilityViolationCode.REQUEST_PAYLOAD_INVALID.name(),
                "Content operation requires content target type but got: " + targetType);
        }
    }

    private void requireAdministrativeTargetType(
        CapabilityTargetEntityType actualTargetType,
        CapabilityTargetEntityType expectedTargetType,
        String message
    ) {
        if (actualTargetType != expectedTargetType) {
            throw new PolicyViolationException(
                CapabilityViolationCode.REQUEST_PAYLOAD_INVALID.name(),
                message
            );
        }
    }

    private void requireAssignmentCampaignLaunchTarget(
        CapabilityAdmissionRequest request,
        CapabilityTargetEntityType targetType
    ) {
        if (targetType != CapabilityTargetEntityType.ASSIGNMENT_CAMPAIGN) {
            throw new PolicyViolationException(
                CapabilityViolationCode.REQUEST_PAYLOAD_INVALID.name(),
                "Assignment campaign launch requires ASSIGNMENT_CAMPAIGN target type"
            );
        }
        if (request.targetEntityId() != null) {
            throw new PolicyViolationException(
                CapabilityViolationCode.REQUEST_PAYLOAD_INVALID.name(),
                "Assignment campaign launch must not reference an already materialized assignment_campaign id"
            );
        }
    }

    private Long requireAssignmentAdministrativeTarget(CapabilityAdmissionFoundationFacts foundationFacts) {
        if (foundationFacts.targetEntityType() != CapabilityTargetEntityType.ASSIGNMENT) {
            throw new PolicyViolationException(
                CapabilityViolationCode.REQUEST_PAYLOAD_INVALID.name(),
                "Assignment administrative command requires ASSIGNMENT target type"
            );
        }
        return foundationFacts.requiredAssignmentId();
    }

    private boolean isAllowedTechnicalSystemAdmission(CapabilityAdmissionRequest request) {
        if (!CapabilityOperationCodes.IMPORT_JOB_LAUNCH.equals(request.operationCode())) {
            return false;
        }
        if (request.targetEntityType() != CapabilityTargetEntityType.IMPORT_JOB) {
            return false;
        }
        if (request.actorUserId() == null || systemActorResolver == null) {
            return false;
        }
        Long resolvedSystemActorUserId = systemActorResolver.resolveSystemActorUserId();
        return resolvedSystemActorUserId != null && Objects.equals(resolvedSystemActorUserId, request.actorUserId());
    }

    private void validatePersonnelExcelDryRun() {}

    private void validatePersonnelExcelApply() {}

    private Authentication requireAuthentication(Long actorUserId) {
        ResolvedAuthenticatedActor resolvedActor = interactiveActorResolver.resolveActor();
        if (!Objects.equals(resolvedActor.actorUserId(), actorUserId)) {
            throw new PolicyViolationException(CapabilityViolationCode.ACTOR_CONTEXT_MISMATCH.name(),
                "Capability admission actorUserId does not match the current authenticated principal");
        }
        return resolvedActor.authentication();
    }

    private boolean hasCommandAuthority(Authentication authentication,
                                        UserOrgFoundationStateReadService.ActorCommandFoundationState actor,
                                        Instant effectiveAt,
                                        String operationCode) {
        if (CapabilityOperationCodes.ASSIGNMENT_CAMPAIGN_LAUNCH.equals(operationCode)
            || CapabilityOperationCodes.ASSIGNMENT_CANCEL.equals(operationCode)
            || CapabilityOperationCodes.ASSIGNMENT_DEADLINE_EXTEND.equals(operationCode)
            || CapabilityOperationCodes.ASSIGNMENT_REPLACE_WITH_NEW.equals(operationCode)) {
            return hasContentAuthority(authentication, actor, effectiveAt);
        }
        if (isAssignedExecutionOperation(operationCode) || isSelfExecutionOperation(operationCode)) {
            return authentication.isAuthenticated();
        }
        if (isContentOperation(operationCode)) {
            return hasContentAuthority(authentication, actor, effectiveAt);
        }
        if (authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) return true;
        Set<String> roleCodes = new LinkedHashSet<>(actor.activePermanentRoleCodes());
        if (!hasAdministrativeRoleCode(roleCodes)) {
            Set<Long> activeTemporaryRoleIds = accessFoundationStateReadService.findActiveTemporaryRoleIds(actor.actorUserId(), effectiveAt);
            if (!activeTemporaryRoleIds.isEmpty()) roleCodes.addAll(userOrgFoundationStateReadService.findRoleCodesByIds(activeTemporaryRoleIds));
        }
        return hasAdministrativeRoleCode(roleCodes);
    }

    private boolean isContentOperation(String operationCode) {
        return CapabilityOperationCodes.CONTENT_DRAFT_CREATE.equals(operationCode)
            || CapabilityOperationCodes.CONTENT_DRAFT_UPDATE.equals(operationCode)
            || CapabilityOperationCodes.CONTENT_PUBLISH.equals(operationCode)
            || CapabilityOperationCodes.CONTENT_ARCHIVE.equals(operationCode)
            || CapabilityOperationCodes.CONTENT_FINAL_ASSIGN.equals(operationCode)
            || CapabilityOperationCodes.CONTENT_FINAL_REPLACE.equals(operationCode)
            || CapabilityOperationCodes.CONTENT_FINAL_CLEAR.equals(operationCode);
    }

    private boolean isAssignedExecutionOperation(String operationCode) {
        return CapabilityOperationCodes.TESTING_ASSIGNED_ATTEMPT_START.equals(operationCode)
            || CapabilityOperationCode.TESTING_ASSIGNED_ATTEMPT_CONTINUE.code().equals(operationCode)
            || CapabilityOperationCodes.TESTING_ASSIGNED_ATTEMPT_SUBMIT.equals(operationCode)
            || CapabilityOperationCodes.TESTING_ASSIGNED_ANSWER_MUTATION.equals(operationCode);
    }

    private boolean isSelfExecutionOperation(String operationCode) {
        return CapabilityOperationCodes.TESTING_SELF_ATTEMPT_START.equals(operationCode)
            || CapabilityOperationCodes.TESTING_SELF_ATTEMPT_CONTINUE.equals(operationCode)
            || CapabilityOperationCodes.TESTING_SELF_ATTEMPT_SUBMIT.equals(operationCode)
            || CapabilityOperationCodes.TESTING_SELF_ATTEMPT_ABANDON.equals(operationCode)
            || CapabilityOperationCodes.TESTING_SELF_ANSWER_MUTATION.equals(operationCode);
    }

    private boolean hasContentAuthority(Authentication authentication,
                                        UserOrgFoundationStateReadService.ActorCommandFoundationState actor,
                                        Instant effectiveAt) {
        if (authentication.getAuthorities().stream().anyMatch(a -> CONTENT_AUTHORITIES.contains(a.getAuthority()))) return true;
        Set<String> roleCodes = new LinkedHashSet<>(actor.activePermanentRoleCodes());
        Set<Long> activeTemporaryRoleIds = accessFoundationStateReadService.findActiveTemporaryRoleIds(actor.actorUserId(), effectiveAt);
        if (!activeTemporaryRoleIds.isEmpty()) roleCodes.addAll(userOrgFoundationStateReadService.findRoleCodesByIds(activeTemporaryRoleIds));
        return roleCodes.stream().map(String::toUpperCase).anyMatch(CONTENT_COMMAND_ROLE_CODES::contains);
    }

    private boolean hasAdministrativeRoleCode(Set<String> roleCodes) {
        return roleCodes.stream().map(String::toUpperCase).anyMatch(COMMAND_ADMIN_ROLE_CODES::contains);
    }

    private UserOrgFoundationStateReadService.TargetUserCommandFoundationState requireExistingUser(Long userId) {
        return userOrgFoundationStateReadService.findTargetUserCommandFoundationState(userId);
    }
    private UserOrgFoundationStateReadService.TargetUserCommandFoundationState requireActiveUser(Long userId) {
        var user = requireExistingUser(userId);
        if (!user.active()) throw new ConflictException("INACTIVE user cannot receive new active assignments: " + user.userId());
        return user;
    }
    private void requireRoleExists(Long roleId) { userOrgFoundationStateReadService.findRoleCodesByIds(Set.of(roleId)); }
    private void requireExistingManagementRelationType(Long managementRelationTypeId) {
        if (!accessFoundationStateReadService.managementRelationTypeExists(managementRelationTypeId)) {
            throw new NotFoundException("Management relation type not found: " + managementRelationTypeId);
        }
    }
    private UserOrgFoundationStateReadService.OrganizationalUnitFoundationState requireNonArchivedOrganizationalUnitTarget(Long organizationalUnitId) {
        if (organizationalUnitId == null) return null;
        var organizationalUnit = userOrgFoundationStateReadService.findOrganizationalUnitFoundationState(organizationalUnitId);
        if (!organizationalUnit.active()) throw new ConflictException("Archived organizational unit cannot be used as target: " + organizationalUnitId);
        return organizationalUnit;
    }
}

