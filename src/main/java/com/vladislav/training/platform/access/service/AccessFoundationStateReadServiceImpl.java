package com.vladislav.training.platform.access.service;

import com.vladislav.training.platform.access.domain.AccessScopeType;
import com.vladislav.training.platform.access.domain.ManagementRelation;
import com.vladislav.training.platform.access.domain.TemporaryAccessArea;
import com.vladislav.training.platform.access.domain.TemporaryManagementDelegation;
import com.vladislav.training.platform.access.domain.TemporaryRoleAssignment;
import com.vladislav.training.platform.access.domain.UserAccessArea;
import com.vladislav.training.platform.access.repository.ManagementRelationRepository;
import com.vladislav.training.platform.access.repository.ManagementRelationTypeRepository;
import com.vladislav.training.platform.access.repository.TemporaryAccessAreaRepository;
import com.vladislav.training.platform.access.repository.TemporaryManagementDelegationRepository;
import com.vladislav.training.platform.access.repository.TemporaryRoleAssignmentRepository;
import com.vladislav.training.platform.access.repository.UserAccessAreaRepository;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.userorg.service.UserOrgFoundationStateReadService;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса {@code AccessFoundationStateReadServiceImpl}.
 */
@Service
@Transactional(readOnly = true)
class AccessFoundationStateReadServiceImpl implements AccessFoundationStateReadService {

    private final UserAccessAreaRepository userAccessAreaRepository;
    private final ManagementRelationRepository managementRelationRepository;
    private final TemporaryAccessAreaRepository temporaryAccessAreaRepository;
    private final TemporaryManagementDelegationRepository temporaryManagementDelegationRepository;
    private final TemporaryRoleAssignmentRepository temporaryRoleAssignmentRepository;
    private final ManagementRelationTypeRepository managementRelationTypeRepository;
    private final UserOrgFoundationStateReadService userOrgFoundationStateReadService;

    AccessFoundationStateReadServiceImpl(
        UserAccessAreaRepository userAccessAreaRepository,
        ManagementRelationRepository managementRelationRepository,
        TemporaryAccessAreaRepository temporaryAccessAreaRepository,
        TemporaryManagementDelegationRepository temporaryManagementDelegationRepository,
        TemporaryRoleAssignmentRepository temporaryRoleAssignmentRepository,
        ManagementRelationTypeRepository managementRelationTypeRepository,
        UserOrgFoundationStateReadService userOrgFoundationStateReadService
    ) {
        this.userAccessAreaRepository = userAccessAreaRepository;
        this.managementRelationRepository = managementRelationRepository;
        this.temporaryAccessAreaRepository = temporaryAccessAreaRepository;
        this.temporaryManagementDelegationRepository = temporaryManagementDelegationRepository;
        this.temporaryRoleAssignmentRepository = temporaryRoleAssignmentRepository;
        this.managementRelationTypeRepository = managementRelationTypeRepository;
        this.userOrgFoundationStateReadService = userOrgFoundationStateReadService;
    }

