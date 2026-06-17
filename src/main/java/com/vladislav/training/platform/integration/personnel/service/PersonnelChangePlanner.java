package com.vladislav.training.platform.integration.personnel.service;

import com.vladislav.training.platform.integration.personnel.model.PersonnelBusinessIntent;
import com.vladislav.training.platform.integration.personnel.model.PersonnelCurrentState;
import com.vladislav.training.platform.integration.personnel.model.PersonnelEmploymentAction;
import com.vladislav.training.platform.integration.personnel.model.PersonnelIdentityResolution;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlan;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlanOutcomeCode;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlannedMutation;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlannedMutationType;
import com.vladislav.training.platform.integration.personnel.model.PersonnelTemporaryAppointmentIntent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Класс {@code PersonnelChangePlanner}.
 */
public class PersonnelChangePlanner {

    public PersonnelPlan plan(PersonnelBusinessIntent intent, PersonnelIdentityResolution identityResolution) {
        Objects.requireNonNull(intent, "intent must not be null");
        Objects.requireNonNull(identityResolution, "identityResolution must not be null");

        if (identityResolution.identityMismatch()) {
            return new PersonnelPlan(
                intent.rowNumber(),
                intent.employeeNumber(),
                PersonnelPlanOutcomeCode.IDENTITY_MISMATCH,
                intent.futureTargetUserStatus(),
                List.of("Identity mismatch: expected externalId=" + identityResolution.expectedExternalId()
                    + ", actual externalId=" + identityResolution.actualExternalId()),
                List.of(),
                null
            );
        }
        if (!identityResolution.resolved()) {
            return planCreateUser(intent);
        }

        PersonnelCurrentState currentState = identityResolution.currentState();
        List<PersonnelPlannedMutation> mutations = new ArrayList<>();

        if (intent.employmentAction() == PersonnelEmploymentAction.DISMISS_TO_INACTIVE) {
            planDismissalCascade(currentState, mutations);
            return finalizePlan(intent, currentState.userId(), mutations);
        }

        planPrimaryHomeChange(intent, currentState, mutations);
        planBasePositionDelta(intent, currentState, mutations);
        planTemporaryAppointment(intent.temporaryAppointmentIntent(), currentState, mutations);

        return finalizePlan(intent, currentState.userId(), mutations);
    }

    private PersonnelPlan planCreateUser(PersonnelBusinessIntent intent) {
        List<String> issues = new ArrayList<>();
        if (intent.requiresExistingEmployee()) {
            issues.add("Unknown employeeNumber requires existing employee resolution for this row shape");
        }
        if (intent.employmentAction() != PersonnelEmploymentAction.ENSURE_ACTIVE) {
            issues.add("Create-user supports only ACTIVE employmentStatus in v1 direct MVP");
        }
        if (intent.temporaryAppointmentIntent() != null) {
            issues.add("Temporary block for newly created employee is fail-closed in v1 direct MVP");
        }
        if (!issues.isEmpty()) {
            return new PersonnelPlan(
                intent.rowNumber(),
                intent.employeeNumber(),
                PersonnelPlanOutcomeCode.FAIL_CLOSED,
                intent.futureTargetUserStatus(),
                issues,
                List.of(),
                null
            );
        }

        List<PersonnelPlannedMutation> mutations = new ArrayList<>();
        mutations.add(new PersonnelPlannedMutation(
            PersonnelPlannedMutationType.CREATE_USER,
            intent.employeeNumber(),
            "Create canonical app_user through owner service"
        ));
        mutations.add(new PersonnelPlannedMutation(
            PersonnelPlannedMutationType.OPEN_PRIMARY_ORG_ASSIGNMENT,
            intent.homeOrgUnitCode(),
            "Open initial primary home organization assignment"
        ));
        planBaseCreatePosition(intent, mutations);

        return new PersonnelPlan(
            intent.rowNumber(),
            intent.employeeNumber(),
            PersonnelPlanOutcomeCode.CREATE_USER_REQUIRED,
            intent.futureTargetUserStatus(),
            List.of(),
            mutations,
            null
        );
    }

