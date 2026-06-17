package com.vladislav.training.platform.integration.personnel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.domain.ManagementRelationType;
import com.vladislav.training.platform.access.service.AccessAdministrationCommandService;
import com.vladislav.training.platform.access.service.ManagementRelationTypeQueryService;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.integration.personnel.model.PersonnelApplyResult;
import com.vladislav.training.platform.integration.personnel.model.PersonnelBusinessIntent;
import com.vladislav.training.platform.integration.personnel.model.PersonnelCurrentState;
import com.vladislav.training.platform.integration.personnel.model.PersonnelIdentityResolution;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlan;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlanOutcomeCode;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlannedMutation;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlannedMutationType;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPositionMapping;
import com.vladislav.training.platform.integration.personnel.model.PersonnelRow;
import com.vladislav.training.platform.integration.personnel.model.PersonnelWorkbookParseResult;
import com.vladislav.training.platform.integration.personnel.service.PersonnelApplyService;
import com.vladislav.training.platform.integration.personnel.service.PersonnelChangePlanner;
import com.vladislav.training.platform.integration.personnel.service.PersonnelCurrentStateReader;
import com.vladislav.training.platform.integration.personnel.service.PersonnelImportAdmissionService;
import com.vladislav.training.platform.integration.personnel.service.PersonnelOwnerMutationExecutor;
import com.vladislav.training.platform.integration.personnel.service.PersonnelRowInterpreter;
import com.vladislav.training.platform.integration.personnel.service.PersonnelWorkbookParser;
import com.vladislav.training.platform.userorg.domain.AppRole;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.service.OrganizationQueryService;
import com.vladislav.training.platform.userorg.service.UserAdministrationCommandService;
import com.vladislav.training.platform.userorg.service.UserQueryService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
/**
 * Проверяет поведение {@code PersonnelApplyRequiresResolvedUserId}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class PersonnelApplyRequiresResolvedUserIdTest {

    @Mock
    private UserAdministrationCommandService userAdministrationCommandService;
    @Mock
    private AccessAdministrationCommandService accessAdministrationCommandService;
    @Mock
    private UserQueryService userQueryService;
    @Mock
    private OrganizationQueryService organizationQueryService;
    @Mock
    private ManagementRelationTypeQueryService managementRelationTypeQueryService;

    @Mock
    private ObjectProvider<PersonnelCurrentStateReader> currentStateReaderProvider;
    @Mock
    private PersonnelWorkbookParser personnelWorkbookParser;
    @Mock
    private PersonnelRowInterpreter personnelRowInterpreter;
    @Mock
    private PersonnelChangePlanner personnelChangePlanner;
    @Mock
    private PersonnelImportAdmissionService personnelImportAdmissionService;
    @Mock
    private com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;
    @Mock
    private PersonnelCurrentStateReader personnelCurrentStateReader;

    @Test
    void mutationBearingPlanWithoutResolvedUserIdFailsClosedWithoutLookupOrOwnerMutation() {
        PersonnelOwnerMutationExecutor executor = executor();

        assertThatThrownBy(() -> executor.execute(intent(), transferPlanWithoutResolvedUserId()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("resolvedUserId")
            .hasMessageContaining("fail-closed");

        verify(userQueryService, never()).findUserByEmployeeNumber("1001");
        verifyNoInteractions(userAdministrationCommandService, accessAdministrationCommandService, organizationQueryService);
    }

    @Test
    void applyServiceReturnsFailedRowForMutationBearingPlanWithoutResolvedUserIdAndDoesNotCallOwnerMutation() {
        PersonnelOwnerMutationExecutor executor = executor();
        PersonnelApplyService service = service(executor);
        PersonnelRow row = row();
        PersonnelBusinessIntent intent = intent();
        when(capabilityAdmissionRequestFactory.createPersonnelExcelApply()).thenReturn(org.mockito.Mockito.mock(CapabilityAdmissionRequest.class));
        when(personnelWorkbookParser.parse(any())).thenReturn(new PersonnelWorkbookParseResult(List.of(row), List.of()));
        when(currentStateReaderProvider.getIfAvailable()).thenReturn(personnelCurrentStateReader);
        when(personnelRowInterpreter.interpret(row)).thenReturn(intent);
        when(personnelCurrentStateReader.resolveIdentity(intent)).thenReturn(PersonnelIdentityResolution.resolved(currentState(101L)));
        when(personnelChangePlanner.plan(eq(intent), any())).thenReturn(transferPlanWithoutResolvedUserId());

        PersonnelApplyResult result = service.apply(new byte[] {1});

        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().getFirst().outcomeCode()).isEqualTo("FAILED");
        assertThat(result.rows().getFirst().issues()).anySatisfy(issue -> assertThat(issue).contains("resolvedUserId"));
        verify(userQueryService, never()).findUserByEmployeeNumber("1001");
        verifyNoInteractions(userAdministrationCommandService, accessAdministrationCommandService, organizationQueryService);
    }

    @Test
    void noChangePlanMayKeepNullResolvedUserIdAndStillReturnsNoChangeWithoutOwnerMutation() {
        PersonnelApplyService service = service(executor());
        PersonnelRow row = row();
        PersonnelBusinessIntent intent = intent();
        when(capabilityAdmissionRequestFactory.createPersonnelExcelApply()).thenReturn(org.mockito.Mockito.mock(CapabilityAdmissionRequest.class));
        when(personnelWorkbookParser.parse(any())).thenReturn(new PersonnelWorkbookParseResult(List.of(row), List.of()));
        when(currentStateReaderProvider.getIfAvailable()).thenReturn(personnelCurrentStateReader);
        when(personnelRowInterpreter.interpret(row)).thenReturn(intent);
        when(personnelCurrentStateReader.resolveIdentity(intent)).thenReturn(PersonnelIdentityResolution.resolved(currentState(101L)));
        when(personnelChangePlanner.plan(eq(intent), any())).thenReturn(new PersonnelPlan(
            2,
            "1001",
            PersonnelPlanOutcomeCode.NO_CHANGE,
            "ACTIVE",
            List.of(),
            List.of()
        ));

        PersonnelApplyResult result = service.apply(new byte[] {1});

        assertThat(result.rows().getFirst().outcomeCode()).isEqualTo("NO_CHANGE");
        verifyNoInteractions(userAdministrationCommandService, accessAdministrationCommandService, userQueryService);
    }

    @Test
    void unresolvedAndIdentityMismatchPlansStopBeforeOwnerMutation() {
        PersonnelApplyService service = service(executor());
        PersonnelRow row = row();
        PersonnelBusinessIntent intent = intent();
        when(capabilityAdmissionRequestFactory.createPersonnelExcelApply()).thenReturn(org.mockito.Mockito.mock(CapabilityAdmissionRequest.class));
        when(personnelWorkbookParser.parse(any())).thenReturn(new PersonnelWorkbookParseResult(List.of(row), List.of()));
        when(currentStateReaderProvider.getIfAvailable()).thenReturn(personnelCurrentStateReader);
        when(personnelRowInterpreter.interpret(row)).thenReturn(intent);

        when(personnelCurrentStateReader.resolveIdentity(intent)).thenReturn(PersonnelIdentityResolution.unresolved("1001"));
        when(personnelChangePlanner.plan(eq(intent), any())).thenReturn(new PersonnelPlan(
            2,
            "1001",
            PersonnelPlanOutcomeCode.UNRESOLVED_EMPLOYEE,
            "ACTIVE",
            List.of("Unresolved"),
            List.of()
        ));

        PersonnelApplyResult unresolved = service.apply(new byte[] {1});
        assertThat(unresolved.rows().getFirst().outcomeCode()).isEqualTo("FAILED");

        when(personnelCurrentStateReader.resolveIdentity(intent)).thenReturn(
            PersonnelIdentityResolution.identityMismatch("1001", "ext-expected", "ext-actual")
        );
        when(personnelChangePlanner.plan(eq(intent), any())).thenReturn(new PersonnelPlan(
            2,
            "1001",
            PersonnelPlanOutcomeCode.IDENTITY_MISMATCH,
            "ACTIVE",
            List.of("Identity mismatch"),
            List.of()
        ));

        PersonnelApplyResult mismatch = service.apply(new byte[] {2});
        assertThat(mismatch.rows().getFirst().outcomeCode()).isEqualTo("FAILED");

        verifyNoInteractions(userAdministrationCommandService, accessAdministrationCommandService, userQueryService);
    }

    @Test
    void transferDismissalAndTemporaryMutationsUseResolvedUserIdWithoutEmployeeNumberFallback() {
        PersonnelOwnerMutationExecutor executor = executor();
        when(organizationQueryService.findOptionalOrganizationalUnitByExternalId("BR1")).thenReturn(java.util.Optional.empty());
        when(organizationQueryService.findOptionalOrganizationalUnitByPath("BR1")).thenReturn(java.util.Optional.of(unit(501L, "BR1")));
        when(userQueryService.findRoleByCode("ROLE_ACTING")).thenReturn(role(701L, "ROLE_ACTING"));
        when(managementRelationTypeQueryService.findManagementRelationTypeByCode("SUPERVISOR")).thenReturn(
            new ManagementRelationType(901L, "SUPERVISOR", "Supervisor", null, instant(), instant())
        );

        executor.execute(intent(), new PersonnelPlan(
            2,
            "1001",
            PersonnelPlanOutcomeCode.PLANNED_CHANGES,
            "ACTIVE",
            List.of(),
            List.of(
                new PersonnelPlannedMutation(PersonnelPlannedMutationType.CLOSE_PRIMARY_ORG_ASSIGNMENT, "HQ", "close"),
                new PersonnelPlannedMutation(PersonnelPlannedMutationType.OPEN_PRIMARY_ORG_ASSIGNMENT, "BR1", "open")
            ),
            101L
        ));
        executor.execute(intent(), new PersonnelPlan(
            2,
            "1001",
            PersonnelPlanOutcomeCode.PLANNED_CHANGES,
            "INACTIVE",
            List.of(),
            List.of(new PersonnelPlannedMutation(
                PersonnelPlannedMutationType.DEACTIVATE_USER_TO_INACTIVE,
                "1001",
                "deactivate"
            )),
            202L
        ));
        executor.execute(temporaryIntent(), new PersonnelPlan(
            2,
            "1001",
            PersonnelPlanOutcomeCode.PLANNED_CHANGES,
            "ACTIVE",
            List.of(),
            List.of(
                new PersonnelPlannedMutation(
                    PersonnelPlannedMutationType.OPEN_TEMPORARY_ROLE_ASSIGNMENT,
                    "ROLE_ACTING",
                    "temp role"
                ),
                new PersonnelPlannedMutation(
                    PersonnelPlannedMutationType.OPEN_TEMPORARY_MANAGEMENT_DELEGATION,
                    "BR1",
                    "temp management"
                )
            ),
            303L
        ));

        verify(userAdministrationCommandService).updateUser(eq(101L), eq("Ivanov"), eq("Ivan"), eq("Ivanovich"), eq("Специалист"));
        verify(userAdministrationCommandService).updateUser(eq(303L), eq("Ivanov"), eq("Ivan"), eq("Ivanovich"), eq("Специалист"));
        verify(userAdministrationCommandService).replacePrimaryHomeUnit(eq(101L), eq(501L), any(Instant.class));
        verify(userAdministrationCommandService).deactivateUser(202L);
        verify(accessAdministrationCommandService).assignTemporaryRoleAssignment(eq(303L), eq(701L), any(Instant.class));
        verify(accessAdministrationCommandService).assignTemporaryManagementDelegation(eq(303L), eq(501L), eq(901L), any(Instant.class));
        verify(userQueryService, never()).findUserByEmployeeNumber("1001");
    }

    private PersonnelOwnerMutationExecutor executor() {
        return new PersonnelOwnerMutationExecutor(
            userAdministrationCommandService,
            accessAdministrationCommandService,
            userQueryService,
            organizationQueryService,
            managementRelationTypeQueryService,
            () -> instant()
        );
    }

    private PersonnelApplyService service(PersonnelOwnerMutationExecutor executor) {
        return new PersonnelApplyService(
            currentStateReaderProvider,
            personnelWorkbookParser,
            personnelRowInterpreter,
            personnelChangePlanner,
            executor,
            personnelImportAdmissionService,
            capabilityAdmissionRequestFactory
        );
    }

    private PersonnelPlan transferPlanWithoutResolvedUserId() {
        return new PersonnelPlan(
            2,
            "1001",
            PersonnelPlanOutcomeCode.PLANNED_CHANGES,
            "ACTIVE",
            List.of(),
            List.of(
                new PersonnelPlannedMutation(PersonnelPlannedMutationType.CLOSE_PRIMARY_ORG_ASSIGNMENT, "HQ", "close"),
                new PersonnelPlannedMutation(PersonnelPlannedMutationType.OPEN_PRIMARY_ORG_ASSIGNMENT, "BR1", "open")
            )
        );
    }

    private PersonnelBusinessIntent intent() {
        return new PersonnelBusinessIntent(
            2,
            "1001",
            null,
            true,
            com.vladislav.training.platform.integration.personnel.model.PersonnelEmploymentAction.ENSURE_ACTIVE,
            "ACTIVE",
            "BR1",
            new PersonnelPositionMapping("DEV", Set.of("ROLE_USER"), false, "SELF", false, true),
            null
        );
    }

    private PersonnelBusinessIntent temporaryIntent() {
        return new PersonnelBusinessIntent(
            2,
            "1001",
            null,
            true,
            com.vladislav.training.platform.integration.personnel.model.PersonnelEmploymentAction.ENSURE_ACTIVE,
            "ACTIVE",
            "HQ",
            new PersonnelPositionMapping("DEV", Set.of("ROLE_USER"), false, "SELF", false, true),
            new com.vladislav.training.platform.integration.personnel.model.PersonnelTemporaryAppointmentIntent(
                new PersonnelPositionMapping("ACTING_HEAD", Set.of("ROLE_ACTING"), false, "UNIT", true, true),
                "BR1",
                java.time.LocalDate.of(2026, 5, 1),
                java.time.LocalDate.of(2026, 5, 31),
                true
            )
        );
    }

    private PersonnelRow row() {
        return new PersonnelRow(
            2,
            "1001",
            "Ivanov",
            "Ivan",
            "Ivanovich",
            "ACTIVE",
            "BR1",
            "DEV",
            null,
            null,
            null,
            null,
            null
        );
    }

    private PersonnelCurrentState currentState(Long userId) {
        return new PersonnelCurrentState(
            userId,
            "1001",
            "ext-1",
            "ACTIVE",
            "HQ",
            Set.of("ROLE_USER"),
            false,
            Set.of(),
            Set.of(),
            Set.of(),
            false
        );
    }

    private OrganizationalUnit unit(Long id, String path) {
        return new OrganizationalUnit(id, null, 1L, path, OrganizationalUnitStatus.ACTIVE, path, 0, path, instant(), instant());
    }

    private AppRole role(Long id, String code) {
        return new AppRole(id, code, code, null, instant(), instant());
    }

    private Instant instant() {
        return Instant.parse("2026-05-11T12:00:00Z");
    }
}
