package com.vladislav.training.platform.integration.personnel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vladislav.training.platform.integration.personnel.model.PersonnelBusinessIntent;
import com.vladislav.training.platform.integration.personnel.model.PersonnelCurrentState;
import com.vladislav.training.platform.integration.personnel.model.PersonnelEmploymentAction;
import com.vladislav.training.platform.integration.personnel.model.PersonnelIdentityResolution;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlan;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlanOutcomeCode;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlannedMutation;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlannedMutationType;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPositionMapping;
import com.vladislav.training.platform.integration.personnel.model.PersonnelTemporaryAppointmentIntent;
import com.vladislav.training.platform.integration.personnel.service.PersonnelChangePlanner;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code PersonnelChangePlanner}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class PersonnelChangePlannerTest {

    private final PersonnelChangePlanner planner = new PersonnelChangePlanner();

    @Test
    void unchangedRowReturnsNoChange() {
        PersonnelBusinessIntent intent = activeIntent("HQ", devMapping(), null);
        PersonnelCurrentState currentState = currentState("HQ", "ACTIVE", devMapping(), false);

        PersonnelPlan plan = planner.plan(intent, PersonnelIdentityResolution.resolved(currentState));

        assertThat(plan.outcomeCode()).isEqualTo(PersonnelPlanOutcomeCode.NO_CHANGE);
        assertThat(plan.plannedMutations()).isEmpty();
    }

    @Test
    void unknownActiveEmployeeNumberBuildsExplicitCreateUserPlan() {
        PersonnelPlan plan = planner.plan(createCapableActiveIntent("HQ", devMapping()), PersonnelIdentityResolution.unresolved("1001"));

        assertThat(plan.outcomeCode()).isEqualTo(PersonnelPlanOutcomeCode.CREATE_USER_REQUIRED);
        assertThat(plan.decision()).isEqualTo(com.vladislav.training.platform.integration.personnel.model.PersonnelRowDecision.CREATE_USER);
        assertThat(plan.plannedMutations())
            .extracting(PersonnelPlannedMutation::mutationType)
            .contains(
                PersonnelPlannedMutationType.CREATE_USER,
                PersonnelPlannedMutationType.OPEN_PRIMARY_ORG_ASSIGNMENT,
                PersonnelPlannedMutationType.OPEN_ROLE_ASSIGNMENT,
                PersonnelPlannedMutationType.OPEN_USER_ACCESS_AREA
            );
    }

    @Test
    void unknownInactiveOrDismissedEmployeeRemainsFailClosedForCreatePath() {
        PersonnelPlan inactivePlan = planner.plan(inactiveCreateIntent(), PersonnelIdentityResolution.unresolved("1001"));
        PersonnelPlan dismissedPlan = planner.plan(dismissedCreateIntent(), PersonnelIdentityResolution.unresolved("1001"));

        assertThat(inactivePlan.outcomeCode()).isEqualTo(PersonnelPlanOutcomeCode.FAIL_CLOSED);
        assertThat(dismissedPlan.outcomeCode()).isEqualTo(PersonnelPlanOutcomeCode.FAIL_CLOSED);
        assertThat(inactivePlan.issues()).anySatisfy(issue -> assertThat(issue).contains("ACTIVE"));
        assertThat(dismissedPlan.issues()).anySatisfy(issue -> assertThat(issue).contains("ACTIVE"));
    }

    @Test
    void externalIdMismatchReturnsIdentityMismatch() {
        PersonnelBusinessIntent intent = new PersonnelBusinessIntent(
            2,
            "1001",
            "ext-expected",
            true,
            PersonnelEmploymentAction.ENSURE_ACTIVE,
            "ACTIVE",
            "HQ",
            devMapping(),
            null
        );

        PersonnelPlan plan = planner.plan(intent, PersonnelIdentityResolution.identityMismatch("1001", "ext-expected", "ext-actual"));

        assertThat(plan.outcomeCode()).isEqualTo(PersonnelPlanOutcomeCode.IDENTITY_MISMATCH);
        assertThat(plan.issues()).anySatisfy(issue -> assertThat(issue).contains("ext-expected").contains("ext-actual"));
    }

    @Test
    void activeBaseStateWithChangedHomeOrgProducesCloseOldPrimaryAndOpenNewPrimary() {
        PersonnelBusinessIntent intent = activeIntent("BR1", devMapping(), null);
        PersonnelCurrentState currentState = currentState("HQ", "ACTIVE", devMapping(), false);

        PersonnelPlan plan = planner.plan(intent, PersonnelIdentityResolution.resolved(currentState));

        assertThat(plan.outcomeCode()).isEqualTo(PersonnelPlanOutcomeCode.PLANNED_CHANGES);
        assertThat(plan.plannedMutations())
            .extracting(PersonnelPlannedMutation::mutationType)
            .containsExactly(
                PersonnelPlannedMutationType.CLOSE_PRIMARY_ORG_ASSIGNMENT,
                PersonnelPlannedMutationType.OPEN_PRIMARY_ORG_ASSIGNMENT
            );
    }

    @Test
    void transferPlanDoesNotCreateTwoActivePrimaryHomes() {
        PersonnelBusinessIntent intent = activeIntent("BR1", devMapping(), null);
        PersonnelCurrentState currentState = currentState("HQ", "ACTIVE", devMapping(), false);

        PersonnelPlan plan = planner.plan(intent, PersonnelIdentityResolution.resolved(currentState));

        assertThat(plan.plannedMutations().stream()
            .filter(mutation -> mutation.mutationType() == PersonnelPlannedMutationType.OPEN_PRIMARY_ORG_ASSIGNMENT)
            .count()).isEqualTo(1);
        assertThat(plan.plannedMutations().stream()
            .filter(mutation -> mutation.mutationType() == PersonnelPlannedMutationType.CLOSE_PRIMARY_ORG_ASSIGNMENT)
            .count()).isEqualTo(1);
    }

    @Test
    void changedBasePositionProducesPlannedRoleAccessAndManagementDelta() {
        PersonnelBusinessIntent intent = activeIntent("HQ", headMapping(), null);
        PersonnelCurrentState currentState = currentState("HQ", "ACTIVE", devMapping(), false);

        PersonnelPlan plan = planner.plan(intent, PersonnelIdentityResolution.resolved(currentState));

        assertThat(plan.plannedMutations())
            .extracting(PersonnelPlannedMutation::mutationType)
            .contains(
                PersonnelPlannedMutationType.OPEN_ROLE_ASSIGNMENT,
                PersonnelPlannedMutationType.OPEN_USER_ACCESS_AREA,
                PersonnelPlannedMutationType.OPEN_MANAGEMENT_RELATION
            );
    }

    @Test
    void dismissedProducesPlannedDismissalClosureCascadeAndFinalInactiveState() {
        PersonnelBusinessIntent intent = new PersonnelBusinessIntent(
            2,
            "1001",
            null,
            true,
            PersonnelEmploymentAction.DISMISS_TO_INACTIVE,
            "INACTIVE",
            "HQ",
            headMapping(),
            null
        );
        PersonnelCurrentState currentState = new PersonnelCurrentState(
            "1001",
            "ext-1",
            "ACTIVE",
            "HQ",
            Set.of("ROLE_USER", "ROLE_MANAGER"),
            true,
            Set.of("UNIT"),
            Set.of("ROLE_MANAGER"),
            Set.of("UNIT"),
            true
        );

        PersonnelPlan plan = planner.plan(intent, PersonnelIdentityResolution.resolved(currentState));

        assertThat(plan.targetUserStatus()).isEqualTo("INACTIVE");
        assertThat(plan.plannedMutations())
            .extracting(PersonnelPlannedMutation::mutationType)
            .contains(
                PersonnelPlannedMutationType.CLOSE_ROLE_ASSIGNMENT,
                PersonnelPlannedMutationType.CLOSE_PRIMARY_ORG_ASSIGNMENT,
                PersonnelPlannedMutationType.CLOSE_MANAGEMENT_RELATION,
                PersonnelPlannedMutationType.CLOSE_USER_ACCESS_AREA,
                PersonnelPlannedMutationType.CLOSE_TEMPORARY_ROLE_ASSIGNMENT,
                PersonnelPlannedMutationType.CLOSE_TEMPORARY_ACCESS_AREA,
                PersonnelPlannedMutationType.CLOSE_TEMPORARY_MANAGEMENT_DELEGATION,
                PersonnelPlannedMutationType.DEACTIVATE_USER_TO_INACTIVE
            );
    }

    @Test
    void temporaryManagerialAppointmentProducesPlannedTemporaryManagementDelegation() {
        PersonnelBusinessIntent intent = activeIntent(
            "HQ",
            devMapping(),
            new PersonnelTemporaryAppointmentIntent(
                actingHeadMapping(),
                "BR1",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                true
            )
        );
        PersonnelCurrentState currentState = currentState("HQ", "ACTIVE", devMapping(), false);

        PersonnelPlan plan = planner.plan(intent, PersonnelIdentityResolution.resolved(currentState));

        assertThat(plan.plannedMutations())
            .extracting(PersonnelPlannedMutation::mutationType)
            .contains(PersonnelPlannedMutationType.OPEN_TEMPORARY_MANAGEMENT_DELEGATION);
    }

    @Test
    void temporaryAppointmentRemainsAdditiveOnly() {
        PersonnelBusinessIntent intent = activeIntent(
            "HQ",
            devMapping(),
            new PersonnelTemporaryAppointmentIntent(
                actingHeadMapping(),
                "BR1",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                true
            )
        );

        PersonnelPlan plan = planner.plan(intent, PersonnelIdentityResolution.resolved(currentState("HQ", "ACTIVE", devMapping(), false)));

        assertThat(plan.plannedMutations())
            .extracting(PersonnelPlannedMutation::mutationType)
            .doesNotContain(
                PersonnelPlannedMutationType.CLOSE_TEMPORARY_ROLE_ASSIGNMENT,
                PersonnelPlannedMutationType.CLOSE_TEMPORARY_ACCESS_AREA,
                PersonnelPlannedMutationType.CLOSE_TEMPORARY_MANAGEMENT_DELEGATION
            );
    }

    @Test
    void unsupportedReplacementOrMultiAppointmentRemainsFailClosed() {
        PersonnelBusinessIntent intent = activeIntent(
            "HQ",
            devMapping(),
            new PersonnelTemporaryAppointmentIntent(
                new PersonnelPositionMapping("DUAL_APPOINTMENT", Set.of("ROLE_MANAGER"), false, "UNIT", true, false),
                "BR1",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                false
            )
        );

        assertThatThrownBy(() -> planner.plan(intent, PersonnelIdentityResolution.resolved(currentState("HQ", "ACTIVE", devMapping(), false))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("additive-only")
            .hasMessageContaining("temporary");
    }

    private PersonnelBusinessIntent activeIntent(
        String homeOrg,
        PersonnelPositionMapping basePositionMapping,
        PersonnelTemporaryAppointmentIntent temporaryIntent
    ) {
        return new PersonnelBusinessIntent(
            2,
            "1001",
            null,
            true,
            PersonnelEmploymentAction.ENSURE_ACTIVE,
            "ACTIVE",
            homeOrg,
            basePositionMapping,
            temporaryIntent
        );
    }

    private PersonnelBusinessIntent createCapableActiveIntent(String homeOrg, PersonnelPositionMapping basePositionMapping) {
        return new PersonnelBusinessIntent(
            2,
            "1001",
            null,
            "Ivanov",
            "Ivan",
            "Ivanovich",
            false,
            PersonnelEmploymentAction.ENSURE_ACTIVE,
            "ACTIVE",
            homeOrg,
            basePositionMapping,
            null
        );
    }

    private PersonnelBusinessIntent inactiveCreateIntent() {
        return new PersonnelBusinessIntent(
            2,
            "1001",
            null,
            "Ivanov",
            "Ivan",
            "Ivanovich",
            true,
            PersonnelEmploymentAction.DEACTIVATE,
            "INACTIVE",
            "HQ",
            devMapping(),
            null
        );
    }

    private PersonnelBusinessIntent dismissedCreateIntent() {
        return new PersonnelBusinessIntent(
            2,
            "1001",
            null,
            "Ivanov",
            "Ivan",
            "Ivanovich",
            true,
            PersonnelEmploymentAction.DISMISS_TO_INACTIVE,
            "INACTIVE",
            "HQ",
            devMapping(),
            null
        );
    }

    private PersonnelCurrentState currentState(
        String homeOrg,
        String status,
        PersonnelPositionMapping mapping,
        boolean temporaryManagementDelegationActive
    ) {
        return new PersonnelCurrentState(
            "1001",
            "ext-1",
            status,
            homeOrg,
            mapping.roleCodes(),
            mapping.managementRelationRequired(),
            Set.of(mapping.accessScopeRequired()),
            Set.of(),
            Set.of(),
            temporaryManagementDelegationActive
        );
    }

    private PersonnelPositionMapping devMapping() {
        return new PersonnelPositionMapping("DEV", Set.of("ROLE_USER"), false, "SELF", false, true);
    }

    private PersonnelPositionMapping headMapping() {
        return new PersonnelPositionMapping("HEAD", Set.of("ROLE_USER", "ROLE_MANAGER"), true, "UNIT", false, true);
    }

    private PersonnelPositionMapping actingHeadMapping() {
        return new PersonnelPositionMapping("ACTING_HEAD", Set.of("ROLE_MANAGER"), false, "UNIT", true, true);
    }
}