    private void planBaseCreatePosition(
        PersonnelBusinessIntent intent,
        List<PersonnelPlannedMutation> mutations
    ) {
        for (String roleCode : intent.basePositionMapping().roleCodes()) {
            mutations.add(new PersonnelPlannedMutation(
                PersonnelPlannedMutationType.OPEN_ROLE_ASSIGNMENT,
                roleCode,
                "Open initial base role assignment"
            ));
        }
        mutations.add(new PersonnelPlannedMutation(
            PersonnelPlannedMutationType.OPEN_USER_ACCESS_AREA,
            intent.basePositionMapping().accessScopeRequired(),
            "Open initial base access scope"
        ));
        if (intent.basePositionMapping().managementRelationRequired()) {
            mutations.add(new PersonnelPlannedMutation(
                PersonnelPlannedMutationType.OPEN_MANAGEMENT_RELATION,
                intent.homeOrgUnitCode(),
                "Open initial management relation"
            ));
        }
    }

    private PersonnelPlan finalizePlan(
        PersonnelBusinessIntent intent,
        Long resolvedUserId,
        List<PersonnelPlannedMutation> mutations
    ) {
        PersonnelPlanOutcomeCode outcomeCode = mutations.isEmpty()
            ? PersonnelPlanOutcomeCode.NO_CHANGE
            : PersonnelPlanOutcomeCode.PLANNED_CHANGES;
        return new PersonnelPlan(
            intent.rowNumber(),
            intent.employeeNumber(),
            outcomeCode,
            intent.futureTargetUserStatus(),
            List.of(),
            mutations,
            resolvedUserId
        );
    }

    private void planDismissalCascade(PersonnelCurrentState currentState, List<PersonnelPlannedMutation> mutations) {
        for (String roleCode : currentState.activeRoleCodes()) {
            mutations.add(new PersonnelPlannedMutation(
                PersonnelPlannedMutationType.CLOSE_ROLE_ASSIGNMENT,
                roleCode,
                "Close active role assignment"
            ));
        }
        if (currentState.primaryHomeOrgUnitCode() != null && !currentState.primaryHomeOrgUnitCode().isBlank()) {
            mutations.add(new PersonnelPlannedMutation(
                PersonnelPlannedMutationType.CLOSE_PRIMARY_ORG_ASSIGNMENT,
                currentState.primaryHomeOrgUnitCode(),
                "Close active primary home organization assignment"
            ));
        }
        if (currentState.managementRelationActive()) {
            mutations.add(new PersonnelPlannedMutation(
                PersonnelPlannedMutationType.CLOSE_MANAGEMENT_RELATION,
                currentState.employeeNumber(),
                "Close active management relation"
            ));
        }
        for (String accessScope : currentState.activeAccessScopeCodes()) {
            mutations.add(new PersonnelPlannedMutation(
                PersonnelPlannedMutationType.CLOSE_USER_ACCESS_AREA,
                accessScope,
                "Close active user access area"
            ));
        }
        for (String roleCode : currentState.activeTemporaryRoleCodes()) {
            mutations.add(new PersonnelPlannedMutation(
                PersonnelPlannedMutationType.CLOSE_TEMPORARY_ROLE_ASSIGNMENT,
                roleCode,
                "Close active temporary role assignment"
            ));
        }
        for (String accessScope : currentState.activeTemporaryAccessScopeCodes()) {
            mutations.add(new PersonnelPlannedMutation(
                PersonnelPlannedMutationType.CLOSE_TEMPORARY_ACCESS_AREA,
                accessScope,
                "Close active temporary access area"
            ));
        }
        if (currentState.activeTemporaryManagementDelegation()) {
            mutations.add(new PersonnelPlannedMutation(
                PersonnelPlannedMutationType.CLOSE_TEMPORARY_MANAGEMENT_DELEGATION,
                currentState.employeeNumber(),
                "Close active temporary management delegation"
            ));
        }
        mutations.add(new PersonnelPlannedMutation(
            PersonnelPlannedMutationType.DEACTIVATE_USER_TO_INACTIVE,
            currentState.employeeNumber(),
            "Deactivate user to final INACTIVE state"
        ));
    }

