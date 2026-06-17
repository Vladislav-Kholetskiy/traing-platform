package com.vladislav.training.platform.application.policy;

import com.vladislav.training.platform.assignment.service.AssignmentAdministrativeAdmissionFoundationStateReadService;
import com.vladislav.training.platform.assignment.service.AssignmentAssignedExecutionAdmissionFoundationStateReadService;
import com.vladislav.training.platform.assignment.service.AssignmentCampaignLaunchFoundationStateReadService;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.testing.admission.SelfExecutionAdmissionFoundationStateReadService;
import java.time.Instant;

/**
 * Класс {@code CapabilityAdmissionFoundationFacts}.
 */
final class CapabilityAdmissionFoundationFacts {

    private final CapabilityAdmissionRequest request;
    private final AssignmentCampaignLaunchFoundationStateReadService assignmentCampaignLaunchFoundationStateReadService;
    private final AssignmentAdministrativeAdmissionFoundationStateReadService assignmentAdministrativeAdmissionFoundationStateReadService;
    private final AssignmentAssignedExecutionAdmissionFoundationStateReadService
        assignmentAssignedExecutionAdmissionFoundationStateReadService;
    private final SelfExecutionAdmissionFoundationStateReadService selfExecutionAdmissionFoundationStateReadService;

    CapabilityAdmissionFoundationFacts(CapabilityAdmissionRequest request) {
        this(request, null, null, null, null);
    }

    CapabilityAdmissionFoundationFacts(
        CapabilityAdmissionRequest request,
        AssignmentCampaignLaunchFoundationStateReadService assignmentCampaignLaunchFoundationStateReadService,
        AssignmentAdministrativeAdmissionFoundationStateReadService assignmentAdministrativeAdmissionFoundationStateReadService,
        AssignmentAssignedExecutionAdmissionFoundationStateReadService assignmentAssignedExecutionAdmissionFoundationStateReadService,
        SelfExecutionAdmissionFoundationStateReadService selfExecutionAdmissionFoundationStateReadService
    ) {
        this.request = request;
        this.assignmentCampaignLaunchFoundationStateReadService = assignmentCampaignLaunchFoundationStateReadService;
        this.assignmentAdministrativeAdmissionFoundationStateReadService = assignmentAdministrativeAdmissionFoundationStateReadService;
        this.assignmentAssignedExecutionAdmissionFoundationStateReadService =
            assignmentAssignedExecutionAdmissionFoundationStateReadService;
        this.selfExecutionAdmissionFoundationStateReadService = selfExecutionAdmissionFoundationStateReadService;
    }

    Long targetEntityId() {
        Long targetEntityId = request.targetEntityId();
        if (targetEntityId == null) {
            throw new PolicyViolationException(
                CapabilityViolationCode.FOUNDATION_FACTS_UNAVAILABLE.name(),
                "Capability admission cannot resolve required target foundation fact for operation " + request.operationCode()
            );
        }
        return targetEntityId;
    }

    CapabilityTargetEntityType targetEntityType() {
        return request.targetEntityType();
    }

    CapabilityAdmissionPayload.RoleAssignment roleAssignment() {
        return payload(CapabilityAdmissionPayload.RoleAssignment.class);
    }

    CapabilityAdmissionPayload.OrganizationAssignment organizationAssignment() {
        return payload(CapabilityAdmissionPayload.OrganizationAssignment.class);
    }

    CapabilityAdmissionPayload.AccessArea accessArea() {
        return payload(CapabilityAdmissionPayload.AccessArea.class);
    }

    CapabilityAdmissionPayload.ManagementRelation managementRelation() {
        return payload(CapabilityAdmissionPayload.ManagementRelation.class);
    }

    CapabilityAdmissionPayload.OrganizationalUnitMutation organizationalUnitMutation() {
        return payload(CapabilityAdmissionPayload.OrganizationalUnitMutation.class);
    }

    CapabilityAdmissionPayload.ContentMutation contentMutation() {
        return payload(CapabilityAdmissionPayload.ContentMutation.class);
    }

    CapabilityAdmissionPayload.TopicFinalControlMutation topicFinalControlMutation() {
        return payload(CapabilityAdmissionPayload.TopicFinalControlMutation.class);
    }

    CapabilityAdmissionPayload.AssignmentCampaignLaunch assignmentCampaignLaunch() {
        return payload(CapabilityAdmissionPayload.AssignmentCampaignLaunch.class);
    }

