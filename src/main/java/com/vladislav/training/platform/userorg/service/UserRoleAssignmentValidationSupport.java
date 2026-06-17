package com.vladislav.training.platform.userorg.service;

import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.ValidationException;
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment;
import com.vladislav.training.platform.userorg.domain.UserRoleAssignment;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.repository.AppUserRepository;
import com.vladislav.training.platform.userorg.repository.UserRoleAssignmentRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Вспомогательный тип {@code UserRoleAssignmentValidationSupport}.
 */
@Service
@Transactional(readOnly = true)
class UserRoleAssignmentValidationSupport {

    private final UserRoleAssignmentRepository userRoleAssignmentRepository;
    private final AppUserRepository appUserRepository;
    private final UserOperatorStateSupport UserOperatorStateSupport;

    UserRoleAssignmentValidationSupport(
            UserRoleAssignmentRepository userRoleAssignmentRepository,
            AppUserRepository appUserRepository,
            UserOperatorStateSupport UserOperatorStateSupport
    ) {
        this.userRoleAssignmentRepository = userRoleAssignmentRepository;
        this.appUserRepository = appUserRepository;
        this.UserOperatorStateSupport = UserOperatorStateSupport;
    }

    void ensureAssignable(UserRoleAssignment assignment) {
        requireCreatePeriod(assignment.validFrom(), assignment.validTo(), "role assignment");
        ensureUserActiveForNewRoleHistory(assignment.userId());
        ensurePermanentRoleOverlapAbsent(assignment.userId(), assignment.roleId(), assignment.validFrom());

        List<UserRoleAssignment> resultingPermanentRoles = new ArrayList<>(
                UserOperatorStateSupport.loadActivePermanentRoleAssignments(assignment.userId(), assignment.validFrom())
        );
        resultingPermanentRoles.add(new UserRoleAssignment(
                null,
                assignment.userId(),
                assignment.roleId(),
                assignment.validFrom(),
                null,
                assignment.validFrom(),
                assignment.validFrom()
        ));
        List<UserOrganizationAssignment> resultingOrganizationAssignments = UserOperatorStateSupport
                .loadActiveOrganizationAssignments(assignment.userId(), assignment.validFrom());
        UserOperatorStateSupport.ensureResultingStateConsistent(
                assignment.userId(),
                assignment.validFrom(),
                resultingPermanentRoles,
                resultingOrganizationAssignments
        );
    }

    void ensureClosable(Long assignmentId, Instant validTo) {
        UserRoleAssignment currentAssignment = userRoleAssignmentRepository.findRoleAssignmentById(assignmentId);
        requireClosePeriod(currentAssignment.validFrom(), validTo, "role assignment");

        List<UserRoleAssignment> resultingPermanentRoles = new ArrayList<>(
                UserOperatorStateSupport.loadActivePermanentRoleAssignments(currentAssignment.userId(), validTo)
        );
        resultingPermanentRoles.removeIf(assignment -> Objects.equals(assignment.id(), currentAssignment.id()));
        List<UserOrganizationAssignment> resultingOrganizationAssignments = UserOperatorStateSupport
                .loadActiveOrganizationAssignments(currentAssignment.userId(), validTo);
        UserOperatorStateSupport.ensureResultingStateConsistent(
                currentAssignment.userId(),
                validTo,
                resultingPermanentRoles,
                resultingOrganizationAssignments
        );
    }

    private void ensureUserActiveForNewRoleHistory(Long userId) {
        AppUser user = appUserRepository.findUserById(userId);
        if (user.status() != UserStatus.ACTIVE) {
            throw new ConflictException("Inactive user cannot receive new active role assignment: " + userId);
        }
    }

    private void ensurePermanentRoleOverlapAbsent(Long userId, Long roleId, Instant validFrom) {
        boolean overlap = userRoleAssignmentRepository.findRoleAssignmentsByUserId(userId).stream()
                .anyMatch(assignment -> Objects.equals(assignment.roleId(), roleId)
                        && isPeriodOpenOrIntersects(assignment.validTo(), validFrom));
        if (overlap) {
            throw new ConflictException(
                    "Role assignment period overlaps existing history for userId=" + userId + ", roleId=" + roleId
            );
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

    private boolean isPeriodOpenOrIntersects(Instant currentValidTo, Instant nextValidFrom) {
        return currentValidTo == null || currentValidTo.isAfter(nextValidFrom);
    }
}
