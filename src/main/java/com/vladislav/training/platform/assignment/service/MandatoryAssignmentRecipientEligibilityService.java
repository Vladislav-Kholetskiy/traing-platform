package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.access.service.TemporaryRoleAssignmentReadService;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.domain.OrganizationalNodeKind;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitType;
import com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.repository.UserOrganizationAssignmentRepository;
import com.vladislav.training.platform.userorg.service.RoleCodeNormalizer;
import com.vladislav.training.platform.userorg.service.OrganizationQueryService;
import com.vladislav.training.platform.userorg.service.UserOrgFoundationStateReadService;
import com.vladislav.training.platform.userorg.service.UserQueryService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Контракт сервиса {@code MandatoryAssignmentRecipientEligibilityService}.
 */
@Service
@Transactional(readOnly = true)
class MandatoryAssignmentRecipientEligibilityService {

    private static final String OPERATOR_ROLE_CODE = "OPERATOR";

    private final OrganizationQueryService organizationQueryService;
    private final UserQueryService userQueryService;
    private final UserOrgFoundationStateReadService userOrgFoundationStateReadService;
    private final TemporaryRoleAssignmentReadService temporaryRoleAssignmentReadService;
    private final UserOrganizationAssignmentRepository userOrganizationAssignmentRepository;

    MandatoryAssignmentRecipientEligibilityService(
        OrganizationQueryService organizationQueryService,
        UserQueryService userQueryService,
        UserOrgFoundationStateReadService userOrgFoundationStateReadService,
        TemporaryRoleAssignmentReadService temporaryRoleAssignmentReadService,
        UserOrganizationAssignmentRepository userOrganizationAssignmentRepository
    ) {
        this.organizationQueryService = organizationQueryService;
        this.userQueryService = userQueryService;
        this.userOrgFoundationStateReadService = userOrgFoundationStateReadService;
        this.temporaryRoleAssignmentReadService = temporaryRoleAssignmentReadService;
        this.userOrganizationAssignmentRepository = userOrganizationAssignmentRepository;
    }

    MandatoryRecipientEligibility evaluateRecipient(Long candidateUserId, String targetUnitPath, Instant effectiveAt) {
        AppUser user = userQueryService.findUserById(candidateUserId);
        if (user.status() != UserStatus.ACTIVE) {
            return MandatoryRecipientEligibility.ineligible(candidateUserId, IneligibilityReason.USER_NOT_ACTIVE);
        }

        UserOrgFoundationStateReadService.UserAccessPolicyFoundationState foundationState =
            userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(candidateUserId, effectiveAt);
        if (!foundationState.active() || !hasEffectiveOperatorRole(candidateUserId, effectiveAt, foundationState)) {
            return MandatoryRecipientEligibility.ineligible(
                candidateUserId,
                IneligibilityReason.NOT_ADMISSIBLE_MANDATORY_OPERATOR
            );
        }

        List<UserOrganizationAssignment> primaryAssignments = userOrganizationAssignmentRepository
            .findActiveOrganizationAssignmentsByUserId(candidateUserId, effectiveAt).stream()
            .filter(assignment -> assignment.assignmentType() == OrganizationAssignmentType.PRIMARY)
            .toList();
        if (primaryAssignments.size() != 1) {
            return MandatoryRecipientEligibility.ineligible(candidateUserId, IneligibilityReason.NO_SINGLE_PRIMARY_HOME_UNIT);
        }

        OrganizationalUnit primaryHomeUnit = organizationQueryService.findOrganizationalUnitById(
            primaryAssignments.getFirst().organizationalUnitId()
        );
        OrganizationalUnitType primaryHomeUnitType = organizationQueryService.findOrganizationalUnitTypeById(
            primaryHomeUnit.organizationalUnitTypeId()
        );
        if (primaryHomeUnit.status() != OrganizationalUnitStatus.ACTIVE
            || primaryHomeUnitType.nodeKind() != OrganizationalNodeKind.LINEAR
            || !primaryHomeUnitType.canBeOperatorHomeUnit()) {
            return MandatoryRecipientEligibility.ineligible(candidateUserId, IneligibilityReason.INVALID_PRIMARY_HOME_UNIT);
        }
        if (!isInTargetSubtree(primaryHomeUnit.path(), targetUnitPath)) {
            return MandatoryRecipientEligibility.ineligible(candidateUserId, IneligibilityReason.OUTSIDE_TARGET_SUBTREE);
        }

        return MandatoryRecipientEligibility.eligible(
            user.id(),
            user.employeeNumber(),
            user.lastName(),
            user.firstName(),
            user.middleName(),
            primaryHomeUnit.id(),
            primaryHomeUnit.path(),
            buildFullName(user)
        );
    }

