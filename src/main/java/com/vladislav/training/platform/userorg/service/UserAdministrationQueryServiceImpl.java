package com.vladislav.training.platform.userorg.service;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContext;
import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadScope;
import com.vladislav.training.platform.access.service.AccessReadType;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.userorg.domain.AppRole;
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment;
import com.vladislav.training.platform.userorg.domain.UserRoleAssignment;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса чтения {@code UserAdministrationQueryServiceImpl}.
 */
@Service
@Transactional(readOnly = true)
public class UserAdministrationQueryServiceImpl implements UserAdministrationQueryService {

    private static final String USER_ENTITY = "app_user";
    private static final String ROLE_ENTITY = "app_role";
    private static final String USER_ROLE_ASSIGNMENT_ENTITY = "user_role_assignment";
    private static final String USER_ORGANIZATION_ASSIGNMENT_ENTITY = "user_organization_assignment";

    private final AccessSpecificationPolicy accessSpecificationPolicy;
    private final AccessPolicyQueryContextResolver queryContextResolver;
    private final UserAdministrationPolicyReadService policyReadService;
    private final UserQueryService userQueryService;
    private final OrganizationPolicyReadFacade organizationPolicyReadFacade;

    public UserAdministrationQueryServiceImpl(
            AccessSpecificationPolicy accessSpecificationPolicy,
            AccessPolicyQueryContextResolver queryContextResolver,
            UserAdministrationPolicyReadService policyReadService,
            UserQueryService userQueryService,
            OrganizationPolicyReadFacade organizationPolicyReadFacade
    ) {
        this.accessSpecificationPolicy = accessSpecificationPolicy;
        this.queryContextResolver = queryContextResolver;
        this.policyReadService = policyReadService;
        this.userQueryService = userQueryService;
        this.organizationPolicyReadFacade = organizationPolicyReadFacade;
    }

    @Override
    public List<AppUser> listUsers(UserStatus status) {
        AccessPolicyQueryContext context = queryContextResolver.resolve(
                AccessReadArea.USER_ADMINISTRATION,
                AccessReadType.LIST,
                USER_ENTITY
        );
        AccessReadScope scope = ensureReadAllowed(context);
        return policyReadService.findUsersWithinScope(scope, status, context.effectiveAt());
    }

    @Override
    public UserAdministrationCard getUserCard(Long userId) {
        AccessPolicyQueryContext context = queryContextResolver.resolve(
                AccessReadArea.USER_ADMINISTRATION,
                AccessReadType.DETAIL,
                userId,
                null,
                USER_ENTITY
        );
        AccessReadScope scope = ensureReadAllowed(context);
        AppUser user = policyReadService.findUserByIdWithinScope(scope, context.effectiveAt(), userId)
                .orElseThrow(() -> new NotFoundException("User not found by id: " + userId));

        List<UserRoleAssignment> activeRoleAssignments =
                policyReadService.findActiveRoleAssignmentsByUserIdWithinScope(scope, context.effectiveAt(), userId);
        List<UserOrganizationAssignment> activeOrganizationAssignments =
                policyReadService.findActiveOrganizationAssignmentsByUserIdWithinScope(scope, context.effectiveAt(), userId);

        return new UserAdministrationCard(
                user,
                toRoleAssignmentViews(activeRoleAssignments),
                toOrganizationAssignmentViews(activeOrganizationAssignments, scope)
        );
    }

    @Override
    public List<UserAdministrationRoleAssignmentView> getRoleHistory(Long userId) {
        AccessPolicyQueryContext context = queryContextResolver.resolve(
                AccessReadArea.USER_ADMINISTRATION,
                AccessReadType.HISTORY,
                userId,
                null,
                USER_ROLE_ASSIGNMENT_ENTITY
        );
        AccessReadScope scope = ensureReadAllowed(context);
        if (policyReadService.findUserByIdWithinScope(scope, context.effectiveAt(), userId).isEmpty()) {
            throw new NotFoundException("User not found by id: " + userId);
        }
        List<UserRoleAssignment> assignments =
                policyReadService.findRoleAssignmentsByUserIdWithinScope(scope, context.effectiveAt(), userId);
        return toRoleAssignmentViews(assignments);
    }

    @Override
    public List<UserAdministrationOrganizationAssignmentView> getOrganizationAssignmentHistory(Long userId) {
        AccessPolicyQueryContext context = queryContextResolver.resolve(
                AccessReadArea.USER_ADMINISTRATION,
                AccessReadType.HISTORY,
                userId,
                null,
                USER_ORGANIZATION_ASSIGNMENT_ENTITY
        );
        AccessReadScope scope = ensureReadAllowed(context);
        if (policyReadService.findUserByIdWithinScope(scope, context.effectiveAt(), userId).isEmpty()) {
            throw new NotFoundException("User not found by id: " + userId);
        }
        List<UserOrganizationAssignment> assignments =
                policyReadService.findOrganizationAssignmentsByUserIdWithinScope(scope, context.effectiveAt(), userId);
        return toOrganizationAssignmentViews(assignments, scope);
    }

