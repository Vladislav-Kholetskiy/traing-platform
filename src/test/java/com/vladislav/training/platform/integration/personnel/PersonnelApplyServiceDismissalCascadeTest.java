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
import com.vladislav.training.platform.integration.personnel.model.PersonnelCurrentState;
import com.vladislav.training.platform.integration.personnel.model.PersonnelIdentityResolution;
import com.vladislav.training.platform.integration.personnel.service.PersonnelApplyService;
import com.vladislav.training.platform.integration.personnel.service.PersonnelCurrentStateReader;
import com.vladislav.training.platform.integration.personnel.service.PersonnelImportAdmissionService;
import com.vladislav.training.platform.integration.personnel.service.PersonnelOwnerMutationExecutor;
import com.vladislav.training.platform.userorg.domain.AppUser;
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
 * Проверяет поведение {@code PersonnelApplyServiceDismissalCascade}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class PersonnelApplyServiceDismissalCascadeTest {

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
    void dismissalPlanUsesOwnerDeactivationCascadeAndEndsWithInactiveTarget() throws Exception {
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
            new PersonnelCurrentState(
                101L,
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
            )
        ));

        PersonnelApplyResult result = personnelApplyService.apply(workbookBytes());

        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().getFirst().outcomeCode()).isEqualTo("SUCCESS");
        assertThat(result.rows().getFirst().targetUserStatus()).isEqualTo("INACTIVE");
        verify(userAdministrationCommandService).deactivateUser(101L);
        verify(userAdministrationCommandService, never()).updateUser(eq(101L), any(), any(), any());
        verifyNoInteractions(accessAdministrationCommandService);
    }

    private byte[] workbookBytes() throws Exception {
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
                "DISMISSED",
                "HQ",
                "HEAD",
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

    private AppUser appUser(Long id) {
        return new AppUser(
            id,
            "1001",
            "ext-1",
            "Ivanov",
            "Ivan",
            "Ivanovich",
            UserStatus.ACTIVE,
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T00:00:00Z")
        );
    }
}
