package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContext;
import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadType;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.service.OrganizationQueryService;
import com.vladislav.training.platform.userorg.service.OrganizationalTargetingQueryService;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@ConditionalOnBean({
    OrganizationQueryService.class,
    OrganizationalTargetingQueryService.class,
    MandatoryAssignmentRecipientEligibilityService.class
})
class AssignmentCampaignPreviewServiceImpl implements AssignmentCampaignPreviewService {

    private static final String NOT_AUTHORIZED = "ACTOR_NOT_AUTHORIZED";
    private static final String PREVIEW_TARGET_FAMILY = "assignment_campaign_preview";

    private final OrganizationQueryService organizationQueryService;
    private final OrganizationalTargetingQueryService organizationalTargetingQueryService;
    private final MandatoryAssignmentRecipientEligibilityService mandatoryRecipientEligibilitySeam;
    private final AccessSpecificationPolicy accessSpecificationPolicy;
    private final AccessPolicyQueryContextResolver contextResolver;

    AssignmentCampaignPreviewServiceImpl(
        OrganizationQueryService organizationQueryService,
        OrganizationalTargetingQueryService organizationalTargetingQueryService,
        MandatoryAssignmentRecipientEligibilityService mandatoryRecipientEligibilitySeam,
        AccessSpecificationPolicy accessSpecificationPolicy,
        AccessPolicyQueryContextResolver contextResolver
    ) {
        this.organizationQueryService = organizationQueryService;
        this.organizationalTargetingQueryService = organizationalTargetingQueryService;
        this.mandatoryRecipientEligibilitySeam = mandatoryRecipientEligibilitySeam;
        this.accessSpecificationPolicy = accessSpecificationPolicy;
        this.contextResolver = contextResolver;
    }

    @Override
    public AssignmentCampaignRecipientPoolPreview previewRecipientPool(RecipientPoolPreviewRequest request) {
        AccessPolicyQueryContext previewContext = contextResolver.resolve(
            AccessReadArea.ASSIGNMENT_CAMPAIGN,
            AccessReadType.DETAIL,
            PREVIEW_TARGET_FAMILY
        );
        ensurePreviewReadAllowed(previewContext);
        AssignmentCampaignTargetingBasis supportedBasis =
            AssignmentCampaignTargetingSupport.requireSupportedPreviewBasis(request.sourceType());

        OrganizationalUnit targetUnit = resolveTargetOrganizationalUnit(request.sourceRef());
        Instant effectiveAt = previewContext.effectiveAt();
        if (targetUnit.status() != OrganizationalUnitStatus.ACTIVE) {
            return buildPreview(supportedBasis, request, targetUnit, false, effectiveAt, List.of());
        }

        Set<Long> candidateUserIds = organizationalTargetingQueryService.resolveCurrentCandidateUserIdsForUnitSubtree(
            targetUnit.path(),
            effectiveAt
        );
        if (candidateUserIds.isEmpty()) {
            return buildPreview(supportedBasis, request, targetUnit, true, effectiveAt, List.of());
        }

        List<PreviewRecipient> recipients = candidateUserIds.stream()
            .sorted()
            .map(candidateUserId -> mandatoryRecipientEligibilitySeam.evaluateRecipient(
                candidateUserId,
                targetUnit.path(),
                effectiveAt
            ))
            .filter(MandatoryAssignmentRecipientEligibilityService.MandatoryRecipientEligibility::eligible)
            .map(eligibility -> new PreviewRecipient(
                eligibility.userId(),
                eligibility.employeeNumber(),
                eligibility.lastName(),
                eligibility.firstName(),
                eligibility.middleName()
            ))
            .sorted(Comparator.comparing(PreviewRecipient::userId))
            .toList();

        return buildPreview(supportedBasis, request, targetUnit, true, effectiveAt, recipients);
    }

    private void ensurePreviewReadAllowed(AccessPolicyQueryContext previewContext) {
        if (!accessSpecificationPolicy.canRead(previewContext)) {
            throw new PolicyViolationException(
                NOT_AUTHORIZED,
                "Actor is not authorized to preview assignment campaign recipient pool"
            );
        }
    }

    private OrganizationalUnit resolveTargetOrganizationalUnit(String sourceRef) {
        try {
            return organizationQueryService.findOrganizationalUnitById(Long.valueOf(sourceRef));
        } catch (NumberFormatException ignored) {
            return organizationQueryService.findOrganizationalUnitByPath(sourceRef);
        }
    }

    private AssignmentCampaignRecipientPoolPreview buildPreview(
        AssignmentCampaignTargetingBasis supportedBasis,
        RecipientPoolPreviewRequest request,
        OrganizationalUnit targetUnit,
        boolean targetingBasisActive,
        Instant previewedAt,
        List<PreviewRecipient> recipients
    ) {
        return new AssignmentCampaignRecipientPoolPreview(
            supportedBasis.value(),
            request.sourceRef(),
            targetUnit.id(),
            targetUnit.path(),
            targetUnit.name(),
            targetingBasisActive,
            previewedAt,
            recipients
        );
    }

}

