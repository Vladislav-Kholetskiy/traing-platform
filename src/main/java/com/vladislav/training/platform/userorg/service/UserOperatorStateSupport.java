package com.vladislav.training.platform.userorg.service;

import com.vladislav.training.platform.access.service.TemporaryRoleAssignmentReadService;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.userorg.domain.AppRole;
import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.domain.OrganizationalNodeKind;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitType;
import com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment;
import com.vladislav.training.platform.userorg.domain.UserRoleAssignment;
import com.vladislav.training.platform.userorg.repository.AppRoleRepository;
import com.vladislav.training.platform.userorg.repository.UserOrganizationAssignmentRepository;
import com.vladislav.training.platform.userorg.repository.UserRoleAssignmentRepository;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Вспомогательный тип {@code UserOperatorStateSupport}.
 */
@Service
@Transactional(readOnly = true)
class UserOperatorStateSupport implements UserOperatorStateValidationService {

    private static final String SYSTEM_OPERATOR_ROLE_CODE = "OPERATOR";

    private final UserRoleAssignmentRepository userRoleAssignmentRepository;
    private final UserOrganizationAssignmentRepository userOrganizationAssignmentRepository;
    private final TemporaryRoleAssignmentReadService temporaryRoleAssignmentReadService;
    private final AppRoleRepository appRoleRepository;
    private final OrganizationalUnitAssignmentValidationSupport organizationalUnitAssignmentValidationSupport;

    UserOperatorStateSupport(
        UserRoleAssignmentRepository userRoleAssignmentRepository,
        UserOrganizationAssignmentRepository userOrganizationAssignmentRepository,
        TemporaryRoleAssignmentReadService temporaryRoleAssignmentReadService,
        AppRoleRepository appRoleRepository,
        OrganizationalUnitAssignmentValidationSupport organizationalUnitAssignmentValidationSupport
    ) {
        this.userRoleAssignmentRepository = userRoleAssignmentRepository;
        this.userOrganizationAssignmentRepository = userOrganizationAssignmentRepository;
        this.temporaryRoleAssignmentReadService = temporaryRoleAssignmentReadService;
        this.appRoleRepository = appRoleRepository;
        this.organizationalUnitAssignmentValidationSupport = organizationalUnitAssignmentValidationSupport;
    }

    List<UserRoleAssignment> loadActivePermanentRoleAssignments(Long userId, Instant activeAt) {
        return userRoleAssignmentRepository.findActiveRoleAssignmentsByUserId(userId, activeAt);
    }

    List<UserOrganizationAssignment> loadActiveOrganizationAssignments(Long userId, Instant activeAt) {
        return userOrganizationAssignmentRepository.findActiveOrganizationAssignmentsByUserId(userId, activeAt);
    }

    void ensureResultingStateConsistent(
        Long userId,
        Instant activeAt,
        List<UserRoleAssignment> resultingPermanentRoles,
        List<UserOrganizationAssignment> resultingOrganizationAssignments
    ) {
        ensureResultingStateConsistent(
            resultingPermanentRoles,
            resultingOrganizationAssignments,
            temporaryRoleAssignmentReadService.findActiveTemporaryRoleIdsByUserId(userId, activeAt)
        );
    }

    boolean isUserInOperatorContour(Long userId, Instant activeAt) {
        return isOperatorContour(
            loadActivePermanentRoleAssignments(userId, activeAt),
            temporaryRoleAssignmentReadService.findActiveTemporaryRoleIdsByUserId(userId, activeAt)
        );
    }

    void ensureCurrentPrimaryHomeUnitTypeAllowed(
        Long userId,
        Instant activeAt,
        Long organizationalUnitId,
        OrganizationalUnitType resultingUnitType
    ) {
        if (!isUserInOperatorContour(userId, activeAt)) {
            return;
        }

        List<UserOrganizationAssignment> activePrimaryAssignments = loadActiveOrganizationAssignments(userId, activeAt).stream()
            .filter(assignment -> assignment.assignmentType() == OrganizationAssignmentType.PRIMARY)
            .toList();
        if (activePrimaryAssignments.size() != 1
            || !Objects.equals(activePrimaryAssignments.getFirst().organizationalUnitId(), organizationalUnitId)) {
            throw new ConflictException("Operator-contour user must have exactly one active PRIMARY home unit");
        }

        ensureOperatorHomeUnitTypeAllowed(organizationalUnitId, resultingUnitType);
    }

    @Override
    public void ensureResultingTemporaryRoleStateConsistent(
        Long userId,
        Instant activeAt,
        List<Long> resultingTemporaryRoleIds
    ) {
        ensureResultingStateConsistent(
            loadActivePermanentRoleAssignments(userId, activeAt),
            loadActiveOrganizationAssignments(userId, activeAt),
            resultingTemporaryRoleIds
        );
    }

    private void ensureResultingStateConsistent(
        List<UserRoleAssignment> resultingPermanentRoles,
        List<UserOrganizationAssignment> resultingOrganizationAssignments,
        List<Long> resultingTemporaryRoleIds
    ) {
        if (!isOperatorContour(resultingPermanentRoles, resultingTemporaryRoleIds)) {
            return;
        }

        List<UserOrganizationAssignment> activePrimaryAssignments = resultingOrganizationAssignments.stream()
            .filter(assignment -> assignment.assignmentType() == OrganizationAssignmentType.PRIMARY)
            .toList();
        if (activePrimaryAssignments.size() != 1) {
            throw new ConflictException("Operator-contour user must have exactly one active PRIMARY home unit");
        }
        organizationalUnitAssignmentValidationSupport.ensureOperatorHomeUnitAllowed(
            activePrimaryAssignments.getFirst().organizationalUnitId()
        );
    }

    private void ensureOperatorHomeUnitTypeAllowed(Long organizationalUnitId, OrganizationalUnitType resultingUnitType) {
        if (resultingUnitType.nodeKind() != OrganizationalNodeKind.LINEAR) {
            throw new ConflictException(
                "Operator home unit must reference LINEAR organizational unit: " + organizationalUnitId
            );
        }
        if (!resultingUnitType.canBeOperatorHomeUnit()) {
            throw new ConflictException(
                "Organizational unit cannot be used as operator home unit by current SCN-17 foundation-state: "
                    + organizationalUnitId
            );
        }
    }

    private boolean isOperatorContour(
        List<UserRoleAssignment> permanentAssignments,
        List<Long> temporaryRoleIds
    ) {
        Set<Long> activeRoleIds = new LinkedHashSet<>();
        for (UserRoleAssignment permanentAssignment : permanentAssignments) {
            activeRoleIds.add(permanentAssignment.roleId());
        }
        activeRoleIds.addAll(temporaryRoleIds);
        for (Long activeRoleId : activeRoleIds) {
            AppRole role = appRoleRepository.findRoleById(activeRoleId);
            if (SYSTEM_OPERATOR_ROLE_CODE.equalsIgnoreCase(RoleCodeNormalizer.normalize(role.code()))) {
                return true;
            }
        }
        return false;
    }
}


