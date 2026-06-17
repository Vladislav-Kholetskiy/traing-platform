package com.vladislav.training.platform.integration.personnel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.domain.AccessScopeType;
import com.vladislav.training.platform.access.service.AccessAdministrationCommandService;
import com.vladislav.training.platform.access.service.ManagementRelationTypeQueryService;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.integration.personnel.model.PersonnelApplyResult;
import com.vladislav.training.platform.integration.personnel.service.PersonnelApplyService;
import com.vladislav.training.platform.integration.personnel.service.PersonnelCurrentStateReader;
import com.vladislav.training.platform.integration.personnel.service.PersonnelImportAdmissionService;
import com.vladislav.training.platform.integration.personnel.service.PersonnelOwnerMutationExecutor;
import com.vladislav.training.platform.userorg.domain.AppRole;
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.service.OrganizationQueryService;
import com.vladislav.training.platform.userorg.service.UserAdministrationCommandService;
import com.vladislav.training.platform.userorg.service.UserQueryService;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
/**
 * Проверяет поведение сервиса {@code PersonnelApplyCreateUser}.
 * Сценарии сосредоточены на прикладной логике.
 */
@ExtendWith(MockitoExtension.class)
class PersonnelApplyCreateUserServiceTest {

    @Mock
    private ObjectProvider<PersonnelCurrentStateReader> currentStateReaderProvider;
    @Mock
    private PersonnelImportAdmissionService personnelImportAdmissionService;
    @Mock
    private com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;
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
    void applyCreateUserUsesOwnerCreateThenOrgRoleAndAccessSeams() throws Exception {
        PersonnelApplyService service = new PersonnelApplyService(
            currentStateReaderProvider,
            personnelImportAdmissionService,
            capabilityAdmissionRequestFactory,
            new PersonnelOwnerMutationExecutor(
                userAdministrationCommandService,
                accessAdministrationCommandService,
                userQueryService,
                organizationQueryService,
                managementRelationTypeQueryService,
                () -> Instant.parse("2026-05-11T10:30:00Z")
            )
        );
        when(capabilityAdmissionRequestFactory.createPersonnelExcelApply()).thenReturn(org.mockito.Mockito.mock(CapabilityAdmissionRequest.class));
        when(currentStateReaderProvider.getIfAvailable()).thenReturn(intent -> com.vladislav.training.platform.integration.personnel.model.PersonnelIdentityResolution.unresolved(intent.employeeNumber()));
        when(userAdministrationCommandService.createUser(any())).thenReturn(new AppUser(
            501L,
            "9001",
            "ext-9001",
            "Ivanov",
            "Ivan",
            "Ivanovich",
            UserStatus.ACTIVE,
            Instant.parse("2026-05-11T10:30:00Z"),
            Instant.parse("2026-05-11T10:30:00Z")
        ));
        when(organizationQueryService.findOptionalOrganizationalUnitByExternalId("HQ")).thenReturn(java.util.Optional.of(unit(601L, "HQ")));
        when(userAdministrationCommandService.assignOrganizationAssignment(
            eq(501L),
            eq(601L),
            eq(OrganizationAssignmentType.PRIMARY),
            any(Instant.class)
        )).thenReturn(new UserOrganizationAssignment(
            701L,
            501L,
            601L,
            OrganizationAssignmentType.PRIMARY,
            Instant.parse("2026-05-11T10:30:00Z"),
            null,
            Instant.parse("2026-05-11T10:30:00Z"),
            Instant.parse("2026-05-11T10:30:00Z")
        ));
        when(userQueryService.findRoleByCode("ROLE_USER")).thenReturn(role(801L, "ROLE_USER"));

        PersonnelApplyResult result = service.apply(createWorkbook("9001", "ext-9001", "ACTIVE", "HQ", "DEV"));

        assertThat(result.rows()).singleElement().satisfies(row -> {
            assertThat(row.outcomeCode()).isEqualTo("SUCCESS");
            assertThat(row.decision()).isEqualTo("CREATE_USER");
            assertThat(row.createdUserId()).isEqualTo(501L);
        });
        verify(userAdministrationCommandService).createUser(any());
        verify(userAdministrationCommandService).assignOrganizationAssignment(eq(501L), eq(601L), eq(OrganizationAssignmentType.PRIMARY), any(Instant.class));
        verify(userAdministrationCommandService).assignRole(eq(501L), eq(801L), any(Instant.class));
        verify(accessAdministrationCommandService).assignUserAccessArea(eq(501L), eq(null), eq(AccessScopeType.GLOBAL), any(Instant.class));
    }

