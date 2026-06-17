package com.vladislav.training.platform.userorg.service;

import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.ValidationException;
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment;
import com.vladislav.training.platform.userorg.domain.UserRoleAssignment;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.repository.AppUserRepository;
import com.vladislav.training.platform.userorg.repository.UserOrganizationAssignmentRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Вспомогательный тип {@code UserOrganizationAssignmentValidationSupport}.
 */
@Service
@Transactional(readOnly = true)
class UserOrganizationAssignmentValidationSupport {

    private final UserOrganizationAssignmentRepository userOrganizationAssignmentRepository;
    private final AppUserRepository appUserRepository;
    private final OrganizationalUnitAssignmentValidationSupport organizationalUnitAssignmentValidationSupport;
    private final UserOperatorStateSupport UserOperatorStateSupport;

    UserOrganizationAssignmentValidationSupport(
            UserOrganizationAssignmentRepository userOrganizationAssignmentRepository,
            AppUserRepository appUserRepository,
            OrganizationalUnitAssignmentValidationSupport organizationalUnitAssignmentValidationSupport,
            UserOperatorStateSupport UserOperatorStateSupport
    ) {
        this.userOrganizationAssignmentRepository = userOrganizationAssignmentRepository;
        this.appUserRepository = appUserRepository;
        this.organizationalUnitAssignmentValidationSupport = organizationalUnitAssignmentValidationSupport;
        this.UserOperatorStateSupport = UserOperatorStateSupport;
    }

    void ensureAssignable(UserOrganizationAssignment assignment) {
        if (assignment.assignmentType() == null) {
            throw new ValidationException("organization assignmentType must not be null");
        }
        requireCreatePeriod(assignment.validFrom(), assignment.validTo(), "organization assignment");
        ensureUserActiveForNewOrganizationHistory(assignment.userId());
        organizationalUnitAssignmentValidationSupport.requireAssignableTarget(assignment.organizationalUnitId());
        if (assignment.assignmentType() == OrganizationAssignmentType.PRIMARY) {
            organizationalUnitAssignmentValidationSupport.ensureOperatorHomeUnitAllowed(assignment.organizationalUnitId());
        }
        ensureOrganizationOverlapAbsent(
                assignment.userId(),
                assignment.organizationalUnitId(),
                assignment.assignmentType(),
                assignment.validFrom()
        );

        List<UserOrganizationAssignment> resultingAssignments = new ArrayList<>(
                UserOperatorStateSupport.loadActiveOrganizationAssignments(assignment.userId(), assignment.validFrom())
        );
        if (assignment.assignmentType() == OrganizationAssignmentType.PRIMARY
                && resultingAssignments.stream().anyMatch(existing -> existing.assignmentType() == OrganizationAssignmentType.PRIMARY)) {
            throw new ConflictException("User cannot have two simultaneously active PRIMARY assignments: " + assignment.userId());
        }
        resultingAssignments.add(new UserOrganizationAssignment(
                null,
                assignment.userId(),
                assignment.organizationalUnitId(),
                assignment.assignmentType(),
                assignment.validFrom(),
                null,
                assignment.validFrom(),
                assignment.validFrom()
        ));
        List<UserRoleAssignment> resultingPermanentRoles = UserOperatorStateSupport.loadActivePermanentRoleAssignments(
                assignment.userId(),
                assignment.validFrom()
        );
        UserOperatorStateSupport.ensureResultingStateConsistent(
                assignment.userId(),
                assignment.validFrom(),
                resultingPermanentRoles,
                resultingAssignments
        );
    }

    void ensureClosable(Long assignmentId, Instant validTo) {
        UserOrganizationAssignment currentAssignment = userOrganizationAssignmentRepository.findOrganizationAssignmentById(assignmentId);
        requireClosePeriod(currentAssignment.validFrom(), validTo, "organization assignment");

        List<UserOrganizationAssignment> resultingAssignments = new ArrayList<>(
                UserOperatorStateSupport.loadActiveOrganizationAssignments(currentAssignment.userId(), validTo)
        );
        resultingAssignments.removeIf(assignment -> Objects.equals(assignment.id(), currentAssignment.id()));
        List<UserRoleAssignment> resultingPermanentRoles = UserOperatorStateSupport.loadActivePermanentRoleAssignments(
                currentAssignment.userId(),
                validTo
        );
        UserOperatorStateSupport.ensureResultingStateConsistent(
                currentAssignment.userId(),
                validTo,
                resultingPermanentRoles,
                resultingAssignments
        );
    }

