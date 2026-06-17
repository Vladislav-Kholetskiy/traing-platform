package com.vladislav.training.platform.userorg.service;

import com.vladislav.training.platform.access.service.AccessFoundationStateReadService;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment;
import com.vladislav.training.platform.userorg.repository.UserOrganizationAssignmentRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Вспомогательный тип {@code OrganizationalUnitStructuralMutationValidationSupport}.
 */
@Service
@Transactional(readOnly = true)
class OrganizationalUnitStructuralMutationValidationSupport {

    private final UserOrganizationAssignmentRepository userOrganizationAssignmentRepository;
    private final AccessFoundationStateReadService accessFoundationStateReadService;
    private final UserOperatorStateSupport UserOperatorStateSupport;

    OrganizationalUnitStructuralMutationValidationSupport(
        UserOrganizationAssignmentRepository userOrganizationAssignmentRepository,
        AccessFoundationStateReadService accessFoundationStateReadService,
        UserOperatorStateSupport UserOperatorStateSupport
    ) {
        this.userOrganizationAssignmentRepository = userOrganizationAssignmentRepository;
        this.accessFoundationStateReadService = accessFoundationStateReadService;
        this.UserOperatorStateSupport = UserOperatorStateSupport;
    }

    void ensureMoveAllowed(List<OrganizationalUnit> currentSubtree, OrganizationalUnit targetParent) {
        if (targetParent == null) {
            return;
        }

        boolean subtreeHasActiveUnits = currentSubtree.stream()
            .anyMatch(unit -> unit.status() == OrganizationalUnitStatus.ACTIVE);
        if (subtreeHasActiveUnits && targetParent.status() != OrganizationalUnitStatus.ACTIVE) {
            throw new ConflictException(
                "Archived organizational unit cannot be parent for an ACTIVE node: " + targetParent.id()
            );
        }
    }

    void ensureArchiveAllowed(List<OrganizationalUnit> currentSubtree, Instant activeAt) {
        for (OrganizationalUnit organizationalUnit : currentSubtree) {
            ensureNoActiveOrganizationAssignments(organizationalUnit.id(), activeAt);
            ensureNoActiveAccessManagementUsage(organizationalUnit.id(), activeAt);
        }
    }

    private void ensureNoActiveOrganizationAssignments(Long organizationalUnitId, Instant activeAt) {
        List<UserOrganizationAssignment> activeAssignments = userOrganizationAssignmentRepository
            .findOrganizationAssignmentsByUnitId(organizationalUnitId)
            .stream()
            .filter(assignment -> isActiveAt(assignment.validFrom(), assignment.validTo(), activeAt))
            .toList();
        if (activeAssignments.isEmpty()) {
            return;
        }

        UserOrganizationAssignment operatorPrimaryAssignment = activeAssignments.stream()
            .filter(assignment -> assignment.assignmentType() == OrganizationAssignmentType.PRIMARY)
            .filter(assignment -> UserOperatorStateSupport.isUserInOperatorContour(assignment.userId(), activeAt))
            .findFirst()
            .orElse(null);
        if (operatorPrimaryAssignment != null) {
            throw new ConflictException(
                "Cannot archive organizational unit subtree because unit is active PRIMARY home unit for operator-contour user: "
                    + organizationalUnitId
            );
        }

        throw new ConflictException(
            "Cannot archive organizational unit subtree because active user_organization_assignment exists: unitId="
                + organizationalUnitId
        );
    }

    private void ensureNoActiveAccessManagementUsage(Long organizationalUnitId, Instant activeAt) {
        AccessFoundationStateReadService.OrganizationalUnitAccessUsage usage = accessFoundationStateReadService
            .findOrganizationalUnitUsage(organizationalUnitId, activeAt);

        if (usage.hasActiveUserAccessArea()) {
            throw new ConflictException(
                "Cannot archive organizational unit subtree because active user_access_area exists: unitId="
                    + organizationalUnitId
            );
        }
        if (usage.hasActiveManagementRelation()) {
            throw new ConflictException(
                "Cannot archive organizational unit subtree because active management_relation exists: unitId="
                    + organizationalUnitId
            );
        }
        if (usage.hasActiveTemporaryAccessArea()) {
            throw new ConflictException(
                "Cannot archive organizational unit subtree because active temporary_access_area exists: unitId="
                    + organizationalUnitId
            );
        }
        if (usage.hasActiveTemporaryManagementDelegation()) {
            throw new ConflictException(
                "Cannot archive organizational unit subtree because active temporary_management_delegation exists: unitId="
                    + organizationalUnitId
            );
        }
    }

    private boolean isActiveAt(Instant validFrom, Instant validTo, Instant activeAt) {
        return !validFrom.isAfter(activeAt) && (validTo == null || validTo.isAfter(activeAt));
    }
}

