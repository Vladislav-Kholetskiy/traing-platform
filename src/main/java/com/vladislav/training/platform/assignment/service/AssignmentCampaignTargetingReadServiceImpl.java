package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadType;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitType;
import com.vladislav.training.platform.userorg.service.OrganizationQueryService;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса {@code AssignmentCampaignTargetingReadServiceImpl}.
 */
@Service
@Transactional(readOnly = true)
class AssignmentCampaignTargetingReadServiceImpl implements AssignmentCampaignTargetingReadService {

    private static final String NOT_AUTHORIZED = "ACTOR_NOT_AUTHORIZED";
    private static final String TARGET_FAMILY = "assignment_campaign_preview";

    private final OrganizationQueryService organizationQueryService;
    private final AccessSpecificationPolicy accessSpecificationPolicy;
    private final AccessPolicyQueryContextResolver contextResolver;

    AssignmentCampaignTargetingReadServiceImpl(
        OrganizationQueryService organizationQueryService,
        AccessSpecificationPolicy accessSpecificationPolicy,
        AccessPolicyQueryContextResolver contextResolver
    ) {
        this.organizationQueryService = organizationQueryService;
        this.accessSpecificationPolicy = accessSpecificationPolicy;
        this.contextResolver = contextResolver;
    }

    @Override
    public List<TargetUnit> findAvailableTargetUnits() {
        ensureReadAllowed();

        List<OrganizationalUnit> activeUnits = organizationQueryService.findUnitsByStatus(OrganizationalUnitStatus.ACTIVE);
        Map<Long, OrganizationalUnitType> typesById = activeUnits.stream()
            .map(OrganizationalUnit::organizationalUnitTypeId)
            .distinct()
            .collect(Collectors.toMap(Function.identity(), organizationQueryService::findOrganizationalUnitTypeById));

        return activeUnits.stream()
            .filter(unit -> {
                OrganizationalUnitType unitType = typesById.get(unit.organizationalUnitTypeId());
                return unitType != null && unitType.canBeCampaignTarget();
            })
            .map(unit -> new TargetUnit(unit.id(), unit.name(), unit.path()))
            .sorted((left, right) -> {
                String leftPath = left.path() == null ? "" : left.path();
                String rightPath = right.path() == null ? "" : right.path();
                return leftPath.compareToIgnoreCase(rightPath);
            })
            .toList();
    }

    private void ensureReadAllowed() {
        if (!accessSpecificationPolicy.canRead(
            contextResolver.resolve(AccessReadArea.ASSIGNMENT_CAMPAIGN, AccessReadType.LIST, TARGET_FAMILY)
        )) {
            throw new PolicyViolationException(
                NOT_AUTHORIZED,
                "Actor is not authorized to read assignment campaign target units"
            );
        }
    }
}