    CapabilityAdmissionPayload.AssignmentCancel assignmentCancel() {
        return payload(CapabilityAdmissionPayload.AssignmentCancel.class);
    }

    CapabilityAdmissionPayload.AssignmentDeadlineExtend assignmentDeadlineExtend() {
        return payload(CapabilityAdmissionPayload.AssignmentDeadlineExtend.class);
    }

    CapabilityAdmissionPayload.AssignmentReplaceWithNew assignmentReplaceWithNew() {
        return payload(CapabilityAdmissionPayload.AssignmentReplaceWithNew.class);
    }

    CapabilityAdmissionPayload.AssignedExecution assignedExecution() {
        return payload(CapabilityAdmissionPayload.AssignedExecution.class);
    }

    CapabilityAdmissionPayload.SelfExecution selfExecution() {
        return payload(CapabilityAdmissionPayload.SelfExecution.class);
    }

    Long requiredUserId(CapabilityAdmissionPayload.RoleAssignment payload) {
        return requiredFoundationFact(payload.userId(), "userId");
    }

    Long requiredRoleId(CapabilityAdmissionPayload.RoleAssignment payload) {
        return requiredFoundationFact(payload.roleId(), "roleId");
    }

    Long requiredUserId(CapabilityAdmissionPayload.OrganizationAssignment payload) {
        return requiredFoundationFact(payload.userId(), "userId");
    }

    Long requiredOrganizationalUnitId(CapabilityAdmissionPayload.OrganizationAssignment payload) {
        return requiredFoundationFact(payload.organizationalUnitId(), "organizationalUnitId");
    }

    Long requiredUserId(CapabilityAdmissionPayload.AccessArea payload) {
        return requiredFoundationFact(payload.userId(), "userId");
    }

    Long optionalOrganizationalUnitId(CapabilityAdmissionPayload.AccessArea payload) {
        return payload.organizationalUnitId();
    }

    Long requiredUserId(CapabilityAdmissionPayload.ManagementRelation payload) {
        return requiredFoundationFact(payload.userId(), "userId");
    }

    Long requiredOrganizationalUnitId(CapabilityAdmissionPayload.ManagementRelation payload) {
        return requiredFoundationFact(payload.organizationalUnitId(), "organizationalUnitId");
    }

    Long requiredManagementRelationTypeId(CapabilityAdmissionPayload.ManagementRelation payload) {
        return requiredFoundationFact(payload.managementRelationTypeId(), "managementRelationTypeId");
    }

    Long optionalNewParentUnitId(CapabilityAdmissionPayload.OrganizationalUnitMutation payload) {
        return payload.newParentUnitId();
    }

    Long requiredParentEntityId(CapabilityAdmissionPayload.ContentMutation payload) {
        return requiredFoundationFact(payload.parentEntityId(), "parentEntityId");
    }

    CapabilityTargetEntityType requiredParentEntityType(CapabilityAdmissionPayload.ContentMutation payload) {
        if (payload.parentEntityType() == null) {
            throw new PolicyViolationException(
                CapabilityViolationCode.REQUEST_PAYLOAD_INVALID.name(),
                "parentEntityType must not be null for content mutation"
            );
        }
        return payload.parentEntityType();
    }

    Long requiredTopicId(CapabilityAdmissionPayload.TopicFinalControlMutation payload) {
        return requiredFoundationFact(payload.topicId(), "topicId");
    }

    Long requiredTestId(CapabilityAdmissionPayload.TopicFinalControlMutation payload) {
        return requiredFoundationFact(payload.testId(), "testId");
    }

    Long optionalTestId(CapabilityAdmissionPayload.TopicFinalControlMutation payload) {
        return payload.testId();
    }

    AssignmentCampaignLaunchFoundationStateReadService.AssignmentCampaignLaunchAdmissionAnchor assignmentCampaignLaunchAdmissionAnchor() {
        CapabilityAdmissionPayload.AssignmentCampaignLaunch payload = assignmentCampaignLaunch();
        return new AssignmentCampaignLaunchFoundationStateReadService.AssignmentCampaignLaunchAdmissionAnchor(
            requiredTextFact(payload.sourceType(), "sourceType"),
            payload.sourceRef()
        );
    }