    UserOrganizationAssignment ensurePrimaryHomeReplacementAllowed(
            Long userId,
            Long newOrganizationalUnitId,
            Instant effectiveAt
    ) {
        ensureUserActiveForPrimaryHomeReplacement(userId);
        if (effectiveAt == null) {
            throw new ValidationException("primary home replace requires effectiveAt");
        }
        organizationalUnitAssignmentValidationSupport.ensureOperatorHomeUnitAllowed(newOrganizationalUnitId);

        List<UserOrganizationAssignment> currentAssignments = new ArrayList<>(
                UserOperatorStateSupport.loadActiveOrganizationAssignments(userId, effectiveAt)
        );
        List<UserOrganizationAssignment> currentPrimaryAssignments = currentAssignments.stream()
                .filter(assignment -> assignment.assignmentType() == OrganizationAssignmentType.PRIMARY)
                .toList();
        if (currentPrimaryAssignments.size() != 1) {
            throw new ConflictException("Primary home unit replace requires exactly one active PRIMARY assignment");
        }
        UserOrganizationAssignment currentPrimary = currentPrimaryAssignments.getFirst();
        if (Objects.equals(currentPrimary.organizationalUnitId(), newOrganizationalUnitId)) {
            throw new ValidationException("Replacement home unit must differ from the current PRIMARY unit");
        }
        if (!effectiveAt.isAfter(currentPrimary.validFrom())) {
            throw new ValidationException("replacement effectiveAt must be greater than current PRIMARY validFrom");
        }

        currentAssignments.removeIf(assignment -> Objects.equals(assignment.id(), currentPrimary.id()));
        currentAssignments.add(new UserOrganizationAssignment(
                null,
                userId,
                newOrganizationalUnitId,
                OrganizationAssignmentType.PRIMARY,
                effectiveAt,
                null,
                effectiveAt,
                effectiveAt
        ));
        List<UserRoleAssignment> resultingPermanentRoles = UserOperatorStateSupport.loadActivePermanentRoleAssignments(
                userId,
                effectiveAt
        );
        UserOperatorStateSupport.ensureResultingStateConsistent(
                userId,
                effectiveAt,
                resultingPermanentRoles,
                currentAssignments
        );
        return currentPrimary;
    }

    private void ensureUserActiveForNewOrganizationHistory(Long userId) {
        AppUser user = appUserRepository.findUserById(userId);
        if (user.status() != UserStatus.ACTIVE) {
            throw new ConflictException("Inactive user cannot receive new active organization assignment: " + userId);
        }
    }

    private void ensureUserActiveForPrimaryHomeReplacement(Long userId) {
        AppUser user = appUserRepository.findUserById(userId);
        if (user.status() != UserStatus.ACTIVE) {
            throw new ConflictException("Inactive user cannot replace PRIMARY home unit: " + userId);
        }
    }

    private void ensureOrganizationOverlapAbsent(
            Long userId,
            Long organizationalUnitId,
            OrganizationAssignmentType assignmentType,
            Instant validFrom
    ) {
        boolean overlap = userOrganizationAssignmentRepository.findOrganizationAssignmentsByUserId(userId).stream()
                .anyMatch(assignment -> Objects.equals(assignment.organizationalUnitId(), organizationalUnitId)
                        && assignment.assignmentType() == assignmentType
                        && isPeriodOpenOrIntersects(assignment.validTo(), validFrom));
        if (overlap) {
            throw new ConflictException(
                    "Organization assignment period overlaps existing history for userId="
                            + userId
                            + ", organizationalUnitId="
                            + organizationalUnitId
                            + ", assignmentType="
                            + assignmentType
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
