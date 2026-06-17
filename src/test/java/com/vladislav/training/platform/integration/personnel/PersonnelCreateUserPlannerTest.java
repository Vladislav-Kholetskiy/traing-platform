package com.vladislav.training.platform.integration.personnel;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.integration.personnel.model.PersonnelBusinessIntent;
import com.vladislav.training.platform.integration.personnel.model.PersonnelEmploymentAction;
import com.vladislav.training.platform.integration.personnel.model.PersonnelIdentityResolution;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlan;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlanOutcomeCode;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlannedMutation;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlannedMutationType;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPositionMapping;
import com.vladislav.training.platform.integration.personnel.model.PersonnelRowDecision;
import com.vladislav.training.platform.integration.personnel.model.PersonnelTemporaryAppointmentIntent;
import com.vladislav.training.platform.integration.personnel.service.PersonnelChangePlanner;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code PersonnelCreateUserPlanner}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class PersonnelCreateUserPlannerTest {

    private final PersonnelChangePlanner planner = new PersonnelChangePlanner();

    @Test
    void dryRunUnknownActiveEmployeeBuildsCreateUserPlan() {
        PersonnelPlan plan = planner.plan(createIntent(null), PersonnelIdentityResolution.unresolved("9001"));

        assertThat(plan.outcomeCode()).isEqualTo(PersonnelPlanOutcomeCode.CREATE_USER_REQUIRED);
        assertThat(plan.decision()).isEqualTo(PersonnelRowDecision.CREATE_USER);
        assertThat(plan.plannedMutations())
            .extracting(PersonnelPlannedMutation::mutationType)
            .containsExactly(
                PersonnelPlannedMutationType.CREATE_USER,
                PersonnelPlannedMutationType.OPEN_PRIMARY_ORG_ASSIGNMENT,
                PersonnelPlannedMutationType.OPEN_ROLE_ASSIGNMENT,
                PersonnelPlannedMutationType.OPEN_USER_ACCESS_AREA
            );
    }

    @Test
    void unknownActiveManagerEmployeeIncludesManagementAndUnitAccessInCreatePlan() {
        PersonnelPlan plan = planner.plan(createHeadIntent(), PersonnelIdentityResolution.unresolved("9002"));

        assertThat(plan.plannedMutations())
            .extracting(PersonnelPlannedMutation::mutationType)
            .contains(
                PersonnelPlannedMutationType.OPEN_ROLE_ASSIGNMENT,
                PersonnelPlannedMutationType.OPEN_USER_ACCESS_AREA,
                PersonnelPlannedMutationType.OPEN_MANAGEMENT_RELATION
            );
    }

    @Test
    void inactiveDismissedAndTemporaryCreateRemainFailClosed() {
        PersonnelPlan inactivePlan = planner.plan(inactiveIntent(), PersonnelIdentityResolution.unresolved("9003"));
        PersonnelPlan dismissedPlan = planner.plan(dismissedIntent(), PersonnelIdentityResolution.unresolved("9004"));
        PersonnelPlan temporaryPlan = planner.plan(temporaryIntent(), PersonnelIdentityResolution.unresolved("9005"));

        assertThat(inactivePlan.decision()).isEqualTo(PersonnelRowDecision.FAIL_CLOSED);
        assertThat(dismissedPlan.decision()).isEqualTo(PersonnelRowDecision.FAIL_CLOSED);
        assertThat(temporaryPlan.decision()).isEqualTo(PersonnelRowDecision.FAIL_CLOSED);
        assertThat(temporaryPlan.issues()).anySatisfy(issue -> assertThat(issue).contains("Temporary block"));
    }

    private PersonnelBusinessIntent createIntent(String externalId) {
        return new PersonnelBusinessIntent(
            2,
            "9001",
            externalId,
            "Ivanov",
            "Ivan",
            "Ivanovich",
            false,
            PersonnelEmploymentAction.ENSURE_ACTIVE,
            "ACTIVE",
            "HQ",
            new PersonnelPositionMapping("DEV", Set.of("ROLE_USER"), false, "SELF", false, true),
            null
        );
    }

    private PersonnelBusinessIntent createHeadIntent() {
        return new PersonnelBusinessIntent(
            2,
            "9002",
            "ext-9002",
            "Petrov",
            "Petr",
            null,
            false,
            PersonnelEmploymentAction.ENSURE_ACTIVE,
            "ACTIVE",
            "HQ",
            new PersonnelPositionMapping("HEAD", Set.of("ROLE_USER", "ROLE_MANAGER"), true, "UNIT", false, true),
            null
        );
    }

    private PersonnelBusinessIntent inactiveIntent() {
        return new PersonnelBusinessIntent(
            2,
            "9003",
            null,
            "Sidorov",
            "Sidr",
            null,
            true,
            PersonnelEmploymentAction.DEACTIVATE,
            "INACTIVE",
            "HQ",
            new PersonnelPositionMapping("DEV", Set.of("ROLE_USER"), false, "SELF", false, true),
            null
        );
    }

    private PersonnelBusinessIntent dismissedIntent() {
        return new PersonnelBusinessIntent(
            2,
            "9004",
            null,
            "Smirnov",
            "Sergey",
            null,
            true,
            PersonnelEmploymentAction.DISMISS_TO_INACTIVE,
            "INACTIVE",
            "HQ",
            new PersonnelPositionMapping("DEV", Set.of("ROLE_USER"), false, "SELF", false, true),
            null
        );
    }

    private PersonnelBusinessIntent temporaryIntent() {
        return new PersonnelBusinessIntent(
            2,
            "9005",
            null,
            "Alexeev",
            "Alexey",
            null,
            true,
            PersonnelEmploymentAction.ENSURE_ACTIVE,
            "ACTIVE",
            "HQ",
            new PersonnelPositionMapping("DEV", Set.of("ROLE_USER"), false, "SELF", false, true),
            new PersonnelTemporaryAppointmentIntent(
                new PersonnelPositionMapping("ACTING_HEAD", Set.of("ROLE_MANAGER"), false, "UNIT", true, true),
                "BR1",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                true
            )
        );
    }
}
