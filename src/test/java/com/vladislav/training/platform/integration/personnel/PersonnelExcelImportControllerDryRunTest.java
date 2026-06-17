package com.vladislav.training.platform.integration.personnel;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.common.web.GlobalExceptionHandler;
import com.vladislav.training.platform.integration.personnel.controller.PersonnelExcelImportController;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlan;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlanOutcomeCode;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlannedMutation;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlannedMutationType;
import com.vladislav.training.platform.integration.personnel.service.PersonnelApplyService;
import com.vladislav.training.platform.integration.personnel.service.PersonnelImportAdmissionService;
import com.vladislav.training.platform.integration.personnel.service.PersonnelWorkbookDryRunFacade;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
/**
 * Проверяет поведение {@code PersonnelExcelImportControllerDryRun}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class PersonnelExcelImportControllerDryRunTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-11T19:00:00Z");

    @Mock
    private PersonnelImportAdmissionService personnelImportAdmissionService;
    @Mock
    private PersonnelWorkbookDryRunFacade personnelWorkbookDryRunFacade;
    @Mock
    private PersonnelApplyService personnelApplyService;
    @Mock
    private CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;
    @Mock
    private CapabilityAdmissionRequest capabilityAdmissionRequest;
    @Mock
    private UtcClock utcClock;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        lenient().when(utcClock.now()).thenReturn(FIXED_INSTANT);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new PersonnelExcelImportController(
                    personnelImportAdmissionService,
                    personnelWorkbookDryRunFacade,
                    personnelApplyService,
                    capabilityAdmissionRequestFactory
                )
            )
            .setControllerAdvice(new GlobalExceptionHandler(utcClock))
            .build();
    }

    @Test
    void multipartDryRunEndpointExistsAndValidRequestDelegatesToAdmissionAndDryRunService() throws Exception {
        when(capabilityAdmissionRequestFactory.createPersonnelExcelDryRun()).thenReturn(capabilityAdmissionRequest);
        when(personnelWorkbookDryRunFacade.dryRun(any())).thenReturn(List.of(
            new PersonnelPlan(
                2,
                "1001",
                PersonnelPlanOutcomeCode.PLANNED_CHANGES,
                "ACTIVE",
                List.of(),
                List.of(
                    new PersonnelPlannedMutation(
                        PersonnelPlannedMutationType.OPEN_PRIMARY_ORG_ASSIGNMENT,
                        "BR1",
                        "Open new primary home organization assignment"
                    )
                )
            )
        ));

        mockMvc.perform(multipart("/api/v1/admin/import/personnel-excel/dry-run")
                .file(new MockMultipartFile(
                    "file",
                    "personnel.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "xlsx-content".getBytes()
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rows[0].rowNumber").value(2))
            .andExpect(jsonPath("$.rows[0].employeeNumber").value("1001"))
            .andExpect(jsonPath("$.rows[0].outcomeCode").value("PLANNED_CHANGES"))
            .andExpect(jsonPath("$.rows[0].decision").value("UPDATE_EXISTING"))
            .andExpect(jsonPath("$.rows[0].targetUserStatus").value("ACTIVE"))
            .andExpect(jsonPath("$.rows[0].plannedMutations[0].mutationType").value("OPEN_PRIMARY_ORG_ASSIGNMENT"))
            .andExpect(jsonPath("$.rows[0].plannedMutations[0].targetRef").value("BR1"));

        verify(capabilityAdmissionRequestFactory).createPersonnelExcelDryRun();
        verify(personnelImportAdmissionService).checkDryRunAdmission(capabilityAdmissionRequest);
        verify(personnelWorkbookDryRunFacade).dryRun(eq("xlsx-content".getBytes()));
    }

    @Test
    void createUserDryRunResponseExposesExplicitCreateDecision() throws Exception {
        when(capabilityAdmissionRequestFactory.createPersonnelExcelDryRun()).thenReturn(capabilityAdmissionRequest);
        when(personnelWorkbookDryRunFacade.dryRun(any())).thenReturn(List.of(
            new PersonnelPlan(
                2,
                "7001",
                PersonnelPlanOutcomeCode.CREATE_USER_REQUIRED,
                "ACTIVE",
                List.of(),
                List.of(
                    new PersonnelPlannedMutation(
                        PersonnelPlannedMutationType.CREATE_USER,
                        "7001",
                        "Create canonical app_user through owner service"
                    )
                )
            )
        ));

        mockMvc.perform(multipart("/api/v1/admin/import/personnel-excel/dry-run")
                .file(new MockMultipartFile(
                    "file",
                    "personnel.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "xlsx-content".getBytes()
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rows[0].outcomeCode").value("CREATE_USER_REQUIRED"))
            .andExpect(jsonPath("$.rows[0].decision").value("CREATE_USER"))
            .andExpect(jsonPath("$.rows[0].plannedMutations[0].mutationType").value("CREATE_USER"));
    }

    @Test
    void missingFileReturnsClientError() throws Exception {
        mockMvc.perform(multipart("/api/v1/admin/import/personnel-excel/dry-run"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void nonMultipartRequestReturnsClientError() throws Exception {
        mockMvc.perform(post("/api/v1/admin/import/personnel-excel/dry-run")
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void deniedActorReturnsForbiddenAndDoesNotCallDryRunService() throws Exception {
        when(capabilityAdmissionRequestFactory.createPersonnelExcelDryRun()).thenReturn(capabilityAdmissionRequest);
        org.mockito.Mockito.doThrow(new PolicyViolationException("PERSONNEL_DRY_RUN_DENIED", "dry-run denied"))
            .when(personnelImportAdmissionService).checkDryRunAdmission(capabilityAdmissionRequest);

        mockMvc.perform(multipart("/api/v1/admin/import/personnel-excel/dry-run")
                .file(new MockMultipartFile(
                    "file",
                    "personnel.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "xlsx-content".getBytes()
                )))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("dry-run denied"));

        verify(personnelWorkbookDryRunFacade, never()).dryRun(any());
    }

    @Test
    void importJobLaunchPermissionAloneDoesNotPassDryRunAdmission() throws Exception {
        when(capabilityAdmissionRequestFactory.createPersonnelExcelDryRun()).thenReturn(capabilityAdmissionRequest);
        org.mockito.Mockito.doThrow(new PolicyViolationException("ACTOR_NOT_AUTHORIZED", "import launch is not enough"))
            .when(personnelImportAdmissionService).checkDryRunAdmission(capabilityAdmissionRequest);

        mockMvc.perform(multipart("/api/v1/admin/import/personnel-excel/dry-run")
                .file(new MockMultipartFile(
                    "file",
                    "personnel.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "xlsx-content".getBytes()
                )))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("import launch is not enough"));
    }

    @Test
    void parserOrServiceFailureIsConvertedToSafeApiErrorWithoutStacktraceLeak() throws Exception {
        when(capabilityAdmissionRequestFactory.createPersonnelExcelDryRun()).thenReturn(capabilityAdmissionRequest);
        when(personnelWorkbookDryRunFacade.dryRun(any()))
            .thenThrow(new IllegalArgumentException("Workbook must contain exactly one visible business sheet"));

        mockMvc.perform(multipart("/api/v1/admin/import/personnel-excel/dry-run")
                .file(new MockMultipartFile(
                    "file",
                    "personnel.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "xlsx-content".getBytes()
                )))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Workbook must contain exactly one visible business sheet"))
            .andExpect(jsonPath("$.error").value("Bad Request"));
    }
}
