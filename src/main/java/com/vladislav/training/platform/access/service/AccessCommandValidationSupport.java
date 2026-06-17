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
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.ValidationException;
import com.vladislav.training.platform.userorg.service.UserOperatorStateValidationService;
import com.vladislav.training.platform.userorg.service.UserOrgFoundationStateReadService;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Вспомогательный тип {@code AccessCommandValidationSupport}.
 */
@Service
@Transactional(readOnly = true)
class AccessCommandValidationSupport {

    private final UserAccessAreaRepository userAccessAreaRepository;
    private final ManagementRelationRepository managementRelationRepository;
    private final TemporaryRoleAssignmentRepository temporaryRoleAssignmentRepository;
    private final TemporaryAccessAreaRepository temporaryAccessAreaRepository;
    private final TemporaryManagementDelegationRepository temporaryManagementDelegationRepository;
    private final ManagementRelationTypeRepository managementRelationTypeRepository;
    private final UserOperatorStateValidationService UserOperatorStateValidationService;
    private final AccessManagementTargetValidationSupport accessManagementTargetValidationSupport;
    private final UserOrgFoundationStateReadService userOrgFoundationStateReadService;

    AccessCommandValidationSupport(
            UserAccessAreaRepository userAccessAreaRepository,
            ManagementRelationRepository managementRelationRepository,
            TemporaryRoleAssignmentRepository temporaryRoleAssignmentRepository,
            TemporaryAccessAreaRepository temporaryAccessAreaRepository,
            TemporaryManagementDelegationRepository temporaryManagementDelegationRepository,
            ManagementRelationTypeRepository managementRelationTypeRepository,
            UserOperatorStateValidationService UserOperatorStateValidationService,
            AccessManagementTargetValidationSupport accessManagementTargetValidationSupport,
            UserOrgFoundationStateReadService userOrgFoundationStateReadService
    ) {
        this.userAccessAreaRepository = userAccessAreaRepository;
        this.managementRelationRepository = managementRelationRepository;
        this.temporaryRoleAssignmentRepository = temporaryRoleAssignmentRepository;
        this.temporaryAccessAreaRepository = temporaryAccessAreaRepository;
        this.temporaryManagementDelegationRepository = temporaryManagementDelegationRepository;
        this.managementRelationTypeRepository = managementRelationTypeRepository;
        this.UserOperatorStateValidationService = UserOperatorStateValidationService;
        this.accessManagementTargetValidationSupport = accessManagementTargetValidationSupport;
        this.userOrgFoundationStateReadService = userOrgFoundationStateReadService;
    }

    void ensureUserAccessAreaAssignable(UserAccessArea userAccessArea) {
        requireCreatePeriod(userAccessArea.validFrom(), userAccessArea.validTo(), "user access area");
        requireActiveUserAt(
                userAccessArea.userId(),
                userAccessArea.validFrom(),
                "user access area"
        );
        requireAccessScopeModel(
                userAccessArea.organizationalUnitId(),
                userAccessArea.accessScopeType(),
                "user access area"
        );
        accessManagementTargetValidationSupport.ensureAccessAreaTargetAllowed(
                userAccessArea.organizationalUnitId(),
                userAccessArea.accessScopeType()
        );

        boolean overlap = userAccessAreaRepository.findUserAccessAreasByUserId(userAccessArea.userId()).stream()
                .anyMatch(existingArea -> sameAccessOverlapKey(
                        userAccessArea.organizationalUnitId(),
                        userAccessArea.accessScopeType(),
                        existingArea.organizationalUnitId(),
                        existingArea.accessScopeType()
                ) && isPeriodOpenOrIntersects(existingArea.validTo(), userAccessArea.validFrom()));
        if (overlap) {
            throwAccessOverlap(
                    userAccessArea.userId(),
                    userAccessArea.organizationalUnitId(),
                    userAccessArea.accessScopeType(),
                    false
            );
        }
    }