    @Test
    void applyCreateUserMayOpenManagerialAccessAndManagementThroughOwnerSeams() throws Exception {
        PersonnelApplyService service = new PersonnelApplyService(
            currentStateReaderProvider,
            personnelImportAdmissionService,
            capabilityAdmissionRequestFactory,
            new PersonnelOwnerMutationExecutor(
                userAdministrationCommandService,
                accessAdministrationCommandService,
                userQueryService,
                organizationQueryService,
                managementRelationTypeQueryService,
                () -> Instant.parse("2026-05-11T10:30:00Z")
            )
        );
        when(capabilityAdmissionRequestFactory.createPersonnelExcelApply()).thenReturn(org.mockito.Mockito.mock(CapabilityAdmissionRequest.class));
        when(currentStateReaderProvider.getIfAvailable()).thenReturn(intent -> com.vladislav.training.platform.integration.personnel.model.PersonnelIdentityResolution.unresolved(intent.employeeNumber()));
        when(userAdministrationCommandService.createUser(any())).thenReturn(new AppUser(
            601L,
            "9002",
            "ext-9002",
            "Petrov",
            "Petr",
            null,
            UserStatus.ACTIVE,
            Instant.parse("2026-05-11T10:30:00Z"),
            Instant.parse("2026-05-11T10:30:00Z")
        ));
        when(organizationQueryService.findOptionalOrganizationalUnitByExternalId("HQ")).thenReturn(java.util.Optional.of(unit(701L, "HQ")));
        when(userAdministrationCommandService.assignOrganizationAssignment(
            eq(601L),
            eq(701L),
            eq(OrganizationAssignmentType.PRIMARY),
            any(Instant.class)
        )).thenReturn(new UserOrganizationAssignment(
            801L,
            601L,
            701L,
            OrganizationAssignmentType.PRIMARY,
            Instant.parse("2026-05-11T10:30:00Z"),
            null,
            Instant.parse("2026-05-11T10:30:00Z"),
            Instant.parse("2026-05-11T10:30:00Z")
        ));
        when(userQueryService.findRoleByCode("ROLE_USER")).thenReturn(role(901L, "ROLE_USER"));
        when(userQueryService.findRoleByCode("ROLE_MANAGER")).thenReturn(role(902L, "ROLE_MANAGER"));
        when(managementRelationTypeQueryService.findManagementRelationTypeByCode("SUPERVISOR")).thenReturn(
            new com.vladislav.training.platform.access.domain.ManagementRelationType(
                903L,
                "SUPERVISOR",
                "Supervisor",
                null,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z")
            )
        );

        PersonnelApplyResult result = service.apply(createWorkbook("9002", "ext-9002", "ACTIVE", "HQ", "HEAD"));

        assertThat(result.rows().getFirst().outcomeCode()).isEqualTo("SUCCESS");
        verify(userAdministrationCommandService).assignRole(eq(601L), eq(901L), any(Instant.class));
        verify(userAdministrationCommandService).assignRole(eq(601L), eq(902L), any(Instant.class));
        verify(accessAdministrationCommandService).assignUserAccessArea(eq(601L), eq(701L), eq(AccessScopeType.UNIT_ONLY), any(Instant.class));
        verify(accessAdministrationCommandService).assignManagementRelation(eq(601L), eq(701L), eq(903L), any(Instant.class));
    }

    @Test
    void deniedApplyStopsBeforeOwnerCreate() throws Exception {
        PersonnelApplyService service = new PersonnelApplyService(
            currentStateReaderProvider,
            personnelImportAdmissionService,
            capabilityAdmissionRequestFactory,
            new PersonnelOwnerMutationExecutor(
                userAdministrationCommandService,
                accessAdministrationCommandService,
                userQueryService,
                organizationQueryService,
                managementRelationTypeQueryService,
                () -> Instant.parse("2026-05-11T10:30:00Z")
            )
        );
        CapabilityAdmissionRequest request = org.mockito.Mockito.mock(CapabilityAdmissionRequest.class);
        when(capabilityAdmissionRequestFactory.createPersonnelExcelApply()).thenReturn(request);
        doThrow(new com.vladislav.training.platform.common.exception.PolicyViolationException("DENIED", "apply denied"))
            .when(personnelImportAdmissionService).checkApplyAdmission(request);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.apply(createWorkbook("9001", "", "ACTIVE", "HQ", "DEV")))
            .isInstanceOf(com.vladislav.training.platform.common.exception.PolicyViolationException.class);

        verify(userAdministrationCommandService, never()).createUser(any());
        verify(accessAdministrationCommandService, never()).assignUserAccessArea(any(), any(), any(), any());
    }

    private byte[] createWorkbook(
        String employeeNumber,
        String externalId,
        String employmentStatus,
        String homeOrgUnitCode,
        String basePositionCode
    ) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("personnel");
            Row header = sheet.createRow(0);
            List<String> headers = List.of(
                "employeeNumber",
                "externalId",
                "lastName",
                "firstName",
                "middleName",
                "employmentStatus",
                "homeOrgUnitCode",
                "basePositionCode",
                "temporaryPositionCode",
                "temporaryOrgUnitCode",
                "temporaryValidFrom",
                "temporaryValidTo",
                "comment"
            );
            for (int index = 0; index < headers.size(); index++) {
                header.createCell(index).setCellValue(headers.get(index));
            }
            Row row = sheet.createRow(1);
            List<String> values = List.of(
                employeeNumber,
                externalId == null ? "" : externalId,
                "Ivanov",
                "Ivan",
                "Ivanovich",
                employmentStatus,
                homeOrgUnitCode,
                basePositionCode,
                "",
                "",
                "",
                "",
                ""
            );
            for (int index = 0; index < values.size(); index++) {
                row.createCell(index).setCellValue(values.get(index));
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private AppRole role(Long id, String code) {
        return new AppRole(id, code, code, null, Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
    }

    private OrganizationalUnit unit(Long id, String externalId) {
        return new OrganizationalUnit(
            id,
            null,
            1L,
            externalId,
            OrganizationalUnitStatus.ACTIVE,
            externalId,
            0,
            externalId,
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T00:00:00Z")
        );
    }
}
