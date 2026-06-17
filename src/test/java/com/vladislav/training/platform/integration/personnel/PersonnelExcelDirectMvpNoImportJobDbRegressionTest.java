package com.vladislav.training.platform.integration.personnel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.vladislav.training.platform.access.domain.AccessScopeType;
import com.vladislav.training.platform.integration.personnel.model.PersonnelApplyResult;
import com.vladislav.training.platform.integration.personnel.model.PersonnelIdentityResolution;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlan;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlanOutcomeCode;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code PersonnelExcelDirectMvpNoImportJobDb} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class PersonnelExcelDirectMvpNoImportJobDbRegressionTest extends AbstractPersonnelExcelDirectMvpDbIntegrationTest {

    @Test
    void dryRunUnknownActiveEmployeeBuildsCreatePlanWithoutDbMutationOrImportJobs() {
        RuntimeCounts countsBefore = runtimeCounts();

        PersonnelPlan plan = personnelWorkbookDryRunFacade.dryRun(
            workbook(row("404", "ACTIVE", "HQ", "DEV"))
        ).getFirst();

        assertThat(plan.outcomeCode()).isEqualTo(PersonnelPlanOutcomeCode.CREATE_USER_REQUIRED);
        assertThat(plan.decision()).isEqualTo(com.vladislav.training.platform.integration.personnel.model.PersonnelRowDecision.CREATE_USER);
        assertThat(runtimeCounts()).isEqualTo(countsBefore);
        verify(userAdministrationCommandService, never()).replacePrimaryHomeUnit(any(), any(), any());
        verify(accessAdministrationCommandService, never()).assignUserAccessArea(any(), any(), any(), any());
    }

    @Test
    void externalIdMismatchReturnsIdentityMismatchAndDoesNotMutateDatabase() {
        RuntimeCounts countsBefore = runtimeCounts();
        var roleUser = saveRole("ROLE_USER");
        var unitType = saveUnitType("TEAM");
        var homeUnit = saveUnit(unitType.getId(), "Headquarters", "HQ");
        var user = saveUser("3001", "ext-actual", UserStatus.ACTIVE);
        savePrimaryHome(user.getId(), homeUnit.getId());
        saveRoleAssignment(user.getId(), roleUser.getId());
        saveUserAccessArea(user.getId(), null, AccessScopeType.GLOBAL);

        PersonnelIdentityResolution resolution = ownerReadPersonnelCurrentStateReader.resolveIdentity(
            businessIntent("3001", "ext-expected", "ACTIVE", "HQ", "DEV")
        );

        assertThat(resolution.resolved()).isFalse();
        assertThat(resolution.identityMismatch()).isTrue();
        assertThat(resolution.expectedExternalId()).isEqualTo("ext-expected");
        assertThat(resolution.actualExternalId()).isEqualTo("ext-actual");
        assertThat(runtimeCounts()).isEqualTo(countsBefore);
        verify(userAdministrationCommandService, never()).replacePrimaryHomeUnit(any(), any(), any());
        verify(accessAdministrationCommandService, never()).assignUserAccessArea(any(), any(), any(), any());
    }

    @Test
    void applyUnknownActiveEmployeeCreatesUserThroughOwnerPathAndLeavesImportJobTablesUntouched() {
        RuntimeCounts countsBefore = runtimeCounts();
        saveRole("ROLE_USER");
        var unitType = saveUnitType("TEAM");
        saveUnit(unitType.getId(), "Headquarters", "HQ");

        PersonnelApplyResult result = personnelApplyService.apply(workbook(row("404", "ACTIVE", "HQ", "DEV")));

        assertThat(result.rows().getFirst().outcomeCode()).isEqualTo("SUCCESS");
        assertThat(result.rows().getFirst().decision()).isEqualTo("CREATE_USER");
        assertThat(result.rows().getFirst().createdUserId()).isNotNull();
        assertThat(appUserRepository.findByEmployeeNumber("404")).isPresent();
        assertThat(importJobRepository.count()).isEqualTo(countsBefore.importJobs());
        assertThat(importJobItemRepository.count()).isEqualTo(countsBefore.importJobItems());
        verify(personnelOwnerMutationExecutor).execute(any(), any());
        verify(accessAdministrationCommandService).assignUserAccessArea(any(), any(), any(), any());
    }
}
