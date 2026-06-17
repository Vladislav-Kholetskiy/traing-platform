package com.vladislav.training.platform.integration.personnel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vladislav.training.platform.access.domain.AccessScopeType;
import com.vladislav.training.platform.integration.personnel.model.PersonnelIdentityResolution;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlan;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlanOutcomeCode;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlannedMutationType;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
/**
 * Проверяет {@code PersonnelExcelDirectMvpDryRunDb} на уровне интеграции.
 * Здесь важна совместная работа нескольких частей приложения.
 */
class PersonnelExcelDirectMvpDryRunDbIntegrationTest extends AbstractPersonnelExcelDirectMvpDbIntegrationTest {

    @Test
    void currentStateReaderResolvesKnownEmployeeNumberFromDatabase() {
        var roleUser = saveRole("ROLE_USER");
        var unitType = saveUnitType("TEAM");
        var homeUnit = saveUnit(unitType.getId(), "Headquarters", "HQ");
        var relationType = saveManagementRelationType("SUPERVISOR");
        var user = saveUser("1001", "ext-1001", UserStatus.ACTIVE);
        savePrimaryHome(user.getId(), homeUnit.getId());
        saveRoleAssignment(user.getId(), roleUser.getId());
        saveUserAccessArea(user.getId(), null, AccessScopeType.GLOBAL);
        saveManagementRelation(user.getId(), homeUnit.getId(), relationType.getId());

        PersonnelIdentityResolution resolution = ownerReadPersonnelCurrentStateReader.resolveIdentity(
            businessIntent("1001", null, "ACTIVE", "HQ", "DEV")
        );

        assertThat(resolution.resolved()).isTrue();
        assertThat(resolution.currentState()).isNotNull();
        assertThat(resolution.currentState().userId()).isEqualTo(user.getId());
        assertThat(resolution.currentState().userStatus()).isEqualTo("ACTIVE");
        assertThat(resolution.currentState().primaryHomeOrgUnitCode()).isEqualTo("HQ");
        assertThat(resolution.currentState().activeRoleCodes()).containsExactly("ROLE_USER");
        assertThat(resolution.currentState().activeAccessScopeCodes()).containsExactly("SELF");
        assertThat(resolution.currentState().managementRelationActive()).isTrue();
    }

    @Test
    void dryRunTransferBuildsPlanAndDoesNotMutateDatabase() {
        RuntimeCounts countsBefore = runtimeCounts();
        var roleUser = saveRole("ROLE_USER");
        var unitType = saveUnitType("TEAM");
        var homeUnit = saveUnit(unitType.getId(), "Headquarters", "HQ");
        var branchUnit = saveUnit(unitType.getId(), "Branch", "BRANCH");
        var user = saveUser("1002", "ext-1002", UserStatus.ACTIVE);
        savePrimaryHome(user.getId(), homeUnit.getId());
        saveRoleAssignment(user.getId(), roleUser.getId());
        saveUserAccessArea(user.getId(), null, AccessScopeType.GLOBAL);

        PersonnelPlan plan = personnelWorkbookDryRunFacade.dryRun(
            workbook(row("1002", "ACTIVE", "BRANCH", "DEV"))
        ).getFirst();

        assertThat(plan.outcomeCode()).isEqualTo(PersonnelPlanOutcomeCode.PLANNED_CHANGES);
        assertThat(plan.plannedMutations())
            .extracting(mutation -> mutation.mutationType())
            .containsExactly(
                PersonnelPlannedMutationType.CLOSE_PRIMARY_ORG_ASSIGNMENT,
                PersonnelPlannedMutationType.OPEN_PRIMARY_ORG_ASSIGNMENT
            );
        assertThat(userOrganizationAssignmentRepository.findActiveByUserId(user.getId(), NOW.plusSeconds(1)))
            .hasSize(1)
            .allMatch(assignment -> assignment.getOrganizationalUnitId().equals(homeUnit.getId()));
        assertThat(runtimeCounts()).isEqualTo(countsBefore);
        verify(userAdministrationCommandService, never()).replacePrimaryHomeUnit(any(), any(), any());
        verify(accessAdministrationCommandService, never()).assignUserAccessArea(any(), any(), any(), any());
    }

    @Test
    void dryRunNoChangeReturnsNoChangeWithoutOwnerMutation() {
        var roleUser = saveRole("ROLE_USER");
        var unitType = saveUnitType("TEAM");
        var homeUnit = saveUnit(unitType.getId(), "Headquarters", "HQ");
        var user = saveUser("1003", "ext-1003", UserStatus.ACTIVE);
        savePrimaryHome(user.getId(), homeUnit.getId());
        saveRoleAssignment(user.getId(), roleUser.getId());
        saveUserAccessArea(user.getId(), null, AccessScopeType.GLOBAL);

        PersonnelPlan plan = personnelWorkbookDryRunFacade.dryRun(
            workbook(row("1003", "ACTIVE", "HQ", "DEV"))
        ).getFirst();

        assertThat(plan.outcomeCode()).isEqualTo(PersonnelPlanOutcomeCode.NO_CHANGE);
        assertThat(plan.plannedMutations()).isEmpty();
        verify(userAdministrationCommandService, never()).replacePrimaryHomeUnit(any(), any(), any());
        verify(accessAdministrationCommandService, never()).assignUserAccessArea(any(), any(), any(), any());
    }

    @Test
    void dryRunApiResponseRemainsSafeAndExplicit() throws Exception {
        var roleUser = saveRole("ROLE_USER");
        var unitType = saveUnitType("TEAM");
        saveUnit(unitType.getId(), "Headquarters", "HQ");
        saveUnit(unitType.getId(), "Branch", "BRANCH");
        var user = saveUser("1004", "ext-1004", UserStatus.ACTIVE);
        savePrimaryHome(user.getId(), organizationalUnitRepository.findByPath("HQ").orElseThrow().getId());
        saveRoleAssignment(user.getId(), roleUser.getId());
        saveUserAccessArea(user.getId(), null, AccessScopeType.GLOBAL);

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "personnel.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            workbook(row("1004", "ACTIVE", "BRANCH", "DEV"))
        );

        String response = mockMvc.perform(multipart("/api/v1/admin/import/personnel-excel/dry-run").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rows[0].outcomeCode").value("PLANNED_CHANGES"))
            .andExpect(jsonPath("$.rows[0].plannedMutations[0].mutationType").value("CLOSE_PRIMARY_ORG_ASSIGNMENT"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertThat(response).doesNotContain("stackTrace");
        assertThat(response).doesNotContain("hibernateLazyInitializer");
        assertThat(response).doesNotContain("AppUserEntity");
    }
}
