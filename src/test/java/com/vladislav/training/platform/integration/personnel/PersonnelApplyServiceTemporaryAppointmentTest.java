package com.vladislav.training.platform.integration.personnel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.domain.AccessScopeType;
import com.vladislav.training.platform.access.service.AccessAdministrationCommandService;
import com.vladislav.training.platform.access.service.ManagementRelationTypeQueryService;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.integration.personnel.model.PersonnelApplyResult;
import com.vladislav.training.platform.integration.personnel.model.PersonnelCurrentState;
import com.vladislav.training.platform.integration.personnel.model.PersonnelIdentityResolution;
import com.vladislav.training.platform.integration.personnel.service.PersonnelApplyService;
import com.vladislav.training.platform.integration.personnel.service.PersonnelCurrentStateReader;
import com.vladislav.training.platform.integration.personnel.service.PersonnelImportAdmissionService;
import com.vladislav.training.platform.integration.personnel.service.PersonnelOwnerMutationExecutor;
import com.vladislav.training.platform.userorg.domain.AppRole;
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.service.OrganizationQueryService;
import com.vladislav.training.platform.userorg.service.UserAdministrationCommandService;
import com.vladislav.training.platform.userorg.service.UserQueryService;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
/**
 * Проверяет поведение {@code PersonnelApplyServiceTemporaryAppointment}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class PersonnelApplyServiceTemporaryAppointmentTest {

    @Mock
    private ObjectProvider<PersonnelCurrentStateReader> currentStateReaderProvider;
    @Mock
    private PersonnelCurrentStateReader personnelCurrentStateReader;
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
    void temporaryAppointmentUsesDedicatedTemporaryAuthoritySeams() throws Exception {
        PersonnelApplyService personnelApplyService = new PersonnelApplyService(
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
        when(currentStateReaderProvider.getIfAvailable()).thenReturn(personnelCurrentStateReader);
        when(personnelCurrentStateReader.resolveIdentity(any())).thenReturn(PersonnelIdentityResolution.resolved(
            new PersonnelCurrentState(101L, "1001", "ext-1", "ACTIVE", "HQ", Set.of("ROLE_USER"), false, Set.of("SELF"), Set.of(), Set.of(), false)
        ));
        when(userQueryService.findRoleByCode("ROLE_MANAGER")).thenReturn(role(701L, "ROLE_MANAGER"));
        when(organizationQueryService.findOptionalOrganizationalUnitByExternalId("BR1")).thenReturn(java.util.Optional.empty());
        when(organizationQueryService.findOptionalOrganizationalUnitByPath("BR1")).thenReturn(java.util.Optional.of(unit(501L, "BR1")));
        when(managementRelationTypeQueryService.findManagementRelationTypeByCode("SUPERVISOR")).thenReturn(
            new com.vladislav.training.platform.access.domain.ManagementRelationType(
                901L,
                "SUPERVISOR",
                "Supervisor",
                null,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z")
            )
        );

        PersonnelApplyResult result = personnelApplyService.apply(workbookBytes("ACTING_HEAD"));

        assertThat(result.rows().getFirst().outcomeCode()).isEqualTo("SUCCESS");
        verify(userAdministrationCommandService).updateUser(eq(101L), eq("Ivanov"), eq("Ivan"), eq("Ivanovich"), eq("Специалист"));
        verify(accessAdministrationCommandService).assignTemporaryRoleAssignment(eq(101L), eq(701L), any(Instant.class));
        verify(accessAdministrationCommandService).assignTemporaryAccessArea(eq(101L), eq(501L), eq(AccessScopeType.UNIT_ONLY), any(Instant.class));
        verify(accessAdministrationCommandService).assignTemporaryManagementDelegation(eq(101L), eq(501L), eq(901L), any(Instant.class));
        verify(accessAdministrationCommandService, never()).assignManagementRelation(any(), any(), any(), any());
    }

    @Test
    void unsupportedMultiAppointmentRemainsFailClosed() throws Exception {
        PersonnelApplyService personnelApplyService = new PersonnelApplyService(
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
        when(currentStateReaderProvider.getIfAvailable()).thenReturn(personnelCurrentStateReader);

        PersonnelApplyResult result = personnelApplyService.apply(workbookBytes("DUAL_APPOINTMENT"));

        assertThat(result.rows().getFirst().outcomeCode()).isEqualTo("FAILED");
        verifyNoInteractions(userAdministrationCommandService, accessAdministrationCommandService, userQueryService);
    }

    private byte[] workbookBytes(String temporaryPositionCode) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("personnel");
            Row header = sheet.createRow(0);
            List<String> headers = List.of(
                "employeeNumber",
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
                "1001",
                "Ivanov",
                "Ivan",
                "Ivanovich",
                "ACTIVE",
                "HQ",
                "DEV",
                temporaryPositionCode,
                "BR1",
                "2026-05-01",
                "2026-05-31",
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
        return new AppRole(
            id,
            code,
            code,
            null,
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T00:00:00Z")
        );
    }

    private OrganizationalUnit unit(Long id, String path) {
        return new OrganizationalUnit(
            id,
            null,
            1L,
            path,
            OrganizationalUnitStatus.ACTIVE,
            path,
            0,
            path,
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T00:00:00Z")
        );
    }
}