    void ensureUserAccessAreaClosable(Long userAccessAreaId, Instant validTo) {
        UserAccessArea currentArea = userAccessAreaRepository.findUserAccessAreaById(userAccessAreaId);
        requireClosePeriod(currentArea.validFrom(), validTo, "user access area");
    }

    void ensureManagementRelationAssignable(ManagementRelation managementRelation) {
        requireCreatePeriod(managementRelation.validFrom(), managementRelation.validTo(), "management relation");
        requireActiveUserAt(
                managementRelation.userId(),
                managementRelation.validFrom(),
                "management relation"
        );
        managementRelationTypeRepository.findManagementRelationTypeById(
                managementRelation.managementRelationTypeId()
        );
        accessManagementTargetValidationSupport.ensureManagementRelationTargetAllowed(
                managementRelation.organizationalUnitId()
        );

        boolean overlap = managementRelationRepository.findManagementRelationsByUserId(managementRelation.userId()).stream()
                .anyMatch(existing -> Objects.equals(existing.organizationalUnitId(), managementRelation.organizationalUnitId())
                        && Objects.equals(existing.managementRelationTypeId(), managementRelation.managementRelationTypeId())
                        && isPeriodOpenOrIntersects(existing.validTo(), managementRelation.validFrom()));
        if (overlap) {
            throw new ConflictException(
                    "Management relation period overlaps existing history for userId="
                            + managementRelation.userId()
                            + ", organizationalUnitId="
                            + managementRelation.organizationalUnitId()
                            + ", managementRelationTypeId="
                            + managementRelation.managementRelationTypeId()
            );
        }
    }

    void ensureManagementRelationClosable(Long managementRelationId, Instant validTo) {
        ManagementRelation currentRelation = managementRelationRepository.findManagementRelationById(managementRelationId);
        requireClosePeriod(currentRelation.validFrom(), validTo, "management relation");
    }

    void ensureTemporaryRoleAssignable(TemporaryRoleAssignment temporaryRoleAssignment) {
        requireCreatePeriod(
                temporaryRoleAssignment.validFrom(),
                temporaryRoleAssignment.validTo(),
                "temporary role assignment"
        );
        requireActiveUserAt(
                temporaryRoleAssignment.userId(),
                temporaryRoleAssignment.validFrom(),
                "temporary role assignment"
        );
        requireRoleExists(temporaryRoleAssignment.roleId());

        boolean overlap = temporaryRoleAssignmentRepository.findTemporaryRoleAssignmentsByUserId(
                        temporaryRoleAssignment.userId()
                ).stream()
                .anyMatch(existing -> Objects.equals(existing.roleId(), temporaryRoleAssignment.roleId())
                        && isPeriodOpenOrIntersects(existing.validTo(), temporaryRoleAssignment.validFrom()));
        if (overlap) {
            throw new ConflictException(
                    "Temporary role assignment period overlaps existing history for userId="
                            + temporaryRoleAssignment.userId()
                            + ", roleId="
                            + temporaryRoleAssignment.roleId()
            );
        }

        Set<Long> resultingTemporaryRoleIds = new LinkedHashSet<>();
        temporaryRoleAssignmentRepository.findActiveTemporaryRoleAssignmentsByUserId(
                temporaryRoleAssignment.userId(),
                temporaryRoleAssignment.validFrom()
        ).forEach(existing -> resultingTemporaryRoleIds.add(existing.roleId()));
        resultingTemporaryRoleIds.add(temporaryRoleAssignment.roleId());

        UserOperatorStateValidationService.ensureResultingTemporaryRoleStateConsistent(
                temporaryRoleAssignment.userId(),
                temporaryRoleAssignment.validFrom(),
                List.copyOf(resultingTemporaryRoleIds)
        );
    }