    private boolean hasEffectiveOperatorRole(
        Long userId,
        Instant effectiveAt,
        UserOrgFoundationStateReadService.UserAccessPolicyFoundationState foundationState
    ) {
        Set<String> effectiveRoleCodes = new LinkedHashSet<>(foundationState.activePermanentRoleCodes());
        List<Long> activeTemporaryRoleIds = temporaryRoleAssignmentReadService.findActiveTemporaryRoleIdsByUserId(
            userId,
            effectiveAt
        );
        if (!activeTemporaryRoleIds.isEmpty()) {
            effectiveRoleCodes.addAll(userOrgFoundationStateReadService.findRoleCodesByIds(activeTemporaryRoleIds));
        }
        return hasOperatorRole(effectiveRoleCodes);
    }

    private boolean hasOperatorRole(Set<String> activeRoleCodes) {
        for (String roleCode : activeRoleCodes) {
            if (OPERATOR_ROLE_CODE.equalsIgnoreCase(RoleCodeNormalizer.normalize(roleCode))) {
                return true;
            }
        }
        return false;
    }

    private boolean isInTargetSubtree(String organizationalUnitPath, String targetPath) {
        return organizationalUnitPath.equals(targetPath) || organizationalUnitPath.startsWith(targetPath + "/");
    }

    private String buildFullName(AppUser user) {
        List<String> parts = new ArrayList<>(3);
        if (user.lastName() != null && !user.lastName().isBlank()) {
            parts.add(user.lastName());
        }
        if (user.firstName() != null && !user.firstName().isBlank()) {
            parts.add(user.firstName());
        }
        if (user.middleName() != null && !user.middleName().isBlank()) {
            parts.add(user.middleName());
        }
        return String.join(" ", parts);
    }

    enum IneligibilityReason {
        USER_NOT_ACTIVE {
            @Override
            String toLaunchMessage(Long candidateUserId) {
                return "Potential recipient is not ACTIVE: " + candidateUserId;
            }
        },
        NOT_ADMISSIBLE_MANDATORY_OPERATOR {
            @Override
            String toLaunchMessage(Long candidateUserId) {
                return "Potential recipient is not an admissible mandatory-assignment operator: " + candidateUserId;
            }
        },
        NO_SINGLE_PRIMARY_HOME_UNIT {
            @Override
            String toLaunchMessage(Long candidateUserId) {
                return "Potential recipient must have exactly one active PRIMARY home unit: " + candidateUserId;
            }
        },
        INVALID_PRIMARY_HOME_UNIT {
            @Override
            String toLaunchMessage(Long candidateUserId) {
                return "Potential recipient does not have a valid active PRIMARY home unit: " + candidateUserId;
            }
        },
        OUTSIDE_TARGET_SUBTREE {
            @Override
            String toLaunchMessage(Long candidateUserId) {
                return "Potential recipient does not have a valid snapshot org-context in the targeted subtree: "
                    + candidateUserId;
            }
        };

        abstract String toLaunchMessage(Long candidateUserId);
    }

    record MandatoryRecipientEligibility(
        Long userId,
        String employeeNumber,
        String lastName,
        String firstName,
        String middleName,
        Long organizationalUnitIdSnapshot,
        String organizationalPathSnapshot,
        String fullNameSnapshot,
        IneligibilityReason ineligibilityReason
    ) {

        static MandatoryRecipientEligibility eligible(
            Long userId,
            String employeeNumber,
            String lastName,
            String firstName,
            String middleName,
            Long organizationalUnitIdSnapshot,
            String organizationalPathSnapshot,
            String fullNameSnapshot
        ) {
            return new MandatoryRecipientEligibility(
                userId,
                employeeNumber,
                lastName,
                firstName,
                middleName,
                organizationalUnitIdSnapshot,
                organizationalPathSnapshot,
                fullNameSnapshot,
                null
            );
        }

        static MandatoryRecipientEligibility ineligible(Long userId, IneligibilityReason ineligibilityReason) {
            return new MandatoryRecipientEligibility(
                userId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                ineligibilityReason
            );
        }

        boolean eligible() {
            return ineligibilityReason == null;
        }

        void requireEligibleForLaunch() {
            if (!eligible()) {
                throw new ConflictException(ineligibilityReason.toLaunchMessage(userId));
            }
        }
    }
}

