package com.vladislav.training.platform.integration.personnel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.domain.AccessScopeType;
import com.vladislav.training.platform.access.domain.ManagementRelation;
import com.vladislav.training.platform.access.domain.TemporaryAccessArea;
import com.vladislav.training.platform.access.domain.TemporaryManagementDelegation;
import com.vladislav.training.platform.access.domain.TemporaryRoleAssignment;
import com.vladislav.training.platform.access.domain.UserAccessArea;
import com.vladislav.training.platform.access.service.AccessAdministrationQueryService;
import com.vladislav.training.platform.access.service.ManagementRelationAdminFilter;
import com.vladislav.training.platform.access.service.TemporaryAccessAreaAdminFilter;
import com.vladislav.training.platform.access.service.TemporaryManagementDelegationAdminFilter;
import com.vladislav.training.platform.access.service.TemporaryRoleAssignmentAdminFilter;
import com.vladislav.training.platform.access.service.UserAccessAreaAdminFilter;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.integration.personnel.model.PersonnelBusinessIntent;
import com.vladislav.training.platform.integration.personnel.model.PersonnelEmploymentAction;
import com.vladislav.training.platform.integration.personnel.model.PersonnelIdentityResolution;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPositionMapping;
import com.vladislav.training.platform.integration.personnel.service.OwnerReadPersonnelCurrentStateReader;
import com.vladislav.training.platform.userorg.domain.AppRole;
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment;
import com.vladislav.training.platform.userorg.domain.UserRoleAssignment;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.service.OrganizationQueryService;
import com.vladislav.training.platform.userorg.service.UserOrganizationAssignmentService;
import com.vladislav.training.platform.userorg.service.UserQueryService;
import com.vladislav.training.platform.userorg.service.UserRoleAssignmentService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет договорённости вокруг {@code PersonnelCurrentStateReaderPersistence}.
 * Тест помогает сохранить предсказуемое поведение.
 */
@ExtendWith(MockitoExtension.class)
class PersonnelCurrentStateReaderPersistenceContractTest {

    private static final Instant NOW = Instant.parse("2026-05-11T12:00:00Z");

    @Mock
    private UserQueryService userQueryService;
    @Mock
    private UserOrganizationAssignmentService userOrganizationAssignmentService;
    @Mock
    private UserRoleAssignmentService userRoleAssignmentService;
    @Mock
    private OrganizationQueryService organizationQueryService;
    @Mock
    private AccessAdministrationQueryService accessAdministrationQueryService;
    @Mock
    private UtcClock utcClock;

    private OwnerReadPersonnelCurrentStateReader reader;

    @BeforeEach
    void setUp() {
        reader = new OwnerReadPersonnelCurrentStateReader(
            userQueryService,
            userOrganizationAssignmentService,
            userRoleAssignmentService,
            organizationQueryService,
            accessAdministrationQueryService,
            utcClock
        );
    }

