package com.vladislav.training.platform.integration.personnel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vladislav.training.platform.access.domain.AccessScopeType;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
/**
 * Проверяет поведение {@code PersonnelExcelDirectMvpSnapshotIsolationDb}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class PersonnelExcelDirectMvpSnapshotIsolationDbTest extends AbstractPersonnelExcelDirectMvpDbIntegrationTest {

    @Test
    void directMvpFlowLeavesImportResultContentAndNotificationContoursUntouched() {
        var roleUser = saveRole("ROLE_USER");
        var unitType = saveUnitType("TEAM");
        var homeUnit = saveUnit(unitType.getId(), "Headquarters", "HQ");
        saveUnit(unitType.getId(), "Branch", "BRANCH");
        var user = saveUser("4001", "ext-4001", UserStatus.ACTIVE);
        savePrimaryHome(user.getId(), homeUnit.getId());
        saveRoleAssignment(user.getId(), roleUser.getId());
        saveUserAccessArea(user.getId(), null, AccessScopeType.GLOBAL);
        RuntimeCounts countsBefore = runtimeCounts();

        personnelWorkbookDryRunFacade.dryRun(workbook(row("4001", "ACTIVE", "BRANCH", "DEV")));
        personnelApplyService.apply(workbook(row("4001", "ACTIVE", "BRANCH", "DEV")));

        assertThat(runtimeCounts()).isEqualTo(countsBefore);
    }

    @Test
    void applyApiReturnsSafeDtoWithoutJpaLeak() throws Exception {
        var roleUser = saveRole("ROLE_USER");
        var unitType = saveUnitType("TEAM");
        var homeUnit = saveUnit(unitType.getId(), "Headquarters", "HQ");
        saveUnit(unitType.getId(), "Branch", "BRANCH");
        var user = saveUser("4002", "ext-4002", UserStatus.ACTIVE);
        savePrimaryHome(user.getId(), homeUnit.getId());
        saveRoleAssignment(user.getId(), roleUser.getId());
        saveUserAccessArea(user.getId(), null, AccessScopeType.GLOBAL);

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "personnel.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            workbook(row("4002", "ACTIVE", "BRANCH", "DEV"))
        );

        String response = mockMvc.perform(multipart("/api/v1/admin/import/personnel-excel/apply").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rows[0].outcomeCode").value("SUCCESS"))
            .andExpect(jsonPath("$.rows[0].appliedMutationTypes[0]").value("CLOSE_PRIMARY_ORG_ASSIGNMENT"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertThat(response).doesNotContain("stackTrace");
        assertThat(response).doesNotContain("hibernateLazyInitializer");
        assertThat(response).doesNotContain("AppUserEntity");
    }
}
