package com.vladislav.training.platform.integration.personnel.service;

import com.vladislav.training.platform.access.domain.AccessScopeType;
import com.vladislav.training.platform.access.service.AccessAdministrationQueryService;
import com.vladislav.training.platform.access.service.ManagementRelationAdminFilter;
import com.vladislav.training.platform.access.service.TemporaryAccessAreaAdminFilter;
import com.vladislav.training.platform.access.service.TemporaryManagementDelegationAdminFilter;
import com.vladislav.training.platform.access.service.TemporaryRoleAssignmentAdminFilter;
import com.vladislav.training.platform.access.service.UserAccessAreaAdminFilter;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.integration.personnel.model.PersonnelBusinessIntent;
import com.vladislav.training.platform.integration.personnel.model.PersonnelCurrentState;
import com.vladislav.training.platform.integration.personnel.model.PersonnelIdentityResolution;
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.service.OrganizationQueryService;
import com.vladislav.training.platform.userorg.service.UserOrganizationAssignmentService;
import com.vladislav.training.platform.userorg.service.UserQueryService;
import com.vladislav.training.platform.userorg.service.UserRoleAssignmentService;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Читатель {@code OwnerReadPersonnelCurrentStateReader}.
 */

@Service
@Transactional(readOnly = true)
public class OwnerReadPersonnelCurrentStateReader implements PersonnelCurrentStateReader {

    private final UserQueryService userQueryService;
    private final UserOrganizationAssignmentService userOrganizationAssignmentService;
    private final UserRoleAssignmentService userRoleAssignmentService;
    private final OrganizationQueryService organizationQueryService;
    private final AccessAdministrationQueryService accessAdministrationQueryService;
    private final UtcClock utcClock;

    public OwnerReadPersonnelCurrentStateReader(
        UserQueryService userQueryService,
        UserOrganizationAssignmentService userOrganizationAssignmentService,
        UserRoleAssignmentService userRoleAssignmentService,
        OrganizationQueryService organizationQueryService,
        AccessAdministrationQueryService accessAdministrationQueryService,
        UtcClock utcClock
    ) {
        this.userQueryService = Objects.requireNonNull(userQueryService, "userQueryService must not be null");
        this.userOrganizationAssignmentService = Objects.requireNonNull(
            userOrganizationAssignmentService,
            "userOrganizationAssignmentService must not be null"
        );
        this.userRoleAssignmentService = Objects.requireNonNull(
            userRoleAssignmentService,
            "userRoleAssignmentService must not be null"
        );
        this.organizationQueryService = Objects.requireNonNull(
            organizationQueryService,
            "organizationQueryService must not be null"
        );
        this.accessAdministrationQueryService = Objects.requireNonNull(
            accessAdministrationQueryService,
            "accessAdministrationQueryService must not be null"
        );
        this.utcClock = Objects.requireNonNull(utcClock, "utcClock must not be null");
    }

    @Override
    public PersonnelIdentityResolution resolveIdentity(PersonnelBusinessIntent intent) {
        AppUser user = userQueryService.findOptionalUserByEmployeeNumber(intent.employeeNumber())
            .orElse(null);
        if (user == null) {
            return PersonnelIdentityResolution.unresolved(intent.employeeNumber());
        }

        if (hasExternalIdGuard(intent) && !Objects.equals(intent.externalIdConsistencyGuard(), user.externalId())) {
            return PersonnelIdentityResolution.identityMismatch(
                intent.employeeNumber(),
                intent.externalIdConsistencyGuard(),
                user.externalId()
            );
        }

        Instant activeAt = utcClock.now();
        return PersonnelIdentityResolution.resolved(new PersonnelCurrentState(
            user.id(),
            user.employeeNumber(),
            user.externalId(),
            user.status().name(),
            resolvePrimaryHomeOrgUnitCode(user.id(), activeAt),
            resolveActiveRoleCodes(user.id(), activeAt),
            !accessAdministrationQueryService.listManagementRelations(
                new ManagementRelationAdminFilter(user.id(), null, null, activeAt)
            ).isEmpty(),
            resolveAccessScopeCodes(user.id(), activeAt),
            resolveTemporaryRoleCodes(user.id(), activeAt),
            resolveTemporaryAccessScopeCodes(user.id(), activeAt),
            !accessAdministrationQueryService.listTemporaryManagementDelegations(
                new TemporaryManagementDelegationAdminFilter(user.id(), null, null, activeAt)
            ).isEmpty()
        ));
    }

    private boolean hasExternalIdGuard(PersonnelBusinessIntent intent) {
        return intent.externalIdConsistencyGuard() != null && !intent.externalIdConsistencyGuard().isBlank();
    }

    private String resolvePrimaryHomeOrgUnitCode(Long userId, Instant activeAt) {
        return userOrganizationAssignmentService.findActiveOrganizationAssignmentsByUserId(userId, activeAt).stream()
            .filter(assignment -> assignment.assignmentType() == OrganizationAssignmentType.PRIMARY)
            .map(assignment -> {
                var organizationalUnit = organizationQueryService.findOrganizationalUnitById(assignment.organizationalUnitId());
                return organizationalUnit.externalId() != null && !organizationalUnit.externalId().isBlank()
                    ? organizationalUnit.externalId()
                    : organizationalUnit.path();
            })
            .findFirst()
            .orElse(null);
    }

    private Set<String> resolveActiveRoleCodes(Long userId, Instant activeAt) {
        Set<String> roleCodes = new LinkedHashSet<>();
        userRoleAssignmentService.findActiveRoleAssignmentsByUserId(userId, activeAt)
            .forEach(assignment -> roleCodes.add(userQueryService.findRoleById(assignment.roleId()).code()));
        return Set.copyOf(roleCodes);
    }

    private Set<String> resolveAccessScopeCodes(Long userId, Instant activeAt) {
        Set<String> scopeCodes = new LinkedHashSet<>();
        accessAdministrationQueryService.listUserAccessAreas(
            new UserAccessAreaAdminFilter(userId, null, null, activeAt)
        ).forEach(accessArea -> scopeCodes.add(toPersonnelScopeCode(accessArea.accessScopeType())));
        return Set.copyOf(scopeCodes);
    }

    private Set<String> resolveTemporaryRoleCodes(Long userId, Instant activeAt) {
        Set<String> roleCodes = new LinkedHashSet<>();
        accessAdministrationQueryService.listTemporaryRoleAssignments(
            new TemporaryRoleAssignmentAdminFilter(userId, null, activeAt)
        ).forEach(assignment -> roleCodes.add(userQueryService.findRoleById(assignment.roleId()).code()));
        return Set.copyOf(roleCodes);
    }

    private Set<String> resolveTemporaryAccessScopeCodes(Long userId, Instant activeAt) {
        Set<String> scopeCodes = new LinkedHashSet<>();
        accessAdministrationQueryService.listTemporaryAccessAreas(
            new TemporaryAccessAreaAdminFilter(userId, null, null, activeAt)
        ).forEach(accessArea -> scopeCodes.add(toPersonnelScopeCode(accessArea.accessScopeType())));
        return Set.copyOf(scopeCodes);
    }

    private String toPersonnelScopeCode(AccessScopeType accessScopeType) {
        return switch (accessScopeType) {
            case GLOBAL -> "SELF";
            case UNIT_ONLY, UNIT_SUBTREE -> "UNIT";
        };
    }
}
