package com.vladislav.training.platform.integration.personnel;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.access.domain.AccessScopeType;
import com.vladislav.training.platform.integration.personnel.model.PersonnelApplyResult;
import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
/**
 * Проверяет {@code PersonnelExcelDirectMvpCreateUserApplyDb} на уровне интеграции.
 * Здесь важна совместная работа нескольких частей приложения.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PersonnelExcelDirectMvpCreateUserApplyDbIntegrationTest extends AbstractPersonnelExcelDirectMvpDbIntegrationTest {

    @Test
    void applyCreateUserPersistsUserExternalIdPrimaryHomeBaseRoleAndAccess() {
        saveRole("ROLE_USER");
        var unitType = saveUnitType("TEAM");
        var homeUnit = saveUnit(unitType.getId(), "Headquarters", "HQ");

        PersonnelApplyResult result = personnelApplyService.apply(
            workbook(rowWithExternalId("8001", "ext-8001", "ACTIVE", "HQ", "DEV"))
        );

        assertThat(result.rows()).singleElement().satisfies(row -> {
            assertThat(row.outcomeCode())
                .withFailMessage("Expected SUCCESS but got issues: %s", row.issues())
                .isEqualTo("SUCCESS");
            assertThat(row.decision()).isEqualTo("CREATE_USER");
            assertThat(row.createdUserId()).isNotNull();
        });

        var createdUser = appUserRepository.findByEmployeeNumber("8001").orElseThrow();
        assertThat(createdUser.getExternalId()).isEqualTo("ext-8001");
        assertThat(createdUser.getLastName()).isEqualTo("Ivanov");
        assertThat(createdUser.getFirstName()).isEqualTo("Ivan");
        assertThat(createdUser.getMiddleName()).isEqualTo("Ivanovich");
        assertThat(userOrganizationAssignmentRepository.findActiveByUserId(createdUser.getId(), NOW.plusSeconds(1)))
            .filteredOn(assignment -> assignment.getAssignmentType() == OrganizationAssignmentType.PRIMARY)
            .singleElement()
            .satisfies(assignment -> assertThat(assignment.getOrganizationalUnitId()).isEqualTo(homeUnit.getId()));
        assertThat(activeRoleCodes(createdUser.getId(), NOW.plusSeconds(1))).containsExactly("ROLE_USER");
        assertThat(userAccessAreaRepository.findActiveByUserId(createdUser.getId(), NOW.plusSeconds(1)))
            .singleElement()
            .satisfies(area -> {
                assertThat(area.getAccessScopeType()).isEqualTo(AccessScopeType.GLOBAL);
                assertThat(area.getOrganizationalUnitId()).isNull();
            });
        assertThat(managementRelationRepository.findActiveByUserId(createdUser.getId(), NOW.plusSeconds(1))).isEmpty();
    }

    @Test
    void applyCreateUserFailsClosedForDismissedInactiveAndTemporaryRows() {
        saveRole("ROLE_USER");
        var unitType = saveUnitType("TEAM");
        saveUnit(unitType.getId(), "Headquarters", "HQ");
        saveUnit(unitType.getId(), "Branch", "BRANCH");

        PersonnelApplyResult inactive = personnelApplyService.apply(workbook(row("8002", "INACTIVE", "HQ", "DEV")));
        PersonnelApplyResult dismissed = personnelApplyService.apply(workbook(row("8003", "DISMISSED", "HQ", "DEV")));
        PersonnelApplyResult temporary = personnelApplyService.apply(
            workbook(row("8004", "ACTIVE", "HQ", "DEV", "ACTING_HEAD", "BRANCH", "2026-05-12", "2026-05-31"))
        );

        assertThat(inactive.rows().getFirst().outcomeCode()).isEqualTo("FAILED");
        assertThat(dismissed.rows().getFirst().outcomeCode()).isEqualTo("FAILED");
        assertThat(temporary.rows().getFirst().outcomeCode()).isEqualTo("FAILED");
        assertThat(appUserRepository.findByEmployeeNumber("8002")).isEmpty();
        assertThat(appUserRepository.findByEmployeeNumber("8003")).isEmpty();
        assertThat(appUserRepository.findByEmployeeNumber("8004")).isEmpty();
    }

    @Test
    void rowLevelCreateTransactionRollsBackHalfCreatedUserWhenSubsequentOwnerMutationFails() {
        saveRole("ROLE_USER");

        PersonnelApplyResult result = personnelApplyService.apply(workbook(row("8005", "ACTIVE", "MISSING_UNIT", "DEV")));

        assertThat(result.rows().getFirst().outcomeCode()).isEqualTo("FAILED");
        assertThat(result.rows().getFirst().decision()).isEqualTo("FAIL_CLOSED");
        assertThat(appUserRepository.findByEmployeeNumber("8005")).isEmpty();
    }
}