    AssignmentCampaignLaunchFoundationStateReadService.AssignmentCampaignLaunchFoundationState
    resolveAssignmentCampaignLaunchFoundationState() {
        AssignmentCampaignLaunchFoundationStateReadService readService = requireAssignmentCampaignLaunchFoundationStateReadService();
        var foundationState = readService.findAssignmentCampaignLaunchFoundationState(assignmentCampaignLaunchAdmissionAnchor());
        if (foundationState == null) {
            throw new PolicyViolationException(
                CapabilityViolationCode.FOUNDATION_FACTS_UNAVAILABLE.name(),
                "Assignment campaign launch foundation facts are unavailable"
            );
        }
        return foundationState;
    }

    Long requiredAssignmentId() {
        return requiredFoundationFact(request.targetEntityId(), "assignmentId");
    }

    Long requiredAssignmentId(CapabilityAdmissionPayload.AssignedExecution payload) {
        return requiredFoundationFact(payload.assignmentId(), "assignmentId");
    }

    Long requiredAssignmentTestId() {
        return requiredFoundationFact(request.targetEntityId(), "assignmentTestId");
    }

    Long requiredSelfTestId() {
        return requiredFoundationFact(request.targetEntityId(), "testId");
    }

    AssignmentAssignedExecutionAdmissionFoundationStateReadService.AssignmentAssignedExecutionAdmissionFoundationState
    resolveAssignmentAssignedExecutionFoundationState() {
        AssignmentAssignedExecutionAdmissionFoundationStateReadService readService =
            requireAssignmentAssignedExecutionAdmissionFoundationStateReadService();
        CapabilityAdmissionPayload.AssignedExecution payload = assignedExecution();
        var foundationState = readService.findAssignmentAssignedExecutionAdmissionFoundationState(
            request.actorUserId(),
            requiredAssignmentId(payload),
            requiredAssignmentTestId()
        );
        if (foundationState == null
            || foundationState.assignmentId() == null
            || foundationState.assignmentTestId() == null
            || !foundationState.assignmentId().equals(requiredAssignmentId(payload))
            || !foundationState.assignmentTestId().equals(requiredAssignmentTestId())) {
            throw new PolicyViolationException(
                CapabilityViolationCode.FOUNDATION_FACTS_UNAVAILABLE.name(),
                "Assigned execution foundation facts are unavailable"
            );
        }
        return foundationState;
    }

    SelfExecutionAdmissionFoundationStateReadService.SelfExecutionAdmissionFoundationState
    resolveSelfExecutionFoundationState() {
        SelfExecutionAdmissionFoundationStateReadService readService = requireSelfExecutionAdmissionFoundationStateReadService();
        selfExecution();
        var foundationState = readService.findSelfExecutionAdmissionFoundationState(
            request.actorUserId(),
            requiredSelfTestId()
        );
        if (foundationState == null
            || foundationState.testId() == null
            || !foundationState.testId().equals(requiredSelfTestId())) {
            throw new PolicyViolationException(
                CapabilityViolationCode.FOUNDATION_FACTS_UNAVAILABLE.name(),
                "Self execution foundation facts are unavailable"
            );
        }
        return foundationState;
    }

    AssignmentAdministrativeAdmissionFoundationStateReadService.AssignmentAdministrativeAdmissionFoundationState
    resolveAssignmentAdministrativeFoundationState(Long assignmentId) {
        AssignmentAdministrativeAdmissionFoundationStateReadService readService =
            requireAssignmentAdministrativeAdmissionFoundationStateReadService();
        var foundationState = readService.findAssignmentAdministrativeAdmissionFoundationState(assignmentId);
        if (foundationState == null || foundationState.assignmentId() == null || !foundationState.assignmentId().equals(assignmentId)) {
            throw new PolicyViolationException(
                CapabilityViolationCode.FOUNDATION_FACTS_UNAVAILABLE.name(),
                "Assignment administrative foundation facts are unavailable"
            );
        }
        return foundationState;
    }

    Instant requiredNewDeadlineAt(CapabilityAdmissionPayload.AssignmentDeadlineExtend payload) {
        if (payload.newDeadlineAt() == null) {
            throw new PolicyViolationException(
                CapabilityViolationCode.REQUEST_PAYLOAD_INVALID.name(),
                "newDeadlineAt must not be null for assignment deadline extension"
            );
        }
        return payload.newDeadlineAt();
    }

    Long requiredCampaignId(CapabilityAdmissionPayload.AssignmentReplaceWithNew payload) {
        return requiredFoundationFact(payload.campaignId(), "campaignId");
    }