    private void planPrimaryHomeChange(
        PersonnelBusinessIntent intent,
        PersonnelCurrentState currentState,
        List<PersonnelPlannedMutation> mutations
    ) {
        if (!Objects.equals(intent.homeOrgUnitCode(), currentState.primaryHomeOrgUnitCode())) {
            if (currentState.primaryHomeOrgUnitCode() != null && !currentState.primaryHomeOrgUnitCode().isBlank()) {
                mutations.add(new PersonnelPlannedMutation(
                    PersonnelPlannedMutationType.CLOSE_PRIMARY_ORG_ASSIGNMENT,
                    currentState.primaryHomeOrgUnitCode(),
                    "Close previous active primary home organization assignment"
                ));
            }
            mutations.add(new PersonnelPlannedMutation(
                PersonnelPlannedMutationType.OPEN_PRIMARY_ORG_ASSIGNMENT,
                intent.homeOrgUnitCode(),
                "Open new primary home organization assignment"
            ));
        }
    }

    private void planBasePositionDelta(
        PersonnelBusinessIntent intent,
        PersonnelCurrentState currentState,
        List<PersonnelPlannedMutation> mutations
    ) {
        Set<String> desiredRoles = intent.basePositionMapping().roleCodes();
        for (String desiredRole : desiredRoles) {
            if (!currentState.activeRoleCodes().contains(desiredRole)) {
                mutations.add(new PersonnelPlannedMutation(
                    PersonnelPlannedMutationType.OPEN_ROLE_ASSIGNMENT,
                    desiredRole,
                    "Open missing base role assignment"
                ));
            }
        }

        String desiredAccessScope = intent.basePositionMapping().accessScopeRequired();
        if (!currentState.activeAccessScopeCodes().contains(desiredAccessScope)) {
            for (String currentAccessScope : currentState.activeAccessScopeCodes()) {
                mutations.add(new PersonnelPlannedMutation(
                    PersonnelPlannedMutationType.CLOSE_USER_ACCESS_AREA,
                    currentAccessScope,
                    "Close outdated access scope"
                ));
            }
            mutations.add(new PersonnelPlannedMutation(
                PersonnelPlannedMutationType.OPEN_USER_ACCESS_AREA,
                desiredAccessScope,
                "Open required base access scope"
            ));
        }

        if (intent.basePositionMapping().managementRelationRequired() && !currentState.managementRelationActive()) {
            mutations.add(new PersonnelPlannedMutation(
                PersonnelPlannedMutationType.OPEN_MANAGEMENT_RELATION,
                intent.homeOrgUnitCode(),
                "Open required management relation"
            ));
        }
        if (!intent.basePositionMapping().managementRelationRequired() && currentState.managementRelationActive()) {
            mutations.add(new PersonnelPlannedMutation(
                PersonnelPlannedMutationType.CLOSE_MANAGEMENT_RELATION,
                currentState.employeeNumber(),
                "Close outdated management relation"
            ));
        }
    }

    private void planTemporaryAppointment(
        PersonnelTemporaryAppointmentIntent temporaryIntent,
        PersonnelCurrentState currentState,
        List<PersonnelPlannedMutation> mutations
    ) {
        if (temporaryIntent == null) {
            return;
        }
        if (!temporaryIntent.additiveOnly() || !temporaryIntent.positionMapping().additiveOnly()) {
            throw new IllegalArgumentException("Only additive-only temporary appointments are supported");
        }
        for (String roleCode : temporaryIntent.positionMapping().roleCodes()) {
            if (!currentState.activeTemporaryRoleCodes().contains(roleCode)) {
                mutations.add(new PersonnelPlannedMutation(
                    PersonnelPlannedMutationType.OPEN_TEMPORARY_ROLE_ASSIGNMENT,
                    roleCode,
                    "Open additive temporary role assignment"
                ));
            }
        }
        if (!currentState.activeTemporaryAccessScopeCodes().contains(temporaryIntent.positionMapping().accessScopeRequired())) {
            mutations.add(new PersonnelPlannedMutation(
                PersonnelPlannedMutationType.OPEN_TEMPORARY_ACCESS_AREA,
                temporaryIntent.positionMapping().accessScopeRequired(),
                "Open additive temporary access area"
            ));
        }
        if (temporaryIntent.positionMapping().temporaryManagementDelegationRequired()
            && !currentState.activeTemporaryManagementDelegation()) {
            mutations.add(new PersonnelPlannedMutation(
                PersonnelPlannedMutationType.OPEN_TEMPORARY_MANAGEMENT_DELEGATION,
                temporaryIntent.orgUnitCode(),
                "Open additive temporary management delegation"
            ));
        }
    }
}
