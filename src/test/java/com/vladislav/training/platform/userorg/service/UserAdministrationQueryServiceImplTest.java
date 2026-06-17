package com.vladislav.training.platform.userorg.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment;
import com.vladislav.training.platform.userorg.domain.UserRoleAssignment;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code UserAdministrationQueryServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class UserAdministrationQueryServiceImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-03-29T12:00:00Z");

    @Mock
    private AccessSpecificationPolicy accessSpecificationPolicy;
    @Mock
    private AccessPolicyQueryContextResolver queryContextResolver;
    @Mock
    private UserAdministrationPolicyReadService policyReadService;
    @Mock
    private UserQueryService userQueryService;
    @Mock
    private OrganizationPolicyReadFacade organizationPolicyReadFacade;

    @Test
    void listUsersRejectsDeniedPolicyBeforeRepositoryMaterialization() {
        UserAdministrationQueryServiceImpl service = new UserAdministrationQueryServiceImpl(
                accessSpecificationPolicy,
                queryContextResolver,
                policyReadService,
                userQueryService,
                organizationPolicyReadFacade
        );
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
                101L,
                AccessReadArea.USER_ADMINISTRATION,
                AccessReadType.LIST,
                FIXED_INSTANT,
                null,
                null,
                "app_user"
        );
        when(queryContextResolver.resolve(
                AccessReadArea.USER_ADMINISTRATION,
                AccessReadType.LIST,
                "app_user"
        )).thenReturn(context);
        when(accessSpecificationPolicy.resolveReadScope(context)).thenReturn(AccessReadScope.denyAll());

        assertThatThrownBy(() -> service.listUsers(UserStatus.ACTIVE))
                .isInstanceOf(PolicyViolationException.class)
                .hasMessageContaining("User administration read is forbidden");

        verifyNoInteractions(policyReadService, userQueryService, organizationPolicyReadFacade);
    }

    @Test
    void listUsersWithoutStatusUsesResolvedPolicyScopeWithoutAddingStatusConstraint() {
        UserAdministrationQueryServiceImpl service = new UserAdministrationQueryServiceImpl(
                accessSpecificationPolicy,
                queryContextResolver,
                policyReadService,
                userQueryService,
                organizationPolicyReadFacade
        );
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
                101L,
                AccessReadArea.USER_ADMINISTRATION,
                AccessReadType.LIST,
                FIXED_INSTANT,
                null,
                null,
                "app_user"
        );
        AccessReadScope scope = AccessReadScope.scoped(Set.of(30L), Set.of("/root/team"));
        AppUser visibleUser = new AppUser(
                55L,
                "EMP-55",
                null,
                "Last",
                "First",
                null,
                UserStatus.ACTIVE,
                FIXED_INSTANT,
                FIXED_INSTANT
        );
        when(queryContextResolver.resolve(
                AccessReadArea.USER_ADMINISTRATION,
                AccessReadType.LIST,
                "app_user"
        )).thenReturn(context);
        when(accessSpecificationPolicy.resolveReadScope(context)).thenReturn(scope);
        when(policyReadService.findUsersWithinScope(scope, null, FIXED_INSTANT)).thenReturn(List.of(visibleUser));

        assertThat(service.listUsers(null)).containsExactly(visibleUser);

        verify(policyReadService).findUsersWithinScope(scope, null, FIXED_INSTANT);
        verifyNoInteractions(userQueryService, organizationPolicyReadFacade);
    }

    @Test
    void getUserCardReturnsNotFoundWhenTargetOutsidePolicyScope() {
        UserAdministrationQueryServiceImpl service = new UserAdministrationQueryServiceImpl(
                accessSpecificationPolicy,
                queryContextResolver,
                policyReadService,
                userQueryService,
                organizationPolicyReadFacade
        );
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
                101L,
                AccessReadArea.USER_ADMINISTRATION,
                AccessReadType.DETAIL,
                FIXED_INSTANT,
                55L,
                null,
                "app_user"
        );
        AccessReadScope scope = AccessReadScope.scoped(Set.of(30L), Set.of());
        when(queryContextResolver.resolve(
                AccessReadArea.USER_ADMINISTRATION,
                AccessReadType.DETAIL,
                55L,
                null,
                "app_user"
        )).thenReturn(context);
        when(accessSpecificationPolicy.resolveReadScope(context)).thenReturn(scope);
        when(policyReadService.findUserByIdWithinScope(scope, FIXED_INSTANT, 55L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUserCard(55L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("55");

        verify(policyReadService).findUserByIdWithinScope(scope, FIXED_INSTANT, 55L);
        verify(policyReadService, never()).findActiveRoleAssignmentsByUserIdWithinScope(scope, FIXED_INSTANT, 55L);
        verify(policyReadService, never()).findActiveOrganizationAssignmentsByUserIdWithinScope(scope, FIXED_INSTANT, 55L);
        verifyNoInteractions(userQueryService, organizationPolicyReadFacade);
    }

    @Test
    void getUserCardUsesPolicyMaterializationThenSafeReferenceAndSafeOrgEnrichment() {
        UserAdministrationQueryServiceImpl service = new UserAdministrationQueryServiceImpl(
                accessSpecificationPolicy,
                queryContextResolver,
                policyReadService,
                userQueryService,
                organizationPolicyReadFacade
        );

        AccessPolicyQueryContext detailContext = new AccessPolicyQueryContext(
                101L,
                AccessReadArea.USER_ADMINISTRATION,
                AccessReadType.DETAIL,
                FIXED_INSTANT,
                55L,
                null,
                "app_user"
        );
        AccessPolicyQueryContext roleReferenceContext = new AccessPolicyQueryContext(
                101L,
                AccessReadArea.USER_ADMINISTRATION,
                AccessReadType.REFERENCE_READ,
                FIXED_INSTANT,
                null,
                null,
                "app_role"
        );

        AccessReadScope scope = AccessReadScope.fullAccess();

        AppUser user = new AppUser(
                55L,
                "EMP-55",
                null,
                "Last",
                "First",
                null,
                UserStatus.ACTIVE,
                FIXED_INSTANT,
                FIXED_INSTANT
        );
        UserRoleAssignment roleAssignment = new UserRoleAssignment(
                5L,
                55L,
                900L,
                FIXED_INSTANT.minusSeconds(3600),
                null,
                FIXED_INSTANT,
                FIXED_INSTANT
        );
        UserOrganizationAssignment organizationAssignment = new UserOrganizationAssignment(
                6L,
                55L,
                30L,
                OrganizationAssignmentType.PRIMARY,
                FIXED_INSTANT.minusSeconds(3600),
                null,
                FIXED_INSTANT,
                FIXED_INSTANT
        );
        AppRole role = new AppRole(
                900L,
                "ADMIN",
                "Administrator",
                null,
                FIXED_INSTANT,
                FIXED_INSTANT
        );
        OrganizationalUnit organizationalUnit = new OrganizationalUnit(
                30L,
                1L,
                10L,
                "Branch",
                OrganizationalUnitStatus.ACTIVE,
                "/root/branch",
                1,
                null,
                FIXED_INSTANT,
                FIXED_INSTANT
        );

        when(queryContextResolver.resolve(
                AccessReadArea.USER_ADMINISTRATION,
                AccessReadType.DETAIL,
                55L,
                null,
                "app_user"
        )).thenReturn(detailContext);
        when(queryContextResolver.resolve(
                AccessReadArea.USER_ADMINISTRATION,
                AccessReadType.REFERENCE_READ,
                "app_role"
        )).thenReturn(roleReferenceContext);

        when(accessSpecificationPolicy.resolveReadScope(detailContext)).thenReturn(scope);
        when(accessSpecificationPolicy.resolveReadScope(roleReferenceContext)).thenReturn(AccessReadScope.fullAccess());

        when(policyReadService.findUserByIdWithinScope(scope, FIXED_INSTANT, 55L)).thenReturn(Optional.of(user));
        when(policyReadService.findActiveRoleAssignmentsByUserIdWithinScope(scope, FIXED_INSTANT, 55L))
                .thenReturn(List.of(roleAssignment));
        when(policyReadService.findActiveOrganizationAssignmentsByUserIdWithinScope(scope, FIXED_INSTANT, 55L))
                .thenReturn(List.of(organizationAssignment));
        when(userQueryService.findAllRoles()).thenReturn(List.of(role));
        when(organizationPolicyReadFacade.findOrganizationalUnitsByIdsWithinScope(scope, List.of(30L)))
            .thenReturn(Map.of(30L, organizationalUnit));

        UserAdministrationCard card = service.getUserCard(55L);

        assertThat(card.user()).isEqualTo(user);
        assertThat(card.activeRoleAssignments()).containsExactly(
                new UserAdministrationRoleAssignmentView(
                        5L,
                        55L,
                        900L,
                        "ADMIN",
                        "Administrator",
                        FIXED_INSTANT.minusSeconds(3600),
                        null,
                        FIXED_INSTANT,
                        FIXED_INSTANT
                )
        );
        assertThat(card.activeOrganizationAssignments()).containsExactly(
                new UserAdministrationOrganizationAssignmentView(
                        6L,
                        55L,
                        30L,
                        "Branch",
                        "/root/branch",
                        OrganizationAssignmentType.PRIMARY,
                        FIXED_INSTANT.minusSeconds(3600),
                        null,
                        FIXED_INSTANT,
                        FIXED_INSTANT
                )
        );

        verify(policyReadService).findUserByIdWithinScope(scope, FIXED_INSTANT, 55L);
        verify(policyReadService).findActiveRoleAssignmentsByUserIdWithinScope(scope, FIXED_INSTANT, 55L);
        verify(policyReadService).findActiveOrganizationAssignmentsByUserIdWithinScope(scope, FIXED_INSTANT, 55L);
        verify(userQueryService).findAllRoles();
        verify(organizationPolicyReadFacade).findOrganizationalUnitsByIdsWithinScope(scope, List.of(30L));
    }

    @Test
    void roleHistoryUsesPolicyScopedMaterializationThenSafeRoleReferenceRead() {
        UserAdministrationQueryServiceImpl service = new UserAdministrationQueryServiceImpl(
                accessSpecificationPolicy,
                queryContextResolver,
                policyReadService,
                userQueryService,
                organizationPolicyReadFacade
        );
        AccessPolicyQueryContext historyContext = new AccessPolicyQueryContext(
                101L,
                AccessReadArea.USER_ADMINISTRATION,
                AccessReadType.HISTORY,
                FIXED_INSTANT,
                55L,
                null,
                "user_role_assignment"
        );
        AccessPolicyQueryContext roleReferenceContext = new AccessPolicyQueryContext(
                101L,
                AccessReadArea.USER_ADMINISTRATION,
                AccessReadType.REFERENCE_READ,
                FIXED_INSTANT,
                null,
                null,
                "app_role"
        );
        AccessReadScope scope = AccessReadScope.fullAccess();

        AppUser user = new AppUser(
                55L,
                "EMP-55",
                null,
                "Last",
                "First",
                null,
                UserStatus.ACTIVE,
                FIXED_INSTANT,
                FIXED_INSTANT
        );
        UserRoleAssignment assignment = new UserRoleAssignment(
                5L,
                55L,
                900L,
                FIXED_INSTANT.minusSeconds(3600),
                null,
                FIXED_INSTANT,
                FIXED_INSTANT
        );
        AppRole role = new AppRole(
                900L,
                "ADMIN",
                "Administrator",
                null,
                FIXED_INSTANT,
                FIXED_INSTANT
        );

        when(queryContextResolver.resolve(
                AccessReadArea.USER_ADMINISTRATION,
                AccessReadType.HISTORY,
                55L,
                null,
                "user_role_assignment"
        )).thenReturn(historyContext);
        when(queryContextResolver.resolve(
                AccessReadArea.USER_ADMINISTRATION,
                AccessReadType.REFERENCE_READ,
                "app_role"
        )).thenReturn(roleReferenceContext);
        when(accessSpecificationPolicy.resolveReadScope(historyContext)).thenReturn(scope);
        when(accessSpecificationPolicy.resolveReadScope(roleReferenceContext)).thenReturn(AccessReadScope.fullAccess());
        when(policyReadService.findUserByIdWithinScope(scope, FIXED_INSTANT, 55L)).thenReturn(Optional.of(user));
        when(policyReadService.findRoleAssignmentsByUserIdWithinScope(scope, FIXED_INSTANT, 55L))
                .thenReturn(List.of(assignment));
        when(userQueryService.findAllRoles()).thenReturn(List.of(role));

        assertThat(service.getRoleHistory(55L)).containsExactly(
                new UserAdministrationRoleAssignmentView(
                        5L,
                        55L,
                        900L,
                        "ADMIN",
                        "Administrator",
                        FIXED_INSTANT.minusSeconds(3600),
                        null,
                        FIXED_INSTANT,
                        FIXED_INSTANT
                )
        );

        verify(policyReadService).findRoleAssignmentsByUserIdWithinScope(scope, FIXED_INSTANT, 55L);
        verify(userQueryService).findAllRoles();
        verifyNoInteractions(organizationPolicyReadFacade);
    }

    @Test
    void organizationHistoryUsesPolicyScopedMaterializationThenSafeOrgEnrichment() {
        UserAdministrationQueryServiceImpl service = new UserAdministrationQueryServiceImpl(
                accessSpecificationPolicy,
                queryContextResolver,
                policyReadService,
                userQueryService,
                organizationPolicyReadFacade
        );
        AccessPolicyQueryContext historyContext = new AccessPolicyQueryContext(
                101L,
                AccessReadArea.USER_ADMINISTRATION,
                AccessReadType.HISTORY,
                FIXED_INSTANT,
                55L,
                null,
                "user_organization_assignment"
        );
        AccessReadScope scope = AccessReadScope.fullAccess();

        AppUser user = new AppUser(
                55L,
                "EMP-55",
                null,
                "Last",
                "First",
                null,
                UserStatus.ACTIVE,
                FIXED_INSTANT,
                FIXED_INSTANT
        );
        UserOrganizationAssignment assignment = new UserOrganizationAssignment(
                5L,
                55L,
                30L,
                OrganizationAssignmentType.PRIMARY,
                FIXED_INSTANT.minusSeconds(3600),
                null,
                FIXED_INSTANT,
                FIXED_INSTANT
        );
        OrganizationalUnit organizationalUnit = new OrganizationalUnit(
                30L,
                1L,
                10L,
                "Branch",
                OrganizationalUnitStatus.ACTIVE,
                "/root/branch",
                1,
                null,
                FIXED_INSTANT,
                FIXED_INSTANT
        );

        when(queryContextResolver.resolve(
                AccessReadArea.USER_ADMINISTRATION,
                AccessReadType.HISTORY,
                55L,
                null,
                "user_organization_assignment"
        )).thenReturn(historyContext);
        when(accessSpecificationPolicy.resolveReadScope(historyContext)).thenReturn(scope);
        when(policyReadService.findUserByIdWithinScope(scope, FIXED_INSTANT, 55L)).thenReturn(Optional.of(user));
        when(policyReadService.findOrganizationAssignmentsByUserIdWithinScope(scope, FIXED_INSTANT, 55L))
                .thenReturn(List.of(assignment));
        when(organizationPolicyReadFacade.findOrganizationalUnitsByIdsWithinScope(scope, List.of(30L)))
            .thenReturn(Map.of(30L, organizationalUnit));

        assertThat(service.getOrganizationAssignmentHistory(55L)).containsExactly(
                new UserAdministrationOrganizationAssignmentView(
                        5L,
                        55L,
                        30L,
                        "Branch",
                        "/root/branch",
                        OrganizationAssignmentType.PRIMARY,
                        FIXED_INSTANT.minusSeconds(3600),
                        null,
                        FIXED_INSTANT,
                        FIXED_INSTANT
                )
        );

        verify(policyReadService).findOrganizationAssignmentsByUserIdWithinScope(scope, FIXED_INSTANT, 55L);
        verify(organizationPolicyReadFacade).findOrganizationalUnitsByIdsWithinScope(scope, List.of(30L));
        verifyNoInteractions(userQueryService);
    }

    @Test
    void roleReferenceReadRejectsDeniedPolicyBeforeDictionaryLoad() {
        UserAdministrationQueryServiceImpl service = new UserAdministrationQueryServiceImpl(
                accessSpecificationPolicy,
                queryContextResolver,
                policyReadService,
                userQueryService,
                organizationPolicyReadFacade
        );
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
                101L,
                AccessReadArea.USER_ADMINISTRATION,
                AccessReadType.REFERENCE_READ,
                FIXED_INSTANT,
                null,
                null,
                "app_role"
        );
        when(queryContextResolver.resolve(
                AccessReadArea.USER_ADMINISTRATION,
                AccessReadType.REFERENCE_READ,
                "app_role"
        )).thenReturn(context);
        when(accessSpecificationPolicy.resolveReadScope(context)).thenReturn(AccessReadScope.denyAll());

        assertThatThrownBy(service::listRoles)
                .isInstanceOf(PolicyViolationException.class)
                .hasMessageContaining("User administration read is forbidden");

        verifyNoInteractions(policyReadService, userQueryService, organizationPolicyReadFacade);
    }

    @Test
    void roleReferenceReadUsesSafeReferenceContourAfterPolicyCheck() {
        UserAdministrationQueryServiceImpl service = new UserAdministrationQueryServiceImpl(
                accessSpecificationPolicy,
                queryContextResolver,
                policyReadService,
                userQueryService,
                organizationPolicyReadFacade
        );
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
                101L,
                AccessReadArea.USER_ADMINISTRATION,
                AccessReadType.REFERENCE_READ,
                FIXED_INSTANT,
                null,
                null,
                "app_role"
        );
        AppRole role = new AppRole(100L, "ADMIN", "Admin", null, FIXED_INSTANT, FIXED_INSTANT);
        when(queryContextResolver.resolve(
                AccessReadArea.USER_ADMINISTRATION,
                AccessReadType.REFERENCE_READ,
                "app_role"
        )).thenReturn(context);
        when(accessSpecificationPolicy.resolveReadScope(context)).thenReturn(AccessReadScope.fullAccess());
        when(userQueryService.findAllRoles()).thenReturn(List.of(role));

        assertThat(service.listRoles()).containsExactly(role);
        verify(userQueryService).findAllRoles();
        verifyNoInteractions(policyReadService, organizationPolicyReadFacade);
    }
}