    @Test
    void knownEmployeeNumberResolvesToCurrentStateUsingCanonicalUserId() {
        when(utcClock.now()).thenReturn(NOW);
        AppUser user = user(10L, "1001", "ext-1", UserStatus.ACTIVE);
        when(userQueryService.findOptionalUserByEmployeeNumber("1001")).thenReturn(java.util.Optional.of(user));
        when(userOrganizationAssignmentService.findActiveOrganizationAssignmentsByUserId(10L, NOW)).thenReturn(List.of(
            organizationAssignment(200L, 10L, 501L, OrganizationAssignmentType.PRIMARY)
        ));
        when(organizationQueryService.findOrganizationalUnitById(501L)).thenReturn(orgUnit(501L, "HQ", "/department/npz/zavodoupravlenie"));
        when(userRoleAssignmentService.findActiveRoleAssignmentsByUserId(10L, NOW)).thenReturn(List.of(
            roleAssignment(301L, 10L, 701L),
            roleAssignment(302L, 10L, 702L)
        ));
        when(userQueryService.findRoleById(701L)).thenReturn(role(701L, "ROLE_USER"));
        when(userQueryService.findRoleById(702L)).thenReturn(role(702L, "ROLE_MANAGER"));
        when(accessAdministrationQueryService.listManagementRelations(new ManagementRelationAdminFilter(10L, null, null, NOW)))
            .thenReturn(List.of(managementRelation(401L, 10L, 501L)));
        when(accessAdministrationQueryService.listUserAccessAreas(new UserAccessAreaAdminFilter(10L, null, null, NOW)))
            .thenReturn(List.of(
                userAccessArea(501L, 10L, null, AccessScopeType.GLOBAL),
                userAccessArea(502L, 10L, 501L, AccessScopeType.UNIT_ONLY)
            ));
        when(accessAdministrationQueryService.listTemporaryRoleAssignments(new TemporaryRoleAssignmentAdminFilter(10L, null, NOW)))
            .thenReturn(List.of(temporaryRoleAssignment(601L, 10L, 702L)));
        when(accessAdministrationQueryService.listTemporaryAccessAreas(new TemporaryAccessAreaAdminFilter(10L, null, null, NOW)))
            .thenReturn(List.of(temporaryAccessArea(701L, 10L, 501L, AccessScopeType.UNIT_SUBTREE)));
        when(accessAdministrationQueryService.listTemporaryManagementDelegations(
            new TemporaryManagementDelegationAdminFilter(10L, null, null, NOW)
        )).thenReturn(List.of(temporaryManagementDelegation(801L, 10L, 501L)));

        PersonnelIdentityResolution resolution = reader.resolveIdentity(intent("1001", null));

        assertThat(resolution.resolved()).isTrue();
        assertThat(resolution.identityMismatch()).isFalse();
        assertThat(resolution.currentState()).isNotNull();
        assertThat(resolution.currentState().employeeNumber()).isEqualTo("1001");
        assertThat(resolution.currentState().externalId()).isEqualTo("ext-1");
        assertThat(resolution.currentState().userStatus()).isEqualTo("ACTIVE");
        assertThat(resolution.currentState().primaryHomeOrgUnitCode()).isEqualTo("HQ");
        assertThat(resolution.currentState().activeRoleCodes()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_MANAGER");
        assertThat(resolution.currentState().managementRelationActive()).isTrue();
        assertThat(resolution.currentState().activeAccessScopeCodes()).containsExactlyInAnyOrder("SELF", "UNIT");
        assertThat(resolution.currentState().activeTemporaryRoleCodes()).containsExactly("ROLE_MANAGER");
        assertThat(resolution.currentState().activeTemporaryAccessScopeCodes()).containsExactly("UNIT");
        assertThat(resolution.currentState().activeTemporaryManagementDelegation()).isTrue();

        verify(userOrganizationAssignmentService).findActiveOrganizationAssignmentsByUserId(10L, NOW);
        verify(userRoleAssignmentService).findActiveRoleAssignmentsByUserId(10L, NOW);
        verify(accessAdministrationQueryService).listManagementRelations(new ManagementRelationAdminFilter(10L, null, null, NOW));
        verify(accessAdministrationQueryService).listUserAccessAreas(new UserAccessAreaAdminFilter(10L, null, null, NOW));
        verify(accessAdministrationQueryService).listTemporaryRoleAssignments(new TemporaryRoleAssignmentAdminFilter(10L, null, NOW));
        verify(accessAdministrationQueryService).listTemporaryAccessAreas(new TemporaryAccessAreaAdminFilter(10L, null, null, NOW));
        verify(accessAdministrationQueryService).listTemporaryManagementDelegations(
            new TemporaryManagementDelegationAdminFilter(10L, null, null, NOW)
        );
    }

    @Test
    void unknownEmployeeNumberReturnsUnresolved() {
        when(userQueryService.findOptionalUserByEmployeeNumber("404")).thenReturn(java.util.Optional.empty());

        PersonnelIdentityResolution resolution = reader.resolveIdentity(intent("404", null));

        assertThat(resolution.resolved()).isFalse();
        assertThat(resolution.identityMismatch()).isFalse();
        assertThat(resolution.currentState()).isNull();
        assertThat(resolution.employeeNumber()).isEqualTo("404");
        verifyNoInteractions(userOrganizationAssignmentService, userRoleAssignmentService, organizationQueryService, accessAdministrationQueryService);
    }

    @Test
    void externalIdMismatchReturnsIdentityMismatch() {
        when(userQueryService.findOptionalUserByEmployeeNumber("1001")).thenReturn(
            java.util.Optional.of(user(10L, "1001", "ext-actual", UserStatus.ACTIVE))
        );

        PersonnelIdentityResolution resolution = reader.resolveIdentity(intent("1001", "ext-expected"));

        assertThat(resolution.resolved()).isFalse();
        assertThat(resolution.identityMismatch()).isTrue();
        assertThat(resolution.expectedExternalId()).isEqualTo("ext-expected");
        assertThat(resolution.actualExternalId()).isEqualTo("ext-actual");
        assertThat(resolution.currentState()).isNull();
        verify(userOrganizationAssignmentService, never()).findActiveOrganizationAssignmentsByUserId(10L, NOW);
        verifyNoInteractions(userRoleAssignmentService, organizationQueryService, accessAdministrationQueryService);
    }

    private PersonnelBusinessIntent intent(String employeeNumber, String externalId) {
        return new PersonnelBusinessIntent(
            2,
            employeeNumber,
            externalId,
            true,
            PersonnelEmploymentAction.ENSURE_ACTIVE,
            "ACTIVE",
            "HQ",
            new PersonnelPositionMapping("DEV", Set.of("ROLE_USER"), false, "SELF", false, true),
            null
        );
    }

    private AppUser user(Long id, String employeeNumber, String externalId, UserStatus status) {
        return new AppUser(
            id,
            employeeNumber,
            externalId,
            "Ivanov",
            "Ivan",
            "Ivanovich",
            status,
            NOW,
            NOW
        );
    }

    private AppRole role(Long id, String code) {
        return new AppRole(id, code, code, code, NOW, NOW);
    }

    private UserOrganizationAssignment organizationAssignment(
        Long id,
        Long userId,
        Long unitId,
        OrganizationAssignmentType type
    ) {
        return new UserOrganizationAssignment(id, userId, unitId, type, NOW, null, NOW, NOW);
    }

    private OrganizationalUnit orgUnit(Long id, String externalId, String path) {
        return new OrganizationalUnit(id, null, 900L, externalId, OrganizationalUnitStatus.ACTIVE, path, 0, externalId, NOW, NOW);
    }

    private UserRoleAssignment roleAssignment(Long id, Long userId, Long roleId) {
        return new UserRoleAssignment(id, userId, roleId, NOW, null, NOW, NOW);
    }

    private ManagementRelation managementRelation(Long id, Long userId, Long unitId) {
        return new ManagementRelation(id, userId, unitId, 1001L, NOW, null, NOW, NOW);
    }

    private UserAccessArea userAccessArea(Long id, Long userId, Long unitId, AccessScopeType scopeType) {
        return new UserAccessArea(id, userId, unitId, scopeType, NOW, null, NOW, NOW);
    }

    private TemporaryRoleAssignment temporaryRoleAssignment(Long id, Long userId, Long roleId) {
        return new TemporaryRoleAssignment(id, userId, roleId, NOW, null, NOW, NOW);
    }

    private TemporaryAccessArea temporaryAccessArea(Long id, Long userId, Long unitId, AccessScopeType scopeType) {
        return new TemporaryAccessArea(id, userId, unitId, scopeType, NOW, null, NOW, NOW);
    }

    private TemporaryManagementDelegation temporaryManagementDelegation(Long id, Long userId, Long unitId) {
        return new TemporaryManagementDelegation(id, userId, unitId, 1001L, NOW, null, NOW, NOW);
    }
}
