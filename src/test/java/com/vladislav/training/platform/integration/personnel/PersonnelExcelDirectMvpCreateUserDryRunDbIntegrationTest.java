package com.vladislav.training.platform.integration.personnel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.vladislav.training.platform.integration.personnel.model.PersonnelPlan;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlanOutcomeCode;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlannedMutationType;
import org.junit.jupiter.api.Test;
/**
 * Проверяет {@code PersonnelExcelDirectMvpCreateUserDryRunDb} на уровне интеграции.
 * Здесь важна совместная работа нескольких частей приложения.
 */
class PersonnelExcelDirectMvpCreateUserDryRunDbIntegrationTest extends AbstractPersonnelExcelDirectMvpDbIntegrationTest {

    @Test
    void dryRunUnknownEmployeeReturnsCreateUserAndDoesNotCreateAppUser() {
        RuntimeCounts countsBefore = runtimeCounts();

        PersonnelPlan plan = personnelWorkbookDryRunFacade.dryRun(
            workbook(rowWithExternalId("7001", "ext-7001", "ACTIVE", "HQ", "DEV"))
        ).getFirst();

        assertThat(plan.outcomeCode()).isEqualTo(PersonnelPlanOutcomeCode.CREATE_USER_REQUIRED);
        assertThat(plan.decision().name()).isEqualTo("CREATE_USER");
        assertThat(plan.plannedMutations())
            .extracting(mutation -> mutation.mutationType())
            .contains(PersonnelPlannedMutationType.CREATE_USER);
        assertThat(appUserRepository.findByEmployeeNumber("7001")).isEmpty();
        assertThat(runtimeCounts()).isEqualTo(countsBefore);
        verify(userAdministrationCommandService, never()).createUser(any());
    }

    @Test
    void dryRunInactiveDismissedAndTemporaryCreateRemainFailClosedWithoutDbMutation() {
        RuntimeCounts countsBefore = runtimeCounts();

        PersonnelPlan inactivePlan = personnelWorkbookDryRunFacade.dryRun(
            workbook(row("7002", "INACTIVE", "HQ", "DEV"))
        ).getFirst();
        PersonnelPlan dismissedPlan = personnelWorkbookDryRunFacade.dryRun(
            workbook(row("7003", "DISMISSED", "HQ", "DEV"))
        ).getFirst();
        PersonnelPlan temporaryPlan = personnelWorkbookDryRunFacade.dryRun(
            workbook(row("7004", "ACTIVE", "HQ", "DEV", "ACTING_HEAD", "BRANCH", "2026-05-12", "2026-05-31"))
        ).getFirst();

        assertThat(inactivePlan.outcomeCode()).isEqualTo(PersonnelPlanOutcomeCode.FAIL_CLOSED);
        assertThat(dismissedPlan.outcomeCode()).isEqualTo(PersonnelPlanOutcomeCode.FAIL_CLOSED);
        assertThat(temporaryPlan.outcomeCode()).isEqualTo(PersonnelPlanOutcomeCode.FAIL_CLOSED);
        assertThat(runtimeCounts()).isEqualTo(countsBefore);
        verify(userAdministrationCommandService, never()).createUser(any());
    }
}
