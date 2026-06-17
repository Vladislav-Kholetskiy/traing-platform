package com.vladislav.training.platform.integration.personnel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.service.AccessAdministrationCommandService;
import com.vladislav.training.platform.access.service.ManagementRelationTypeQueryService;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.integration.personnel.model.PersonnelApplyResult;
import com.vladislav.training.platform.integration.personnel.model.PersonnelBusinessIntent;
import com.vladislav.training.platform.integration.personnel.model.PersonnelCurrentState;
import com.vladislav.training.platform.integration.personnel.model.PersonnelIdentityResolution;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlan;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlanOutcomeCode;
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
 * Проверяет договорённости вокруг {@code PersonnelApplyResolvedIdentity}.
 * Тест помогает сохранить предсказуемое поведение.
 */
@ExtendWith(MockitoExtension.class)
class PersonnelApplyResolvedIdentityContractTest {

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

    @Test
    void unresolvedOrIdentityMismatchStopsBeforeOwnerMutation() {
        PersonnelOwnerMutationExecutor executor = new PersonnelOwnerMutationExecutor(
            userAdministrationCommandService,
            accessAdministrationCommandService,
            userQueryService,
            organizationQueryService,
            managementRelationTypeQueryService,
            () -> Instant.parse("2026-05-11T12:00:00Z")
        );
        PersonnelApplyService service = new PersonnelApplyService(
            currentStateReaderProvider,
            personnelWorkbookParser,
            personnelRowInterpreter,
            personnelChangePlanner,
            executor,
            personnelImportAdmissionService,
            capabilityAdmissionRequestFactory
        );
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

        PersonnelApplyResult result = service.apply(new byte[] {1});

        assertThat(result.rows().getFirst().outcomeCode()).isEqualTo("FAILED");
        verifyNoInteractions(userAdministrationCommandService, accessAdministrationCommandService, userQueryService);
    }

    @Test
    void applyUsesResolvedIdentityContextBeforeOwnerMutationAndDoesNotNeedSecondEmployeeLookup() {
        PersonnelOwnerMutationExecutor executor = new PersonnelOwnerMutationExecutor(
            userAdministrationCommandService,
            accessAdministrationCommandService,
            userQueryService,
            organizationQueryService,
            managementRelationTypeQueryService,
            () -> Instant.parse("2026-05-11T12:00:00Z")
        );
        PersonnelApplyService service = new PersonnelApplyService(
            currentStateReaderProvider,
            personnelWorkbookParser,
            personnelRowInterpreter,
            personnelChangePlanner,
            executor,
            personnelImportAdmissionService,
            capabilityAdmissionRequestFactory
        );
        PersonnelRow row = row();
        PersonnelBusinessIntent intent = intent();
        PersonnelCurrentState currentState = new PersonnelCurrentState(
            101L,
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
        PersonnelPlan transferPlan = new PersonnelPlan(
            2,
            "1001",
            PersonnelPlanOutcomeCode.PLANNED_CHANGES,
            "ACTIVE",
            List.of(),
            List.of(
                new com.vladislav.training.platform.integration.personnel.model.PersonnelPlannedMutation(
                    com.vladislav.training.platform.integration.personnel.model.PersonnelPlannedMutationType.CLOSE_PRIMARY_ORG_ASSIGNMENT,
                    "HQ",
                    "Close primary"
                ),
                new com.vladislav.training.platform.integration.personnel.model.PersonnelPlannedMutation(
                    com.vladislav.training.platform.integration.personnel.model.PersonnelPlannedMutationType.OPEN_PRIMARY_ORG_ASSIGNMENT,
                    "BR1",
                    "Open primary"
                )
            ),
            101L
        );
        when(capabilityAdmissionRequestFactory.createPersonnelExcelApply()).thenReturn(org.mockito.Mockito.mock(CapabilityAdmissionRequest.class));
        when(personnelWorkbookParser.parse(any())).thenReturn(new PersonnelWorkbookParseResult(List.of(row), List.of()));
        when(currentStateReaderProvider.getIfAvailable()).thenReturn(personnelCurrentStateReader);
        when(personnelRowInterpreter.interpret(row)).thenReturn(intent);
        when(personnelCurrentStateReader.resolveIdentity(intent)).thenReturn(PersonnelIdentityResolution.resolved(currentState));
        when(personnelChangePlanner.plan(eq(intent), any())).thenReturn(transferPlan);
        when(organizationQueryService.findOptionalOrganizationalUnitByExternalId("BR1")).thenReturn(java.util.Optional.empty());
        when(organizationQueryService.findOptionalOrganizationalUnitByPath("BR1")).thenReturn(java.util.Optional.of(
            new com.vladislav.training.platform.userorg.domain.OrganizationalUnit(
                501L,
                null,
                1L,
                "BR1",
                com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus.ACTIVE,
                "BR1",
                0,
                "BR1",
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z")
            )
        ));

        PersonnelApplyResult result = service.apply(new byte[] {1});

        assertThat(result.rows().getFirst().outcomeCode()).isEqualTo("SUCCESS");
        verify(userAdministrationCommandService).updateUser(eq(101L), eq("Ivanov"), eq("Ivan"), eq("Ivanovich"), eq("Специалист"));
        verify(userAdministrationCommandService).replacePrimaryHomeUnit(eq(101L), eq(501L), any(Instant.class));
        verify(userQueryService, never()).findUserByEmployeeNumber("1001");
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
}
