package com.vladislav.training.platform.integration.personnel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.domain.AccessScopeType;
import com.vladislav.training.platform.access.service.AccessAdministrationCommandService;
import com.vladislav.training.platform.access.service.ManagementRelationTypeQueryService;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.integration.personnel.model.PersonnelApplyResult;
import com.vladislav.training.platform.integration.personnel.model.PersonnelBusinessIntent;
import com.vladislav.training.platform.integration.personnel.model.PersonnelCurrentState;
import com.vladislav.training.platform.integration.personnel.model.PersonnelEmploymentAction;
import com.vladislav.training.platform.integration.personnel.model.PersonnelIdentityResolution;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPositionMapping;
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
import java.time.LocalDate;
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
 * Проверяет поведение {@code PersonnelApplyServiceTransfer}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class PersonnelApplyServiceTransferTest {

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
    void transferPlanCallsExistingOwnerPrimaryHomeReplacementSeamWithoutDirectJpaPatch() throws Exception {
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
            new PersonnelCurrentState(101L, "1001", "ext-1", "ACTIVE", "HQ", Set.of("ROLE_USER"), false, Set.of(), Set.of(), Set.of(), false)
        ));
        when(organizationQueryService.findOptionalOrganizationalUnitByExternalId("BR1")).thenReturn(
            java.util.Optional.of(unit(501L, "BR1", "/department/br1"))
        );

        PersonnelApplyResult result = personnelApplyService.apply(workbookBytes("1001", "ACTIVE", "BR1", "DEV", "", "", "", ""));

        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().getFirst().outcomeCode()).isEqualTo("SUCCESS");
        verify(userAdministrationCommandService).updateUser(eq(101L), eq("Ivanov"), eq("Ivan"), eq("Ivanovich"), eq("Специалист"));
        verify(userAdministrationCommandService).replacePrimaryHomeUnit(eq(101L), eq(501L), any(Instant.class));
    }

    @Test
    void baseRoleAccessAndManagementDeltasCallOwnerSeams() throws Exception {
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
            new PersonnelCurrentState(101L, "1001", "ext-1", "ACTIVE", "HQ", Set.of("ROLE_USER"), false, Set.of(), Set.of(), Set.of(), false)
        ));
        when(userQueryService.findRoleByCode("ROLE_MANAGER")).thenReturn(role(701L, "ROLE_MANAGER"));
        when(organizationQueryService.findOptionalOrganizationalUnitByExternalId("HQ")).thenReturn(
            java.util.Optional.of(unit(601L, "HQ", "/department/hq"))
        );
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

        PersonnelApplyResult result = personnelApplyService.apply(workbookBytes("1001", "ACTIVE", "HQ", "HEAD", "", "", "", ""));

        assertThat(result.rows().getFirst().outcomeCode()).isEqualTo("SUCCESS");
        verify(userAdministrationCommandService).updateUser(
            eq(101L),
            eq("Ivanov"),
            eq("Ivan"),
            eq("Ivanovich"),
            eq("Генеральный директор")
        );
        verify(userAdministrationCommandService).assignRole(eq(101L), eq(701L), any(Instant.class));
        verify(accessAdministrationCommandService).assignUserAccessArea(eq(101L), eq(601L), eq(AccessScopeType.UNIT_ONLY), any(Instant.class));
        verify(accessAdministrationCommandService).assignManagementRelation(eq(101L), eq(601L), eq(901L), any(Instant.class));
    }

    private byte[] workbookBytes(
        String employeeNumber,
        String employmentStatus,
        String homeOrgUnitCode,
        String basePositionCode,
        String temporaryPositionCode,
        String temporaryOrgUnitCode,
        String temporaryValidFrom,
        String temporaryValidTo
    ) throws Exception {
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
                employeeNumber,
                "Ivanov",
                "Ivan",
                "Ivanovich",
                employmentStatus,
                homeOrgUnitCode,
                basePositionCode,
                temporaryPositionCode,
                temporaryOrgUnitCode,
                temporaryValidFrom,
                temporaryValidTo,
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

    private OrganizationalUnit unit(Long id, String externalId, String path) {
        return new OrganizationalUnit(
            id,
            null,
            1L,
            externalId,
            OrganizationalUnitStatus.ACTIVE,
            path,
            0,
            externalId,
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T00:00:00Z")
        );
    }
}