    Long requiredCurrentAssignmentCampaignId(
        AssignmentAdministrativeAdmissionFoundationStateReadService.AssignmentAdministrativeAdmissionFoundationState foundationState
    ) {
        if (foundationState == null || foundationState.campaignId() == null) {
            throw new PolicyViolationException(
                CapabilityViolationCode.FOUNDATION_FACTS_UNAVAILABLE.name(),
                "Assignment administrative foundation facts are missing campaignId anchor"
            );
        }
        return foundationState.campaignId();
    }

    void requireAssignmentReplacementCampaignCompatibility(
        CapabilityAdmissionPayload.AssignmentReplaceWithNew payload,
        AssignmentAdministrativeAdmissionFoundationStateReadService.AssignmentAdministrativeAdmissionFoundationState foundationState
    ) {
        Long payloadCampaignId = requiredCampaignId(payload);
        Long currentCampaignId = requiredCurrentAssignmentCampaignId(foundationState);
        if (!currentCampaignId.equals(payloadCampaignId)) {
            throw new PolicyViolationException(
                CapabilityViolationCode.REQUEST_PAYLOAD_INVALID.name(),
                "Assignment replacement campaignId must match current assignment campaignId"
            );
        }
    }

    private Long requiredFoundationFact(Long foundationFactId, String fieldName) {
        if (foundationFactId == null) {
            throw new PolicyViolationException(
                CapabilityViolationCode.FOUNDATION_FACTS_UNAVAILABLE.name(),
                "Capability admission cannot resolve required foundation fact: " + fieldName
            );
        }
        return foundationFactId;
    }

    private String requiredTextFact(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new PolicyViolationException(
                CapabilityViolationCode.FOUNDATION_FACTS_UNAVAILABLE.name(),
                "Capability admission cannot resolve required foundation fact: " + fieldName
            );
        }
        return value;
    }

    private <T extends CapabilityAdmissionPayload> T payload(Class<T> payloadType) {
        CapabilityAdmissionPayload payload = request.payloadContext();
        if (!payloadType.isInstance(payload)) {
            throw new PolicyViolationException(
                CapabilityViolationCode.REQUEST_PAYLOAD_INVALID.name(),
                "Capability admission requires payload type " + payloadType.getSimpleName()
                    + " for operation " + request.operationCode()
            );
        }
        return payloadType.cast(payload);
    }

    private AssignmentCampaignLaunchFoundationStateReadService requireAssignmentCampaignLaunchFoundationStateReadService() {
        if (assignmentCampaignLaunchFoundationStateReadService == null) {
            throw new PolicyViolationException(
                CapabilityViolationCode.FOUNDATION_FACTS_UNAVAILABLE.name(),
                "Assignment campaign launch foundation-state reader is not available"
            );
        }
        return assignmentCampaignLaunchFoundationStateReadService;
    }

    private AssignmentAdministrativeAdmissionFoundationStateReadService
    requireAssignmentAdministrativeAdmissionFoundationStateReadService() {
        if (assignmentAdministrativeAdmissionFoundationStateReadService == null) {
            throw new PolicyViolationException(
                CapabilityViolationCode.FOUNDATION_FACTS_UNAVAILABLE.name(),
                "Assignment administrative foundation-state reader is not available"
            );
        }
        return assignmentAdministrativeAdmissionFoundationStateReadService;
    }

    private AssignmentAssignedExecutionAdmissionFoundationStateReadService
    requireAssignmentAssignedExecutionAdmissionFoundationStateReadService() {
        if (assignmentAssignedExecutionAdmissionFoundationStateReadService == null) {
            throw new PolicyViolationException(
                CapabilityViolationCode.FOUNDATION_FACTS_UNAVAILABLE.name(),
                "Assignment assigned-execution foundation-state reader is not available"
            );
        }
        return assignmentAssignedExecutionAdmissionFoundationStateReadService;
    }

    private SelfExecutionAdmissionFoundationStateReadService requireSelfExecutionAdmissionFoundationStateReadService() {
        if (selfExecutionAdmissionFoundationStateReadService == null) {
            throw new PolicyViolationException(
                CapabilityViolationCode.FOUNDATION_FACTS_UNAVAILABLE.name(),
                "Self-execution foundation-state reader is not available"
            );
        }
        return selfExecutionAdmissionFoundationStateReadService;
    }
}