    @Override
    public List<AppRole> listRoles() {
        ensureRoleReferenceReadAllowed();
        return userQueryService.findAllRoles();
    }

    @Override
    public UserAdministrationRoleAssignmentView toRoleAssignmentView(UserRoleAssignment assignment) {
        Map<Long, AppRole> rolesById = loadRolesById(List.of(assignment));
        AppRole role = rolesById.get(assignment.roleId());
        if (role == null) {
            throw new NotFoundException("Role not found by id: " + assignment.roleId());
        }
        return new UserAdministrationRoleAssignmentView(
                assignment.id(),
                assignment.userId(),
                assignment.roleId(),
                role.code(),
                role.name(),
                assignment.validFrom(),
                assignment.validTo(),
                assignment.createdAt(),
                assignment.updatedAt()
        );
    }

    @Override
    public UserAdministrationOrganizationAssignmentView toOrganizationAssignmentView(UserOrganizationAssignment assignment) {
        OrganizationalUnit organizationalUnit = organizationPolicyReadFacade.findOrganizationalUnitById(
                assignment.organizationalUnitId()
        );
        return new UserAdministrationOrganizationAssignmentView(
                assignment.id(),
                assignment.userId(),
                assignment.organizationalUnitId(),
                organizationalUnit.name(),
                organizationalUnit.path(),
                assignment.assignmentType(),
                assignment.validFrom(),
                assignment.validTo(),
                assignment.createdAt(),
                assignment.updatedAt()
        );
    }

    private List<UserAdministrationRoleAssignmentView> toRoleAssignmentViews(List<UserRoleAssignment> assignments) {
        Map<Long, AppRole> rolesById = loadRolesById(assignments);
        return assignments.stream()
                .map(assignment -> {
                    AppRole role = rolesById.get(assignment.roleId());
                    if (role == null) {
                        throw new NotFoundException("Role not found by id: " + assignment.roleId());
                    }
                    return new UserAdministrationRoleAssignmentView(
                            assignment.id(),
                            assignment.userId(),
                            assignment.roleId(),
                            role.code(),
                            role.name(),
                            assignment.validFrom(),
                            assignment.validTo(),
                            assignment.createdAt(),
                            assignment.updatedAt()
                    );
                })
                .toList();
    }

    private List<UserAdministrationOrganizationAssignmentView> toOrganizationAssignmentViews(
            List<UserOrganizationAssignment> assignments,
            AccessReadScope scope
    ) {
        Map<Long, OrganizationalUnit> organizationalUnitsById = loadOrganizationalUnitsById(assignments, scope);
        return assignments.stream()
                .map(assignment -> {
                    OrganizationalUnit organizationalUnit = organizationalUnitsById.get(assignment.organizationalUnitId());
                    if (organizationalUnit == null) {
                        throw new NotFoundException(
                                "Organizational unit not found by id: " + assignment.organizationalUnitId()
                        );
                    }
                    return new UserAdministrationOrganizationAssignmentView(
                            assignment.id(),
                            assignment.userId(),
                            assignment.organizationalUnitId(),
                            organizationalUnit.name(),
                            organizationalUnit.path(),
                            assignment.assignmentType(),
                            assignment.validFrom(),
                            assignment.validTo(),
                            assignment.createdAt(),
                            assignment.updatedAt()
                    );
                })
                .toList();
    }

    private Map<Long, AppRole> loadRolesById(List<UserRoleAssignment> assignments) {
        if (assignments.isEmpty()) {
            return Map.of();
        }

        ensureRoleReferenceReadAllowed();

        Set<Long> roleIds = assignments.stream()
                .map(UserRoleAssignment::roleId)
                .collect(Collectors.toSet());

        return userQueryService.findAllRoles().stream()
                .filter(role -> roleIds.contains(role.id()))
                .collect(Collectors.toMap(AppRole::id, Function.identity()));
    }

    private Map<Long, OrganizationalUnit> loadOrganizationalUnitsById(
            List<UserOrganizationAssignment> assignments,
            AccessReadScope scope
    ) {
        if (assignments.isEmpty()) {
            return Map.of();
        }

        List<Long> organizationalUnitIds = assignments.stream()
                .map(UserOrganizationAssignment::organizationalUnitId)
                .distinct()
                .toList();

        return organizationPolicyReadFacade.findOrganizationalUnitsByIdsWithinScope(scope, organizationalUnitIds);
    }

    private void ensureRoleReferenceReadAllowed() {
        ensureReadAllowed(queryContextResolver.resolve(
                AccessReadArea.USER_ADMINISTRATION,
                AccessReadType.REFERENCE_READ,
                ROLE_ENTITY
        ));
    }

    private AccessReadScope ensureReadAllowed(AccessPolicyQueryContext context) {
        AccessReadScope scope = accessSpecificationPolicy.resolveReadScope(context);
        if (!scope.readAllowed()) {
            throw new PolicyViolationException("User administration read is forbidden by AccessSpecificationPolicy");
        }
        return scope;
    }
}