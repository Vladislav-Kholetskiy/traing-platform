package com.vladislav.training.platform.integration.personnel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.vladislav.training.platform.access.domain.AccessScopeType;
import com.vladislav.training.platform.integration.personnel.model.PersonnelApplyResult;
import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import java.util.Comparator;
import org.junit.jupiter.api.Test;
/**
 * Проверяет {@code PersonnelExcelDirectMvpApplyDb} на уровне интеграции.
 * Здесь важна совместная работа нескольких частей приложения.
 */
class PersonnelExcelDirectMvpApplyDbIntegrationTest extends AbstractPersonnelExcelDirectMvpDbIntegrationTest {

    @Test
    void applyTransferChangesPrimaryHomeThroughOwnerServicePath() {
        var roleUser = saveRole("ROLE_USER");
        var unitType = saveUnitType("TEAM");
        var homeUnit = saveUnit(unitType.getId(), "Headquarters", "HQ");
        var branchUnit = saveUnit(unitType.getId(), "Branch", "BRANCH");
        var user = saveUser("2001", "ext-2001", UserStatus.ACTIVE);
        savePrimaryHome(user.getId(), homeUnit.getId());
        saveRoleAssignment(user.getId(), roleUser.getId());
        saveUserAccessArea(user.getId(), null, AccessScopeType.GLOBAL);

        PersonnelApplyResult result = personnelApplyService.apply(workbook(row("2001", "ACTIVE", "BRANCH", "DEV")));

        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().getFirst().outcomeCode()).isEqualTo("SUCCESS");
        verify(userAdministrationCommandService).replacePrimaryHomeUnit(eq(user.getId()), eq(branchUnit.getId()), eq(NOW));
        assertThat(userOrganizationAssignmentRepository.findActiveByUserId(user.getId(), NOW.plusSeconds(1)))
            .filteredOn(assignment -> assignment.getAssignmentType() == OrganizationAssignmentType.PRIMARY)
            .hasSize(1)
            .allMatch(assignment -> assignment.getOrganizationalUnitId().equals(branchUnit.getId()));
    }

    @Test
    void applyNoChangeReturnsNoChangeAndDoesNotCallOwnerMutationPath() {
        var roleUser = saveRole("ROLE_USER");
        var unitType = saveUnitType("TEAM");
        var homeUnit = saveUnit(unitType.getId(), "Headquarters", "HQ");
        var user = saveUser("2002", "ext-2002", UserStatus.ACTIVE);
        savePrimaryHome(user.getId(), homeUnit.getId());
        saveRoleAssignment(user.getId(), roleUser.getId());
        saveUserAccessArea(user.getId(), null, AccessScopeType.GLOBAL);

        PersonnelApplyResult result = personnelApplyService.apply(workbook(row("2002", "ACTIVE", "HQ", "DEV")));

        assertThat(result.rows().getFirst().outcomeCode()).isEqualTo("NO_CHANGE");
        verify(personnelOwnerMutationExecutor, never()).execute(any(), any());
        verify(userAdministrationCommandService, never()).replacePrimaryHomeUnit(any(), any(), any());
        verify(accessAdministrationCommandService, never()).assignUserAccessArea(any(), any(), any(), any());
    }

    @Test
    void applyBasePositionDeltaUsesOwnerServicesForRoleAccessAndManagement() {
        var roleUser = saveRole("ROLE_USER");
        var roleManager = saveRole("ROLE_MANAGER");
        var unitType = saveUnitType("TEAM");
        var homeUnit = saveUnit(unitType.getId(), "Headquarters", "HQ");
        var relationType = saveManagementRelationType("SUPERVISOR");
        var user = saveUser("2003", "ext-2003", UserStatus.ACTIVE);
        savePrimaryHome(user.getId(), homeUnit.getId());
        saveRoleAssignment(user.getId(), roleUser.getId());
        saveUserAccessArea(user.getId(), null, AccessScopeType.GLOBAL);

        PersonnelApplyResult result = personnelApplyService.apply(workbook(row("2003", "ACTIVE", "HQ", "HEAD")));

        assertThat(result.rows().getFirst().outcomeCode()).isEqualTo("SUCCESS");
        verify(userAdministrationCommandService).assignRole(eq(user.getId()), eq(roleManager.getId()), eq(NOW));
        verify(accessAdministrationCommandService).assignUserAccessArea(eq(user.getId()), eq(homeUnit.getId()), eq(AccessScopeType.UNIT_ONLY), eq(NOW));
        verify(accessAdministrationCommandService).assignManagementRelation(eq(user.getId()), eq(homeUnit.getId()), eq(relationType.getId()), eq(NOW));
        assertThat(activeRoleCodes(user.getId(), NOW.plusSeconds(1))).containsExactlyInAnyOrder("ROLE_USER", "ROLE_MANAGER");
        assertThat(userAccessAreaRepository.findActiveByUserId(user.getId(), NOW.plusSeconds(1)))
            .singleElement()
            .satisfies(area -> {
                assertThat(area.getAccessScopeType()).isEqualTo(AccessScopeType.UNIT_ONLY);
                assertThat(area.getOrganizationalUnitId()).isEqualTo(homeUnit.getId());
            });
        assertThat(managementRelationRepository.findActiveByUserId(user.getId(), NOW.plusSeconds(1))).hasSize(1);
    }

    @Test
    void applyDismissalClosesAuthorityCascadeAndEndsInactive() {
        var roleUser = saveRole("ROLE_USER");
        var roleManager = saveRole("ROLE_MANAGER");
        var unitType = saveUnitType("TEAM");
        var homeUnit = saveUnit(unitType.getId(), "Headquarters", "HQ");
        var relationType = saveManagementRelationType("SUPERVISOR");
        var user = saveUser("2004", "ext-2004", UserStatus.ACTIVE);
        savePrimaryHome(user.getId(), homeUnit.getId());
        saveRoleAssignment(user.getId(), roleUser.getId());
        saveRoleAssignment(user.getId(), roleManager.getId());
        saveUserAccessArea(user.getId(), homeUnit.getId(), AccessScopeType.UNIT_ONLY);
        saveManagementRelation(user.getId(), homeUnit.getId(), relationType.getId());
        saveTemporaryRoleAssignment(user.getId(), roleManager.getId());
        saveTemporaryAccessArea(user.getId(), homeUnit.getId(), AccessScopeType.UNIT_ONLY);
        saveTemporaryManagementDelegation(user.getId(), homeUnit.getId(), relationType.getId());

        PersonnelApplyResult result = personnelApplyService.apply(workbook(row("2004", "DISMISSED", "HQ", "HEAD")));

        assertThat(result.rows().getFirst().outcomeCode()).isEqualTo("SUCCESS");
        assertThat(result.rows().getFirst().targetUserStatus()).isEqualTo("INACTIVE");
        verify(userAdministrationCommandService).deactivateUser(user.getId());
        assertThat(appUserRepository.findById(user.getId()).orElseThrow().getStatus()).isEqualTo(UserStatus.INACTIVE);
        assertThat(userRoleAssignmentRepository.findActiveByUserId(user.getId(), NOW.plusSeconds(1))).isEmpty();
        assertThat(userOrganizationAssignmentRepository.findActiveByUserId(user.getId(), NOW.plusSeconds(1))).isEmpty();
        assertThat(managementRelationRepository.findActiveByUserId(user.getId(), NOW.plusSeconds(1))).isEmpty();
        assertThat(userAccessAreaRepository.findActiveByUserId(user.getId(), NOW.plusSeconds(1))).isEmpty();
        assertThat(temporaryRoleAssignmentRepository.findActiveByUserId(user.getId(), NOW.plusSeconds(1))).isEmpty();
        assertThat(temporaryAccessAreaRepository.findActiveByUserId(user.getId(), NOW.plusSeconds(1))).isEmpty();
        assertThat(temporaryManagementDelegationRepository.findActiveByUserId(user.getId(), NOW.plusSeconds(1))).isEmpty();
    }

    @Test
    void applyTemporaryManagerialAppointmentCreatesDedicatedTemporaryDelegation() {
        var roleUser = saveRole("ROLE_USER");
        var roleManager = saveRole("ROLE_MANAGER");
        var unitType = saveUnitType("TEAM");
        var homeUnit = saveUnit(unitType.getId(), "Headquarters", "HQ");
        var branchUnit = saveUnit(unitType.getId(), "Branch", "BRANCH");
        var relationType = saveManagementRelationType("SUPERVISOR");
        var user = saveUser("2005", "ext-2005", UserStatus.ACTIVE);
        savePrimaryHome(user.getId(), homeUnit.getId());
        saveRoleAssignment(user.getId(), roleUser.getId());
        saveUserAccessArea(user.getId(), null, AccessScopeType.GLOBAL);

        PersonnelApplyResult result = personnelApplyService.apply(workbook(
            row("2005", "ACTIVE", "HQ", "DEV", "ACTING_HEAD", "BRANCH", "2026-05-12", "2026-05-31")
        ));

        assertThat(result.rows().getFirst().outcomeCode()).isEqualTo("SUCCESS");
        verify(accessAdministrationCommandService).assignTemporaryRoleAssignment(eq(user.getId()), eq(roleManager.getId()), eq(NOW));
        verify(accessAdministrationCommandService).assignTemporaryAccessArea(
            eq(user.getId()),
            eq(branchUnit.getId()),
            eq(AccessScopeType.UNIT_ONLY),
            eq(NOW)
        );
        verify(accessAdministrationCommandService).assignTemporaryManagementDelegation(
            eq(user.getId()),
            eq(branchUnit.getId()),
            eq(relationType.getId()),
            eq(NOW)
        );
        assertThat(temporaryRoleAssignmentRepository.findActiveByUserId(user.getId(), NOW.plusSeconds(1))).hasSize(1);
        assertThat(temporaryAccessAreaRepository.findActiveByUserId(user.getId(), NOW.plusSeconds(1)))
            .singleElement()
            .satisfies(area -> assertThat(area.getOrganizationalUnitId()).isEqualTo(branchUnit.getId()));
        assertThat(temporaryManagementDelegationRepository.findActiveByUserId(user.getId(), NOW.plusSeconds(1)))
            .singleElement()
            .satisfies(delegation -> assertThat(delegation.getOrganizationalUnitId()).isEqualTo(branchUnit.getId()));
    }

    @Test
    void unsupportedTemporaryReplacementRemainsFailClosed() {
        var roleUser = saveRole("ROLE_USER");
        var unitType = saveUnitType("TEAM");
        var homeUnit = saveUnit(unitType.getId(), "Headquarters", "HQ");
        var branchUnit = saveUnit(unitType.getId(), "Branch", "BRANCH");
        var user = saveUser("2006", "ext-2006", UserStatus.ACTIVE);
        savePrimaryHome(user.getId(), homeUnit.getId());
        saveRoleAssignment(user.getId(), roleUser.getId());
        saveUserAccessArea(user.getId(), null, AccessScopeType.GLOBAL);

        PersonnelApplyResult result = personnelApplyService.apply(workbook(
            row("2006", "ACTIVE", "HQ", "DEV", "DUAL_APPOINTMENT", "BRANCH", "2026-05-12", "2026-05-31")
        ));

        assertThat(result.rows().getFirst().outcomeCode()).isEqualTo("FAILED");
        verify(userAdministrationCommandService, never()).replacePrimaryHomeUnit(any(), any(), any());
        verify(accessAdministrationCommandService, never()).assignTemporaryManagementDelegation(any(), any(), any(), any());
        assertThat(temporaryManagementDelegationRepository.findAllByUserIdOrderByValidFromDescIdDesc(user.getId())).isEmpty();
        assertThat(branchUnit.getId()).isNotNull();
    }
}