    void ensureTemporaryRoleClosable(Long temporaryRoleAssignmentId, Instant validTo) {
        TemporaryRoleAssignment currentAssignment = temporaryRoleAssignmentRepository.findTemporaryRoleAssignmentById(
                temporaryRoleAssignmentId
        );
        requireClosePeriod(currentAssignment.validFrom(), validTo, "temporary role assignment");

        Set<Long> resultingTemporaryRoleIds = new LinkedHashSet<>();
        temporaryRoleAssignmentRepository.findActiveTemporaryRoleAssignmentsByUserId(currentAssignment.userId(), validTo)
                .stream()
                .filter(existing -> !Objects.equals(existing.id(), currentAssignment.id()))
                .forEach(existing -> resultingTemporaryRoleIds.add(existing.roleId()));

        UserOperatorStateValidationService.ensureResultingTemporaryRoleStateConsistent(
                currentAssignment.userId(),
                validTo,
                List.copyOf(resultingTemporaryRoleIds)
        );
    }

    void ensureTemporaryAccessAreaAssignable(TemporaryAccessArea temporaryAccessArea) {
        requireCreatePeriod(
                temporaryAccessArea.validFrom(),
                temporaryAccessArea.validTo(),
                "temporary access area"
        );
        requireActiveUserAt(
                temporaryAccessArea.userId(),
                temporaryAccessArea.validFrom(),
                "temporary access area"
        );
        requireAccessScopeModel(
                temporaryAccessArea.organizationalUnitId(),
                temporaryAccessArea.accessScopeType(),
                "temporary access area"
        );
        accessManagementTargetValidationSupport.ensureAccessAreaTargetAllowed(
                temporaryAccessArea.organizationalUnitId(),
                temporaryAccessArea.accessScopeType()
        );

        boolean overlap = temporaryAccessAreaRepository.findTemporaryAccessAreasByUserId(temporaryAccessArea.userId()).stream()
                .anyMatch(existingArea -> sameAccessOverlapKey(
                        temporaryAccessArea.organizationalUnitId(),
                        temporaryAccessArea.accessScopeType(),
                        existingArea.organizationalUnitId(),
                        existingArea.accessScopeType()
                ) && isPeriodOpenOrIntersects(existingArea.validTo(), temporaryAccessArea.validFrom()));
        if (overlap) {
            throwAccessOverlap(
                    temporaryAccessArea.userId(),
                    temporaryAccessArea.organizationalUnitId(),
                    temporaryAccessArea.accessScopeType(),
                    true
            );
        }
    }

    void ensureTemporaryAccessAreaClosable(Long temporaryAccessAreaId, Instant validTo) {
        TemporaryAccessArea currentArea = temporaryAccessAreaRepository.findTemporaryAccessAreaById(temporaryAccessAreaId);
        requireClosePeriod(currentArea.validFrom(), validTo, "temporary access area");
    }

    void ensureTemporaryManagementAssignable(TemporaryManagementDelegation temporaryManagementDelegation) {
        requireCreatePeriod(
                temporaryManagementDelegation.validFrom(),
                temporaryManagementDelegation.validTo(),
                "temporary management delegation"
        );
        requireActiveUserAt(
                temporaryManagementDelegation.userId(),
                temporaryManagementDelegation.validFrom(),
                "temporary management delegation"
        );
        managementRelationTypeRepository.findManagementRelationTypeById(
                temporaryManagementDelegation.managementRelationTypeId()
        );
        accessManagementTargetValidationSupport.ensureManagementRelationTargetAllowed(
                temporaryManagementDelegation.organizationalUnitId()
        );

        boolean overlap = temporaryManagementDelegationRepository.findTemporaryManagementDelegationsByUserId(
                        temporaryManagementDelegation.userId()
                ).stream()
                .anyMatch(existing -> Objects.equals(existing.organizationalUnitId(), temporaryManagementDelegation.organizationalUnitId())
                        && Objects.equals(existing.managementRelationTypeId(), temporaryManagementDelegation.managementRelationTypeId())
                        && isPeriodOpenOrIntersects(existing.validTo(), temporaryManagementDelegation.validFrom()));
        if (overlap) {
            throw new ConflictException(
                    "Temporary management delegation period overlaps existing history for userId="
                            + temporaryManagementDelegation.userId()
                            + ", organizationalUnitId="
                            + temporaryManagementDelegation.organizationalUnitId()
                            + ", managementRelationTypeId="
                            + temporaryManagementDelegation.managementRelationTypeId()
            );
        }
    }

