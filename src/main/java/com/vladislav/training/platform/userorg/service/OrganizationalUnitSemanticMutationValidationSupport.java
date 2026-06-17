package com.vladislav.training.platform.userorg.service;

import com.vladislav.training.platform.access.service.AccessFoundationStateReadService;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.domain.OrganizationalNodeKind;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitType;
import com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment;
import com.vladislav.training.platform.userorg.repository.OrganizationalUnitRepository;
import com.vladislav.training.platform.userorg.repository.UserOrganizationAssignmentRepository;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Вспомогательный тип {@code OrganizationalUnitSemanticMutationValidationSupport}.
 */
@Service
@Transactional(readOnly = true)
class OrganizationalUnitSemanticMutationValidationSupport {

    private final OrganizationalUnitRepository organizationalUnitRepository;
    private final UserOrganizationAssignmentRepository userOrganizationAssignmentRepository;
    private final AccessFoundationStateReadService accessFoundationStateReadService;
    private final UserOperatorStateSupport UserOperatorStateSupport;

    OrganizationalUnitSemanticMutationValidationSupport(
        OrganizationalUnitRepository organizationalUnitRepository,
        UserOrganizationAssignmentRepository userOrganizationAssignmentRepository,
        AccessFoundationStateReadService accessFoundationStateReadService,
        UserOperatorStateSupport UserOperatorStateSupport
    ) {
        this.organizationalUnitRepository = organizationalUnitRepository;
        this.userOrganizationAssignmentRepository = userOrganizationAssignmentRepository;
        this.accessFoundationStateReadService = accessFoundationStateReadService;
        this.UserOperatorStateSupport = UserOperatorStateSupport;
    }

    void ensureUnitTypeMutationAllowed(
        OrganizationalUnitType currentUnitType,
        OrganizationalUnitType candidateUnitType,
        Instant activeAt
    ) {
        if (!hasSemanticMutation(currentUnitType, candidateUnitType)) {
            return;
        }

        organizationalUnitRepository.findUnitsByStatus(OrganizationalUnitStatus.ACTIVE).stream()
            .filter(unit -> Objects.equals(unit.organizationalUnitTypeId(), currentUnitType.id()))
            .forEach(unit -> ensureResultingUnitSemanticsAllowed(unit, candidateUnitType, activeAt));
    }

    void ensureUnitTypeReassignmentAllowed(
        OrganizationalUnit currentUnit,
        OrganizationalUnitType candidateUnitType,
        Instant activeAt
    ) {
        if (Objects.equals(currentUnit.organizationalUnitTypeId(), candidateUnitType.id())) {
            return;
        }

        ensureResultingUnitSemanticsAllowed(currentUnit, candidateUnitType, activeAt);
    }

    private void ensureResultingUnitSemanticsAllowed(
        OrganizationalUnit organizationalUnit,
        OrganizationalUnitType resultingUnitType,
        Instant activeAt
    ) {
        Long organizationalUnitId = organizationalUnit.id();
        AccessFoundationStateReadService.OrganizationalUnitAccessUsage usage = accessFoundationStateReadService
            .findOrganizationalUnitUsage(organizationalUnitId, activeAt);

        ensureAccessAreaTargetsStillAllowed(organizationalUnitId, resultingUnitType, usage);
        ensureManagementTargetsStillAllowed(organizationalUnitId, resultingUnitType, usage);
        ensureOperatorContourAssignmentsStillAllowed(organizationalUnitId, resultingUnitType, activeAt);
    }

    private void ensureAccessAreaTargetsStillAllowed(
        Long organizationalUnitId,
        OrganizationalUnitType resultingUnitType,
        AccessFoundationStateReadService.OrganizationalUnitAccessUsage usage
    ) {
        if (!resultingUnitType.canHaveAccessArea()) {
            if (usage.hasActiveUserAccessArea()) {
                throw new ConflictException(
                    "Resulting organizational unit type would invalidate active user_access_area target: "
                        + organizationalUnitId
                );
            }
            if (usage.hasActiveTemporaryAccessArea()) {
                throw new ConflictException(
                    "Resulting organizational unit type would invalidate active temporary_access_area target: "
                        + organizationalUnitId
                );
            }
            return;
        }

        if (resultingUnitType.nodeKind() == OrganizationalNodeKind.FUNCTIONAL
            && !resultingUnitType.participatesInSubtreeScope()) {
            if (usage.hasActiveSubtreeUserAccessArea()) {
                throw new ConflictException(
                    "Resulting organizational unit type would invalidate active user_access_area UNIT_SUBTREE target "
                        + "for FUNCTIONAL unit without participatesInSubtreeScope=true: "
                        + organizationalUnitId
                );
            }
            if (usage.hasActiveSubtreeTemporaryAccessArea()) {
                throw new ConflictException(
                    "Resulting organizational unit type would invalidate active temporary_access_area UNIT_SUBTREE target "
                        + "for FUNCTIONAL unit without participatesInSubtreeScope=true: "
                        + organizationalUnitId
                );
            }
        }
    }

    private void ensureManagementTargetsStillAllowed(
        Long organizationalUnitId,
        OrganizationalUnitType resultingUnitType,
        AccessFoundationStateReadService.OrganizationalUnitAccessUsage usage
    ) {
        if (resultingUnitType.canHaveManagementRelation()) {
            return;
        }
        if (usage.hasActiveManagementRelation()) {
            throw new ConflictException(
                "Resulting organizational unit type would invalidate active management_relation target: "
                    + organizationalUnitId
            );
        }
        if (usage.hasActiveTemporaryManagementDelegation()) {
            throw new ConflictException(
                "Resulting organizational unit type would invalidate active temporary_management_delegation target: "
                    + organizationalUnitId
            );
        }
    }

    private void ensureOperatorContourAssignmentsStillAllowed(
        Long organizationalUnitId,
        OrganizationalUnitType resultingUnitType,
        Instant activeAt
    ) {
        userOrganizationAssignmentRepository.findOrganizationAssignmentsByUnitId(organizationalUnitId).stream()
            .filter(assignment -> assignment.assignmentType() == OrganizationAssignmentType.PRIMARY)
            .filter(assignment -> isActiveAt(assignment.validFrom(), assignment.validTo(), activeAt))
            .map(UserOrganizationAssignment::userId)
            .distinct()
            .forEach(userId -> UserOperatorStateSupport.ensureCurrentPrimaryHomeUnitTypeAllowed(
                userId,
                activeAt,
                organizationalUnitId,
                resultingUnitType
            ));
    }

    private boolean hasSemanticMutation(
        OrganizationalUnitType currentUnitType,
        OrganizationalUnitType candidateUnitType
    ) {
        return currentUnitType.nodeKind() != candidateUnitType.nodeKind()
            || currentUnitType.canBeOperatorHomeUnit() != candidateUnitType.canBeOperatorHomeUnit()
            || currentUnitType.canBeCampaignTarget() != candidateUnitType.canBeCampaignTarget()
            || currentUnitType.participatesInSubtreeScope() != candidateUnitType.participatesInSubtreeScope()
            || currentUnitType.canHaveManagementRelation() != candidateUnitType.canHaveManagementRelation()
            || currentUnitType.canHaveAccessArea() != candidateUnitType.canHaveAccessArea();
    }

    private boolean isActiveAt(Instant validFrom, Instant validTo, Instant activeAt) {
        return !validFrom.isAfter(activeAt) && (validTo == null || validTo.isAfter(activeAt));
    }
}