    @Override
    public OrganizationalUnitAccessUsage findOrganizationalUnitUsage(Long organizationalUnitId, Instant activeAt) {
        boolean hasActiveUserAccessArea = false;
        boolean hasActiveSubtreeUserAccessArea = false;
        for (UserAccessArea accessArea : userAccessAreaRepository.findUserAccessAreasByOrganizationalUnitId(organizationalUnitId)) {
            if (!isActiveAt(accessArea.validFrom(), accessArea.validTo(), activeAt)) {
                continue;
            }
            hasActiveUserAccessArea = true;
            if (accessArea.accessScopeType() == AccessScopeType.UNIT_SUBTREE) {
                hasActiveSubtreeUserAccessArea = true;
            }
        }

        boolean hasActiveManagementRelation = false;
        for (ManagementRelation relation : managementRelationRepository.findManagementRelationsByOrganizationalUnitId(
            organizationalUnitId
        )) {
            if (isActiveAt(relation.validFrom(), relation.validTo(), activeAt)) {
                hasActiveManagementRelation = true;
                break;
            }
        }

        boolean hasActiveTemporaryAccessArea = false;
        boolean hasActiveSubtreeTemporaryAccessArea = false;
        for (TemporaryAccessArea accessArea : temporaryAccessAreaRepository.findTemporaryAccessAreasByOrganizationalUnitId(
            organizationalUnitId
        )) {
            if (!isActiveAt(accessArea.validFrom(), accessArea.validTo(), activeAt)) {
                continue;
            }
            hasActiveTemporaryAccessArea = true;
            if (accessArea.accessScopeType() == AccessScopeType.UNIT_SUBTREE) {
                hasActiveSubtreeTemporaryAccessArea = true;
            }
        }

        boolean hasActiveTemporaryManagementDelegation = false;
        for (TemporaryManagementDelegation delegation : temporaryManagementDelegationRepository
            .findTemporaryManagementDelegationsByOrganizationalUnitId(organizationalUnitId)) {
            if (isActiveAt(delegation.validFrom(), delegation.validTo(), activeAt)) {
                hasActiveTemporaryManagementDelegation = true;
                break;
            }
        }

        return new OrganizationalUnitAccessUsage(
            hasActiveUserAccessArea,
            hasActiveSubtreeUserAccessArea,
            hasActiveManagementRelation,
            hasActiveTemporaryAccessArea,
            hasActiveSubtreeTemporaryAccessArea,
            hasActiveTemporaryManagementDelegation
        );
    }

    @Override
    public ManagerialScopeFoundationState findActorManagerialScope(Long actorUserId, Instant activeAt) {
        Set<Long> unitAnchorIds = new LinkedHashSet<>();
        for (ManagementRelation relation : managementRelationRepository.findActiveManagementRelationsByUserId(
            actorUserId,
            activeAt
        )) {
            unitAnchorIds.add(relation.organizationalUnitId());
        }
        for (TemporaryManagementDelegation delegation : temporaryManagementDelegationRepository
            .findActiveTemporaryManagementDelegationsByUserId(actorUserId, activeAt)) {
            unitAnchorIds.add(delegation.organizationalUnitId());
        }

        Set<String> subtreePaths = new LinkedHashSet<>();
        for (Long unitAnchorId : unitAnchorIds) {
            UserOrgFoundationStateReadService.OrganizationalUnitFoundationState unit =
                userOrgFoundationStateReadService.findOrganizationalUnitFoundationState(unitAnchorId);
            if (unit.participatesInSubtreeScope() && unit.path() != null && !unit.path().isBlank()) {
                subtreePaths.add(unit.path());
            }
        }

        return new ManagerialScopeFoundationState(
            Set.copyOf(unitAnchorIds),
            Set.copyOf(subtreePaths)
        );
    }


    @Override
    public Set<Long> findActiveTemporaryRoleIds(Long userId, Instant activeAt) {
        Set<Long> roleIds = new LinkedHashSet<>();
        for (TemporaryRoleAssignment assignment : temporaryRoleAssignmentRepository.findTemporaryRoleAssignmentsByUserId(userId)) {
            if (isActiveAt(assignment.validFrom(), assignment.validTo(), activeAt)) {
                roleIds.add(assignment.roleId());
            }
        }
        return Set.copyOf(roleIds);
    }

    @Override
    public boolean managementRelationTypeExists(Long managementRelationTypeId) {
        try {
            managementRelationTypeRepository.findManagementRelationTypeById(managementRelationTypeId);
            return true;
        } catch (NotFoundException exception) {
            return false;
        }
    }

    private boolean isActiveAt(Instant validFrom, Instant validTo, Instant activeAt) {
        return !validFrom.isAfter(activeAt) && (validTo == null || validTo.isAfter(activeAt));
    }
}