    void ensureTemporaryManagementClosable(Long temporaryManagementDelegationId, Instant validTo) {
        TemporaryManagementDelegation currentDelegation = temporaryManagementDelegationRepository.findTemporaryManagementDelegationById(
                temporaryManagementDelegationId
        );
        requireClosePeriod(currentDelegation.validFrom(), validTo, "temporary management delegation");
    }

    private void requireAccessScopeModel(
            Long organizationalUnitId,
            AccessScopeType accessScopeType,
            String label
    ) {
        if (accessScopeType == null) {
            throw new ValidationException(label + " accessScopeType must not be null");
        }
        if (accessScopeType == AccessScopeType.GLOBAL && organizationalUnitId != null) {
            throw new ValidationException(label + " GLOBAL scope requires organizationalUnitId to be null");
        }
        if (accessScopeType != AccessScopeType.GLOBAL && organizationalUnitId == null) {
            throw new ValidationException(label + " unit-scoped access requires organizationalUnitId");
        }
    }

    private void requireCreatePeriod(Instant validFrom, Instant validTo, String label) {
        if (validFrom == null) {
            throw new ValidationException(label + " validFrom must not be null");
        }
        if (validTo != null) {
            throw new ValidationException(label + " validTo must be null for create command");
        }
    }

    private void requireClosePeriod(Instant validFrom, Instant validTo, String label) {
        if (validTo == null || !validTo.isAfter(validFrom)) {
            throw new ValidationException(label + " validTo must be greater than validFrom");
        }
    }

    private void requireActiveUserAt(Long userId, Instant effectiveAt, String label) {
        UserOrgFoundationStateReadService.UserAccessPolicyFoundationState userState =
                userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(userId, effectiveAt);
        if (!userState.active()) {
            throw new ConflictException("INACTIVE user cannot receive new active " + label + ": " + userId);
        }
    }

    private void requireRoleExists(Long roleId) {
        userOrgFoundationStateReadService.findRoleCodesByIds(List.of(roleId));
    }

    private boolean isPeriodOpenOrIntersects(Instant currentValidTo, Instant nextValidFrom) {
        return currentValidTo == null || currentValidTo.isAfter(nextValidFrom);
    }

    private boolean sameAccessOverlapKey(
            Long targetOrganizationalUnitId,
            AccessScopeType targetAccessScopeType,
            Long currentOrganizationalUnitId,
            AccessScopeType currentAccessScopeType
    ) {
        if (targetAccessScopeType == AccessScopeType.GLOBAL) {
            return currentAccessScopeType == AccessScopeType.GLOBAL;
        }
        return Objects.equals(targetOrganizationalUnitId, currentOrganizationalUnitId)
                && targetAccessScopeType == currentAccessScopeType;
    }

    private void throwAccessOverlap(
            Long userId,
            Long organizationalUnitId,
            AccessScopeType accessScopeType,
            boolean temporary
    ) {
        String prefix = temporary ? "Temporary access area" : "User access area";
        if (accessScopeType == AccessScopeType.GLOBAL) {
            throw new ConflictException(prefix + " period overlaps existing GLOBAL history for userId=" + userId);
        }
        throw new ConflictException(
                prefix
                        + " period overlaps existing history for userId="
                        + userId
                        + ", organizationalUnitId="
                        + organizationalUnitId
                        + ", accessScopeType="
                        + accessScopeType
        );
    }
}
